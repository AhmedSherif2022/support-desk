package com.supportdesk.audit.listener;

import com.supportdesk.audit.entity.AuditAction;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.audit.event.AccessDeniedEvent;
import com.supportdesk.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditEventListener auditEventListener;

    @Test
    void onAccessDenied_successfulPersistenceAndFieldMapping() {
        Instant now = Instant.now();
        AccessDeniedEvent event = new AccessDeniedEvent(
                123L,
                "john_doe",
                "/api/admin/users",
                "GET",
                "192.168.1.100",
                now
        );

        auditEventListener.onAccessDenied(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(123L);
        assertThat(savedLog.getUsername()).isEqualTo("john_doe");
        assertThat(savedLog.getAction()).isEqualTo(AuditAction.ACCESS_DENIED);
        assertThat(savedLog.getEndpoint()).isEqualTo("/api/admin/users");
        assertThat(savedLog.getHttpMethod()).isEqualTo("GET");
        assertThat(savedLog.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(savedLog.getTimestamp()).isEqualTo(now);
    }

    @Test
    void onAccessDenied_anonymousUser() {
        Instant now = Instant.now();
        AccessDeniedEvent event = new AccessDeniedEvent(
                null,
                null,
                "/api/secure/endpoint",
                "POST",
                "10.0.0.1",
                now
        );

        auditEventListener.onAccessDenied(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertThat(savedLog.getUserId()).isNull();
        assertThat(savedLog.getUsername()).isNull();
        assertThat(savedLog.getAction()).isEqualTo(AuditAction.ACCESS_DENIED);
        assertThat(savedLog.getEndpoint()).isEqualTo("/api/secure/endpoint");
        assertThat(savedLog.getHttpMethod()).isEqualTo("POST");
        assertThat(savedLog.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(savedLog.getTimestamp()).isEqualTo(now);
    }

    @Test
    void onAccessDenied_repositoryFailureDoesNotThrow() {
        AccessDeniedEvent event = new AccessDeniedEvent(
                1L,
                "user",
                "/test",
                "GET",
                "127.0.0.1",
                Instant.now()
        );

        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("Database error"));

        assertDoesNotThrow(() -> auditEventListener.onAccessDenied(event));
    }

    @Test
    void onAccessDenied_oversizedFieldsAreSafelyTruncated() {
        String longUsername = "u".repeat(150);
        String longEndpoint = "/api/" + "a".repeat(600);
        String longMethod = "METHOD_TOO_LONG";
        String longIp = "1".repeat(60);
        Instant now = Instant.now();

        AccessDeniedEvent event = new AccessDeniedEvent(
                999L,
                longUsername,
                longEndpoint,
                longMethod,
                longIp,
                now
        );

        auditEventListener.onAccessDenied(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertThat(savedLog.getUsername()).hasSize(100);
        assertThat(savedLog.getEndpoint()).hasSize(500);
        assertThat(savedLog.getHttpMethod()).hasSize(10);
        assertThat(savedLog.getIpAddress()).hasSize(45);
    }

    @Test
    void onAccessDenied_nullFieldsHandledSafely() {
        AccessDeniedEvent event = new AccessDeniedEvent(
                null,
                null,
                null,
                null,
                null,
                null
        );

        auditEventListener.onAccessDenied(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertThat(savedLog.getEndpoint()).isEqualTo("UNKNOWN");
        assertThat(savedLog.getHttpMethod()).isEqualTo("UNKNOWN");
        assertThat(savedLog.getTimestamp()).isNotNull();
    }
}
