package com.supportdesk.audit.repository;

import com.supportdesk.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /** One entity's history, newest first. Uses idx_audit_logs_entity. */
    List<AuditLog> findByEntityAndEntityIdOrderByCreatedAtDescIdDesc(String entity, String entityId);
}
