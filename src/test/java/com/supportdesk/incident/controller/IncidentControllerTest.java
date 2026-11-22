package com.supportdesk.incident.controller;
import com.supportdesk.auth.config.SecurityConfig;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.auth.security.SecurityErrorWriter;
import com.supportdesk.auth.service.JwtService;
import com.supportdesk.common.config.ClockConfig;
import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.incident.dto.IncidentDtos.IncidentDto;
import com.supportdesk.incident.dto.IncidentDtos.IncidentTicketDto;
import com.supportdesk.incident.enums.IncidentStatus;
import com.supportdesk.incident.exception.IncidentNotFoundException;
import com.supportdesk.incident.service.IncidentService;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** E6-S3 read API through the real SecurityConfig: agents read incidents, nobody else does, errors come back as ApiError. */
@WebMvcTest(IncidentController.class)
@Import({SecurityConfig.class, ClockConfig.class, SecurityErrorWriter.class})
class IncidentControllerTest
{
    private static final Instant AT = Instant.parse("2026-11-22T10:30:00Z");
    private static final IncidentDto INCIDENT = new IncidentDto(7L, "ERR_AUTH_503", Platform.MOBILE, "AUTH",
            IncidentStatus.OPEN, 5, 5, AT.minusSeconds(900), AT.minusSeconds(300), AT, AT, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentService incidentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;


    @Test
    void agentListsTheOpenIncidentsByDefault() throws Exception
    {
        when(incidentService.list(eq(IncidentStatus.OPEN), any(Pageable.class)))
                .thenReturn(PageResponse.of(new PageImpl<>(List.of(INCIDENT))));

        mockMvc.perform(get("/api/incidents").with(loggedInAs(Role.AGENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].errorCode").value("ERR_AUTH_503"))
                .andExpect(jsonPath("$.items[0].platform").value("MOBILE"))
                .andExpect(jsonPath("$.items[0].affectedCustomerCount").value(5))
                .andExpect(jsonPath("$.items[0].ticketCount").value(5))
                .andExpect(jsonPath("$.items[0].status").value("OPEN"));
    }

    @Test
    void statusAllListsEveryIncident() throws Exception
    {
        when(incidentService.list(isNull(), any(Pageable.class)))
                .thenReturn(PageResponse.of(new PageImpl<>(List.of(INCIDENT))));

        mockMvc.perform(get("/api/incidents?status=all").with(loggedInAs(Role.AGENT)))
                .andExpect(status().isOk());

        verify(incidentService).list(isNull(), any(Pageable.class));
    }

    @Test
    void anUnknownStatusIsARequestError() throws Exception
    {
        mockMvc.perform(get("/api/incidents?status=BOGUS").with(loggedInAs(Role.AGENT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_ERROR"));

        verify(incidentService, never()).list(any(), any());
    }

    @Test
    void agentReadsOneIncidentAndItsTickets() throws Exception
    {
        when(incidentService.get(7L)).thenReturn(INCIDENT);
        when(incidentService.tickets(7L)).thenReturn(List.of(new IncidentTicketDto(2104L, "Login failed",
                TicketStatus.NEW, TicketPriority.CRITICAL, "CIF-100249", "Laila Hany", "LOGIN_FAILED",
                AT.minusSeconds(600), AT)));

        mockMvc.perform(get("/api/incidents/7").with(loggedInAs(Role.AGENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.service").value("AUTH"))
                .andExpect(jsonPath("$.affectedCustomerCount").value(5));

        mockMvc.perform(get("/api/incidents/7/tickets").with(loggedInAs(Role.AGENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketNumber").value(2104))
                .andExpect(jsonPath("$[0].priority").value("CRITICAL"))
                .andExpect(jsonPath("$[0].customerCif").value("CIF-100249"))
                .andExpect(jsonPath("$[0].nationalId").doesNotExist())
                .andExpect(jsonPath("$[0].mobile").doesNotExist());
    }

    @Test
    void anUnknownIncidentIs404() throws Exception
    {
        when(incidentService.get(99L)).thenThrow(new IncidentNotFoundException(99L));

        mockMvc.perform(get("/api/incidents/99").with(loggedInAs(Role.AGENT)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void aNonNumericIdIsARequestError() throws Exception
    {
        mockMvc.perform(get("/api/incidents/abc").with(loggedInAs(Role.AGENT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_ERROR"));
    }

    @ParameterizedTest(name = "{0} cannot read incidents")
    @EnumSource(value = Role.class, names = {"GUIDE_ADMIN", "REVIEWER"})
    void otherRolesCannotReadIncidents(Role role) throws Exception
    {
        mockMvc.perform(get("/api/incidents").with(loggedInAs(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(incidentService, never()).list(any(), any());
    }

    @Test
    void readingIncidentsNeedsALogin() throws Exception
    {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/incidents/7"))
                .andExpect(status().isUnauthorized());
    }


    private static RequestPostProcessor loggedInAs(Role role)
    {
        var badge = new AuthenticatedUser(1L, role.name().toLowerCase(), role);
        return authentication(new UsernamePasswordAuthenticationToken(
                badge, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
