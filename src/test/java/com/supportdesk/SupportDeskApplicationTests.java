package com.supportdesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
                        "Guides", "Guide runs")))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/audit/logs'].get").exists())
                .andExpect(jsonPath("$.paths['/api/customers/{cif}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/customers/{cif}/verify'].post").exists())
                .andExpect(jsonPath("$.paths['/api/tickets'].post").exists())
                .andExpect(jsonPath("$.paths['/api/tickets/precheck'].post").exists())
                .andExpect(jsonPath("$.paths['/api/tickets/queue'].get").exists())
                .andExpect(jsonPath("$.paths['/api/tickets/{number}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/tickets/{number}/status'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/guides/recommended'].get").exists())
                .andExpect(jsonPath("$.paths['/api/guide-runs'].post").exists())
                .andExpect(jsonPath("$.paths['/api/guide-runs/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/guide-runs/{id}/answers'].post").exists())
                .andExpect(jsonPath("$.paths['/api/guide-runs/{id}/answers/{position}'].delete").exists())
                .andExpect(jsonPath("$.components.schemas.QueueResponse").exists())
                .andExpect(jsonPath("$.components.schemas.QueueTicketResponse").exists());
    }
}
