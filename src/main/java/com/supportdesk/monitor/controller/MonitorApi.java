package com.supportdesk.monitor.controller;
import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.common.exception.ApiError;
import com.supportdesk.monitor.dto.MonitorDtos.JobRunDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
@RequestMapping("/api/admin/monitor")
@Tag(name = "Monitor job", description = "Guide Admin: run the monitor job and see its runs (E6-S1, E8-S4)")
public interface MonitorApi
{
    @PostMapping("/run")
    @Operation(summary = "Run the monitor job now",
            description = "Processes waiting error rows right away and returns the run's numbers.")
    @ApiResponse(responseCode = "200", description = "The finished run")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    JobRunDto runNow();

    @GetMapping("/runs")
    @Operation(summary = "Recent job runs, newest first")
    @ApiResponse(responseCode = "200", description = "One page of runs")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<JobRunDto> runs(@ParameterObject @PageableDefault(size = 20) Pageable pageable);
}