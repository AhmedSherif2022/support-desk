package com.supportdesk.auth.security;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;


public final class SecurityErrorWriter implements AuthenticationEntryPoint, AccessDeniedHandler
{

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                "Please log in to continue.", request.getRequestURI());
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                "You do not have permission to do this.", request.getRequestURI());
    }
    private static void write(HttpServletResponse response,
                              int status,
                              String code,
                              String message,
                              String path) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"" + code + "\","
                + "\"message\":\"" + message + "\","
                + "\"fieldErrors\":{},"
                + "\"path\":\"" + escape(path) + "\","
                + "\"timestamp\":\"" + Instant.now() + "\"}");
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
