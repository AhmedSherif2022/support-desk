package com.supportdesk.auth.security;
import com.supportdesk.common.enums.ErrorCode;
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
        write(response, ErrorCode.UNAUTHORIZED, request.getRequestURI());
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, ErrorCode.FORBIDDEN, request.getRequestURI());
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
