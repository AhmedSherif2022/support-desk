package com.supportdesk.audit.enums;
import io.swagger.v3.oas.annotations.media.Schema;

// Adding a value also needs the ck_audit_logs_action constraint widened in a migration.
@Schema(enumAsRef = true, description = "What an audit record is about.\n"
        + "- ACCESS_DENIED: a request was refused for the user's role (403)\n"
        + "- USER_LOGIN, USER_LOCKED: a login, and an account locked after failed logins\n"
        + "- CUSTOMER_SEARCHED, CUSTOMER_DATA_VIEWED: a customer search, and a Customer 360 opened\n"
        + "- CALLER_VERIFIED, CALLER_VERIFICATION_FAILED: the caller's identity checks passed or failed\n"
        + "- CUSTOMER_FIELD_REVEALED: one masked field (national ID or mobile) was shown\n"
        + "- TICKET_CREATED, TICKET_STATUS_CHANGED: a ticket was created, or took a step (the record names the step)\n"
        + "- TICKET_TRANSITION_REJECTED: a step was refused because it does not start from the ticket's status\n"
        + "- CUSTOMER_CONTACTED: what the customer was told was logged\n"
        + "- SLA_WARNING, SLA_BREACHED: a ticket's SLA became at risk, or breached\n"
        + "- GUIDE_STARTED, GUIDE_COMPLETED: a troubleshooting guide was started or finished on a ticket\n"
        + "- GUIDE_STEP_FLAGGED: an agent reported a step of a finished run as wrong or unclear\n"
        + "- GUIDE_FEEDBACK_RESOLVED: the Guide Admin marked a flagged step done\n"
        + "- MONITOR_JOB_RUN: the monitor job ran (rows read, tickets created, duplicates, grouped, unknown codes)\n"
        + "- ERROR_EVENT_JOINED: a detected error joined a customer's open ticket instead of making a new one\n"
        + "- INCIDENT_CREATED: enough different customers hit the same error on the same platform, so their tickets "
        + "were grouped into one incident and set to CRITICAL\n"
        + "- INCIDENT_TICKETS_LINKED: later tickets of the same error and platform joined an existing incident\n"
        + "- SETTING_CHANGED: the Guide Admin changed a runtime setting (the record holds the old and new value)")
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
    GUIDE_COMPLETED,
    GUIDE_STEP_FLAGGED,
    GUIDE_FEEDBACK_RESOLVED,
    MONITOR_JOB_RUN,
    ERROR_EVENT_JOINED,
    INCIDENT_CREATED,
    INCIDENT_TICKETS_LINKED,
    SETTING_CHANGED
}

