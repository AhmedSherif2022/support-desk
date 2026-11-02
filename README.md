# Digital Support Portal — Backend

Spring Boot backend for the bank's internal customer-service portal. Agents find and verify a caller, see what went wrong
for them (Customer 360), and work tickets against an SLA. Guide Admins and Reviewers maintain the troubleshooting guides.
Every important action is written to an append-only audit trail.

Requirements come from **Digital Support Portal — Epics, Stories and Acceptance Criteria** (10 epics, 52 stories).
Story IDs below (`E2-S4`, …) match that document.

> **Scope of this repo:** backend only. Acceptance criteria about screens, keyboard shortcuts, dialogs or layout belong
> to the frontend and are marked **Frontend** below; the backend provides the data they need.

---

## Status at a glance

| Epic | Backend status | Stories |
|---|---|---|
| **E1** Access, roles and audit | **Mostly done** | 4 / 4 started · open: DB grants, Admin landing page, Team Leader alert |
| **E2** Caller verification and Customer 360 | **Mostly done** | 6 / 6 started · open: incident/guide links, ADO numbers (need E4–E6) |
| **E3** Tickets, queue and SLA | **Mostly done** | 7 / 7 started · open: incident link, Work tab, triage filter (need E4–E6, E8) |
| **E4** Guided troubleshooting | Not started | 0 / 5 |
| **E5** Azure DevOps integration | Not started | 0 / 7 (escalation statuses exist in the state machine) |
| **E6** Automatic tickets and incidents | Not started | 0 / 6 |
| **E7** Guide authoring and review | Not started | 0 / 7 (`/api/admin/**`, `/api/review/**` are already role-protected) |
| **E8** Triage, analytics and feedback | Not started | 0 / 4 |
| **E9** Notifications | Not started | 0 / 2 (SLA warning/breach events already published) |
| **E10** Platform and non-functional | **Partly done** | Global error handler and OpenAPI done · Dockerfile, health, correlation IDs open |

Automated tests: **153 passing** · API: **25 operations** in Swagger · Postman: **51 requests** passing live.

---

## Quick start

**You need:** JDK 21 or newer (JDK 26 works), Docker Desktop.

```bash
docker compose up -d     # PostgreSQL 16 on localhost:5434
./mvnw spring-boot:run   # the app on http://localhost:8080 (profile "local")
```

Flyway creates the schema and, on the `local` profile only, loads demo data from `src/main/resources/db/seed/`.

| What | Where |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html — log in, press **Authorize**, paste the `accessToken` |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Postman | import `postman/SupportDesk.postman_collection.json`, run it top to bottom with the Collection Runner |
| Status report (PDF) | `docs/E1-E3-alignment-report.pdf` — every E1–E3 criterion with before / now / where |

**Seeded staff** (local only, password `Password123!`): `mona` AGENT · `youssef` GUIDE_ADMIN · `nour` REVIEWER

**Seeded customers:** Sara `CIF-100245` (3 failed logins with `ERR_AUTH_503` in the last hours) · Mai `CIF-100311`
(not enrolled) · Ahmed `CIF-100101` (no failures). The activity seed re-runs on every start, so "the last 6 hours"
always has data.

### Running the tests

```bash
./mvnw test
```

Unit tests need nothing. Integration tests (`*IntegrationTest`, `SupportDeskApplicationTests`) use the local
PostgreSQL from `docker compose`, so start it first. They leave small test users and tickets behind on purpose:
audit rows can never be deleted.

### Configuration

| Setting | Default | Notes |
|---|---|---|
| `JWT_SECRET` | dev-only value in `application-local.yml` | **Required** outside the local profile (base64, 256-bit or more) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Frontend origins |
| `supportdesk.auth.session-timeout` | `15m` | Access token (JWT) lifetime |
| `supportdesk.auth.inactivity-timeout` | `30m` | Refresh token lifetime, pushed out on every refresh |
| `supportdesk.auth.max-failed-attempts` / `failure-window` / `lock-duration` | `5` / `15m` / `15m` | Lockout rule |
| `supportdesk.tickets.sla.targets` | 2h / 8h / 24h / 72h | Critical / High / Medium / Low |
| `supportdesk.tickets.sla.warning-at-percent` | `75` | Amber SLA meter and `SLA_WARNING` |

Before deploying behind a proxy, set `server.forward-headers-strategy: native` so audit IPs come from trusted proxies
only, and turn Swagger off in production with `springdoc.api-docs.enabled: false`.

---

## Project structure

Every feature uses the same layers as `common`. A feature only has the layers it needs.

```
src/main/java/com/supportdesk/
├── common/    config · dto · entity · enums · exception          shared building blocks
├── auth/      config · controller · dto · entity · enums · exception · repository · security · service
├── audit/     controller · dto · entity · enums · event · listener · repository · service
├── customer/  controller · dto · entity · enums · exception · repository · service
└── ticket/    config · controller · dto · entity · enums · event · exception · listener · repository · service
```

Tests mirror the package of what they test; end-to-end HTTP tests sit in the feature's `controller` test package.

**Conventions**
- Controllers implement an `…Api` interface that holds the mappings and all Swagger annotations.
- Every error body is `{code, message, fieldErrors, path, timestamp}`; codes come from `common/enums/ErrorCode`.
- Business audits go through `AuditService`, which joins the caller's transaction (`MANDATORY`).
- Migrations are numbered `V1…V9`; agree the next number with the team before adding one. Local-only demo data lives in `db/seed`.
- One branch per story or step; never commit straight to `main`.

---

## What is done

### E1 Access, roles and audit

| Story | Status | Notes |
|---|---|---|
| **E1-S1** Sign in with role | Mostly | JWT login with access + refresh token. 5 failures **within 15 minutes** lock for 15 minutes. `USER_LOGIN` (with role and IP) and `USER_LOCKED` are audited. 30 minutes without activity ends the session (sliding refresh token). *Open:* Admin should land on Review queue, but the permission matrix gives Admin no review access (team decision). *Frontend:* keep unsaved form text. |
| **E1-S2** Backend enforces permissions | Done | One permission matrix drives `SecurityConfig`: no token → 401, wrong role → 403 + `ACCESS_DENIED` (with role and the real client IP). `/api/auth/me` returns `allowedScreens`. `PermissionMatrixTest` covers every role × area. *Frontend:* "Not available for your role". |
| **E1-S3** Append-only audit log | Mostly | Each record has user, role, action, entity, entity ID, time, before and after, written **in the business transaction**. Rows are `@Immutable`, and a database trigger blocks UPDATE, DELETE and TRUNCATE. `GET /api/audit/logs` (Admin, Reviewer) filters by user, action, entity and date, with paging. Index on (entity, entity_id, created_at). *Open:* an INSERT/SELECT-only database user. |
| **E1-S4** Data-view auditing | Mostly | `CUSTOMER_DATA_VIEWED` on Customer 360. `CUSTOMER_SEARCHED` stores what matched, never the search text. More than 30 searches in 10 minutes → 429. *Open:* notify the Team Leader (needs E9 and the role). |

### E2 Caller verification and Customer 360

| Story | Status | Notes |
|---|---|---|
| **E2-S1** Search customers | Mostly | `GET /api/customers/search?q=` for CIF, national ID, mobile (`01…`, `20…`, `+20…`) or ticket number. Results: name, segment, masked mobile, enrollment status, open-ticket count. "No match. Check the digits and the ID type." *Open:* ADO number (E5). *Frontend:* "/" focuses search. |
| **E2-S2** Verification gate | Done | Nothing about a customer is returned before verification (403 `CALLER_NOT_VERIFIED`). Two of the checks must pass ("Two checks must pass."). `CALLER_VERIFIED` and `CALLER_VERIFICATION_FAILED` are audited. End verification locks the customer again. |
| **E2-S3** Masked fields with audited reveal | Done | National ID shows the last 4 digits, mobile the first 3 and last 4 (server-side). `POST /{cif}/reveal` returns one field and audits `CUSTOMER_FIELD_REVEALED` with the field name. *Frontend:* re-mask on leave. |
| **E2-S4** Diagnosis from logs | Partly | `GET /{cif}/diagnosis`: most frequent failure of the last 6 hours (count, event type, time range, error code, service), services with no failures, and a device/network hint when nothing failed. *Open:* name a matching incident (E6) and offer "Run guide" (E4). |
| **E2-S5** Activity timeline | Done | `GET /{cif}/activity`: default last 6 hours + failures only; Today, 7 days, Custom; platform and service chips; counts per chip; empty-state hint. Backed by `customer_activity_log`. |
| **E2-S6** Customer tickets, incidents and RCA | Partly | `GET /{cif}/tickets` with origin and status; "Repeat caller" when more than one ticket in 20 days. *Open:* Azure work item number and the Incidents & RCA tab (E5, E6). |

### E3 Tickets, queue and SLA

| Story | Status | Notes |
|---|---|---|
| **E3-S1** Create a ticket | Mostly | Verified customer only; platform, category, priority and description required; `TRX-` reference for Transfers and Payments; the customer's latest failed error code on that service is attached; duplicate warning (precheck) and 409. *Open:* link to an open incident, origin "From incident" (E6). |
| **E3-S2** Ticket lifecycle | Done | Fixed state machine incl. Reopen. `TICKET_STATUS_CHANGED` with before/after; `TICKET_TRANSITION_REJECTED` is kept even though the change rolls back. |
| **E3-S3** SLA calculation | Done | Targets 2h / 8h / 24h / 72h. One value (`slaLabel`: time left, Paused, Met, Breached Xh Ym) and `slaPercentUsed` for the meter. Warning at 75%; `SLA_WARNING` and `SLA_BREACHED` fire once (scheduler every minute). Pauses on Waiting on customer; keeps running through "SRE fixed it". |
| **E3-S4** Triaged queue on Home | Mostly | Risk order: confirm-fix first, paused last, otherwise most SLA used. Each row has ID, issue, customer, segment, platform, error code, SLA, origin and one named action. Chips All, At risk, Confirm fix, Auto-created with counts. *Open:* hide tickets waiting for triage (E8). |
| **E3-S5** Ticket workspace with pinned next step | Partly | One next step per status, shared by queue and ticket. Timeline, Customer contact and Audit tabs, newest first, with counts. *Open:* Work tab content — guide runs, SRE panel, same-incident tickets (E4–E6). |
| **E3-S6** Log customer communication | Mostly | `POST /api/tickets/{n}/contacts` → Customer contact tab + `CUSTOMER_CONTACTED`. *Open:* log the guide's customer script automatically (E4). |
| **E3-S7** Ticket list | Mostly | `GET /api/tickets`: the agent's own tickets, All / Open / Resolved & closed, search by ticket number or CIF, 8 per page. *Open:* search by ADO number (E5). |

### E10 Platform (partly)

- Global exception handler with one consistent error body, no stack traces (E10-S2). ✔
- OpenAPI documentation with Swagger UI and JWT Authorize. ✔
- Secrets come from environment variables; only a dev-only JWT default exists in `application-local.yml`. ✔
- `docker compose` starts PostgreSQL only (backend and frontend not containerised yet).

---

## What remains

### Open items in E1–E3

| Item | Story | Blocked by |
|---|---|---|
| Admin lands on Review queue | E1-S1 | Team decision: the matrix gives Admin no `/api/review` access |
| App DB user with INSERT + SELECT only on `audit_logs` | E1-S3 | Team / ops decision (changes the Docker and Flyway setup) |
| Notify Team Leader when searches are rate-limited | E1-S4 | E9 notifications and a Team Leader role |
| Role and permission tables (roles are an enum today) | E1 tech | Team decision |
| Search, list and Customer 360 by Azure work item number | E2-S1, E2-S6, E3-S7 | E5 |
| Incident in diagnosis; "Run guide" | E2-S4 | E6, E4 |
| Incidents & RCA tab | E2-S6 | E5, E6 |
| Link a new ticket to an open incident | E3-S1 | E6 |
| Keep triage tickets out of the queue | E3-S4 | E8 |
| Work tab: guide runs, SRE panel, same-incident tickets | E3-S5 | E4, E5, E6 |
| Auto-log the guide's customer script | E3-S6 | E4 |
| Dedicated test for the SLA_WARNING / SLA_BREACHED audit rows | E3-S3 | — |

### Not started

**E4 Guided troubleshooting** (5 stories)
- S1 recommend guides by platform, category and error code; no match → next step Escalate.
- S2 runner: one question at a time, Previous, log hints; `GUIDE_STARTED` with the version.
- S3 outcome (Resolved / Escalate / Follow-up) with the customer script; session saved; `GUIDE_COMPLETED`.
- S4 version pinning for runs in progress.
- S5 flag a step to the Guide Admin feedback queue.
- *Tech:* guide matching query, `TroubleshootingSession` / `TroubleshootingAnswer`, runner API, outcome handler, `GuideFeedback`.

**E5 Azure DevOps integration** (7 stories)
- S1 escalate with reason and urgency; outbox record in the same transaction; work item never contains CIF, national ID or mobile.
- S2 join an open incident's work item instead of duplicating.
- S3 reliable delivery: exponential backoff, idempotency key, Failed after 10 attempts with an alert.
- S4 receive service hooks: signature check, revision ordering, state mapping, `AZURE_WEBHOOK_RECEIVED`.
- S5 read-only SRE panel. S6 Ask SRE for update (once an hour). S7 Confirm the fix with the customer.
- *Tech:* outbox + worker, REST client with credentials from Key Vault, webhook endpoint, 15-minute reconciliation job, masked customer token, contract tests.

**E6 Automatic tickets and incidents** (6 stories)
- S1 monitor job every minute over `detected_error_event` (`SKIP LOCKED`, processed once, `MONITOR_JOB_RUN`).
- S2 duplicate protection (same customer + error within 24 hours joins the open ticket).
- S3 group 5+ customers with the same error and platform within 15 minutes into one incident; thresholds editable (`SETTING_CHANGED`).
- S4 rule-based auto-escalation. S5 incident bar and customer script. S6 call through an incident's customers.
- *Tech:* `error_rule`, `monitor_job_run`, `Incident` / `IncidentTicket`, settings table, seed with the 9 test cases.

**E7 Guide authoring and review** (7 stories)
- S1 library · S2 edit a decision tree as a new draft version · S3 validation (missing start, dead routes, loops, unreachable steps).
- S4 preview · S5 review with a real diff · S6 four-eyes approval (`GUIDE_SELF_APPROVAL_BLOCKED`, `GUIDE_PUBLISHED`, scheduled publish).
- S7 live update to agents and rollback through review.
- *Tech:* `Guide`, `GuideVersion` (tree JSON), `GuideMatchRule`, shared validator, diff service, publish scheduler, WebSocket/SSE.

**E8 Triage, analytics and feedback** (4 stories)
- S1 triage bucket for unknown error codes; map to a rule (`ERROR_RULE_CREATED`) reclassifies open triage tickets.
- S2 guide analytics with a "Fix next" verdict · S3 agent feedback queue · S4 error rules and job view, Run job now.

**E9 Notifications** (2 stories)
- S1 in-app notifications for agents and Team Leaders, unread count and latest 4.
- S2 real-time delivery within 5 seconds, reconnect and catch up.
- *Tech:* `Notification` table and service, WebSocket/SSE channel shared with guide publishing, event-to-recipient mapping.

**E10 Platform and non-functional** (remaining)
- S1 Dockerfiles and one `docker compose up` for backend, frontend and database with seed data and the 9 monitor test cases.
- S2 structured logs with a correlation ID (also sent to Azure); `/health` for database, Azure reachability and job status (Spring Boot Actuator is not added yet).
- S3 accessibility and S4 Arabic/RTL (frontend; guide text in Arabic and English per step needs E7).
- *Tech:* accessibility checks in CI, i18n setup.

### Frontend (whole portal)

Sign-in, Home queue, Customer 360 with tabs and chips, verification dialog, ticket list and workspace, guide runner and
editor, review/diff, triage, analytics and notifications screens — with loading, empty and error states, keyboard and
screen-reader support, and Arabic/RTL.

---

## Open team decisions

1. Where the Admin lands after sign-in (Review queue vs. current matrix).
2. An INSERT/SELECT-only database user for the audit table.
3. Role and permission tables instead of the enum.
4. Team Leader: a real role, or only a notification recipient.
5. Migration numbering across parallel branches (`V7`–`V9` are taken).
