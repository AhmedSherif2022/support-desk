package com.supportdesk.ticket.controller;

import com.supportdesk.auth.config.SecurityConfig;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.auth.security.SecurityErrorWriter;
import com.supportdesk.auth.service.JwtService;
import com.supportdesk.common.config.ClockConfig;
import com.supportdesk.ticket.enums.QueueChip;
import com.supportdesk.ticket.dto.QueueResponse;
import com.supportdesk.ticket.dto.QueueTicketResponse;
import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.service.TicketQueryService;
import com.supportdesk.ticket.service.TicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import({SecurityConfig.class, ClockConfig.class, SecurityErrorWriter.class})
class TicketControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private TicketQueryService ticketQueryService;

    @MockitoBean
    private com.supportdesk.ticket.service.TicketListService ticketListService;

    @MockitoBean
    private com.supportdesk.ticket.service.TicketWorkspaceService workspaceService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void agentCanLoadWholeQueueWithoutChipOrWithEmptyChip() throws Exception
    {
        Instant due = Instant.parse("2026-11-01T14:00:00Z");
        QueueResponse sample = new QueueResponse(
                QueueChip.ALL,
                Map.of("ALL", 2L, "AT_RISK", 2L, "CONFIRM_FIX", 0L, "AUTO_CREATED", 0L),
                List.of(
                        new QueueTicketResponse(1L, "#2002", "Locked out", "CIF-100101", "Ahmed Ali", null,
                                "Authentication / Login Failed", null, "ERR_AUTH_503", null, TicketPriority.CRITICAL,
                                TicketStatus.NEW, "Mona", SlaState.BREACHED, "Breached 1h 0m", due, 150, -3600L, false,
                                com.supportdesk.ticket.enums.NextAction.START_GUIDE, "Start guide"),
                        new QueueTicketResponse(2L, "#2001", "Card declined", "CIF-100245", "Sara Mahmoud", null,
                                "Payments / Payment Failed", null, null, null, TicketPriority.HIGH,
                                TicketStatus.TROUBLESHOOTING, "Mona", SlaState.AT_RISK, "1h 30m left", due, 81, 5400L, false,
                                com.supportdesk.ticket.enums.NextAction.ESCALATE, "Escalate")));
        when(ticketQueryService.queue(null)).thenReturn(sample);

        mockMvc.perform(get("/api/tickets/queue").with(loggedInAs(Role.AGENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chip").value("ALL"))
                .andExpect(jsonPath("$.counts.ALL").value(2))
                .andExpect(jsonPath("$.counts.AT_RISK").value(2))
                .andExpect(jsonPath("$.counts.CONFIRM_FIX").value(0))
                .andExpect(jsonPath("$.tickets[0].nextActionLabel").value("Start guide"))
                .andExpect(jsonPath("$.tickets[0].number").value("#2002"))
                .andExpect(jsonPath("$.tickets[1].number").value("#2001"));

        mockMvc.perform(get("/api/tickets/queue").param("chip", "").with(loggedInAs(Role.AGENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chip").value("ALL"));
    }

    @ParameterizedTest
    @EnumSource(value = QueueChip.class, names = {"AT_RISK", "CONFIRM_FIX", "AUTO_CREATED"})
    void agentCanFilterQueueBySlaChip(QueueChip chip) throws Exception
    {
        when(ticketQueryService.queue(chip)).thenReturn(new QueueResponse(
                chip,
                Map.of("ALL", 4L, "AT_RISK", 2L, "CONFIRM_FIX", 1L, "AUTO_CREATED", 0L),
                List.of()));

        mockMvc.perform(get("/api/tickets/queue").param("chip", chip.name()).with(loggedInAs(Role.AGENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chip").value(chip.name()))
                .andExpect(jsonPath("$.counts.ALL").value(4));

        verify(ticketQueryService).queue(chip);
    }

    @Test
    void unknownChipReturns400RequestError() throws Exception
    {
        mockMvc.perform(get("/api/tickets/queue").param("chip", "UNKNOWN").with(loggedInAs(Role.AGENT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_ERROR"));
    }

    @Test
    void queueRequiresAuthentication() throws Exception
    {
        mockMvc.perform(get("/api/tickets/queue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"GUIDE_ADMIN", "REVIEWER"})
    void nonAgentRolesAreForbiddenFromQueue(Role role) throws Exception
    {
        mockMvc.perform(get("/api/tickets/queue").with(loggedInAs(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private static RequestPostProcessor loggedInAs(Role role)
    {
        var badge = new AuthenticatedUser(1L, role.name().toLowerCase(), role);
        return authentication(new UsernamePasswordAuthenticationToken(
                badge, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
