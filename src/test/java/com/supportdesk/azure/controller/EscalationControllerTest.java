package com.supportdesk.azure.controller;

import com.supportdesk.auth.config.SecurityConfig;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.auth.security.SecurityErrorWriter;
import com.supportdesk.auth.service.JwtService;
import com.supportdesk.azure.dto.EscalationDtos.EscalateRequest;
import com.supportdesk.azure.dto.EscalationDtos.EscalationDto;
import com.supportdesk.azure.enums.EscalationUrgency;
import com.supportdesk.azure.enums.SyncStatus;
import com.supportdesk.azure.service.EscalationService;
import com.supportdesk.common.config.ClockConfig;
import com.supportdesk.ticket.enums.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** E5-S1 HTTP contract through the real SecurityConfig. */
@WebMvcTest(EscalationController.class)
@Import({SecurityConfig.class, ClockConfig.class, SecurityErrorWriter.class})
class EscalationControllerTest
{
    private static final String URL = "/api/tickets/2001/escalation";
    private static final EscalationDto PENDING = new EscalationDto("#2001", TicketStatus.ESCALATED,
            EscalationUrgency.HIGH, "Debited but not credited", "Mona", Instant.parse("2026-11-26T10:00:00Z"),
            SyncStatus.PENDING, "pending · retrying", null, null, "Sent to SRE.");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EscalationService escalationService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void agentEscalatesAndGets201WithPendingSync() throws Exception
    {
        when(escalationService.escalate(eq(2001L), any())).thenReturn(PENDING);

        mockMvc.perform(post(URL).with(as(Role.AGENT)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Debited but not credited\",\"urgency\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketStatus").value("ESCALATED"))
                .andExpect(jsonPath("$.syncStatus").value("PENDING"))
                .andExpect(jsonPath("$.syncLabel").value("pending · retrying"));
        verify(escalationService).escalate(2001L, new EscalateRequest("Debited but not credited", EscalationUrgency.HIGH));
    }

    @Test
    void reasonAndUrgencyAreRequired() throws Exception
    {
        mockMvc.perform(post(URL).with(as(Role.AGENT)).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.reason").exists())
                .andExpect(jsonPath("$.fieldErrors.urgency").exists());
        verify(escalationService, never()).escalate(any(), any());
    }

    @Test
    void agentReadsTheEscalation() throws Exception
    {
        when(escalationService.get(2001L)).thenReturn(PENDING);

        mockMvc.perform(get(URL).with(as(Role.AGENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber").value("#2001"));
    }

    @ParameterizedTest(name = "{0} cannot escalate")
    @EnumSource(value = Role.class, names = {"REVIEWER", "GUIDE_ADMIN"})
    void onlyAgentsEscalate(Role role) throws Exception
    {
        mockMvc.perform(post(URL).with(as(role)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Debited but not credited\",\"urgency\":\"HIGH\"}"))
                .andExpect(status().isForbidden());
        verify(escalationService, never()).escalate(any(), any());
    }


    private static RequestPostProcessor as(Role role)
    {
        var badge = new AuthenticatedUser(1L, role.name().toLowerCase(), role);
        return authentication(new UsernamePasswordAuthenticationToken(
                badge, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
