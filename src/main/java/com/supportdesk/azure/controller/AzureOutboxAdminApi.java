package com.supportdesk.azure.controller;

import com.supportdesk.azure.dto.EscalationDtos.OutboxSummaryDto;
import com.supportdesk.common.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Guide Admin: the failed-integration monitor for Azure DevOps (E5-S3). */
@RequestMapping("/api/admin/azure/outbox")
@Tag(name = "Escalation to SRE")
public interface AzureOutboxAdminApi
{
    @GetMapping
    @Operation(summary = "Azure DevOps delivery status",
            description = "How many calls wait to be sent or retried, and the ones that failed the maximum number of "
                    + "times (each also logged an ALERT and audited AZURE_SYNC_FAILED).")
    @ApiResponse(responseCode = "200", description = "Pending and failed counts, and the failed messages")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    OutboxSummaryDto summary();
}
