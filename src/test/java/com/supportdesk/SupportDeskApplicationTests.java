package com.supportdesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SupportDeskApplicationTests
{
    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiSpecDocumentsAuthCustomersTicketsAndQueue() throws Exception
    {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[*].name", containsInAnyOrder("Audit", "Auth", "Customers", "Tickets",
                        "Guides", "Guide runs", "Guide feedback", "Ticket review", "Monitor job", "Incidents",
                        "Incident settings", "Ticket categories (admin)", "Service health", "Service health settings",
                        "Escalation to SRE")))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/tickets/{number}/escalation'].post").exists())
                .andExpect(jsonPath("$.paths['/api/tickets/{number}/escalation'].get").exists())
                .andExpect(jsonPath("$.paths['/api/admin/azure/outbox'].get").exists())
                .andExpect(jsonPath("$.paths['/api/admin/categories'].get").exists())
                .andExpect(jsonPath("$.paths['/api/admin/categories'].post").exists())
                .andExpect(jsonPath("$.paths['/api/admin/categories/{code}/subcategories'].get").exists())
                .andExpect(jsonPath("$.paths['/api/admin/categories/{code}/subcategories'].post").exists())
                .andExpect(jsonPath("$.paths['/api/incidents'].get").exists())
                .andExpect(jsonPath("$.paths['/api/incidents/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/incidents/{id}/tickets'].get").exists())
                .andExpect(jsonPath("$.paths['/api/admin/incident-settings'].get").exists())
                .andExpect(jsonPath("$.paths['/api/admin/incident-settings'].put").exists())
                .andExpect(jsonPath("$.components.schemas.IncidentStatus.description", not(emptyOrNullString())))
                .andExpect(jsonPath("$.paths['/api/audit/logs'].get").exists())
                .andExpect(jsonPath("$.paths['/api/customers/{cif}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/customers/{cif}/verify'].post").exists())
                .andExpect(jsonPath("$.paths['/api/tickets'].post").exists())
                .andExpect(jsonPath("$.paths['/api/tickets/precheck'].post").exists())
                .andExpect(jsonPath("$.paths['/api/tickets/queue'].get").exists())
                .andExpect(jsonPath("$.paths['/api/tickets/{number}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/tickets/{number}/steps'].post").exists())
                .andExpect(jsonPath("$.paths['/api/review/tickets'].get").exists())
                .andExpect(jsonPath("$.paths['/api/review/tickets/{number}/steps'].post").exists())
                .andExpect(jsonPath("$.paths['/api/guides/recommended'].get").exists())
                .andExpect(jsonPath("$.paths['/api/guide-runs'].post").exists())
                .andExpect(jsonPath("$.paths['/api/guide-runs/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/guide-runs/{id}/answers'].post").exists())
                .andExpect(jsonPath("$.paths['/api/guide-runs/{id}/answers/{position}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/guide-runs/{id}/flags'].post").exists())
                .andExpect(jsonPath("$.paths['/api/admin/guide-feedback'].get").exists())
                .andExpect(jsonPath("$.components.schemas.QueueResponse").exists())
                // agent APIs: every enum they expose is one shared, described schema; no backlog ids in titles
                .andExpect(jsonPath("$.components.schemas['TicketPriority','SlaState','TicketOrigin','Platform','Segment','EnrollmentStatus','QueueChip','QueueSort','GuideRunStatus','GuideFeedbackStatus','OutcomeType','Role','AuditAction'].description", everyItem(not(emptyOrNullString()))))
                .andExpect(jsonPath("$.paths['/api/tickets/queue'].get.parameters[?(@.name == 'sort')].schema['$ref']")
                        .value(hasItem("#/components/schemas/QueueSort")))
                .andExpect(jsonPath("$.components.schemas.QueueTicketResponse.properties.slaState['$ref']")
                        .value("#/components/schemas/SlaState"))
                .andExpect(jsonPath("$.paths['/api/customers/search'].get.summary").value("Search customers"))
                .andExpect(jsonPath("$.paths['/api/guide-runs/{id}'].get.parameters[0].description").exists())
                .andExpect(jsonPath("$.components.schemas.DiagnosisDto.properties.summary.example")
                        .value("3 failed LOGIN events with ERR_AUTH_503 on AUTHENTICATION in the last 6 hours."))
                .andExpect(jsonPath("$.paths['/api/tickets'].get.parameters[?(@.name == 'page')].schema.type")
                        .value(hasItem("integer")))
                .andExpect(jsonPath("$.paths['/api/tickets/categories'].get.summary").value("Ticket categories"))
                .andExpect(jsonPath("$.components.schemas.TicketDto.properties.activeGuideRunId").exists())
                .andExpect(jsonPath("$.paths['/api/guide-runs'].get.summary").value("Guide runs on a ticket"))
                .andExpect(jsonPath("$.paths['/api/guide-runs/{id}/flags'].post.summary").value("Flag a step"))
                .andExpect(jsonPath("$.components.schemas.DiagnosisDto.properties.suggestedGuide").exists())
                // the step machine: one shared schema per enum, each step described with its move
                .andExpect(jsonPath("$.components.schemas.TicketStep.enum", hasSize(15)))
                .andExpect(jsonPath("$.components.schemas.TicketStep.description",
                        containsString("`SEND_TO_REVIEW`: SRE_FIXED → IN_REVIEW")))
                .andExpect(jsonPath("$.components.schemas.TicketStatus.enum", hasItem("IN_REVIEW")))
                .andExpect(jsonPath("$.components.schemas.TicketDto.properties.availableSteps.items['$ref']")
                        .value("#/components/schemas/TicketStep"))
                .andExpect(jsonPath("$.paths['/api/tickets/{number}/steps'].post.requestBody.content['application/json'].examples['Pick up ticket']").exists())
                .andExpect(jsonPath("$.paths['/api/tickets/{number}/steps'].post.responses['422'].content['*/*'].examples['Not your step']").exists())
                .andExpect(jsonPath("$.paths['/api/review/tickets/{number}/steps'].post.responses['422'].content['*/*'].examples['No reason']").exists())
                .andExpect(jsonPath("$.components.schemas.QueueTicketResponse").exists());
    }
}
