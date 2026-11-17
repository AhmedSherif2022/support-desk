-- One flag per agent, step and run: a second one is refused (409), and this index backs that check.
-- V14 allowed repeats, so a database may already hold some: keep each agent's first flag of a step.
DELETE FROM guide_feedback later
    USING guide_feedback earlier
WHERE later.session_id = earlier.session_id
  AND later.step_key = earlier.step_key
  AND later.agent_id = earlier.agent_id
  AND later.id > earlier.id;

CREATE UNIQUE INDEX uk_guide_feedback_agent_step ON guide_feedback (session_id, step_key, agent_id);
-- The unique index starts with session_id, so it serves the foreign key too.
DROP INDEX idx_guide_feedback_session_id;

-- E8-S3: the Guide Admin marks a flag done.
ALTER TABLE audit_logs DROP CONSTRAINT ck_audit_logs_action;
ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_action CHECK (action IN (
    'ACCESS_DENIED', 'USER_LOGIN', 'USER_LOCKED', 'CUSTOMER_DATA_VIEWED',
    'CUSTOMER_SEARCHED', 'CALLER_VERIFIED', 'CALLER_VERIFICATION_FAILED', 'CUSTOMER_FIELD_REVEALED',
    'TICKET_CREATED', 'TICKET_STATUS_CHANGED', 'TICKET_TRANSITION_REJECTED', 'CUSTOMER_CONTACTED',
    'SLA_WARNING', 'SLA_BREACHED', 'GUIDE_STARTED', 'GUIDE_COMPLETED', 'GUIDE_STEP_FLAGGED',
    'GUIDE_FEEDBACK_RESOLVED'));
