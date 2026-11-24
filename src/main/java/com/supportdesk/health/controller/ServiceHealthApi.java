package com.supportdesk.health.controller;
import com.supportdesk.common.exception.ApiError;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * HTTP contract for the agent home service-health panel. This is the bank's services (mobile app, payments, …),
 * not this backend's own health at /actuator/health.
 */
@RequestMapping("/api/service-health")
@Tag(name = "Service health", description = "Agent home: one row per bank service on MOBILE and on WEB. "
        + "DOWN means an open incident still has a ticket the agent has not closed. "
        + "DEGRADED means enough distinct customers errored in the window set by the Guide Admin. "
        + "A critical or SLA-breached ticket is a notice on the row and does not change the status.")
public interface ServiceHealthApi
{
    @GetMapping
    @Operation(summary = "Service health for the home screen")
    @ApiResponse(responseCode = "200", description = "One row per fixed service and platform, worst status first")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ServiceHealthResponse panel();
}
