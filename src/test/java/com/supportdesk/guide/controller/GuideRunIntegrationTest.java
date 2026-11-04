package com.supportdesk.guide.controller;

import com.jayway.jsonpath.JsonPath;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.repository.AuditLogRepository;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.service.JwtService;
import com.supportdesk.guide.enums.GuideVersionState;
import com.supportdesk.guide.repository.GuideVersionRepository;
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
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** E4-S1 and E4-S2 against the real database and the local guide seed. Each test uses a new agent and new tickets. */
@SpringBootTest
@AutoConfigureMockMvc
class GuideRunIntegrationTest
{
    // From the local seed: Sara's latest login failure is ERR_AUTH_503; Mai has none, so her ticket gets
    // the Login Failed category's default code, which must not count as hers.
    private static final String SARA = "CIF-100245";
    private static final String MAI = "CIF-100311";
    private static final String SARAS_LOGIN_ERROR = "ERR_AUTH_503";

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private GuideVersionRepository guideVersions;

    private String token;

    @BeforeEach
    void newVerifiedAgent() throws Exception
    {
        String username = "e4_" + UUID.randomUUID().toString().substring(0, 8);
        String mobile = String.format("+2098%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        User agent = userRepository.save(new User(username, "$2a$10$unused", "Guide Test",
                username + "@test.local", mobile, Role.AGENT));
        token = "Bearer " + jwtService.generateToken(agent).token();
        verify(SARA);
    }

    // ---- E4-S1 ----

    @Test
    void theErrorCodeMatchComesFirstAndOnlyLiveVersionsAreOffered() throws Exception
    {
        long ticket = createTicket(SARA, "LOGIN_FAILED", "MOBILE");

        recommended(ticket).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Login failed: server error"))
                .andExpect(jsonPath("$[0].liveVersion").value(2))
                .andExpect(jsonPath("$[0].matchedBy").value("ERROR_CODE"))
                .andExpect(jsonPath("$[0].matchLabel").value("Matches error " + SARAS_LOGIN_ERROR))
                .andExpect(jsonPath("$[1].title").value("Login failed: general checks"))
                .andExpect(jsonPath("$[1].matchedBy").value("CATEGORY"));
        ticket(ticket).andExpect(jsonPath("$.nextAction").value("START_GUIDE"));
    }

    @Test
    void aCategoryDefaultErrorCodeIsNotTheCustomersSoItNeitherMatchesNorHints() throws Exception
    {
        verify(MAI);
        long ticket = createTicket(MAI, "LOGIN_FAILED", "MOBILE");
        ticket(ticket).andExpect(jsonPath("$.errorCode").value(SARAS_LOGIN_ERROR));   // the category default

        recommended(ticket).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].matchedBy", everyItem(is("CATEGORY"))));

        long run = startRun(ticket, guideId("Login failed: server error"));
        answer(run, "app-version", "yes").andExpect(jsonPath("$.currentStep.stepKey").value("other-device"))
                .andExpect(jsonPath("$.currentStep.hint").doesNotExist());
    }

    @Test
    void withNoGuideForThePlatformOrCategoryTheNextStepIsEscalate() throws Exception
    {
        for (long ticket : List.of(createTicket(SARA, "APP_CRASHING", "MOBILE"),
                                   createTicket(SARA, "LOGIN_FAILED", "BRANCH"))) {
            recommended(ticket).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
            ticket(ticket).andExpect(jsonPath("$.nextAction").value("ESCALATE"))
                    .andExpect(jsonPath("$.nextActionLabel").value("Escalate"));
        }
    }

    @Test
    void recommendingForAnUnknownTicketIs404() throws Exception
    {
        recommended(999_999_999L).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }

    // ---- E4-S2 ----

    @Test
    void startingMovesTheTicketToTroubleshootingAndAuditsTheVersion() throws Exception
    {
        long ticket = createTicket(SARA, "LOGIN_FAILED", "MOBILE");

        start(ticket, guideId("Login failed: server error")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.path", hasSize(0)))
                .andExpect(jsonPath("$.currentStep.stepKey").value("app-version"))
                .andExpect(jsonPath("$.currentStep.stepNumber").value(1))
                .andExpect(jsonPath("$.currentStep.maxSteps").value(3))
                .andExpect(jsonPath("$.currentStep.options", hasSize(2)));

        ticket(ticket).andExpect(jsonPath("$.status").value("TROUBLESHOOTING"));
        assertThat(auditRows(ticket, AuditAction.GUIDE_STARTED)).singleElement()
                .satisfies(log -> assertThat(log.getAfterState())
                        .contains("\"version\":2").contains("Login failed: server error"));
    }

    @Test
    void aTicketHasOneUnfinishedRunAtATime() throws Exception
    {
        long ticket = createTicket(SARA, "LOGIN_FAILED", "MOBILE");
        long guide = guideId("Login failed: server error");
        start(ticket, guide).andExpect(status().isCreated());

        start(ticket, guide).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GUIDE_RUN_ALREADY_ACTIVE"));
    }

    @Test
    void aTicketMovedOnByHandCanStartAGuideAgain() throws Exception
    {
        long ticket = createTicket(SARA, "LOGIN_FAILED", "MOBILE");
        long guide = guideId("Login failed: server error");
        long first = startRun(ticket, guide);
        move(ticket, "RESOLVED").andExpect(status().isOk());
        move(ticket, "REOPENED").andExpect(status().isOk());

        start(ticket, guide).andExpect(status().isCreated());
        run(first).andExpect(jsonPath("$.status").value("COMPLETED"));
        answer(first, "app-version", "yes").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GUIDE_RUN_NOT_ACTIVE"));
    }

    @Test
    void onlyAGuideThatFitsTheTicketCanBeStarted() throws Exception
    {
        start(createTicket(SARA, "LOGIN_FAILED", "MOBILE"), guideId("Payment failed"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GUIDE_DOES_NOT_FIT"));
        start(createTicket(SARA, "LOGIN_FAILED", "MOBILE"), 999_999_999L).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GUIDE_NOT_FOUND"));
    }

    @Test
    void aResolvedTicketCannotStartAGuideAndNothingIsRecorded() throws Exception
    {
        long ticket = createTicket(SARA, "LOGIN_FAILED", "MOBILE");
        move(ticket, "RESOLVED").andExpect(status().isOk());

        start(ticket, guideId("Login failed: server error")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        assertThat(auditRows(ticket, AuditAction.GUIDE_STARTED)).isEmpty();
    }

    @Test
    void answersFollowThePublishedRouteWithTheErrorCodeHintToTheOutcome() throws Exception
    {
        long run = startServerErrorRun();

        answer(run, "app-version", "yes").andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep.stepKey").value("other-device"))
                .andExpect(jsonPath("$.currentStep.stepNumber").value(2))
                .andExpect(jsonPath("$.currentStep.hint.answerKey").value("no"))
                .andExpect(jsonPath("$.path[0].answerLabel").value("Yes"));
        answer(run, "other-device", "no")
                .andExpect(jsonPath("$.currentStep.stepKey").value("status-page"))
                .andExpect(jsonPath("$.currentStep.hint").doesNotExist());
        answer(run, "status-page", "no").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUTCOME_REACHED"))
                .andExpect(jsonPath("$.currentStep").doesNotExist())
                .andExpect(jsonPath("$.outcome.type").value("ESCALATE"))
                .andExpect(jsonPath("$.outcome.customerScript", not(emptyString())))
                .andExpect(jsonPath("$.path", hasSize(3)));

        answer(run, "status-page", "no").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_GUIDE_ROUTE"));
    }

    @Test
    void theServerOnlyAcceptsAnAnswerToTheCurrentStep() throws Exception
    {
        long run = startServerErrorRun();

        answer(run, "status-page", "no").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_GUIDE_ROUTE"));
        answer(run, "app-version", "maybe").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_GUIDE_ROUTE"));
        run(run).andExpect(jsonPath("$.path", hasSize(0)));
    }

    @Test
    void previousReturnsToTheStepWithItsAnswerClearedAndRepeatingItChangesNothing() throws Exception
    {
        long run = startServerErrorRun();
        answer(run, "app-version", "yes");
        answer(run, "other-device", "no");
        answer(run, "status-page", "yes").andExpect(jsonPath("$.status").value("OUTCOME_REACHED"));

        back(run, 3).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.currentStep.stepKey").value("status-page"));
        back(run, 2).andExpect(jsonPath("$.currentStep.stepKey").value("other-device"))
                .andExpect(jsonPath("$.path", hasSize(1)));
        back(run, 2).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep.stepKey").value("other-device"));
        back(run, 1).andExpect(jsonPath("$.currentStep.stepKey").value("app-version"))
                .andExpect(jsonPath("$.path", hasSize(0)));
        back(run, 0).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void anUnknownRunIs404() throws Exception
    {
        run(999_999_999L).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GUIDE_RUN_NOT_FOUND"));
    }


    private long startServerErrorRun() throws Exception
    {
        return startRun(createTicket(SARA, "LOGIN_FAILED", "MOBILE"), guideId("Login failed: server error"));
    }

    private long startRun(long ticket, long guide) throws Exception
    {
        String body = start(ticket, guide).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long guideId(String title)
    {
        return guideVersions.findByState(GuideVersionState.LIVE).stream()
                .filter(version -> version.getGuide().getTitle().equals(title))
                .findFirst().orElseThrow().getGuide().getId();
    }

    private void verify(String cif) throws Exception
    {
        mockMvc.perform(post("/api/customers/" + cif + "/verify").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passedChecks\":[\"DATE_OF_BIRTH\",\"MOBILE_ON_FILE\"]}"))
                .andExpect(status().isOk());
    }

    private long createTicket(String cif, String category, String platform) throws Exception
    {
        String body = """
                {"cif":"%s","categoryCode":"%s","priority":"HIGH","description":"Cannot log in to the app.",
                 "platform":"%s","allowDuplicate":true}
                """.formatted(cif, category, platform);
        String json = mockMvc.perform(post("/api/tickets").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(JsonPath.<String>read(json, "$.number").substring(1));
    }

    private ResultActions recommended(long ticket) throws Exception
    {
        return mockMvc.perform(get("/api/guides/recommended").param("ticket", String.valueOf(ticket))
                .header(HttpHeaders.AUTHORIZATION, token));
    }

    private ResultActions ticket(long ticket) throws Exception
    {
        return mockMvc.perform(get("/api/tickets/" + ticket).header(HttpHeaders.AUTHORIZATION, token));
    }

    private ResultActions move(long ticket, String to) throws Exception
    {
        return mockMvc.perform(patch("/api/tickets/" + ticket + "/status").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + to + "\"}"));
    }

    private ResultActions start(long ticket, long guide) throws Exception
    {
        return mockMvc.perform(post("/api/guide-runs").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticketNumber\":" + ticket + ",\"guideId\":" + guide + "}"));
    }

    private ResultActions run(long run) throws Exception
    {
        return mockMvc.perform(get("/api/guide-runs/" + run).header(HttpHeaders.AUTHORIZATION, token));
    }

    private ResultActions answer(long run, String step, String answer) throws Exception
    {
        return mockMvc.perform(post("/api/guide-runs/" + run + "/answers").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stepKey\":\"" + step + "\",\"answerKey\":\"" + answer + "\"}"));
    }

    private ResultActions back(long run, int position) throws Exception
    {
        return mockMvc.perform(delete("/api/guide-runs/" + run + "/answers/" + position)
                .header(HttpHeaders.AUTHORIZATION, token));
    }

    private List<AuditLog> auditRows(long ticket, AuditAction action)
    {
        return auditLogRepository.findByEntityAndEntityIdOrderByCreatedAtDescIdDesc("TICKET", String.valueOf(ticket))
                .stream().filter(log -> log.getAction() == action).toList();
    }
}
