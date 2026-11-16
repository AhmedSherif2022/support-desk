package com.supportdesk.ticket.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.supportdesk.ticket.enums.StepActor.AGENT;
import static com.supportdesk.ticket.enums.StepActor.GUIDE;
import static com.supportdesk.ticket.enums.StepActor.REVIEWER;
import static com.supportdesk.ticket.enums.StepActor.SYSTEM;
import static com.supportdesk.ticket.enums.TicketStatus.CLOSED;
import static com.supportdesk.ticket.enums.TicketStatus.ESCALATED;
import static com.supportdesk.ticket.enums.TicketStatus.IN_REVIEW;
import static com.supportdesk.ticket.enums.TicketStatus.NEW;
import static com.supportdesk.ticket.enums.TicketStatus.REOPENED;
import static com.supportdesk.ticket.enums.TicketStatus.RESOLVED;
import static com.supportdesk.ticket.enums.TicketStatus.SRE_FIXED;
import static com.supportdesk.ticket.enums.TicketStatus.TROUBLESHOOTING;
import static com.supportdesk.ticket.enums.TicketStatus.WAITING_ON_CUSTOMER;
import static com.supportdesk.ticket.enums.TicketStatus.WITH_SRE;

/**
 * Every arrow of the agreed ticket diagram: who takes the step and the move it makes. The one source of truth:
 * TicketStatusMachine builds its allowed moves from this list. The SYSTEM steps come from Azure DevOps (Epic 5);
 * nothing takes them yet.
 */
@Schema(enumAsRef = true, description = "A step moves a ticket from one status to another. It names who takes it and the move it makes. Three moves have two steps: one taken by the agent or the system, one by a guide, so the audit shows who moved the ticket.\n"
        + "\n"
        + "**Agent** (`POST /api/tickets/{number}/steps`):\n"
        + "- `PICK_UP_TICKET`: NEW → TROUBLESHOOTING\n"
        + "- `REQUEST_DETAILS`: TROUBLESHOOTING → WAITING_ON_CUSTOMER (SLA clock pauses)\n"
        + "- `DETAILS_RECEIVED`: WAITING_ON_CUSTOMER → TROUBLESHOOTING (SLA clock resumes)\n"
        + "- `APPLY_FIX`: TROUBLESHOOTING → RESOLVED\n"
        + "- `ARCHIVE`: RESOLVED → CLOSED\n"
        + "\n"
        + "**Reviewer** (`POST /api/review/tickets/{number}/steps`):\n"
        + "- `REVIEWER_APPROVES`: IN_REVIEW → RESOLVED\n"
        + "- `REVIEWER_REJECTS`: IN_REVIEW → REOPENED (reason required)\n"
        + "\n"
        + "**Guide** (applied by `POST /api/guide-runs/{id}/complete`, never sent by hand):\n"
        + "- `GUIDE_SOLVED`: TROUBLESHOOTING → RESOLVED\n"
        + "- `GUIDE_FOLLOW_UP`: TROUBLESHOOTING → WAITING_ON_CUSTOMER\n"
        + "- `GUIDE_HAND_OVER`: TROUBLESHOOTING → ESCALATED\n"
        + "\n"
        + "**System** (Azure DevOps integration, not built yet; nothing takes these steps yet):\n"
        + "- `HAND_OVER`: TROUBLESHOOTING → ESCALATED\n"
        + "- `SRE_ACCEPTS`: ESCALATED → WITH_SRE\n"
        + "- `DEPLOY_FIX`: WITH_SRE → SRE_FIXED\n"
        + "- `SEND_TO_REVIEW`: SRE_FIXED → IN_REVIEW\n"
        + "- `FIX_FAILED`: REOPENED → WITH_SRE")
@Getter
@RequiredArgsConstructor
public enum TicketStep
{
    PICK_UP_TICKET   (AGENT,    NEW,                 TROUBLESHOOTING),
    REQUEST_DETAILS  (AGENT,    TROUBLESHOOTING,     WAITING_ON_CUSTOMER),
    DETAILS_RECEIVED (AGENT,    WAITING_ON_CUSTOMER, TROUBLESHOOTING),
    APPLY_FIX        (AGENT,    TROUBLESHOOTING,     RESOLVED),
    ARCHIVE          (AGENT,    RESOLVED,            CLOSED),

    GUIDE_SOLVED     (GUIDE,    TROUBLESHOOTING,     RESOLVED),
    GUIDE_FOLLOW_UP  (GUIDE,    TROUBLESHOOTING,     WAITING_ON_CUSTOMER),
    GUIDE_HAND_OVER  (GUIDE,    TROUBLESHOOTING,     ESCALATED),

    HAND_OVER        (SYSTEM,   TROUBLESHOOTING,     ESCALATED),
    SRE_ACCEPTS      (SYSTEM,   ESCALATED,           WITH_SRE),
    DEPLOY_FIX       (SYSTEM,   WITH_SRE,            SRE_FIXED),
    SEND_TO_REVIEW   (SYSTEM,   SRE_FIXED,           IN_REVIEW),
    FIX_FAILED       (SYSTEM,   REOPENED,            WITH_SRE),

    REVIEWER_APPROVES(REVIEWER, IN_REVIEW,           RESOLVED),
    REVIEWER_REJECTS (REVIEWER, IN_REVIEW,           REOPENED);

    private final StepActor actor;
    private final TicketStatus from;
    private final TicketStatus to;
}
