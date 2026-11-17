package com.supportdesk.guide.controller;

import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.common.exception.ApiError;
import com.supportdesk.guide.dto.GuideDtos.GuideFeedbackDto;
import com.supportdesk.guide.enums.GuideFeedbackStatus;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * HTTP contract for the Guide Admin's Agent feedback queue (E4-S5, E8-S3): read the flags and mark them done.
 * Fixing the guide itself is authoring (E7). {@link GuideFeedbackAdminController} only implements this and delegates.
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
                    + "An agent flags a step of a run once. Filter by status: status=OPEN gives the open flags, and "
                    + "its totalItems is the open count for the nav. Paging: page (from 0), "
                    + "size (default 20, max 100), sort by createdAt, id or status (default createdAt,desc then id,desc; "
                    + "send both as sort=createdAt,desc&sort=id,desc to keep the tie-break).")
    @ApiResponse(responseCode = "200", description = "One page of flagged steps")
    @ApiResponse(responseCode = "400", description = "REQUEST_ERROR: status is not OPEN or RESOLVED, page or size is "
            + "not a number, or sort names a field other than createdAt, id or status",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<GuideFeedbackDto> queue(
            @Parameter(description = "OPEN or RESOLVED; omit for every flag.", example = "OPEN")
            @RequestParam(required = false) GuideFeedbackStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable);

    @PostMapping("/{id}/done")
    @Operation(summary = "Mark a flag done",
            description = "Sets the flag to RESOLVED and audits GUIDE_FEEDBACK_RESOLVED on the flag (entity "
                    + "GUIDE_FEEDBACK). Marking a flag that is already done changes nothing and records nothing.")
    @ApiResponse(responseCode = "200", description = "The flag, now RESOLVED")
    @ApiResponse(responseCode = "400", description = "REQUEST_ERROR: id is not a number",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "GUIDE_FEEDBACK_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "DATA_CONFLICT: someone else changed the flag at the same "
            + "time; reload and try again",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    GuideFeedbackDto markDone(@Parameter(description = "Flag id, from the queue.", example = "5") @PathVariable Long id);
}
