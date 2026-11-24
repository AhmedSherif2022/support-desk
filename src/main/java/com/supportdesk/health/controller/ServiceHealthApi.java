package com.supportdesk.health.controller;
import com.supportdesk.common.exception.ApiError;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
    @Operation(summary = "Service health for the home screen",
            description = "Eight rows, always: AUTH, PAYMENTS, CARDS and TRANSFERS, each on MOBILE and WEB. "
                    + "Worst status first (DOWN, DEGRADED, UNKNOWN, OPERATIONAL). "
                    + "DOWN: an OPEN incident for that service and platform still has a linked ticket that is not "
                    + "RESOLVED or CLOSED. DEGRADED: distinct customers with any error in the window "
                    + "(GET /api/admin/service-health-settings) is at least the threshold. "
                    + "UNKNOWN: neither of those, and the monitor job has not started or finished in the last 5 minutes "
                    + "(dataStale is then true). OPERATIONAL: no reported problem. "
                    + "notices lists open AUTO_CREATED tickets that are CRITICAL or have slaState BREACHED. "
                    + "They are there so the agent notices them; they do not change status.")
    @ApiResponse(responseCode = "200", description = "One row per fixed service and platform, worst status first",
            content = @Content(schema = @Schema(implementation = ServiceHealthResponse.class),
                    examples = @ExampleObject(name = "Payments on mobile is down", value = "{\"dataStale\":false,\"evaluatedAt\":\"2026-11-24T12:00:00Z\",\"monitorLastActivityAt\":\"2026-11-24T11:59:30Z\",\"windowMinutes\":15,\"minCustomers\":5,\"services\":[{\"service\":\"PAYMENTS\",\"platform\":\"MOBILE\",\"status\":\"DOWN\",\"distinctCustomers\":6,\"openIncidentIds\":[12],\"notices\":[{\"ticketNumber\":2104,\"critical\":true,\"slaBreached\":false}]},{\"service\":\"AUTH\",\"platform\":\"WEB\",\"status\":\"OPERATIONAL\",\"distinctCustomers\":0,\"openIncidentIds\":[],\"notices\":[]}]}")))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ServiceHealthResponse panel();
}
