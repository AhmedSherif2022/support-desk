package com.supportdesk.audit;

import com.supportdesk.audit.entity.AuditAction;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.audit.repository.AuditLogRepository;
import com.supportdesk.auth.entity.Role;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {com.supportdesk.SupportDeskApplication.class, AuditLoggingIntegrationTest.TestSecuredController.class})
@AutoConfigureMockMvc
class AuditLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private User agentUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();

        agentUser = userRepository.findByUsernameIgnoreCase("test_agent").orElseGet(() -> {
            User u = new User("test_agent", "$2a$10$dummyhash", "Test Agent", "agent@test.com", "+1000000001", Role.AGENT);
            return userRepository.save(u);
        });

        adminUser = userRepository.findByUsernameIgnoreCase("test_admin").orElseGet(() -> {
            User u = new User("test_admin", "$2a$10$dummyhash", "Test Admin", "admin@test.com", "+1000000002", Role.GUIDE_ADMIN);
            return userRepository.save(u);
        });
    }

    @Test
    void urlLevelForbidden_createsExactlyOneAuditLog() throws Exception {
        String agentToken = jwtService.generateToken(agentUser).token();

        mockMvc.perform(get("/api/admin/some-endpoint")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(1);

        AuditLog log = logs.get(0);
        assertThat(log.getUserId()).isEqualTo(agentUser.getId());
        assertThat(log.getUsername()).isEqualTo(agentUser.getUsername());
        assertThat(log.getAction()).isEqualTo(AuditAction.ACCESS_DENIED);
        assertThat(log.getEndpoint()).isEqualTo("/api/admin/some-endpoint");
        assertThat(log.getHttpMethod()).isEqualTo("GET");
        assertThat(log.getCreatedAt()).isNotNull();
    }

    @Test
    void permittedRequest_doesNotCreateAuditLog() throws Exception {
        String agentToken = jwtService.generateToken(agentUser).token();

        // /api/auth/me is authenticated() and permitted for any logged-in user
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).isEmpty();
    }

    @Test
    void unauthenticatedRequest_returns401AndDoesNotCreateAuditLog() throws Exception {
        mockMvc.perform(get("/api/tickets/123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).isEmpty();
    }

    @Test
    void preAuthorizeForbidden_createsAuditLog() throws Exception {
        String agentToken = jwtService.generateToken(agentUser).token();

        mockMvc.perform(get("/api/test-secured/admin-only")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(1);

        AuditLog log = logs.get(0);
        assertThat(log.getUserId()).isEqualTo(agentUser.getId());
        assertThat(log.getUsername()).isEqualTo("test_agent");
        assertThat(log.getAction()).isEqualTo(AuditAction.ACCESS_DENIED);
        assertThat(log.getEndpoint()).isEqualTo("/api/test-secured/admin-only");
    }

    @Test
    void urlLevelForbidden_withXForwardedFor_logsForwardedIp() throws Exception {
        String agentToken = jwtService.generateToken(agentUser).token();

        mockMvc.perform(get("/api/admin/some-endpoint")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .header("X-Forwarded-For", "203.0.113.50, 198.51.100.1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getIpAddress()).isEqualTo("203.0.113.50");
    }

    @RestController
    static class TestSecuredController {
        @GetMapping("/api/test-secured/admin-only")
        @PreAuthorize("hasRole('GUIDE_ADMIN')")
        public String adminOnly() {
            return "ok";
        }
    }
}
