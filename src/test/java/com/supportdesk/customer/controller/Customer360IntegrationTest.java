package com.supportdesk.customer.controller;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2 against the real database and the local seed (Sara CIF-100245 with 3 failed logins in the last hours,
 * Ahmed CIF-100101 with no failures). Each test uses its own new agent, so verifications never leak between tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Customer360IntegrationTest
{
    private static final String SARA = "CIF-100245";
    private static final String AHMED = "CIF-100101";

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbc;

    private String token;
    private long baseline;

    @BeforeEach
    void newAgent()
    {
        String username = "c360_" + UUID.randomUUID().toString().substring(0, 8);
        String mobile = String.format("+2098%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        User agent = userRepository.save(new User(username, "$2a$10$unused", "Customer 360 Test",
                username + "@test.local", mobile, Role.AGENT));
        token = "Bearer " + jwtService.generateToken(agent).token();
        baseline = auditLogRepository.findAll().stream().mapToLong(AuditLog::getId).max().orElse(0);
    }

    // ---- E2-S1 search ----

    @Test
    void searchFindsByCifNationalIdMobileOrTicketAndMasksTheMobile() throws Exception
    {
        search(SARA).andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].cif").value(SARA))
                .andExpect(jsonPath("$.results[0].fullName").value("Sara Mahmoud"))
                .andExpect(jsonPath("$.results[0].segment").value("PREMIUM"))
                .andExpect(jsonPath("$.results[0].maskedMobile").value("+20••••••4567"))
                .andExpect(jsonPath("$.results[0].enrollmentStatus").value("ENROLLED"))
                .andExpect(jsonPath("$.results[0].openTicketCount").isNumber())
                .andExpect(jsonPath("$.results[0].matchedBy").value("CIF"));
        search("29001011234567").andExpect(jsonPath("$.results[0].matchedBy").value("NATIONAL_ID"));
        search("01001234567").andExpect(jsonPath("$.results[0].matchedBy").value("MOBILE"));
        search("#2001").andExpect(jsonPath("$.results[0].matchedBy").value("TICKET"))
                .andExpect(jsonPath("$.results[0].cif").value(SARA));     // the ticket's owner
    }

    @Test
    void noMatchSaysHowToFixTheSearch() throws Exception
    {
        search("CIF-000000").andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isEmpty())
                .andExpect(jsonPath("$.message").value("No match. Check the digits and the ID type."));
    }

    @Test
    void searchIsAuditedWithoutTheSearchText() throws Exception
    {
        search("29001011234567").andExpect(status().isOk());

        AuditLog log = onlyNew(AuditAction.CUSTOMER_SEARCHED);
        assertThat(log.getAfterState()).contains("NATIONAL_ID").doesNotContain("29001011234567");
    }

    @Test
    void the31stSearchIn10MinutesIs429() throws Exception
    {
        for (int i = 0; i < 30; i++) {
            search("CIF-000000").andExpect(status().isOk());
        }
        search("CIF-000000").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_SEARCHES"));
    }

    // ---- E2-S2 verification gate ----

    @Test
    void nothingShowsBeforeVerificationAndEndingItLocksAgain() throws Exception
    {
        for (String path : List.of("", "/diagnosis", "/activity", "/tickets")) {
            mockMvc.perform(get("/api/customers/" + SARA + path).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("CALLER_NOT_VERIFIED"));
        }

        verify(SARA, "DATE_OF_BIRTH").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Two checks must pass."));
        verify(SARA, "DATE_OF_BIRTH", "MOBILE_ON_FILE").andExpect(status().isOk());
        assertThat(onlyNew(AuditAction.CALLER_VERIFIED).getAfterState()).contains("DATE_OF_BIRTH", "MOBILE_ON_FILE");

        open(SARA, "").andExpect(status().isOk())
                .andExpect(jsonPath("$.verifiedAt").isNotEmpty());

        mockMvc.perform(post("/api/customers/" + SARA + "/end-call").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        open(SARA, "").andExpect(status().isForbidden());
    }

    @Test
    void callerFailedIsAuditedAndShowsNothing() throws Exception
    {
        verify(SARA, "DATE_OF_BIRTH", "MOBILE_ON_FILE").andExpect(status().isOk());

        mockMvc.perform(post("/api/customers/" + SARA + "/verification-failed").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        onlyNew(AuditAction.CALLER_VERIFICATION_FAILED);
        open(SARA, "").andExpect(status().isForbidden());
    }

    // ---- E2-S3 masking and reveal ----

    @Test
    void verifiedCustomerStaysMaskedAndRevealIsAuditedByFieldName() throws Exception
    {
        verify(SARA, "DATE_OF_BIRTH", "MOBILE_ON_FILE");

        open(SARA, "").andExpect(jsonPath("$.maskedNationalId").value("••••••••••4567"))
                .andExpect(jsonPath("$.maskedMobile").value("+20••••••4567"));
        onlyNew(AuditAction.CUSTOMER_DATA_VIEWED);

        mockMvc.perform(post("/api/customers/" + SARA + "/reveal").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"field\":\"MOBILE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.field").value("MOBILE"))
                .andExpect(jsonPath("$.value").value("+201001234567"));
        assertThat(onlyNew(AuditAction.CUSTOMER_FIELD_REVEALED).getAfterState())
                .contains("MOBILE").doesNotContain("201001234567");
    }

    // ---- E2-S4 diagnosis ----

    @Test
    void diagnosisNamesTheMostFrequentFailureAndTheServicesToRuleOut() throws Exception
    {
        verify(SARA, "DATE_OF_BIRTH", "MOBILE_ON_FILE");

        open(SARA, "/diagnosis").andExpect(status().isOk())
                .andExpect(jsonPath("$.hasFailures").value(true))
                .andExpect(jsonPath("$.topFailure.count").value(3))
                .andExpect(jsonPath("$.topFailure.eventType").value("LOGIN"))
                .andExpect(jsonPath("$.topFailure.errorCode").value("ERR_AUTH_503"))
                .andExpect(jsonPath("$.topFailure.service").value("AUTHENTICATION"))
                .andExpect(jsonPath("$.topFailure.firstAt").isNotEmpty())
                .andExpect(jsonPath("$.summary").value("3 failed LOGIN events with ERR_AUTH_503 on AUTHENTICATION in the last 6 hours."))
                .andExpect(jsonPath("$.servicesWithoutFailures", hasItems("OTP", "PAYMENTS", "TRANSFERS", "MOBILE_APP")))
                .andExpect(jsonPath("$.servicesWithoutFailures", not(hasItem("AUTHENTICATION"))));
    }

    @Test
    void noFailuresSuggestsDeviceOrNetwork() throws Exception
    {
        verify(AHMED, "DATE_OF_BIRTH", "MOBILE_ON_FILE");

        open(AHMED, "/diagnosis").andExpect(jsonPath("$.hasFailures").value(false))
                .andExpect(jsonPath("$.summary").value("No failures in the last 6 hours. Check the customer's device or network."))
                .andExpect(jsonPath("$.servicesWithoutFailures.length()").value(5));
    }

    // ---- E2-S5 activity ----

    @Test
    void activityDefaultsToLast6HoursFailuresOnlyAndChipsFilter() throws Exception
    {
        verify(SARA, "DATE_OF_BIRTH", "MOBILE_ON_FILE");

        open(SARA, "/activity").andExpect(jsonPath("$.range").value("LAST_6_HOURS"))
                .andExpect(jsonPath("$.failuresOnly").value(true))
                .andExpect(jsonPath("$.events.length()").value(3))
                .andExpect(jsonPath("$.events[*].failure", everyItem(org.hamcrest.Matchers.is(true))))
                .andExpect(jsonPath("$.counts.byService.AUTHENTICATION").value(3));

        open(SARA, "/activity?failuresOnly=false&platform=WEB").andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].eventType").value("TRANSFER"))
                .andExpect(jsonPath("$.counts.total").value(5));                    // chips count before the chip filter

        open(SARA, "/activity?range=LAST_7_DAYS&failuresOnly=false").andExpect(jsonPath("$.events.length()").value(7));
        open(SARA, "/activity?range=CUSTOM").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CUSTOM_RANGE_REQUIRED"));
    }

    @Test
    void emptyActivityExplainsHowToWidenTheFilter() throws Exception
    {
        verify(AHMED, "DATE_OF_BIRTH", "MOBILE_ON_FILE");

        open(AHMED, "/activity").andExpect(jsonPath("$.events").isEmpty())
                .andExpect(jsonPath("$.emptyMessage").isNotEmpty());
    }

    // ---- E2-S6 tickets ----

    @Test
    void customerTicketsShowOriginAndStatusAndFlagRepeatCallers() throws Exception
    {
        verify(SARA, "DATE_OF_BIRTH", "MOBILE_ON_FILE");
        Integer recent = jdbc.queryForObject("select count(*) from tickets t join customers c on c.id = t.customer_id "
                + "where c.cif = ? and t.created_at > now() - interval '20 days'", Integer.class, SARA);

        open(SARA, "/tickets").andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets[*].number", hasItem("#2001")))
                .andExpect(jsonPath("$.tickets[0].origin").isNotEmpty())
                .andExpect(jsonPath("$.tickets[0].status").isNotEmpty())
                .andExpect(jsonPath("$.repeatCaller").value(recent > 1));
    }


    private ResultActions search(String q) throws Exception
    {
        return mockMvc.perform(get("/api/customers/search").param("q", q).header(HttpHeaders.AUTHORIZATION, token));
    }

    private ResultActions verify(String cif, String... checks) throws Exception
    {
        String body = "{\"passedChecks\":[" + String.join(",", java.util.Arrays.stream(checks).map(c -> "\"" + c + "\"").toList()) + "]}";
        return mockMvc.perform(post("/api/customers/" + cif + "/verify").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions open(String cif, String path) throws Exception
    {
        return mockMvc.perform(get("/api/customers/" + cif + path).header(HttpHeaders.AUTHORIZATION, token));
    }

    private AuditLog onlyNew(AuditAction action)
    {
        List<AuditLog> rows = auditLogRepository.findAll().stream()
                .filter(log -> log.getId() > baseline && log.getAction() == action)
                .toList();
        assertThat(rows).as("new " + action + " rows").hasSize(1);
        return rows.get(0);
    }
}
