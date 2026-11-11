# Digital Support Portal

Epics, stories and acceptance criteria for Phase 1. Each epic ends with its tech tasks.

Oct 6, 2026 · @Walid

## How to read this

Phase 1 has 10 epics and 52 stories for the Customer Service Agent and the Guide Admin. Each story has acceptance criteria (ACs) in Given / When / Then form and an ID you can copy into the backlog. Tech tasks are listed at the end of each epic.

SRE has no portal stories: SRE works in Azure DevOps, and the portal reacts to it through E5. Team Leader appears only as a notification recipient. The v3 prototype demonstrates the stories in E2–E9; sign-in (E1-S1) and RTL (E10-S4) are not in it yet.

### Roles

| Role                   | Short name | Works in                            |
|------------------------|------------|-------------------------------------|
| Customer Service Agent | Agent      | Portal — calls, tickets, guides     |
| Guide Admin (author)   | Admin      | Portal — guides, rules, triage      |
| Guide Admin (reviewer) | Reviewer   | Portal — approves guide versions    |
| SRE                    | SRE        | Azure DevOps only                   |
| System                 | System     | Monitor job, outbox worker, webhook |

### Epic overview

| Epic                                    | Goal                                                                 | Stories | Priority |
|-----------------------------------------|----------------------------------------------------------------------|---------|----------|
| E1 Access, roles and audit              | Right people see the right data; every action is traceable           | 4       | P1       |
| E2 Caller verification and Customer 360 | Agent knows what happened before the caller finishes explaining      | 6       | P1       |
| E3 Tickets, queue and SLA               | Agent always knows the next ticket and the next step                 | 7       | P1       |
| E4 Guided troubleshooting               | Solve on the first call with versioned decision trees                | 5       | P1       |
| E5 Azure DevOps integration             | Escalation reaches SRE reliably; SRE state flows back                | 7       | P1       |
| E6 Automatic tickets and incidents      | Errors become tickets before the customer calls; outages are grouped | 6       | P1       |
| E7 Guide authoring and review           | Admins change guides safely and live, with four-eyes review          | 7       | P1       |
| E8 Triage, analytics and feedback       | Gaps in knowledge get found and fixed                                | 4       | P2       |
| E9 Notifications                        | The right person hears about changes                                 | 2       | P1       |
| E10 Platform and non-functional         | Runnable, observable, accessible, secure                             | 4       | P1       |

## Contents

- [E1 Access, roles and audit](#e1-access-roles-and-audit)
- [E2 Caller verification and Customer 360](#e2-caller-verification-and-customer-360)
- [E3 Tickets, queue and SLA](#e3-tickets-queue-and-sla)
- [E4 Guided troubleshooting](#e4-guided-troubleshooting)
- [E5 Azure DevOps integration](#e5-azure-devops-integration)
- [E6 Automatic tickets and incidents](#e6-automatic-tickets-and-incidents)
- [E7 Guide authoring and review](#e7-guide-authoring-and-review)
- [E8 Triage, analytics and feedback](#e8-triage-analytics-and-feedback)
- [E9 Notifications](#e9-notifications)
- [E10 Platform and non-functional](#e10-platform-and-non-functional)

## E1 Access, roles and audit

Goal: only the right role sees each screen and field, and every important action is recorded.

### E1-S1 Sign in with role

As a portal user, I want to sign in and land on my role's home, so that I see only my work.

- Given valid credentials, when I sign in, then I land on Home (Agent) or Review queue (Admin, Reviewer).
- Given 5 failed attempts in 15 minutes, when I try again, then the account is locked for 15 minutes and USER_LOCKED is audited.
- Given 30 minutes of inactivity, when I act, then I'm asked to sign in again and unsaved form text is kept.
- Given I sign in, then USER_LOGIN is audited with role and IP.

### E1-S2 Backend enforces permissions

As the bank, I want every API to check the role, so that hiding a button is never the only protection.

- Given an Agent token, when it calls any guide-admin API, then the API returns 403 and ACCESS_DENIED is audited.
- Given an Admin token, when it calls a customer or ticket API, then the API returns 403.
- Given a direct URL to a screen my role can't use, when I open it, then I see "Not available for your role".
- Given the permission matrix in the Features doc, then an automated test covers every row.

### E1-S3 Append-only audit log

As a reviewer of operations, I want an audit trail that can't be edited, so that we can prove what happened.

- Given any action in the audit event list, when it happens, then a record with user, role, action, entity, entity ID, time, before and after is written in the same transaction.
- Given the audit table, then there is no update or delete API, and the DB user for the app has INSERT and SELECT only.
- Given Admin or Reviewer, when they open Audit log, then they can filter by user, action, entity and date, with pagination.

### E1-S4 Data-view auditing

As the bank, I want every view of customer data recorded, so that insider misuse is visible.

- Given a verified caller, when the Agent opens Customer 360, then CUSTOMER_DATA_VIEWED is audited.
- Given a customer search, then CUSTOMER_SEARCHED is audited without storing the raw search text.
- Given more than 30 searches by one Agent in 10 minutes, then further searches return 429 and the Team Leader is notified.

### E1 tech tasks

- [ ] Auth service: JWT (15 min) + refresh token, lockout, session timeout.
- [ ] Role and permission tables; method-level guards on every controller.
- [ ] AuditLog entity, insert-only repository, DB grants, index on (entity, entity_id, created_at).
- [ ] Audit interceptor that writes in the business transaction.
- [ ] Rate limiter on search endpoint (per user).
- [ ] Integration tests: one per row of the permission matrix.

## E2 Caller verification and Customer 360

Goal: the Agent knows what went wrong for this customer before the caller finishes explaining.

### E2-S1 Search customers

As an Agent, I want one search box for CIF, national ID, mobile, ticket or ADO number, so that I find the caller fast.

- Given any of those values, when I search, then matching customers show with name, segment, masked mobile, enrollment status and open-ticket count.
- Given a ticket or ADO number, when I search, then the owning customer is the result.
- Given no match, then I see "No match. Check the digits and the ID type."
- Given I press "/" anywhere outside a field, then focus moves to the search box.

### E2-S2 Verification gate

As an Agent, I want to verify the caller before any detail shows, so that we never disclose data to the wrong person.

- Given an unverified customer, when I open them, then the verification dialog appears and nothing else about the customer is shown.
- Given fewer than 2 of 3 checks ticked, when I confirm, then confirmation is blocked with "Two checks must pass".
- Given 2 or more checks, when I confirm, then CALLER_VERIFIED is audited and Customer 360 opens with "Caller verified HH:MM".
- Given I choose "Caller failed", then CALLER_VERIFICATION_FAILED is audited and no data shows.
- Given I choose "End verification", then the customer is locked again and I return to Home.

### E2-S3 Masked fields with audited reveal

As an Agent, I want sensitive fields masked even after verification, so that I see full values only when needed.

- Given a verified customer, then national ID shows the last 4 digits and mobile shows the first 3 and last 4.
- Given I click Reveal on one field, then only that field is unmasked and CUSTOMER_FIELD_REVEALED is audited with the field name.
- Given I end verification or leave the customer, then revealed fields are masked again.

### E2-S4 Diagnosis from logs

As an Agent, I want a plain-language summary of what failed, so that I don't read raw logs on a live call.

- Given failures in the last 6 hours, when Customer 360 opens, then "What the logs already say" states the count, event type, time range, error code and service of the most frequent failure.
- Given the error matches an open incident, then the panel names the incident and says a new ticket will link to it.
- Given a guide matches the error code, then the panel offers "Run guide" with its live version.
- Given no failures, then the panel says so and suggests checking device or network.
- Given other services had no failures, then the panel lists them so the Agent can rule them out.

### E2-S5 Activity timeline

As an Agent, I want to filter the customer's activity by time, platform, service and result, so that I can track exactly what happened.

- Given Customer 360 opens, then the default filter is Last 6 hours + Failures only.
- Given I pick Today, 7 days or Custom range, then the list and counts update.
- Given I toggle Mobile, Web or a service chip, then only matching events show; clicking an active chip clears it.
- Given each event, then it shows time, event type, platform, service, result and error code; failures are marked red.
- Given no events match, then I see an empty state explaining how to widen the filter.

### E2-S6 Customer tickets, incidents and RCA

As an Agent, I want the customer's ticket and incident history in one place, so that the customer doesn't repeat their story.

- Given the Tickets tab, then each ticket shows an origin tag (Manual, Auto-created, From incident), its Azure work item number if any, and status.
- Given the Incidents & RCA tab, then each linked incident or escalated ticket shows SRE state and the RCA summary when available.
- Given more than one ticket in 20 days, then a "Repeat caller" note appears.

### E2 tech tasks

- [ ] Search API across customer, ticket and ADO ID with masking applied server-side.
- [ ] Verification session per agent+customer (server-side, expires with the session).
- [ ] Reveal endpoint returning one field, with audit.
- [ ] customer_activity_log read API with time, platform, service, result filters and index on (cif, created_at).
- [ ] Diagnosis service: group failures, match incidents and guides.
- [ ] Customer 360 screen, tabs, filter chips, empty states.

## E3 Tickets, queue and SLA

Goal: the Agent always knows which ticket to touch next and what the next step on it is.

### E3-S1 Create a ticket

As an Agent, I want to create a ticket for a verified customer, so that the issue is tracked against an SLA.

- Given a verified customer, when I create a ticket, then platform, category, issue, priority and description are required.
- Given category Transfers or Payments, then a transaction reference starting with TRX- is required.
- Given the customer's latest failed log matches the ticket's service, then its error code is attached automatically.
- Given the customer already has an open ticket for the same issue, then a warning offers to open that ticket instead.
- Given an open incident on the same service, then the form says the ticket will link to it, and on save it does (origin "From incident").

### E3-S2 Ticket lifecycle

As the bank, I want tickets to follow fixed states, so that reports and SLAs are reliable.

- Given the state machine (New, Troubleshooting, Waiting on customer, Escalated, With SRE, SRE fixed it — confirm, Resolved, Reopened, Closed), when any transition outside it is requested, then the API rejects it and TICKET_TRANSITION_REJECTED is audited.
- Given each allowed transition, then TICKET_STATUS_CHANGED is audited with before and after.
- Given Resolved, when the customer calls back, then the Agent can Reopen it.

### E3-S3 SLA calculation

As an Agent, I want one SLA number with a meter, so that I see risk at a glance.

- Given priority, then target is Critical 2h, High 8h, Medium 24h, Low 72h.
- Given the ticket header, then it shows one value: time left, "Paused", "Met", or "breached Xh Ym", plus a meter.
- Given 75% of target used, then the meter turns amber and SLA_WARNING fires once; at 100% it turns red and SLA_BREACHED fires once.
- Given status Waiting on customer, then the clock pauses and resumes when troubleshooting resumes.
- Given status "SRE fixed it — confirm", then the clock keeps running until the Agent resolves.

### E3-S4 Triaged queue on Home

As an Agent, I want one queue ordered by risk, so that I work top-down.

- Given open tickets, then Home lists them ordered by SLA used, with "confirm" tickets near the top and paused tickets at the bottom.
- Given each row, then it shows ID, issue, customer, segment, platform, error code, SLA state, origin and one named action.
- Given filter chips All, At risk, Confirm fix, Auto-created, then counts are shown and the list filters.
- Given tickets waiting for Guide Admin triage, then they are not in this queue.

### E3-S5 Ticket workspace with pinned next step

As an Agent, I want the ticket to tell me the next step, so that nothing stalls.

- Given the ticket status, then the pinned bar shows exactly one next step: Start guide, Escalate, Resume, Wait for SRE, Ask SRE for update, Call & confirm, or Close.
- Given the Work tab, then guide runs, the read-only SRE panel, a note box, details, same-incident tickets and recent failures are shown.
- Given Timeline, Customer contact and Audit tabs, then each shows its history newest first with counts in the tab label.

### E3-S6 Log customer communication

As an Agent, I want to log what I told the customer, so that the next person knows.

- Given text in the note box, when I add it, then it appears in Customer contact with time and CUSTOMER_CONTACTED is audited.
- Given a guide outcome with a customer script, then the script is logged automatically as "Told customer: …".

### E3-S7 Ticket list

As an Agent, I want to search and filter all my tickets, so that I can find older ones.

- Given the list, then I can filter by All, Open, Resolved & closed and search by ticket, CIF or ADO number.
- Given more than 8 results, then the list paginates with Previous and Next.

### E3 tech tasks

- [ ] Ticket, TicketStatusHistory, Comment entities; state machine service with transition table.
- [ ] SLA service (target by priority, pause intervals) and SLA monitor job every minute.
- [ ] Queue API with risk ordering and filter counts in one call.
- [ ] Next-action resolver shared by queue rows and the ticket bar.
- [ ] Duplicate check (same CIF + subcategory, open).
- [ ] Home, ticket list and ticket workspace screens with loading, empty and error states.

## E4 Guided troubleshooting

Goal: Agents solve issues on the first call by following the published decision tree.

### E4-S1 Recommend guides

As an Agent, I want the right guide suggested, so that I don't search for it.

- Given a ticket, then guides whose platform matches and whose category or error code matches are recommended, showing title and live version.
- Given a guide matches by error code, then that match is preferred and named.
- Given no guide matches, then the next step becomes Escalate.

### E4-S2 Run a guide step by step

As an Agent, I want one question at a time, so that I can talk to the customer while I follow it.

- Given I start a guide, then the ticket moves to Troubleshooting and GUIDE_STARTED is audited with the version.
- Given a step, then I see the question, my answers so far, progress, and the options; Next is blocked until I choose one.
- Given an answer, then the next step or outcome follows the published route.
- Given Previous, then I return to the last step with my answer cleared.
- Given the step has a log hint and the customer's error code matches, then a hint suggests the likely answer.

### E4-S3 Outcome and customer script

As an Agent, I want to know what to say at the end, so that every customer hears a consistent message.

- Given the guide reaches an outcome, then I see its type (Resolved, Escalate, Follow-up), its result text and the "Say to the customer" script.
- Given Resolved, then I can resolve the ticket; Escalate opens the escalation dialog; Follow-up sets Waiting on customer and pauses SLA.
- Given any outcome, then the session (guide, version, path, duration, outcome) is saved to the ticket and GUIDE_COMPLETED is audited.

### E4-S4 Version pinning

As an Agent, I want my run to stay on the version I started, so that a publish mid-call doesn't change the questions.

- Given a run started on v3, when v4 is published, then my run continues on v3 to the end.
- Given a saved session, then it always displays with the version it used.

### E4-S5 Flag a step

As an Agent, I want to report a wrong or unclear step, so that the guide gets fixed.

- Given a finished run, when I flag a step with a comment, then it reaches the Guide Admin feedback queue with guide, step, agent and time.
- Given an empty comment, then sending is blocked.

### E4 tech tasks

- [ ] Guide matching query (platform, category, error code) on published versions.
- [ ] TroubleshootingSession and TroubleshootingAnswer tables; store version number.
- [ ] Runner API: start, answer, back, finish — server validates each route against the version's tree.
- [ ] Outcome handler: resolve / escalate / follow-up with ticket transitions.
- [ ] GuideFeedback table and endpoint.
- [ ] Runner screen (one question, progress, answered-path list, hint, script).

## E5 Azure DevOps integration

Goal: escalations reach SRE in Azure DevOps reliably, and SRE's changes flow back to the Agent. Azure is the source of truth for engineering work; the portal is the source of truth for the customer.

### E5-S1 Escalate to SRE

As an Agent, I want to escalate with a reason and urgency, so that SRE gets everything they need.

- Given an open ticket, when I escalate, then reason and urgency are required and the ticket moves to Escalated.
- Given escalation, then an outbox record is written in the same transaction as the ticket change.
- Given the outbox worker sends it, then the work item contains ticket ID and URL, masked customer token, platform, service, category, error code, summary, log snapshot, correlation ID, priority, SLA, guide path and reason — never CIF, national ID or mobile.
- Given success, then the ticket shows ADO number, state and "synced".

### E5-S2 Link to an open incident instead of duplicating

As SRE (in Azure), I want one work item per incident, so that I'm not flooded.

- Given an open incident on the same service and error code, when an Agent escalates, then the ticket links to the incident's work item and no new work item is created.
- Given the escalation dialog, then it tells the Agent which incident and work item it will join.

### E5-S3 Reliable delivery

As the bank, I want Azure outages to never block customer service, so that calls keep flowing.

- Given Azure is unavailable, when I escalate, then the ticket is saved, sync shows "pending · retrying" and the Agent is told no action is needed.
- Given retries, then the worker uses exponential backoff and the same idempotency key, so a retry never creates a second work item.
- Given 10 failed attempts, then sync shows Failed and the failed-integration monitor alerts.
- Given Azure recovers, then pending items are created and the tickets update.

### E5-S4 Receive Azure changes

As an Agent, I want the ticket to update when SRE acts, so that I don't have to ask.

- Given an Azure service hook, when it arrives, then the signature is checked and unknown work items are rejected and logged.
- Given an event older than the stored revision, then it is ignored.
- Given Assigned or Active, then the ticket moves to With SRE and shows the owner.
- Given a comment tagged public, then it shows as Latest update; other comments never reach the portal.
- Given Resolved, then the ticket moves to "SRE fixed it — confirm", the RCA summary is stored and the Agent is notified.
- Given Reopened, then the ticket returns to With SRE.
- Given every event, then AZURE_WEBHOOK_RECEIVED is audited; for an incident work item, every linked ticket updates.

### E5-S5 Read-only SRE panel

As an Agent, I want to see the technical status without changing it, so that I can answer the customer.

- Given an escalated ticket, then the SRE panel shows work item, sync state and revision, incident, SRE state, owner, latest public update, escalation reason and RCA.
- Given the panel, then there is no control that changes Azure state; the work item number is shown without a link.

### E5-S6 Ask SRE for an update

As an Agent, I want to nudge SRE without leaving the portal, so that a breached ticket moves.

- Given a ticket With SRE that is not part of an incident, when I click Ask SRE for update, then a comment is posted to the work item, the Team Leader is notified and the timeline records it.
- Given I asked less than an hour ago, then the action is blocked and tells me when I can ask again.
- Given a ticket linked to an incident, then the action is not offered; the next step is to wait for the incident fix.

### E5-S7 Confirm the fix with the customer

As an Agent, I want to confirm with the customer before closing, so that "fixed" means fixed for them.

- Given "SRE fixed it — confirm", then the pinned bar shows "Call and confirm" with Confirmed — resolve and Still not working.
- Given Confirmed, then the ticket resolves and the SLA stops.
- Given Still not working, then a reopen comment is posted to the work item and the ticket returns to With SRE.

### E5 tech tasks

- [ ] Outbox table, worker, backoff, idempotency key = ticket or incident ID.
- [ ] Azure DevOps REST client (create work item, add comment); PAT or app credentials from Key Vault.
- [ ] Webhook endpoint with signature check, revision ordering, state mapping table.
- [ ] Reconciliation job for open work items (every 15 minutes).
- [ ] Integration status field on ticket and incident; failed-integration alert.
- [ ] Masked customer token generator.
- [ ] Contract tests against an Azure DevOps test project.

## E6 Automatic tickets and incidents

Goal: errors become tickets before the customer calls, and outages are grouped into one incident.

### E6-S1 Monitor job creates tickets

As the System, I want to turn new error rows into tickets every minute, so that the bank acts first.

- Given unprocessed rows in detected_error_event, when the job runs, then each row is locked, processed once and marked with its ticket ID.
- Given a known error code, then the ticket gets the rule's category, issue and priority, with origin Auto-created.
- Given the job stops mid-run, when it restarts, then no row is processed twice and no duplicate ticket exists.
- Given each run, then rows read, created, duplicates and grouped are stored and MONITOR_JOB_RUN is audited.

### E6-S2 Duplicate protection

As an Agent, I want one ticket per customer per error, so that my queue isn't flooded.

- Given an open ticket for the same customer and error code in the last 24 hours, when a new row arrives, then it is added to that ticket's timeline instead.
- Given an open manual ticket for the same customer and error, then the row joins that ticket.

### E6-S3 Group into an incident

As the bank, I want mass failures grouped automatically, so that SRE sees one problem.

- Given 5 or more customers with the same error code and platform within 15 minutes, then one incident is created with one Azure work item and every matching ticket links to it.
- Given fewer than 5, then tickets stay separate.
- Given an incident is created, then linked tickets become Critical and every Agent sees the incident bar.
- Given the thresholds, then the Admin can change them and SETTING_CHANGED is audited.

### E6-S4 Rule-based auto-escalation

As the System, I want some errors escalated without waiting for a call, so that money issues move fast.

- Given a rule with auto-escalate (e.g. TRX_DEBIT_NO_CREDIT), when its ticket is created and not grouped, then it is escalated to Azure with reason "Auto-escalated by rule".

### E6-S5 Incident bar and customer script

As an Agent, I want the open incident above everything, so that I handle every related call the same way.

- Given an open incident, then Home shows its ID, service, error code, platform, affected customers, SRE state and work item.
- Given "What to tell customers", then a script opens that matches the incident's state.
- Given the incident is resolved by SRE and tickets still need confirmation, then the bar turns green and shows how many customers to confirm.

### E6-S6 Call through an incident's customers

As an Agent, I want one list for all customers of an incident, so that confirming 8 people is one flow.

- Given "Work through all N", then a list shows each ticket, customer, masked mobile, status, SLA and the call script.
- Given a ticket in "SRE fixed it — confirm", then I can record Confirmed, Still failing or No answer.
- Given Confirmed, then that ticket resolves; Still failing posts a reopen comment to the incident work item; No answer logs an attempt and keeps the ticket open.
- Given each ticket, then it keeps its own SLA and audit.

### E6 tech tasks

- [ ] detected_error_event, error_rule, monitor_job_run tables; seed script with the 9 test cases.
- [ ] Scheduled job with SELECT … FOR UPDATE SKIP LOCKED.
- [ ] Grouping service and Incident / IncidentTicket tables.
- [ ] Settings table for thresholds with audit.
- [ ] Incident bar component and script templates per state.
- [ ] Batch call-through screen and endpoint (one result per ticket).

## E7 Guide authoring and review

Goal: Guide Admins change guides safely, with a second person approving, and changes reach agents without a release.

### E7-S1 Guide library

As an Admin, I want all guides in one place, so that I can see what's live and what's in progress.

- Given the library, then each guide shows title, description, match rules, live version, any draft or review version, runs and resolve rate.
- Given a Reviewer, then guides open read-only.

### E7-S2 Edit a decision tree

As an Admin, I want to edit steps, answers, routes and outcomes, so that the guide matches reality.

- Given a live guide, when I edit, then a new draft version is created; the live version never changes.
- Given a draft, then I can add, edit and delete steps, answers, routes, log hints and outcomes, and set the start step.
- Given each outcome, then I can set type, result text and the customer script.
- Given match rules, then I can set platforms, categories and error codes, and see how many open tickets the guide would appear for.

### E7-S3 Validation

As an Admin, I want broken guides blocked, so that agents never hit a dead end.

- Given a draft, then validation runs on every change and lists problems: missing start, empty question or answer, route to a missing step, loop, unreachable step.
- Given any problem, then Submit for review is disabled.

### E7-S4 Preview and test

As an Admin or Reviewer, I want to run a version before it goes live, so that I feel the flow.

- Given Preview, then the runner opens on that version and nothing is saved to any ticket.

### E7-S5 Review with a real diff

As a Reviewer, I want to see exactly what changed, so that I never approve blind.

- Given a version in review, then the diff shows published vs proposed side by side: steps added, removed, reworded, routes added or removed (marked "new route") and outcome changes.
- Given an outcome type changed, then it is flagged "Check this one" with how many recent runs ended there.
- Given a step no answer routes to, then it is marked Unreachable and approval is blocked.
- Given Changes only or Whole guide, then the diff switches view.
- Given the header, then change count, author's reason and scope (runs, error codes, platforms) are shown.

### E7-S6 Approve, schedule or send back

As a Reviewer, I want to decide with four eyes, so that no one publishes alone.

- Given I authored the version, then approve is disabled and an attempt is audited as GUIDE_SELF_APPROVAL_BLOCKED.
- Given Approve & publish now, then the version goes live, the previous live version is archived and GUIDE_PUBLISHED is audited.
- Given Approve, publish in 1 hour, then the version is Scheduled and a job publishes it at that time.
- Given Send back, then a note is required and the version returns to Draft with the note.

### E7-S7 Live update and rollback

As an Admin, I want changes to reach agents instantly and be reversible, so that fixes are fast and safe.

- Given a publish, then every Agent gets "Guide updated" and recommendations use the new version without reloading.
- Given an archived version, when I roll back, then a new version with that content is created and goes through review.

### E7 tech tasks

- [ ] Guide, GuideVersion (tree JSON, state), GuideMatchRule tables.
- [ ] Validator (reachability, loops, missing targets) shared by editor and publish API.
- [ ] Diff service producing typed changes (added, removed, reworded, route, outcome type).
- [ ] Review endpoints with self-approval check; scheduler for publish_at.
- [ ] Publish event over WebSocket/SSE; cache eviction for published guides.
- [ ] Editor, review/diff and library screens.

## E8 Triage, analytics and feedback

Goal: knowledge gaps get found and fixed by the Guide Admin, not discovered by agents on calls.

### E8-S1 Triage bucket for unknown errors

As an Admin, I want tickets from unknown error codes in my queue, so that agents aren't handed problems nobody has described.

- Given an error code with no rule, when the monitor job creates its ticket, then it goes to Triage (not the agent queue) and is not grouped or escalated.
- Given Map to a rule, then I set category, issue, priority and auto-escalate; ERROR_RULE_CREATED is audited.
- Given a rule is saved, then every open triage ticket with that code takes the new values and moves to the agent queue.
- Given the customer calls before triage, then the Agent still finds the ticket through search and can escalate it.

### E8-S2 Guide analytics with a verdict

As an Admin, I want to know which guide to fix next, so that I spend time where it matters.

- Given the last 30 days, then the page opens with "Fix next: \<guide\>" based on escalation and abandon rates, naming the worst step.
- Given each guide, then runs and the share resolved on the call, escalated, follow-up and abandoned are shown.
- Given error codes seen with no matching guide, then they are listed with event counts.
- Given abandoned steps, then they are ranked by count.

### E8-S3 Agent feedback queue

As an Admin, I want agents' flags in one list, so that I can act on them.

- Given open flags, then each shows guide, step, comment, agent and time, with Fix in editor and Mark done.
- Given the nav, then the open count is shown.

### E8-S4 Error rules and job view

As an Admin, I want to see and tune how errors become tickets, so that the queue stays useful.

- Given the page, then rules, thresholds (group size, window, duplicate window, job frequency) and recent job runs are shown.
- Given Run job now, then the job runs and the result is shown.

### E8 tech tasks

- [ ] Triage flag on ticket; queue and grouping exclude it.
- [ ] Rule creation endpoint that reclassifies open triage tickets in one transaction.
- [ ] Analytics queries over sessions (outcome per end key, abandoned = started without finish after 30 min).
- [ ] Triage, analytics, feedback and rules screens.

## E9 Notifications

Goal: each person hears about changes that need them, and nothing else.

### E9-S1 In-app notifications

As an Agent, I want a list of what changed on my tickets, so that I act on it.

- Given these events, then the Agent is notified: work item created, SRE resolved (call to confirm), SLA warning, SLA breached, known incident started, guide updated.
- Given the Team Leader, then they get Ask SRE for update requests and SLA breaches for their team.
- Given the nav and Home, then the unread count and the latest 4 unread items are shown; opening the list marks them read.

### E9-S2 Real-time delivery

As an Agent, I want notifications without refreshing, so that I see an incident the moment it starts.

- Given the portal is open, when a notification is created, then it appears within 5 seconds.
- Given the connection drops, then it reconnects and loads anything missed.

### E9 tech tasks

- [ ] Notification table and service (recipient by role or user).
- [ ] WebSocket/SSE channel shared with guide publish events.
- [ ] Event-to-recipient mapping config.

## E10 Platform and non-functional

Goal: the system runs anywhere with one command, is observable, accessible and safe.

### E10-S1 Run with Docker

As a developer, I want one command to start everything, so that setup takes minutes.

- Given docker compose up, then backend, frontend, database (with seed data and the 9 monitor test cases) start and the README's demo script works.
- Given environment config, then no secret is in the repository.

### E10-S2 Observability

As an operator, I want logs and health checks, so that I can see problems.

- Given any request, then structured logs carry a correlation ID that is also sent to Azure.
- Given /health, then it reports database, Azure reachability and job status.
- Given errors, then a global handler returns a consistent error body with no stack trace.

### E10-S3 Accessibility

As an Agent, I want the portal usable by keyboard and screen reader, so that everyone can work.

- Given any control, then it is a real button, link or labelled input with a visible focus ring and at least 44px target.
- Given state colours, then text meets WCAG AA on its tint (danger \#B42318, warning \#92400E, success \#166534).
- Given status, then it is never shown by colour alone.

### E10-S4 Arabic and RTL

As an Agent, I want the portal in Arabic, so that I work in my language.

- Given Arabic, then all text is translated and the layout mirrors, including the severity bar, breadcrumb and SLA meter.
- Given guide text, then Admins can enter Arabic and English per step.

### E10 tech tasks

- [ ] Dockerfiles, docker-compose, seed scripts, README with demo script.
- [ ] Logging with correlation ID filter; health endpoint; global exception handler.
- [ ] OpenAPI documentation.
- [ ] Accessibility checks in CI (axe) and contrast tokens.
- [ ] i18n setup with RTL stylesheet.
