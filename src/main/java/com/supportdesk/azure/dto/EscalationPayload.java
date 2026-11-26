package com.supportdesk.azure.dto;

import com.supportdesk.azure.client.AzureDevOpsClient.WorkItemRequest;
import com.supportdesk.azure.enums.EscalationUrgency;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * E5-S1: everything SRE gets in the work item: ticket ID and URL, masked customer token, platform, service,
 * category, error code, summary, log snapshot, correlation ID, priority, SLA, guide path and reason.
 * By construction it has no field for CIF, national ID, mobile or the customer's name, and free text is scrubbed
 * before it gets here ({@link com.supportdesk.azure.service.PersonalDataScrubber}).
 */
public record EscalationPayload(
        String idempotencyKey,
        String ticketNumber,
        String ticketUrl,
        String customerToken,
        String platform,
        String service,
        String category,
        String errorCode,
        String summary,
        List<String> logSnapshot,
        String correlationId,
        String priority,
        Instant slaDueAt,
        String slaState,
        String guidePath,
        String reason,
        EscalationUrgency urgency)
{
    /** The Azure DevOps work item: title, HTML description, priority from the urgency, and search tags. */
    public WorkItemRequest toWorkItemRequest()
    {
        String title = "[Ticket " + ticketNumber + "] " + category + " (" + platform + ")";

        Map<String, String> rows = new LinkedHashMap<>();
        rows.put("Ticket", ticketNumber);
        rows.put("Ticket URL", ticketUrl);
        rows.put("Customer", customerToken);
        rows.put("Platform", platform);
        rows.put("Service", service);
        rows.put("Category", category);
        rows.put("Error code", orDash(errorCode));
        rows.put("Priority", priority);
        rows.put("SLA", slaState + (slaDueAt == null ? "" : " (due " + slaDueAt + ")"));
        rows.put("Urgency", urgency.name());
        rows.put("Escalation reason", reason);
        rows.put("Correlation ID", orDash(correlationId));
        rows.put("Guide path", orDash(guidePath));

        StringBuilder html = new StringBuilder("<h3>Escalated from the Support Desk portal</h3><table>");
        rows.forEach((k, v) -> html.append("<tr><td><b>").append(esc(k)).append("</b></td><td>")
                .append(esc(v)).append("</td></tr>"));
        html.append("</table><h4>Summary</h4><p>").append(esc(summary)).append("</p>");
        html.append("<h4>Log snapshot</h4>");
        if (logSnapshot.isEmpty()) {
            html.append("<p>No failures in the 24 hours before the escalation.</p>");
        } else {
            html.append("<ul>");
            logSnapshot.forEach(line -> html.append("<li>").append(esc(line)).append("</li>"));
            html.append("</ul>");
        }

        List<String> tags = new ArrayList<>(List.of("support-desk", "service:" + service, "platform:" + platform));
        return new WorkItemRequest(idempotencyKey, title, html.toString(), urgency.getAzurePriority(), tags);
    }

    private static String esc(String s)
    {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }

    private static String orDash(String s)
    {
        return s == null || s.isBlank() ? "-" : s;
    }
}
