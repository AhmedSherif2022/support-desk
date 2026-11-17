package com.supportdesk.guide.controller;

import com.jayway.jsonpath.JsonPath;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.service.JwtService;
import com.supportdesk.guide.entity.GuideRun;
import com.supportdesk.guide.entity.GuideVersion;
import com.supportdesk.guide.enums.GuideVersionState;
import com.supportdesk.guide.repository.GuideRunRepository;
import com.supportdesk.guide.repository.GuideVersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E4-S4 against the real database and the local guide seed. "Login failed: server error" ships with a retired v1,
 * the live v2 and a v3 draft; these tests publish v3 or retire v2 mid-run, and put the seed back afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GuideVersionPinningIntegrationTest
{
    private static final String SARA = "CIF-100245";
    private static final String GUIDE = "Login failed: server error";
    private static final String V2_QUESTION = "Is the app on version 5.2 or later?";
    private static final String V3_QUESTION = "Draft: is the app on version 6.0 or later?";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private GuideVersionRepository guideVersions;
    @Autowired private GuideRunRepository guideRuns;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactions;

    private String token;
    private long guideId;

    @BeforeEach
    void newVerifiedAgent() throws Exception
    {
        String username = "e4s4_" + UUID.randomUUID().toString().substring(0, 8);
        String mobile = String.format("+2098%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        User agent = userRepository.save(new User(username, "$2a$10$unused", "Pinning Test",
                username + "@test.local", mobile, Role.AGENT));
        token = "Bearer " + jwtService.generateToken(agent).token();
        verify(SARA);
        // Tests share the local database: put the seed back first in case an earlier run died mid-test.
        guideId = jdbc.queryForObject("SELECT id FROM guides WHERE title = ?", Long.class, GUIDE);
        restoreTheSeed();
        assertThat(liveVersion().getVersionNumber()).as("the seed's live version").isEqualTo(2);
    }

    /** Whatever a test did to the seed's versions, the next one finds v2 live and v3 a draft again. */
    @AfterEach
    void restoreTheSeed()
    {
        setState(guideId, 3, GuideVersionState.DRAFT);
        setState(guideId, 2, GuideVersionState.LIVE);
    }

    @Test
    void aRunStartsOnTheLiveVersionAndKeepsItWhenANewerOneIsPublished() throws Exception
    {
        long run = startRun(createTicket(), guideId);
        run(run).andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.currentStep.question").value(V2_QUESTION));

        publish(3);                                                              // a new version goes live mid-call
        assertThat(liveVersion().getVersionNumber()).isEqualTo(3);

        // The open run still asks v2's questions and follows v2's routes: "yes" leads on to other-device (v3 would end).
        run(run).andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.currentStep.question").value(V2_QUESTION))
                .andExpect(jsonPath("$.currentStep.maxSteps").value(3));
        answer(run, "app-version", "yes").andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.currentStep.stepKey").value("other-device"))
                .andExpect(jsonPath("$.path[0].question").value(V2_QUESTION));
        answer(run, "other-device", "no").andExpect(jsonPath("$.currentStep.stepKey").value("status-page"));
        answer(run, "status-page", "no").andExpect(jsonPath("$.status").value("OUTCOME_REACHED"))
                .andExpect(jsonPath("$.outcome.key").value("escalate-sre"))
                .andExpect(jsonPath("$.path", hasSize(3)));

        // A run started now gets the new live version, so the two sit side by side on different versions.
        long newRun = startRun(createTicket(), guideId);
        run(newRun).andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.currentStep.question").value(V3_QUESTION));
        run(run).andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void aSavedRunKeepsItsVersionAndStepsAfterTheGuideIsRetired() throws Exception
    {
        long run = startRun(createTicket(), guideId);
        answer(run, "app-version", "yes");
        answer(run, "other-device", "yes").andExpect(jsonPath("$.status").value("OUTCOME_REACHED"));
        complete(run).andExpect(status().isOk());

        setState(guideId, 2, GuideVersionState.RETIRED);                         // no live version at all now
        assertThat(guideVersions.findByGuideIdAndState(guideId, GuideVersionState.LIVE)).isEmpty();

        run(run).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.guideTitle").value(GUIDE))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.path", hasSize(2)))
                .andExpect(jsonPath("$.path[0].stepKey").value("app-version"))
                .andExpect(jsonPath("$.path[0].question").value(V2_QUESTION))
                .andExpect(jsonPath("$.path[1].stepKey").value("other-device"))
                .andExpect(jsonPath("$.outcome.key").value("clear-app-data"));
        start(createTicket(), guideId).andExpect(status().isNotFound())           // but nothing new starts on it
                .andExpect(jsonPath("$.code").value("GUIDE_NOT_FOUND"));
    }

    @Test
    void aRunCannotBeMovedToAnotherVersion() throws Exception
    {
        long runId = startRun(createTicket(), guideId);
        long pinned = versionIdOf(runId);
        GuideVersion other = guideVersions.findAll().stream()
                .filter(version -> version.getId() != pinned).findFirst().orElseThrow();

        // No code path offers it: the entity has no setter, and the column is not updatable even if the field is forced.
        assertThat(Arrays.stream(GuideRun.class.getMethods()).map(java.lang.reflect.Method::getName))
                .noneMatch(name -> name.equals("setPinnedVersion"));
        transactions.executeWithoutResult(status -> {
            GuideRun run = guideRuns.findById(runId).orElseThrow();
            ReflectionTestUtils.setField(run, "pinnedVersion", other);
            guideRuns.saveAndFlush(run);
        });

        assertThat(versionIdOf(runId)).isEqualTo(pinned);
        run(runId).andExpect(jsonPath("$.version").value(2));
    }

    private GuideVersion liveVersion()
    {
        return guideVersions.findByState(GuideVersionState.LIVE).stream()
                .filter(version -> version.getGuide().getTitle().equals(GUIDE))
                .findFirst().orElseThrow();
    }

    private void publish(int versionNumber)
    {
        setState(guideId, 2, GuideVersionState.RETIRED);                         // one live version per guide
        setState(guideId, versionNumber, GuideVersionState.LIVE);
    }

    private void setState(long guide, int versionNumber, GuideVersionState state)
    {
        // A draft was never published; a live or retired version keeps (or gets) its publish time.
        jdbc.update("UPDATE guide_versions SET state = ?, published_at = CASE WHEN ? = 'DRAFT' THEN NULL "
                + "ELSE COALESCE(published_at, now()) END WHERE guide_id = ? AND version_number = ?",
                state.name(), state.name(), guide, versionNumber);
    }

    private long versionIdOf(long run)
    {
        return jdbc.queryForObject("SELECT guide_version_id FROM troubleshooting_sessions WHERE id = ?", Long.class, run);
    }

    private long startRun(long ticket, long guide) throws Exception
    {
        String body = start(ticket, guide).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void verify(String cif) throws Exception
    {
        mockMvc.perform(post("/api/customers/" + cif + "/verify").header(HttpHeaders.AUTHORIZATION, token)
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
        String json = mockMvc.perform(post("/api/tickets").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(JsonPath.<String>read(json, "$.number").substring(1));
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

    private ResultActions complete(long run) throws Exception
    {
        return mockMvc.perform(post("/api/guide-runs/" + run + "/complete").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content("{}"));
    }
}
