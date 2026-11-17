package com.supportdesk.guide.controller;

import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.common.exception.ApiError;
import com.supportdesk.guide.dto.GuideDtos.GuideFeedbackDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * HTTP contract for the Guide Admin's Agent feedback queue (E4-S5). Read only for now: the admin acts on a report
 * through authoring (E7). {@link GuideFeedbackAdminController} only implements this and delegates.
 */
@RequestMapping("/api/admin/guide-feedback")
@PreAuthorize("hasRole('GUIDE_ADMIN')")
@Tag(name = "Guide feedback", description = "Steps that agents flagged as wrong or unclear after finishing a guide. "
        + "Guide Admin only.")
public interface GuideFeedbackAdminApi
{
    @GetMapping
    @Operation(summary = "Agent feedback queue",
            description = "Every flagged step, newest first, with the guide, the version the run was pinned to, the "
                    + "question as it read on that version, the agent, the comment and the status. "
                    + "Paging: page (from 0), size (max 100), sort (e.g. createdAt,desc).")
    @ApiResponse(responseCode = "200", description = "One page of flagged steps")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<GuideFeedbackDto> queue(
            @ParameterObject @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable);
}
