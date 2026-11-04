package com.supportdesk.guide.controller;

import com.supportdesk.common.exception.ApiError;
import com.supportdesk.guide.dto.GuideDtos.RecommendedGuideDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * HTTP contract for finding guides. {@link GuideController} only implements this and delegates.
 */
@RequestMapping("/api/guides")
@PreAuthorize("hasRole('AGENT')")
@Tag(name = "Guides", description = "Find the troubleshooting guides that fit a ticket.")
public interface GuideApi
{
    @GetMapping("/recommended")
    @Operation(summary = "Recommend guides for a ticket",
            description = "Live guides whose platform matches the ticket and whose error code or category matches, "
                    + "error-code matches first. An empty list means no guide fits, and the ticket's next step is Escalate.")
    @ApiResponse(responseCode = "200", description = "Matching guides, best first (may be empty)")
    @ApiResponse(responseCode = "400", description = "REQUEST_ERROR: ticket is missing or not a number",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    List<RecommendedGuideDto> recommended(
            @Parameter(description = "Ticket number, without the #.", example = "2002") @RequestParam("ticket") Long ticketNumber);
}
