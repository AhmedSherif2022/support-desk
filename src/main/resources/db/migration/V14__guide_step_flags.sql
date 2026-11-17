-- E4-S5: an agent flags a wrong or unclear step of a finished guide run, for the Guide Admin's feedback queue.
CREATE TABLE guide_step_flags
(
    id            BIGSERIAL     PRIMARY KEY,
    session_id    BIGINT        NOT NULL REFERENCES troubleshooting_sessions (id),
    step_key      VARCHAR(60)   NOT NULL,
    comment       VARCHAR(1000) NOT NULL,
    flagged_by_id BIGINT        NOT NULL REFERENCES users (id),
    flagged_at    TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_guide_step_flags_session ON guide_step_flags (session_id);

ALTER TABLE audit_logs DROP CONSTRAINT ck_audit_logs_action;
ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_action CHECK (action IN (
    'ACCESS_DENIED', 'USER_LOGIN', 'USER_LOCKED', 'CUSTOMER_DATA_VIEWED',
    'CUSTOMER_SEARCHED', 'CALLER_VERIFIED', 'CALLER_VERIFICATION_FAILED', 'CUSTOMER_FIELD_REVEALED',
    'TICKET_CREATED', 'TICKET_STATUS_CHANGED', 'TICKET_TRANSITION_REJECTED', 'CUSTOMER_CONTACTED',
    'SLA_WARNING', 'SLA_BREACHED', 'GUIDE_STARTED', 'GUIDE_COMPLETED', 'GUIDE_STEP_FLAGGED'));
