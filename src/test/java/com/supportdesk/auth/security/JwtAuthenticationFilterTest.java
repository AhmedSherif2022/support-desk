package com.supportdesk.auth.security;

import com.supportdesk.auth.config.AuthProperties;
import com.supportdesk.auth.entity.Role;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.repository.UserRepository;
import com.supportdesk.auth.service.AuthService;
import com.supportdesk.auth.service.JwtService;
import com.supportdesk.auth.service.LoginAttemptService;
import com.supportdesk.auth.service.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest
{

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    private JwtService jwtService;
    private AuthService authService;
    private JwtAuthenticationFilter filter;
    private User user;

    @BeforeEach
    void setUp()
    {
        String secret = Base64.getEncoder().encodeToString(new byte[32]);
        Clock clock = Clock.systemUTC();
        jwtService = new JwtService(
                new AuthProperties(secret, Duration.ofMinutes(30), 5, Duration.ofMinutes(15)), clock);
        authService = new AuthService(
                userRepository, mock(PasswordEncoder.class), mock(LoginAttemptService.class), jwtService, refreshTokenService);
        filter = new JwtAuthenticationFilter(jwtService, userRepository, clock);

        user = new User("agent", "hash", "Agent Smith", "agent@example.com", "+201000000000", Role.AGENT);
        lenient().when(userRepository.findByUsernameIgnoreCase("agent")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearContext()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenAuthenticatesBeforeLogout() throws Exception
    {
        String token = jwtService.generateToken(user).token();

        assertThat(filterWith(token)).isNotNull();
    }

    @Test
    void tokenIsRejectedAfterLogout() throws Exception
    {
        String token = jwtService.generateToken(user).token();

        logout();

        assertThat(filterWith(token)).isNull();
    }

    @Test
    void newLoginAfterLogoutAuthenticates() throws Exception
    {
        jwtService.generateToken(user);
        logout();
        String newToken = jwtService.generateToken(user).token();

        assertThat(filterWith(newToken)).isNotNull();
    }

    @Test
    void logoutDeletesRefreshToken()
    {
        logout();

        verify(refreshTokenService).deleteByUserId(USER_ID);
    }

    private void logout()
    {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        authService.logout(USER_ID);
    }

    private Authentication filterWith(String token) throws Exception
    {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
