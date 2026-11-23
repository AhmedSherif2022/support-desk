package com.supportdesk.incident.controller;
import com.supportdesk.auth.config.SecurityConfig;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.auth.security.SecurityErrorWriter;
import com.supportdesk.auth.service.JwtService;
import com.supportdesk.common.config.ClockConfig;
import com.supportdesk.incident.dto.IncidentDtos.IncidentSettingsDto;
import com.supportdesk.incident.dto.IncidentDtos.UpdateIncidentSettingsRequest;
import com.supportdesk.incident.service.IncidentSettingsService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** E6-S3 threshold API through the real SecurityConfig: the Guide Admin reads and changes them, agents cannot. */
@WebMvcTest(IncidentSettingsAdminController.class)
@Import({SecurityConfig.class, ClockConfig.class, SecurityErrorWriter.class})
class IncidentSettingsAdminControllerTest
{
    private static final String URL = "/api/admin/incident-settings";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentSettingsService settingsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;


    @Test
    void guideAdminReadsTheThresholds() throws Exception
    {
        when(settingsService.current()).thenReturn(new IncidentSettingsDto(5, 15));

        mockMvc.perform(get(URL).with(loggedInAs(Role.GUIDE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minAffectedCustomers").value(5))
                .andExpect(jsonPath("$.groupingWindowMinutes").value(15));
    }

    @Test
    void guideAdminChangesTheThresholds() throws Exception
    {
        when(settingsService.update(new UpdateIncidentSettingsRequest(3, 30)))
                .thenReturn(new IncidentSettingsDto(3, 30));

        mockMvc.perform(put(URL).with(loggedInAs(Role.GUIDE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minAffectedCustomers\":3,\"groupingWindowMinutes\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minAffectedCustomers").value(3))
                .andExpect(jsonPath("$.groupingWindowMinutes").value(30));

        verify(settingsService).update(new UpdateIncidentSettingsRequest(3, 30));
    }

    @Test
    void zeroOrNegativeValuesAreRefusedBeforeTheServiceWithFieldErrors() throws Exception
    {
        mockMvc.perform(put(URL).with(loggedInAs(Role.GUIDE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minAffectedCustomers\":0,\"groupingWindowMinutes\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.minAffectedCustomers").exists())
                .andExpect(jsonPath("$.fieldErrors.groupingWindowMinutes").exists());

        verify(settingsService, never()).update(any());
    }

    @Test
    void missingValuesAreRefused() throws Exception
    {
        mockMvc.perform(put(URL).with(loggedInAs(Role.GUIDE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.minAffectedCustomers").exists())
                .andExpect(jsonPath("$.fieldErrors.groupingWindowMinutes").exists());

        verify(settingsService, never()).update(any());
    }

    @Test
    void aBodyThatIsNotJsonIsMalformed() throws Exception
    {
        mockMvc.perform(put(URL).with(loggedInAs(Role.GUIDE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @ParameterizedTest(name = "{0} cannot read the thresholds")
    @EnumSource(value = Role.class, names = {"AGENT", "REVIEWER"})
    void otherRolesCannotReadTheThresholds(Role role) throws Exception
    {
        mockMvc.perform(get(URL).with(loggedInAs(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @ParameterizedTest(name = "{0} cannot change the thresholds")
    @EnumSource(value = Role.class, names = {"AGENT", "REVIEWER"})
    void otherRolesCannotChangeTheThresholds(Role role) throws Exception
    {
        mockMvc.perform(put(URL).with(loggedInAs(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minAffectedCustomers\":1,\"groupingWindowMinutes\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(settingsService, never()).update(any());
    }

    @Test
    void theThresholdsNeedALogin() throws Exception
    {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minAffectedCustomers\":1,\"groupingWindowMinutes\":1}"))
                .andExpect(status().isUnauthorized());

        verify(settingsService, never()).update(any());
    }


    private static RequestPostProcessor loggedInAs(Role role)
    {
        var badge = new AuthenticatedUser(1L, role.name().toLowerCase(), role);
        return authentication(new UsernamePasswordAuthenticationToken(
                badge, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
