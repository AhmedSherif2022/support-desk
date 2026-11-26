package com.supportdesk.azure.controller;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.audit.repository.AuditLogRepository;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.service.JwtService;
import com.supportdesk.azure.client.AzureDevOpsClient;
import com.supportdesk.azure.client.AzureDevOpsClient.WorkItemRequest;
import com.supportdesk.azure.client.FakeAzureDevOpsClient;
import com.supportdesk.azure.service.OutboxWorker;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E5-S1 and E5-S3 against the real database, with the in-memory Azure DevOps (local profile, mode FAKE).
 * The scheduled worker is pushed out to 1 hour for tests (src/test/resources/config/application.yml); each test runs it
 * itself with {@link OutboxWorker#runOnce()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EscalationIntegrationTest
{
    private static final String NUMBER = "100101";                       // Ahmed Ali, CIF-100101 (local seed)
    private static final String DESCRIPTION = "Ahmed Ali (CIF-100101, mobile 01001234567, ID 28803031234569) "
            + "says the transfer left his account but never arrived.";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private OutboxWorker worker;
    @Autowired private AzureDevOpsClient client;

    private FakeAzureDevOpsClient azure;
    private String agent;
    private long baseline;

    @BeforeEach
    void setUp()
    {
        azure = (FakeAzureDevOpsClient) client;
        azure.setAvailable(true);
        agent = tokenFor(Role.AGENT);
        baseline = auditLogRepository.findAll().stream().mapToLong(AuditLog::getId).max().orElse(0);
    }

    @AfterEach
    void azureBack()
    {
        azure.setAvailable(true);
    }

    @Test
    void escalationIsQueuedInTheSameTransactionThenSentAndSynced() throws Exception
    {
        long ticket = troubleshootingTicket();

        escalate(ticket, "{\"reason\":\"Debited but not credited; guide reached escalate.\",\"urgency\":\"HIGH\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketStatus").value("ESCALATED"))
                .andExpect(jsonPath("$.urgency").value("HIGH"))
                .andExpect(jsonPath("$.syncStatus").value("PENDING"))
                .andExpect(jsonPath("$.syncLabel").value("pending · retrying"))
                .andExpect(jsonPath("$.workItemId").doesNotExist());
        assertThat(outboxStatus(ticket)).isEqualTo("PENDING");

        worker.runOnce();

        String json = escalation(ticket).andExpect(status().isOk())
                .andExpect(jsonPath("$.syncStatus").value("SYNCED"))
                .andExpect(jsonPath("$.syncLabel").value("synced"))
                .andExpect(jsonPath("$.azureState").value("New"))
                .andReturn().getResponse().getContentAsString();
        long workItemId = ((Number) JsonPath.read(json, "$.workItemId")).longValue();

        // what Azure received: every field, and no personal data even though the agent typed it
        WorkItemRequest sent = azure.requestFor(workItemId);
        assertThat(sent.idempotencyKey()).isEqualTo("TICKET-" + ticket);
        assertThat(sent.priority()).isEqualTo(2);
        assertThat(sent.descriptionHtml())
                .contains("#" + ticket, "/tickets/" + ticket, "CUST-", "MOBILE", "TRANSFERS", "Debited but not credited")
                .doesNotContain("100101", "01001234567", "28803031234569")
                .doesNotContainIgnoringCase("ahmed ali");
        assertThat(newAudit(AuditAction.TICKET_ESCALATED)).extracting(AuditLog::getEntityId)
                .contains(String.valueOf(ticket));
    }

    @Test
    void azureDownKeepsTheTicketSavedRetriesWithTheSameKeyThenSyncsOnce() throws Exception
    {
        long ticket = troubleshootingTicket();
        azure.setAvailable(false);

        escalate(ticket, "{\"reason\":\"App crashes on every login attempt.\",\"urgency\":\"MEDIUM\"}")
                .andExpect(status().isCreated());
        worker.runOnce();

        escalation(ticket).andExpect(jsonPath("$.syncStatus").value("PENDING"))
                .andExpect(jsonPath("$.ticketStatus").value("ESCALATED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("no action is needed")));
        Map<String, Object> row = outbox(ticket);
        assertThat(((Number) row.get("attempts")).intValue()).isEqualTo(1);
        assertThat((Boolean) row.get("in_future")).isTrue();                // backoff: not due yet

        azure.setAvailable(true);
        worker.runOnce();
        assertThat(((Number) outbox(ticket).get("attempts")).intValue()).isEqualTo(1);   // still waiting

        makeDue(ticket);
        worker.runOnce();
        escalation(ticket).andExpect(jsonPath("$.syncStatus").value("SYNCED"));
        assertThat(azure.createdCountFor("TICKET-" + ticket)).isEqualTo(1);
    }

    @Test
    void tenFailuresMarkItFailedAuditAndShowInTheMonitor() throws Exception
    {
        long ticket = troubleshootingTicket();
        azure.setAvailable(false);
        escalate(ticket, "{\"reason\":\"Card blocked abroad, customer stuck.\",\"urgency\":\"CRITICAL\"}")
                .andExpect(status().isCreated());

        for (int i = 0; i < 10; i++) {
            makeDue(ticket);
            worker.runOnce();
        }

        assertThat(outboxStatus(ticket)).isEqualTo("FAILED");
        assertThat(((Number) outbox(ticket).get("attempts")).intValue()).isEqualTo(10);
        escalation(ticket).andExpect(jsonPath("$.syncStatus").value("FAILED"))
                .andExpect(jsonPath("$.syncLabel").value("failed"));
        assertThat(newAudit(AuditAction.AZURE_SYNC_FAILED)).extracting(AuditLog::getEntityId)
                .contains("TICKET-" + ticket);

        mockMvc.perform(get("/api/admin/azure/outbox").header(HttpHeaders.AUTHORIZATION, tokenFor(Role.GUIDE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failedMessages[?(@.idempotencyKey == 'TICKET-" + ticket + "')].attempts").value(10));
    }

    @Test
    void escalationRules() throws Exception
    {
        long newTicket = createTicket();                                   // still NEW, not picked up
        escalate(newTicket, "{\"reason\":\"Needs SRE right away please.\",\"urgency\":\"LOW\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        escalation(newTicket).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ESCALATION_NOT_FOUND"));

        long ticket = troubleshootingTicket();
        escalate(ticket, "{\"reason\":\"short\"}").andExpect(status().isBadRequest());
        escalate(ticket, "{\"reason\":\"Payment failed at three merchants.\",\"urgency\":\"HIGH\"}")
                .andExpect(status().isCreated());
        escalate(ticket, "{\"reason\":\"Payment failed at three merchants.\",\"urgency\":\"HIGH\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_ESCALATED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM azure_outbox WHERE idempotency_key = ?", Integer.class,
                "TICKET-" + ticket)).isEqualTo(1);
    }


    /** A ticket for CIF-100101 picked up by the agent (status TROUBLESHOOTING). */
    private long troubleshootingTicket() throws Exception
    {
        long number = createTicket();
        postJson("/api/tickets/" + number + "/steps", "{\"step\":\"PICK_UP_TICKET\"}").andExpect(status().isOk());
        return number;
    }

    private long createTicket() throws Exception
    {
        postJson("/api/customers/" + NUMBER + "/verify", "{\"passedChecks\":[\"DATE_OF_BIRTH\",\"MOBILE_ON_FILE\"]}")
                .andExpect(status().isOk());
        String json = postJson("/api/tickets", "{\"cif\":\"" + NUMBER + "\",\"categoryCode\":\"TRANSFER_FAILED\","
                + "\"priority\":\"HIGH\",\"title\":\"Transfer for Ahmed Ali failed\",\"description\":\"" + DESCRIPTION
                + "\",\"reference\":\"TRX-2026-00981\",\"platform\":\"MOBILE\",\"allowDuplicate\":true}")
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(((String) JsonPath.read(json, "$.number")).replace("#", ""));
    }

    private ResultActions escalate(long ticket, String body) throws Exception
    {
        return postJson("/api/tickets/" + ticket + "/escalation", body);
    }

    private ResultActions escalation(long ticket) throws Exception
    {
        return mockMvc.perform(get("/api/tickets/" + ticket + "/escalation").header(HttpHeaders.AUTHORIZATION, agent));
    }

    private ResultActions postJson(String url, String body) throws Exception
    {
        return mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, agent)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Map<String, Object> outbox(long ticket)
    {
        return jdbc.queryForMap("SELECT status, attempts, next_attempt_at > now() AS in_future FROM azure_outbox "
                + "WHERE idempotency_key = ?", "TICKET-" + ticket);
    }

    private String outboxStatus(long ticket)
    {
        return (String) outbox(ticket).get("status");
    }

    private void makeDue(long ticket)
    {
        jdbc.update("UPDATE azure_outbox SET next_attempt_at = now() - interval '1 second' WHERE idempotency_key = ?",
                "TICKET-" + ticket);
    }

    private List<AuditLog> newAudit(AuditAction action)
    {
        return auditLogRepository.findAll().stream()
                .filter(a -> a.getId() > baseline && a.getAction() == action)
                .toList();
    }

    private String tokenFor(Role role)
    {
        String username = "esc_" + role.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        String mobile = String.format("+2094%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        User user = userRepository.save(new User(username, "$2a$10$unused", "Escalation Test",
                username + "@test.local", mobile, role));
        return "Bearer " + jwtService.generateToken(user).token();
    }
}
