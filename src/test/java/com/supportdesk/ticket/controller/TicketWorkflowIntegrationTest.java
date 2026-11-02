package com.supportdesk.ticket.controller;

import com.jayway.jsonpath.JsonPath;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.audit.repository.AuditLogRepository;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E3 against the real database: create (with the auto-attached error code), lifecycle audits, the pinned next step,
 * customer contact, the Timeline and Audit tabs, and the paged ticket list. Each test uses its own new agent.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TicketWorkflowIntegrationTest
{
    private static final String SARA = "CIF-100245";

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    private String token;

    @BeforeEach
    void newVerifiedAgent() throws Exception
    {
        String username = "e3_" + UUID.randomUUID().toString().substring(0, 8);
        String mobile = String.format("+2097%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        User agent = userRepository.save(new User(username, "$2a$10$unused", "Ticket Test",
                username + "@test.local", mobile, Role.AGENT));
        token = "Bearer " + jwtService.generateToken(agent).token();
        mockMvc.perform(post("/api/customers/" + SARA + "/verify").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passedChecks\":[\"DATE_OF_BIRTH\",\"MOBILE_ON_FILE\"]}"))
                .andExpect(status().isOk());
    }

    // ---- E3-S1 ----

    @Test
    void createAttachesTheLatestFailedErrorCodeOfThatServiceAndIsAudited() throws Exception
    {
        // Payment Failed has no default error code; Sara's latest PAYMENTS failure is PAY_TIMEOUT.
        long number = create("PAYMENT_FAILED", "HIGH")
                .andExpect(jsonPath("$.errorCode").value("PAY_TIMEOUT"))
                .andExpect(jsonPath("$.slaLabel", endsWith("left")))
                .andExpect(jsonPath("$.slaPercentUsed").value(0))
                .andExpect(jsonPath("$.nextAction").value("START_GUIDE"))
                .andExpect(jsonPath("$.nextActionLabel").value("Start guide"))
                .andReturn().getResponse().getContentAsString().transform(TicketWorkflowIntegrationTest::numberOf);

        assertThat(rows(number, AuditAction.TICKET_CREATED)).hasSize(1);
    }

    @Test
    void priorityIsRequired() throws Exception
    {
        create("PAYMENT_FAILED", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.priority").value("Choose the priority."));
    }

    // ---- E3-S2 / S3 / S5 ----

    @Test
    void statusChangesAreAuditedWithBeforeAndAfterAndRejectionsSurviveTheirRollback() throws Exception
    {
        long number = numberOf(create("PAYMENT_FAILED", "HIGH").andReturn().getResponse().getContentAsString());

        move(number, "TROUBLESHOOTING").andExpect(status().isOk())
                .andExpect(jsonPath("$.nextActionLabel").value("Escalate"));
        AuditLog changed = rows(number, AuditAction.TICKET_STATUS_CHANGED).get(0);
        assertThat(changed.getBeforeState()).contains("NEW");
        assertThat(changed.getAfterState()).contains("TROUBLESHOOTING");

        move(number, "CLOSED").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        assertThat(rows(number, AuditAction.TICKET_TRANSITION_REJECTED)).singleElement()
                .satisfies(log -> assertThat(log.getAfterState()).contains("CLOSED"));

        move(number, "WAITING_ON_CUSTOMER").andExpect(jsonPath("$.slaLabel").value("Paused"))
                .andExpect(jsonPath("$.nextActionLabel").value("Resume"));
    }

    // ---- E3-S5 / S6 tabs ----

    @Test
    void contactIsLoggedAndTabsListHistoryNewestFirstWithCounts() throws Exception
    {
        long number = numberOf(create("PAYMENT_FAILED", "HIGH").andReturn().getResponse().getContentAsString());
        move(number, "TROUBLESHOOTING");

        mockMvc.perform(post("/api/tickets/" + number + "/contacts").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Told the customer we are on it.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value("Told the customer we are on it."));
        assertThat(rows(number, AuditAction.CUSTOMER_CONTACTED)).hasSize(1);

        tab(number, "contacts").andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].author").value("Ticket Test"));
        tab(number, "timeline").andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.items[0].action").value("TICKET_STATUS_CHANGED"))   // newest first
                .andExpect(jsonPath("$.items[1].action").value("TICKET_CREATED"));
        tab(number, "audit").andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.items[0].action").value("CUSTOMER_CONTACTED"));
    }

    // ---- E3-S7 ----

    @Test
    void ticketListFiltersSearchesAndPagesByEight() throws Exception
    {
        long first = numberOf(create("PAYMENT_FAILED", "HIGH").andReturn().getResponse().getContentAsString());
        for (int i = 0; i < 8; i++) {
            create("PAYMENT_FAILED", "LOW").andExpect(status().isCreated());
        }

        list("").andExpect(jsonPath("$.size").value(8))
                .andExpect(jsonPath("$.items.length()").value(8))
                .andExpect(jsonPath("$.totalItems").value(9))
                .andExpect(jsonPath("$.totalPages").value(2));
        list("?page=1").andExpect(jsonPath("$.items.length()").value(1));
        mockMvc.perform(get("/api/tickets").param("q", "#" + first).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].number").value("#" + first));
        list("?q=" + SARA + "&view=OPEN").andExpect(jsonPath("$.totalItems").value(9));
        list("?view=RESOLVED_CLOSED").andExpect(jsonPath("$.totalItems").value(0));
    }


    private ResultActions create(String category, String priority) throws Exception
    {
        String body = """
                {"cif":"%s","categoryCode":"%s",%s"description":"Card payment fails at checkout.",
                 "reference":"TRX-2026-01111","platform":"MOBILE","allowDuplicate":true}
                """.formatted(SARA, category, priority == null ? "" : "\"priority\":\"" + priority + "\",");
        return mockMvc.perform(post("/api/tickets").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions move(long number, String to) throws Exception
    {
        return mockMvc.perform(patch("/api/tickets/" + number + "/status").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + to + "\"}"));
    }

    private ResultActions tab(long number, String tab) throws Exception
    {
        return mockMvc.perform(get("/api/tickets/" + number + "/" + tab).header(HttpHeaders.AUTHORIZATION, token));
    }

    private ResultActions list(String query) throws Exception
    {
        return mockMvc.perform(get("/api/tickets" + query).header(HttpHeaders.AUTHORIZATION, token));
    }

    private List<AuditLog> rows(long number, AuditAction action)
    {
        return auditLogRepository.findByEntityAndEntityIdOrderByCreatedAtDescIdDesc("TICKET", String.valueOf(number))
                .stream().filter(log -> log.getAction() == action).toList();
    }

    private static long numberOf(String ticketJson)
    {
        String number = JsonPath.read(ticketJson, "$.number");
        return Long.parseLong(number.substring(1));
    }
}
