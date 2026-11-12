package com.supportdesk.audit.controller;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.audit.repository.AuditLogRepository;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.service.JwtService;
import com.supportdesk.auth.service.PasswordDigest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.IllegalTransactionStateException;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E1-S1 / E1-S3 / E1-S4 against the real database: what gets audited, that the trail cannot be changed,
 * and the Audit log search. The trail is append-only, so each test only looks at rows it added itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditTrailIntegrationTest
{
    private static final String PASSWORD = "Audit-Test-2026";

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private AuditService auditService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbc;

    private long baseline;

    @BeforeEach
    void rememberWhereTheTrailEnds()
    {
        baseline = auditLogRepository.findAll().stream().mapToLong(AuditLog::getId).max().orElse(0);
    }

    // ---- E1-S1 ----

    @Test
    void loginIsAuditedWithRoleAndIp() throws Exception
    {
        User agent = newUser(Role.AGENT);

        login(agent.getUsername(), PASSWORD).andExpect(status().isOk());

        AuditLog log = onlyNewRow(AuditAction.USER_LOGIN);
        assertThat(log.getUsername()).isEqualTo(agent.getUsername());
        assertThat(log.getRole()).isEqualTo(Role.AGENT);
        assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(log.getEntity()).isEqualTo("USER");
        assertThat(log.getEntityId()).isEqualTo(String.valueOf(agent.getId()));
    }

    @Test
    void loginAcceptsTheSha256TheBrowserSends() throws Exception
    {
        User agent = newUser(Role.AGENT);

        login(agent.getUsername(), PasswordDigest.sha256Hex(PASSWORD)).andExpect(status().isOk());
        login(agent.getUsername(), PasswordDigest.sha256Hex("wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.retryAt").doesNotExist());     // only a lock says when to retry
    }

    @Test
    void fifthWrongPasswordLocksTheAccountAndIsAudited() throws Exception
    {
        User agent = newUser(Role.AGENT);

        for (int attempt = 1; attempt <= 4; attempt++) {
            login(agent.getUsername(), "wrong").andExpect(status().isUnauthorized());
        }
        login(agent.getUsername(), "wrong")
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.endsWith("Try again in 15 minutes.")))
                .andExpect(jsonPath("$.retryAt").exists())
                .andExpect(jsonPath("$.retryAfterSeconds").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.greaterThan(890), org.hamcrest.Matchers.lessThanOrEqualTo(900))))
                .andExpect(header().exists("Retry-After"));

        // still locked, even with the right password
        login(agent.getUsername(), PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.retryAfterSeconds").exists());

        AuditLog log = onlyNewRow(AuditAction.USER_LOCKED);
        assertThat(log.getUsername()).isEqualTo(agent.getUsername());
        assertThat(log.getRole()).isEqualTo(Role.AGENT);
        assertThat(log.getAfterState()).contains("lockedUntil");
    }

    @Test
    void refreshPushesTheInactivityTimeoutOut() throws Exception
    {
        User agent = newUser(Role.AGENT);
        String body = login(agent.getUsername(), PASSWORD).andReturn().getResponse().getContentAsString();
        String refreshToken = com.jayway.jsonpath.JsonPath.read(body, "$.refreshToken");
        jdbc.update("update refresh_tokens set expirydate = ? where token = ?",
                Timestamp.from(Instant.now().plus(Duration.ofMinutes(1))), refreshToken);   // nearly idle out

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk());

        Timestamp expiry = jdbc.queryForObject("select expirydate from refresh_tokens where token = ?",
                Timestamp.class, refreshToken);
        assertThat(expiry.toInstant()).isAfter(Instant.now().plus(Duration.ofMinutes(29)));
    }

    // ---- E1-S4 ----

    @Test
    void openingACustomerIsAudited() throws Exception
    {
        User agent = newUser(Role.AGENT);

        String token = bearer(agent);
        mockMvc.perform(post("/api/customers/CIF-100245/verify").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passedChecks\":[\"DATE_OF_BIRTH\",\"MOBILE_ON_FILE\"]}"))
                .andExpect(status().isOk());   // E2-S2: nothing shows before verification
        mockMvc.perform(get("/api/customers/CIF-100245").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        AuditLog log = onlyNewRow(AuditAction.CUSTOMER_DATA_VIEWED);
        assertThat(log.getUsername()).isEqualTo(agent.getUsername());
        assertThat(log.getRole()).isEqualTo(Role.AGENT);
        assertThat(log.getEntity()).isEqualTo("CUSTOMER");
        assertThat(log.getEntityId()).isEqualTo("CIF-100245");
    }

    // ---- E1-S3 ----

    @Test
    void auditRowsCannotBeChangedOrDeleted() throws Exception
    {
        login(newUser(Role.AGENT).getUsername(), PASSWORD).andExpect(status().isOk());
        long id = onlyNewRow(AuditAction.USER_LOGIN).getId();

        assertThatThrownBy(() -> jdbc.update("update audit_logs set username = 'someone_else' where id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from audit_logs where id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void auditOutsideABusinessTransactionIsRefused()
    {
        // Proves the "same transaction" rule: there is no way to write an audit row on its own.
        assertThatThrownBy(() -> auditService.record(AuditAction.USER_LOGIN, null, "USER", "1", null, null))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void adminSearchesTheTrailWithFiltersAndPaging() throws Exception
    {
        User agent = newUser(Role.AGENT);
        login(agent.getUsername(), PASSWORD).andExpect(status().isOk());
        login(agent.getUsername(), PASSWORD).andExpect(status().isOk());

        mockMvc.perform(get("/api/audit/logs")
                        .param("username", agent.getUsername())
                        .param("action", "USER_LOGIN")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(newUser(Role.GUIDE_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].username").value(agent.getUsername()))
                .andExpect(jsonPath("$.items[0].action").value("USER_LOGIN"))
                .andExpect(jsonPath("$.items[0].role").value("AGENT"))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void reviewerCanReadTheTrailButAgentCannot() throws Exception
    {
        mockMvc.perform(get("/api/audit/logs").header(HttpHeaders.AUTHORIZATION, bearer(newUser(Role.REVIEWER))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/audit/logs").header(HttpHeaders.AUTHORIZATION, bearer(newUser(Role.AGENT))))
                .andExpect(status().isForbidden());
    }


    private org.springframework.test.web.servlet.ResultActions login(String username, String password) throws Exception
    {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    private AuditLog onlyNewRow(AuditAction action)
    {
        List<AuditLog> rows = auditLogRepository.findAll().stream()
                .filter(log -> log.getId() > baseline && log.getAction() == action)
                .toList();
        assertThat(rows).as("new " + action + " rows").hasSize(1);
        return rows.get(0);
    }

    private String bearer(User user)
    {
        return "Bearer " + jwtService.generateToken(user).token();
    }

    /** A fresh user per test: the trail is permanent, and a locked user would stay locked across runs. */
    private User newUser(Role role)
    {
        String username = "audit_" + role.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        String mobile = String.format("+2099%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        return userRepository.save(new User(username, passwordEncoder.encode(PasswordDigest.sha256Hex(PASSWORD)), "Audit Test",
                username + "@test.local", mobile, role));
    }
}
