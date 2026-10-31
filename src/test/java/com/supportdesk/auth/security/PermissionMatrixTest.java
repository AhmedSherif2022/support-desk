package com.supportdesk.auth.security;

import com.supportdesk.auth.config.SecurityConfig;
import com.supportdesk.auth.controller.AuthController;
import com.supportdesk.auth.entity.Role;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.service.AuthService;
import com.supportdesk.auth.service.JwtService;
import com.supportdesk.common.config.ClockConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.supportdesk.auth.entity.Role.AGENT;
import static com.supportdesk.auth.entity.Role.GUIDE_ADMIN;
import static com.supportdesk.auth.entity.Role.REVIEWER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E1-S2: proves the permission matrix through the real SecurityConfig.
 * No token → 401, wrong role → 403, right role → let through, for every role and every URL area.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ClockConfig.class})
class PermissionMatrixTest
{

    // The agreed matrix, written out on purpose instead of read from Permissions:
    // a wrong edit to Permissions must make this test fail, not quietly change what it expects.
    private static final Map<String, Set<Role>> AREAS = Map.of(
            "/api/admin/**",     Set.of(GUIDE_ADMIN),
            "/api/review/**",    Set.of(REVIEWER),
            "/api/audit/**",     Set.of(GUIDE_ADMIN, REVIEWER),
            "/api/customers/**", Set.of(AGENT),
            "/api/tickets/**",   Set.of(AGENT),
            "/api/queue/**",     Set.of(AGENT));

    private static final Map<Role, List<String>> SCREENS = Map.of(
            AGENT,       List.of("HOME", "CUSTOMER_SEARCH", "MY_TICKETS", "NOTIFICATIONS"),
            GUIDE_ADMIN, List.of("GUIDE_LIBRARY", "ERROR_RULES", "TRIAGE", "AGENT_FEEDBACK",
                    "GUIDE_ANALYTICS", "AUDIT_LOG", "NOTIFICATIONS"),
            REVIEWER,    List.of("REVIEW_QUEUE", "AUDIT_LOG", "NOTIFICATIONS"));

    @Autowired
    private MockMvc mockMvc;

    // SecurityConfig and AuthController need these; the matrix itself never reaches them.
    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;


    // ---- the matrix itself ----

    @Test
    void permissionsMatchTheAgreedMatrix()
    {
        Map<String, Set<Role>> actual = Permissions.RULES.stream()
                .collect(Collectors.toMap(Permissions.Rule::pattern, Permissions.Rule::roles));

        assertThat(actual).isEqualTo(AREAS);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Role.class)
    void everyRoleHasAnAreaAndScreens(Role role)
    {
        assertThat(AREAS.values()).anyMatch(roles -> roles.contains(role));
        assertThat(SCREENS.get(role)).as("screens for " + role).isNotEmpty();
    }


    // ---- enforced on real requests ----

    static Stream<Arguments> everyAreaAndRole()
    {
        return AREAS.keySet().stream()
                .flatMap(area -> Arrays.stream(Role.values()).map(role -> Arguments.of(area, role)));
    }

    @ParameterizedTest(name = "{1} on {0}")
    @MethodSource("everyAreaAndRole")
    void eachRoleOpensOnlyItsOwnAreas(String area, Role role) throws Exception
    {
        var result = mockMvc.perform(get(sampleUrl(area)).with(loggedInAs(role)));

        if (AREAS.get(area).contains(role)) {
            // Let through by security; no controller exists yet, so anything but 401/403 is a pass.
            assertThat(result.andReturn().getResponse().getStatus()).isNotIn(401, 403);
        } else {
            result.andExpect(status().isForbidden())
                  .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    static Stream<String> areas()
    {
        return AREAS.keySet().stream();
    }

    @ParameterizedTest(name = "no token on {0}")
    @MethodSource("areas")
    void everyAreaNeedsALogin(String area) throws Exception
    {
        mockMvc.perform(get(sampleUrl(area)))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @ParameterizedTest(name = "POST {0}")
    @MethodSource("publicPostUrls")
    void loginAndRefreshNeedNoLogin(String url) throws Exception
    {
        var status = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    static Stream<String> publicPostUrls()
    {
        return Stream.of("/api/auth/login", "/api/auth/refresh");
    }


    // ---- GET /api/auth/me: "what can I open?" ----

    @ParameterizedTest(name = "{0}")
    @EnumSource(Role.class)
    void meListsTheScreensOfTheLoggedInRole(Role role) throws Exception
    {
        mockMvc.perform(get("/api/auth/me").with(loggedInAs(role)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.username").value(usernameOf(role)))
               .andExpect(jsonPath("$.role").value(role.name()))
               .andExpect(jsonPath("$.allowedScreens", contains(SCREENS.get(role).toArray())));   // same order
    }

    @Test
    void meNeedsALogin() throws Exception
    {
        mockMvc.perform(get("/api/auth/me"))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Role.class)
    void everyRoleCanLogOut(Role role) throws Exception
    {
        mockMvc.perform(post("/api/auth/logout").with(loggedInAs(role)))
               .andExpect(status().isNoContent());

        verify(authService).logout();
    }

    @Test
    void logoutNeedsALogin() throws Exception
    {
        mockMvc.perform(post("/api/auth/logout"))
               .andExpect(status().isUnauthorized());
    }


    /** "/api/admin/**" → "/api/admin/anything" */
    private static String sampleUrl(String area)
    {
        return area.replace("/**", "/anything");
    }

    private static String usernameOf(Role role)
    {
        return role.name().toLowerCase();
    }

    /** Logs the request in the way JwtAuthenticationFilter would: the badge as principal, ROLE_x as authority. */
    private static RequestPostProcessor loggedInAs(Role role)
    {
        var badge = new AuthenticatedUser(1L, usernameOf(role), role);
        return authentication(new UsernamePasswordAuthenticationToken(
                badge, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
