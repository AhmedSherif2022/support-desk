package com.supportdesk.azure.dto;

import com.supportdesk.azure.enums.EscalationUrgency;
import com.supportdesk.azure.enums.SyncStatus;
import com.supportdesk.ticket.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class EscalationDtos
{
    private EscalationDtos()
    {
    }

    /** E5-S1: reason and urgency are both required. */
    public record EscalateRequest(
            @Schema(description = "Why SRE is needed: what was tried and what failed. Do not include customer data; "
                    + "CIFs, national IDs, mobiles and the customer's name are removed before anything reaches Azure.",
                    example = "Transfer debited but not credited; guide reached 'escalate'; core banking shows no credit.")
            @NotBlank(message = "Say why SRE is needed.") @Size(min = 10, max = 500) String reason,
            @Schema(description = "Becomes the Azure work item priority (CRITICAL = 1 ... LOW = 4).", example = "HIGH")
            @NotNull(message = "Choose the urgency.") EscalationUrgency urgency)
    {
    }

    /** What the agent sees about the escalation (E5-S1, E5-S3). */
    public record EscalationDto(
            @Schema(example = "#2001") String ticketNumber,
            @Schema(example = "ESCALATED") TicketStatus ticketStatus,
            @Schema(example = "HIGH") EscalationUrgency urgency,
            String reason,
            @Schema(example = "Mona") String escalatedBy,
            Instant escalatedAt,
            @Schema(example = "SYNCED") SyncStatus syncStatus,
            @Schema(description = "Text for the sync chip.", example = "synced") String syncLabel,
            @Schema(description = "Azure DevOps work item number; null until Azure confirms.", example = "70001",
                    types = {"integer", "null"}) Long workItemId,
            @Schema(description = "Azure DevOps state, e.g. New, Active, Resolved.", example = "New",
                    types = {"string", "null"}) String azureState,
            @Schema(description = "One sentence for the agent.",
                    example = "Azure DevOps is not reachable right now. We keep retrying automatically; no action is needed.")
            String message)
    {
    }

    /** Guide Admin: the failed-integration monitor (E5-S3). */
    public record OutboxSummaryDto(
            @Schema(example = "2") long pending,
            @Schema(example = "0") long failed,
            @Schema(description = "Messages that gave up; each fired the failed-integration alert.")
            List<FailedMessageDto> failedMessages)
    {
    }

    public record FailedMessageDto(
            @Schema(example = "TICKET-2001") String idempotencyKey,
            @Schema(example = "CREATE_WORK_ITEM") String operation,
            @Schema(example = "10") int attempts,
            String lastError,
            Instant lastAttemptAt)
    {
    }
}
