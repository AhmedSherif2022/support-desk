package com.supportdesk.auth.security;

import com.supportdesk.audit.event.AccessDeniedEvent;
import com.supportdesk.common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Component
public final class SecurityErrorWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ApplicationEventPublisher eventPublisher;

    public SecurityErrorWriter(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, ErrorCode.UNAUTHORIZED, request.getRequestURI());
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        publishAccessDeniedEvent(request);
        write(response, ErrorCode.FORBIDDEN, request.getRequestURI());
    }

    private void publishAccessDeniedEvent(HttpServletRequest request) {
        if (eventPublisher == null) {
            return;
        }
        try {
            Long userId = null;
            String username = null;

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof AuthenticatedUser authUser) {
                    userId = authUser.id();
                    username = authUser.username();
                } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                    username = userDetails.getUsername();
                } else if (principal instanceof String principalStr && !"anonymousUser".equals(principalStr)) {
                    username = principalStr;
                } else if (authentication.getName() != null && !"anonymousUser".equals(authentication.getName())) {
                    username = authentication.getName();
                }
            }

            String endpoint = request.getRequestURI();
            String httpMethod = request.getMethod();
            String ipAddress = extractClientIp(request);
            Instant timestamp = Instant.now();

            AccessDeniedEvent event = new AccessDeniedEvent(
                    userId,
                    username,
                    endpoint,
                    httpMethod,
                    ipAddress,
                    timestamp
            );

            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish AccessDeniedEvent", e);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            String firstIp = xfHeader.split(",")[0].trim();
            if (!firstIp.isEmpty()) {
                return firstIp;
            }
        }
        return request.getRemoteAddr();
    }

    private static void write(HttpServletResponse response,
                              ErrorCode errorCode,
                              String path) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"" + errorCode.name() + "\","
                + "\"message\":\"" + errorCode.getMessage() + "\","
                + "\"fieldErrors\":{},"
                + "\"path\":\"" + escape(path) + "\","
                + "\"timestamp\":\"" + Instant.now() + "\"}");
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
