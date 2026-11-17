package com.supportdesk.guide.controller;

import com.jayway.jsonpath.JsonPath;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.repository.AuditLogRepository;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.service.JwtService;
import com.supportdesk.guide.entity.GuideFeedback;
import com.supportdesk.guide.enums.GuideFeedbackStatus;
import com.supportdesk.guide.enums.GuideVersionState;
import com.supportdesk.guide.repository.GuideFeedbackRepository;
import com.supportdesk.guide.repository.GuideVersionRepository;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** E4-S5 against the real database and the local guide seed. Each test uses a new agent, a new admin and new tickets. */
@SpringBootTest
@AutoConfigureMockMvc
class GuideFeedbackIntegrationTest
{
    private static final String SARA = "CIF-100245";
    private static final String GUIDE = "Login failed: server error";

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private GuideFeedbackRepository feedbackRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private GuideVersionRepository guideVersions;
    @Autowired private JdbcTemplate jdbc;

    private User agent;
    private String agentToken;
    private String adminToken;

    @BeforeEach
    void newVerifiedAgentAndAnAdmin() throws Exception
    {
        agent = newUser(Role.AGENT, "Flag Agent");
        agentToken = "Bearer " + jwtService.generateToken(agent).token();
        adminToken = "Bearer " + jwtService.generateToken(newUser(Role.GUIDE_ADMIN, "Flag Admin")).token();
        verify(SARA);
    }

    // ---- flagging ----

    @Test
    void aFinishedRunCanFlagAStepOnItsPathAndTheReportIsOpenAndAudited() throws Exception
    {
        long ticket = createTicket();
        long run = completedEscalateRun(ticket);                                 // app-version, other-device, status-page

        String json = flag(run, "other-device", "  The hint contradicts the question.  ").andExpect(status().isCreated())
                .andExpect(jsonPath("$.guideRunId").value(run))
                .andExpect(jsonPath("$.ticketNumber").value("#" + ticket))
                .andExpect(jsonPath("$.guideTitle").value(GUIDE))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.stepKey").value("other-device"))
                .andExpect(jsonPath("$.question").value("Can the customer log in on the web or on another device?"))
                .andExpect(jsonPath("$.agentUsername").value(agent.getUsername()))
                .andExpect(jsonPath("$.agentName").value("Flag Agent"))
                .andExpect(jsonPath("$.comment").value("The hint contradicts the question."))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();
        long feedbackId = ((Number) JsonPath.read(json, "$.id")).longValue();

        // Saved against the exact session, the pinned version and the logged-in agent, OPEN.
        Map<String, Object> row = jdbc.queryForMap("SELECT session_id, guide_version_id, agent_id, status "
                + "FROM guide_feedback WHERE id = ?", feedbackId);
        assertThat(((Number) row.get("session_id")).longValue()).isEqualTo(run);
        assertThat(((Number) row.get("guide_version_id")).longValue()).isEqualTo(liveVersionId());
        assertThat(((Number) row.get("agent_id")).longValue()).isEqualTo(agent.getId());
        assertThat(row.get("status")).isEqualTo("OPEN");
        assertThat(feedbackRepository.findById(feedbackId)).get()
                .extracting(GuideFeedback::getStatus).isEqualTo(GuideFeedbackStatus.OPEN);

        assertThat(auditRows(ticket, AuditAction.GUIDE_STEP_FLAGGED)).singleElement()
                .satisfies(log -> {
                    assertThat(log.getUserId()).isEqualTo(agent.getId());
                    assertThat(log.getAfterState())
                            .contains("\"feedbackId\":" + feedbackId)
                            .contains("\"guideRunId\":" + run)
                            .contains("\"version\":2")
                            .contains("\"stepKey\":\"other-device\"")
                            .doesNotContain("contradicts");                 // the comment is not audited
                });
    }

    @Test
    void aBlankCommentIs400WithTheFieldNamed() throws Exception
    {
        long ticket = createTicket();
        long run = completedEscalateRun(ticket);

        for (String comment : List.of("\"\"", "\"   \"", "null")) {
            mockMvc.perform(post("/api/guide-runs/" + run + "/flags").header(HttpHeaders.AUTHORIZATION, agentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"stepKey\":\"app-version\",\"comment\":" + comment + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors.comment").exists());
        }
        flag(run, "  ", "Fine comment").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.stepKey").exists());
        nothingWasRecorded(ticket);
    }

    @Test
    void anUnfinishedRunCannotBeFlagged() throws Exception
    {
        long ticket = createTicket();
        long run = startRun(ticket);
        answer(run, "app-version", "yes");
        answer(run, "other-device", "no");
        answer(run, "status-page", "no").andExpect(jsonPath("$.status").value("OUTCOME_REACHED"));   // but not finished

        flag(run, "app-version", "Unclear").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GUIDE_RUN_STILL_ACTIVE"));
        nothingWasRecorded(ticket);
    }

    @Test
    void onlyAStepTakenDuringTheRunCanBeFlagged() throws Exception
    {
        long ticket = createTicket();
        long run = startRun(ticket);
        answer(run, "app-version", "yes");
        answer(run, "other-device", "yes").andExpect(jsonPath("$.status").value("OUTCOME_REACHED"));   // skips status-page
        complete(run).andExpect(status().isOk());

        flag(run, "status-page", "Unclear").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GUIDE_STEP_NOT_TAKEN"));
        flag(run, "no-such-step", "Unclear").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GUIDE_STEP_NOT_TAKEN"));
        flag(run, "clear-app-data", "Unclear").andExpect(status().isUnprocessableEntity());          // outcome, not a step
        nothingWasRecorded(ticket);

        flag(run, "app-version", "Fine").andExpect(status().isCreated());
    }

    @Test
    void flaggingAnUnknownRunIs404() throws Exception
    {
        flag(999_999_999L, "app-version", "Unclear").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GUIDE_RUN_NOT_FOUND"));
    }

    // ---- admin queue ----

    @Test
    void theAdminReadsTheQueueNewestFirstInPages() throws Exception
    {
        long run = completedEscalateRun(createTicket());
        long first = flagged(run, "app-version", "First");
        long second = flagged(run, "other-device", "Second");
        long third = flagged(run, "status-page", "Third");

        String page = queue(adminToken, "?size=2").andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(third))
                .andExpect(jsonPath("$.items[0].stepKey").value("status-page"))
                .andExpect(jsonPath("$.items[0].question").value("Does the status page show a login incident right now?"))
                .andExpect(jsonPath("$.items[0].guideTitle").value(GUIDE))
                .andExpect(jsonPath("$.items[0].version").value(2))
                .andExpect(jsonPath("$.items[0].agentUsername").value(agent.getUsername()))
                .andExpect(jsonPath("$.items[0].comment").value("Third"))
                .andExpect(jsonPath("$.items[0].status").value("OPEN"))
                .andExpect(jsonPath("$.items[1].id").value(second))
                .andReturn().getResponse().getContentAsString();
        long total = ((Number) JsonPath.read(page, "$.totalItems")).longValue();
        assertThat(total).isGreaterThanOrEqualTo(3);
        assertThat(((Number) JsonPath.read(page, "$.totalPages")).longValue()).isEqualTo((total + 1) / 2);

        queue(adminToken, "?size=2&page=1").andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.items[0].id").value(first));
    }

    @Test
    void theQueueSortsOnlyByItsOwnColumns() throws Exception
    {
        for (String sort : List.of("run.ticket.customer.nationalId,asc", "agent.passwordHash", "bogus")) {
            queue(adminToken, "?sort=" + sort).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("REQUEST_ERROR"));
        }
        queue(adminToken, "?sort=status,asc").andExpect(status().isOk());
    }

    @Test
    void anAgentGets403OnTheQueueAndNoTokenGets401() throws Exception
    {
        queue(agentToken, "").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/admin/guide-feedback")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void anAdminCannotFlagASteps() throws Exception
    {
        long run = completedEscalateRun(createTicket());
        mockMvc.perform(post("/api/guide-runs/" + run + "/flags").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stepKey\":\"app-version\",\"comment\":\"Unclear\"}"))
                .andExpect(status().isForbidden());
    }

    // ---- helpers ----

    private void nothingWasRecorded(long ticket)
    {
        assertThat(auditRows(ticket, AuditAction.GUIDE_STEP_FLAGGED)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guide_feedback f JOIN troubleshooting_sessions s "
                + "ON s.id = f.session_id JOIN tickets t ON t.id = s.ticket_id WHERE t.ticket_number = ?",
                Long.class, ticket)).isZero();
    }

    private User newUser(Role role, String fullName)
    {
        String username = "e4s5_" + UUID.randomUUID().toString().substring(0, 8);
        String mobile = String.format("+2098%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        return userRepository.save(new User(username, "$2a$10$unused", fullName, username + "@test.local", mobile, role));
    }

    private long liveVersionId()
    {
        return guideVersions.findByState(GuideVersionState.LIVE).stream()
                .filter(version -> version.getGuide().getTitle().equals(GUIDE))
                .findFirst().orElseThrow().getId();
    }

    private long guideId()
    {
        return guideVersions.findByState(GuideVersionState.LIVE).stream()
                .filter(version -> version.getGuide().getTitle().equals(GUIDE))
                .findFirst().orElseThrow().getGuide().getId();
    }

    private long completedEscalateRun(long ticket) throws Exception
    {
        long run = startRun(ticket);
        answer(run, "app-version", "yes");
        answer(run, "other-device", "no");
        answer(run, "status-page", "no").andExpect(jsonPath("$.status").value("OUTCOME_REACHED"));
        complete(run).andExpect(status().isOk()).andExpect(jsonPath("$.outcomeType").value("ESCALATE"));
        return run;
    }

    private long flagged(long run, String step, String comment) throws Exception
    {
        String json = flag(run, step, comment).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private long startRun(long ticket) throws Exception
    {
        String json = mockMvc.perform(post("/api/guide-runs").header(HttpHeaders.AUTHORIZATION, agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketNumber\":" + ticket + ",\"guideId\":" + guideId() + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private void verify(String cif) throws Exception
    {
        mockMvc.perform(post("/api/customers/" + cif + "/verify").header(HttpHeaders.AUTHORIZATION, agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passedChecks\":[\"DATE_OF_BIRTH\",\"MOBILE_ON_FILE\"]}"))
                .andExpect(status().isOk());
    }

    private long createTicket() throws Exception
    {
        String body = """
                {"cif":"%s","categoryCode":"LOGIN_FAILED","priority":"HIGH","description":"Cannot log in to the app.",
                 "platform":"MOBILE","allowDuplicate":true}
                """.formatted(SARA);
        String json = mockMvc.perform(post("/api/tickets").header(HttpHeaders.AUTHORIZATION, agentToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(JsonPath.<String>read(json, "$.number").substring(1));
    }

    private ResultActions answer(long run, String step, String answer) throws Exception
    {
        return mockMvc.perform(post("/api/guide-runs/" + run + "/answers").header(HttpHeaders.AUTHORIZATION, agentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stepKey\":\"" + step + "\",\"answerKey\":\"" + answer + "\"}"));
    }

    private ResultActions complete(long run) throws Exception
    {
        return mockMvc.perform(post("/api/guide-runs/" + run + "/complete").header(HttpHeaders.AUTHORIZATION, agentToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    private ResultActions flag(long run, String step, String comment) throws Exception
    {
        return mockMvc.perform(post("/api/guide-runs/" + run + "/flags").header(HttpHeaders.AUTHORIZATION, agentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stepKey\":\"" + step + "\",\"comment\":\"" + comment + "\"}"));
    }

    private ResultActions queue(String token, String query) throws Exception
    {
        return mockMvc.perform(get("/api/admin/guide-feedback" + query).header(HttpHeaders.AUTHORIZATION, token));
    }

    private List<AuditLog> auditRows(long ticket, AuditAction action)
    {
        return auditLogRepository.findByEntityAndEntityIdOrderByCreatedAtDescIdDesc("TICKET", String.valueOf(ticket))
                .stream().filter(log -> log.getAction() == action).toList();
    }
}
