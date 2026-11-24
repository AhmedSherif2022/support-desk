# Service health panel

* **Author:** ywsosman (Youssef Waleed)
* **Date:** 2026-11-24
* **Status:** Decisions locked by the author on 2026-11-24. Building on branch `feature/service-health`.

## Overview

Agents need to know, before they pick up a call, whether a bank service is broken for everyone. The agent Home screen gets a panel with one row per service on each platform, for example `PAYMENTS` on `MOBILE`. Each row shows one status:

* **DOWN**: an OPEN incident for that service and platform still has a linked ticket the agent has not closed (status other than RESOLVED or CLOSED). It clears when the agent closes those tickets. The incident row itself is left OPEN, because closing incidents is E5 and is not part of this change.
* **DEGRADED**: not DOWN, and the number of distinct customers with an error for that service and platform in the window is at least the threshold.
* **UNKNOWN**: not DOWN or DEGRADED, and the monitor job has not run recently. A stopped job must not look healthy.
* **OPERATIONAL**: none of the above.

An open auto-created ticket that is CRITICAL or past its SLA does not change the status. It is returned on the row as a notice so the agent can see it. The rows themselves come from a fixed list (AUTH, PAYMENTS, CARDS, TRANSFERS, each on MOBILE and WEB), so a healthy service still appears.

A Guide Admin sets the window and the threshold. They are stored in the existing `app_settings` table.

"Service health" here means the bank's customer-facing services. It does not mean this backend's own health (`/actuator/health`).

## Analysis

Six Goldfish reviewers read the concept against the code on `main` (design, codebase details, zoom-out, comprehension, implementation readiness, structure). They agreed on the points below.

### What already exists and can be reused

* `detected_error_event` has `platform` (MOBILE or WEB), `service`, `error_code`, `occurred_at`, `customer_id` and `ticket_id` (`V17__monitor_job.sql`).
* `incident` has `error_code`, `platform`, `service` and `status` (OPEN, SRE_FIXED, RESOLVED) (`V18__incident_grouping.sql`).
* `tickets` has `priority`, `sla_state` and `origin` (`V6__create_ticket_tables.sql`).
* `IncidentSettingsService` already shows the whole settings pattern. It reads `app_settings` with a default when a value is missing or bad, validates input, and audits each change as `SETTING_CHANGED`. Guide Admins reach it under `/api/admin/**`.
* `HOME` is already an agent screen in `Permissions.SCREENS`.

### Problems with the concept as written

1. **DOWN would never clear (raised by 3 reviewers).** Nothing in the code moves an incident out of OPEN. `Incident.markSreFixed()` and `Incident.resolve()` have no callers, because the incident lifecycle was postponed to E5. With the current rule, a service that has had one incident shows DOWN forever.

2. **One customer can mark a whole service DOWN (raised by all 6).**
   * An error rule can be CRITICAL by itself. The seed makes `TRX_DEBIT_NO_CREDIT` CRITICAL, so one customer's debit-not-credited error turns TRANSFERS on MOBILE DOWN.
   * An SLA breach measures how fast the support desk answered, not whether the bank's service works. One ticket left waiting on a customer can keep a working service DOWN for days.
   * Incident grouping already raises every linked ticket to CRITICAL. Those tickets stay CRITICAL after the outage ends, so the CRITICAL rule keeps the service DOWN until every one of them is closed.

3. **Tickets have no service (raised by all 6).** `tickets` has `platform` and `error_code` but no `service`. The only link is `detected_error_event.ticket_id`. Duplicate detection matches customer and error code only, so one ticket can hold errors from MOBILE and WEB, or from two services. An incident's `service` is copied from its first error only.

4. **The list of rows is not defined (raised by all 6).** `service` is free text (`VARCHAR(40)`) and is not upper-cased, so `Payments` and `PAYMENTS` would be two rows. A service that has never failed never appears, so the panel cannot say "Payments is fine". A retired service stays forever. Error events use `AUTH`, `PAYMENTS`, `CARDS`, `TRANSFERS`, while ticket categories use `AUTHENTICATION`, `OTP`, `TRANSFERS`, `PAYMENTS`, `MOBILE_APP`. Incidents and tickets allow `BRANCH`, but error events do not.

5. **"Error count" is not defined (raised by all 6).** Raw error rows let one customer retrying ten times make a service DEGRADED. Incident grouping counts distinct customers, so admins will expect the same here. It is also open whether unprocessed, `UNKNOWN_CODE`, `FAILED` and `DUPLICATE` rows count, and whether "reaches" means `>=`.

6. **Stale data looks healthy (raised by 4).** Every signal depends on the monitor job. If it stops or is switched off, counts stop growing and every row turns green with no warning.

7. **A bare status is not useful (raised by 4).** The agent needs to see why a row is DOWN or DEGRADED: the incident, the error count against the threshold, and since when.

8. **Query cost (raised by 4).** Every agent's Home screen will call this. No index fits a count by service, platform and time. The existing one starts with `error_code`, and `detected_error_event` has no retention policy.

9. **Two lookalike settings (raised by 4).** `incident.grouping.window-minutes` and `incident.grouping.min-customers` already exist. A second window and threshold need clearly different names. If the DEGRADED threshold is higher than the incident threshold, DEGRADED can never show, because an incident makes the row DOWN first.

### Counter-arguments

* `GET /api/incidents` already lists open incidents with platform, service and affected customers. If DOWN only means "open incident", the panel adds one new signal: DEGRADED.
* With only error data and no count of successful requests, the app cannot prove a service works. OPERATIONAL really means "no reported problems".

## Goals vs. Non-Goals

* **In-Scope Goals:**
  * A read-only agent endpoint that returns one row per service and platform, each with a status and a reason.
  * Two Guide Admin settings for DEGRADED (window and threshold), with defaults in a new migration and changes audited as `SETTING_CHANGED`.
  * Local seed data that shows each status.
* **Out-of-Scope Non-Goals:**
  * This backend's own health (`/actuator/health`).
  * The front-end panel (there is no UI code in this repository).
  * Status history, notifications and manual overrides such as planned maintenance.
  * Per-service thresholds (one global threshold for now).
  * The incident lifecycle (E5).

## Alternatives Weighed

* **Precomputed snapshot table written by the monitor job.** Suggested by the design reviewer for constant read cost and history. Deferred: the zoom-out reviewer found no load that needs it yet, and it adds a table and a writer.
* **Service catalog table owned by the Guide Admin.** Fixes free-text names and missing healthy services. Not decided yet; see open decision 3.
* **Putting health into the home queue response.** Rejected: it would tie ticket queueing to monitoring. A separate endpoint is simpler.
* **Extending `/api/admin/incident-settings`.** Rejected: its PUT needs both fields today, so new required fields would break existing clients. Use a separate settings endpoint.

## Key Decisions & Staging for Next Steps

Decided by the author on 2026-11-24:

1. A CRITICAL or SLA-breached ticket is not DOWN. It is a notice on the row.
2. DOWN ends when the agent closes the linked ticket (RESOLVED or CLOSED). This change does not add an incident-close action.
3. The service list is fixed: AUTH, PAYMENTS, CARDS, TRANSFERS on MOBILE and WEB. These are the names the error events already use.
4. DEGRADED counts distinct customers, including every error outcome, by `occurred_at`. "At least the threshold" means `>=`.
5. If the monitor job's last activity is older than 5 minutes, the response says the data is stale, and a row that would have been OPERATIONAL is UNKNOWN.

Still out of scope: the front end, status history, per-service thresholds, and the E5 incident lifecycle.
