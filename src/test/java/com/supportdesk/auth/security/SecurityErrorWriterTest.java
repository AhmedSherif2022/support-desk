package com.supportdesk.auth.security;

import com.supportdesk.audit.event.AccessDeniedEvent;
import com.supportdesk.auth.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SecurityErrorWriterTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SecurityErrorWriter securityErrorWriter;

    @BeforeEach
    void setUp() {
        securityErrorWriter = new SecurityErrorWriter(eventPublisher);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handle_returns403AndPublishesEventForAuthenticatedUser() throws IOException {
        AuthenticatedUser user = new AuthenticatedUser(42L, "agent_smith", Role.AGENT);
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_AGENT")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets/review");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityErrorWriter.handle(request, response, new AccessDeniedException("Forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("\"code\":\"FORBIDDEN\"");
        assertThat(response.getContentAsString()).contains("\"path\":\"/api/tickets/review\"");

        ArgumentCaptor<AccessDeniedEvent> eventCaptor = ArgumentCaptor.forClass(AccessDeniedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        AccessDeniedEvent event = eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(42L);
        assertThat(event.username()).isEqualTo("agent_smith");
        assertThat(event.endpoint()).isEqualTo("/api/tickets/review");
        assertThat(event.httpMethod()).isEqualTo("GET");
        assertThat(event.ipAddress()).isEqualTo("192.168.1.50");
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    void handle_ignoresSpoofedXForwardedFor() throws IOException {
        AuthenticatedUser user = new AuthenticatedUser(1L, "user1", Role.AGENT);
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_AGENT")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/sensitive");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityErrorWriter.handle(request, response, new AccessDeniedException("Forbidden"));

        ArgumentCaptor<AccessDeniedEvent> eventCaptor = ArgumentCaptor.forClass(AccessDeniedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue().ipAddress()).isEqualTo("10.0.0.1");   // the real connection, not the header
    }

    @Test
    void handle_anonymousUser_publishesEventWithNullUserIdAndUsername() throws IOException {
        var anonymousAuth = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonymousAuth);

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/tickets/1");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityErrorWriter.handle(request, response, new AccessDeniedException("Access is denied"));

        assertThat(response.getStatus()).isEqualTo(403);

        ArgumentCaptor<AccessDeniedEvent> eventCaptor = ArgumentCaptor.forClass(AccessDeniedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        AccessDeniedEvent event = eventCaptor.getValue();
        assertThat(event.userId()).isNull();
        assertThat(event.username()).isNull();
        assertThat(event.endpoint()).isEqualTo("/api/tickets/1");
        assertThat(event.httpMethod()).isEqualTo("DELETE");
        assertThat(event.ipAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void handle_nullAuthentication_publishesEventWithNullUser() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityErrorWriter.handle(request, response, new AccessDeniedException("Access is denied"));

        assertThat(response.getStatus()).isEqualTo(403);

        ArgumentCaptor<AccessDeniedEvent> eventCaptor = ArgumentCaptor.forClass(AccessDeniedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        AccessDeniedEvent event = eventCaptor.getValue();
        assertThat(event.userId()).isNull();
        assertThat(event.username()).isNull();
        assertThat(event.endpoint()).isEqualTo("/api/admin");
    }

    @Test
    void commence_returns401AndDoesNotPublishEvent() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityErrorWriter.commence(request, response, new AuthenticationException("Unauthorized") {});

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":\"UNAUTHORIZED\"");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void handle_publisherThrows_stillReturns403() throws IOException {
        org.mockito.Mockito.doThrow(new RuntimeException("Publisher error"))
                .when(eventPublisher).publishEvent(any());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityErrorWriter.handle(request, response, new AccessDeniedException("Forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":\"FORBIDDEN\"");
    }

    @Test
    void handle_nullPublisher_stillReturns403() throws IOException {
        SecurityErrorWriter writerWithoutPublisher = new SecurityErrorWriter(null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        writerWithoutPublisher.handle(request, response, new AccessDeniedException("Forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":\"FORBIDDEN\"");
    }

    @Test
    void handle_blankXForwardedFor_fallsBackToRemoteAddr() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets");
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityErrorWriter.handle(request, response, new AccessDeniedException("Forbidden"));

        ArgumentCaptor<AccessDeniedEvent> eventCaptor = ArgumentCaptor.forClass(AccessDeniedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue().ipAddress()).isEqualTo("10.0.0.5");
    }

    @Test
    void handle_userDetailsPrincipal_extractsUsername() throws IOException {
        org.springframework.security.core.userdetails.User springUser =
                new org.springframework.security.core.userdetails.User("custom_user", "pass", List.of());
        var auth = new UsernamePasswordAuthenticationToken(springUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityErrorWriter.handle(request, response, new AccessDeniedException("Forbidden"));

        ArgumentCaptor<AccessDeniedEvent> eventCaptor = ArgumentCaptor.forClass(AccessDeniedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue().username()).isEqualTo("custom_user");
        assertThat(eventCaptor.getValue().userId()).isNull();
    }
}
