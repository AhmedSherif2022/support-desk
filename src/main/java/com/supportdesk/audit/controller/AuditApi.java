package com.supportdesk.audit.controller;

import com.supportdesk.audit.dto.AuditLogResponse;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.common.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;

/**
 * HTTP contract for the Audit log screen (E1-S3). Read only: the audit trail has no create, update or delete API.
 * {@link AuditController} only implements this and delegates.
 */
@RequestMapping("/api/audit")
@PreAuthorize("hasAnyRole('GUIDE_ADMIN', 'REVIEWER')")
@Tag(name = "Audit", description = "Read the append-only audit trail. Admin and Reviewer only.")
public interface AuditApi
{
    @GetMapping("/logs")
    @Operation(summary = "Search the audit log",
            description = "Newest first. Every filter is optional; combine them freely. "
                    + "Paging: page (from 0), size (max 100), sort (e.g. createdAt,desc).")
    @ApiResponse(responseCode = "200", description = "One page of audit records")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: only Admin and Reviewer may read the audit log",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<AuditLogResponse> search(
            @Parameter(example = "mona") @RequestParam(required = false) String username,
            @RequestParam(required = false) AuditAction action,
            @Parameter(example = "CUSTOMER") @RequestParam(required = false) String entity,
            @Parameter(example = "CIF-100245") @RequestParam(required = false) String entityId,
            @Parameter(description = "From this time (inclusive), ISO-8601.", example = "2026-11-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Up to this time (exclusive), ISO-8601.", example = "2026-11-03T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable);
}
