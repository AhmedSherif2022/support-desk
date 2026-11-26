package com.supportdesk.azure.dto;

import com.supportdesk.azure.client.AzureDevOpsClient.WorkItemRequest;
import com.supportdesk.azure.enums.EscalationUrgency;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** E5-S1: the work item carries every field SRE needs, escaped, with the urgency as Azure priority. */
class EscalationPayloadTest
{
    @Test
    void workItemHasAllTheFieldsAndTheUrgencyAsPriority()
    {
        EscalationPayload payload = new EscalationPayload("TICKET-2001", "#2001", "https://portal/tickets/2001",
                "CUST-ABC123DEF456", "MOBILE", "PAYMENTS", "Payments / Payment Failed", "ERR_PAY_42",
                "QR payment failed <script>", List.of("2026-11-26T10:00:00Z MOBILE PAYMENTS QR_PAY ERR_PAY_42"),
                "corr-77", "HIGH", Instant.parse("2026-11-26T18:00:00Z"), "5h 10m left",
                "QR guide: s1=yes > s2=no -> ESCALATE", "Debited, not credited", EscalationUrgency.CRITICAL);

        WorkItemRequest request = payload.toWorkItemRequest();

        assertThat(request.idempotencyKey()).isEqualTo("TICKET-2001");
        assertThat(request.keyTag()).isEqualTo("PortalKey-TICKET-2001");
        assertThat(request.priority()).isEqualTo(1);
        assertThat(request.title()).isEqualTo("[Ticket #2001] Payments / Payment Failed (MOBILE)");
        assertThat(request.tags()).contains("support-desk", "service:PAYMENTS", "platform:MOBILE");
        assertThat(request.descriptionHtml())
                .contains("#2001", "https://portal/tickets/2001", "CUST-ABC123DEF456", "MOBILE", "PAYMENTS",
                        "Payments / Payment Failed", "ERR_PAY_42", "corr-77", "HIGH", "5h 10m left",
                        "QR guide: s1=yes &gt; s2=no -&gt; ESCALATE", "Debited, not credited", "CRITICAL",
                        "2026-11-26T10:00:00Z MOBILE PAYMENTS QR_PAY ERR_PAY_42")
                .contains("&lt;script&gt;")
                .doesNotContain("<script>");
    }
}
