package com.supportdesk.monitor.controller;
import com.supportdesk.auth.config.SecurityConfig;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.security.AuthenticatedUser;
import com.supportdesk.auth.security.SecurityErrorWriter;
import com.supportdesk.auth.service.JwtService;
import com.supportdesk.common.config.ClockConfig;
import com.supportdesk.monitor.entity.MonitorJobRun;
import com.supportdesk.monitor.enums.EventOutcome;
import com.supportdesk.monitor.enums.JobTrigger;
import com.supportdesk.monitor.repository.MonitorJobRunRepository;
import com.supportdesk.monitor.service.MonitorJob;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(MonitorController.class)
@Import({SecurityConfig.class, ClockConfig.class, SecurityErrorWriter.class})
class MonitorControllerTest
{
    private static final Instant START = Instant.parse("2026-11-22T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitorJob monitorJob;

    @MockitoBean
    private MonitorJobRunRepository runRepository;


    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;




    @Test
    void guideAdminRunsTheJobAndGetsItsNumbers() throws Exception
    {
        MonitorJobRun run = new MonitorJobRun(JobTrigger.MANUAL, START);
        run.count(EventOutcome.CREATED);
        run.count(EventOutcome.CREATED);
        run.count(EventOutcome.DUPLICATE);
        run.count(EventOutcome.UNKNOWN_CODE);
        run.finish(START.plusSeconds(2));
        when(monitorJob.run(JobTrigger.MANUAL)).thenReturn(run);

        mockMvc.perform(post("/api/admin/monitor/run").with(loggedInAs(Role.GUIDE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trigger").value("MANUAL"))
                .andExpect(jsonPath("$.rowsRead").value(4))
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.duplicates").value(1))
                .andExpect(jsonPath("$.unknownCodes").value(1))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(monitorJob).run(JobTrigger.MANUAL);      // "Run job now" is MANUAL, never SCHEDULE
    }

    @Test
    void guideAdminSeesTheRuns() throws Exception
    {
        MonitorJobRun run = new MonitorJobRun(JobTrigger.SCHEDULE, START);
        when(runRepository.findAllByOrderByStartedAtDescIdDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(run)));

        mockMvc.perform(get("/api/admin/monitor/runs").with(loggedInAs(Role.GUIDE_ADMIN)))
                .andExpect(status().isOk());

        verify(runRepository).findAllByOrderByStartedAtDescIdDesc(any(Pageable.class));
    }




    @ParameterizedTest(name = "{0} cannot run the job")
    @EnumSource(value = Role.class, names = {"AGENT", "REVIEWER"})
    void otherRolesCannotRunTheJob(Role role) throws Exception
    {
        mockMvc.perform(post("/api/admin/monitor/run").with(loggedInAs(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(monitorJob, never()).run(any());
    }

    @ParameterizedTest(name = "{0} cannot see the runs")
    @EnumSource(value = Role.class, names = {"AGENT", "REVIEWER"})
    void otherRolesCannotSeeTheRuns(Role role) throws Exception
    {
        mockMvc.perform(get("/api/admin/monitor/runs").with(loggedInAs(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }



    @Test
    void runningTheJobNeedsALogin() throws Exception
    {
        mockMvc.perform(post("/api/admin/monitor/run"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(monitorJob, never()).run(any());
    }



    private static RequestPostProcessor loggedInAs(Role role)
    {
        var badge = new AuthenticatedUser(1L, role.name().toLowerCase(), role);
        return authentication(new UsernamePasswordAuthenticationToken(
                badge, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
