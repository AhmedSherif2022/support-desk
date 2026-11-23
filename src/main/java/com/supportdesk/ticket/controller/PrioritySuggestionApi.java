package com.supportdesk.ticket.controller;

import com.supportdesk.common.exception.ApiError;
import com.supportdesk.ticket.dto.PrioritySuggestionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/** HTTP contract for the create-ticket form's priority suggestion. {@link PrioritySuggestionController} implements it. */
@RequestMapping("/api/tickets")
@Tag(name = "Tickets")
public interface PrioritySuggestionApi
{
    @GetMapping("/categories/{code}/suggested-priority")
    @Operation(summary = "Suggested priority for a sub-category",
            description = "Call it when the agent picks a sub-category on the create-ticket form, and pre-select "
                    + "suggestedPriority. Starts from the sub-category's default priority; when at least 3 manual tickets "
                    + "in the same sub-category in the last 90 days exist and more than half used another priority, "
                    + "that priority is suggested instead (basedOn = HISTORY). reason explains it in one sentence.")
    @ApiResponse(responseCode = "200", description = "The suggestion")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "CATEGORY_NOT_FOUND: unknown code; SUBCATEGORY_REQUIRED: the code "
            + "is a top-level category",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PrioritySuggestionDto suggestedPriority(
            @Parameter(description = "A sub-category code from GET /api/tickets/categories.", example = "OTP_NOT_RECEIVED")
            @PathVariable String code);
}
