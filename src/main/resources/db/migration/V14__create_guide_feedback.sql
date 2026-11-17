-- E4-S5: an agent flags a step of a finished run as wrong or unclear. The report keeps the exact session and the
-- pinned version, so the admin reviews the question the agent actually saw, not whatever is live now.
CREATE TABLE guide_feedback
(
    id               BIGSERIAL     PRIMARY KEY,
    session_id       BIGINT        NOT NULL REFERENCES troubleshooting_sessions (id),
    guide_version_id BIGINT        NOT NULL REFERENCES guide_versions (id),
    step_key         VARCHAR(60)   NOT NULL,
    agent_id         BIGINT        NOT NULL REFERENCES users (id),
    comment          VARCHAR(2000) NOT NULL,
    status           VARCHAR(15)   NOT NULL DEFAULT 'OPEN',
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    version          BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT ck_guide_feedback_status CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'DISMISSED'))
);

-- The admin queue reads newest first; the other indexes serve the foreign keys and a per-version review.
CREATE INDEX idx_guide_feedback_created_at ON guide_feedback (created_at DESC, id DESC);
CREATE INDEX idx_guide_feedback_session_id ON guide_feedback (session_id);
CREATE INDEX idx_guide_feedback_guide_version_id ON guide_feedback (guide_version_id);
CREATE INDEX idx_guide_feedback_agent_id ON guide_feedback (agent_id);

ALTER TABLE audit_logs DROP CONSTRAINT ck_audit_logs_action;
ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_action CHECK (action IN (
    'ACCESS_DENIED', 'USER_LOGIN', 'USER_LOCKED', 'CUSTOMER_DATA_VIEWED',
    'CUSTOMER_SEARCHED', 'CALLER_VERIFIED', 'CALLER_VERIFICATION_FAILED', 'CUSTOMER_FIELD_REVEALED',
    'TICKET_CREATED', 'TICKET_STATUS_CHANGED', 'TICKET_TRANSITION_REJECTED', 'CUSTOMER_CONTACTED',
    'SLA_WARNING', 'SLA_BREACHED', 'GUIDE_STARTED', 'GUIDE_COMPLETED', 'GUIDE_STEP_FLAGGED'));
