package com.supportdesk.audit.enums;

// Adding a value also needs the ck_audit_logs_action constraint widened in a migration.
public enum AuditAction {
    ACCESS_DENIED,
    USER_LOGIN,
    USER_LOCKED,
    CUSTOMER_DATA_VIEWED,
    CUSTOMER_SEARCHED,
    CALLER_VERIFIED,
    CALLER_VERIFICATION_FAILED,
    CUSTOMER_FIELD_REVEALED,
    TICKET_CREATED,
    TICKET_STATUS_CHANGED,
    TICKET_TRANSITION_REJECTED,
    CUSTOMER_CONTACTED,
    SLA_WARNING,
    SLA_BREACHED,
    GUIDE_STARTED,
    GUIDE_COMPLETED
    }

