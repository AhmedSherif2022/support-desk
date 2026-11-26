-- E5 Azure DevOps integration, contract (Member 1: S1 escalate, S3 reliable delivery).
-- escalation: one row per escalated ticket (reason, urgency, the Azure work item and its sync state).
-- azure_outbox: every call to Azure, written in the same transaction as the ticket change and sent by a worker
-- with retries and backoff. The idempotency key (TICKET-<number>, INCIDENT-<id>) is unique, so a retry or a
-- second escalation never creates a second work item.

CREATE TABLE escalation
(
    id              BIGSERIAL     PRIMARY KEY,
    ticket_id       BIGINT        NOT NULL REFERENCES tickets (id),
    reason          VARCHAR(500)  NOT NULL,
    urgency         VARCHAR(10)   NOT NULL,
    escalated_by_id BIGINT        REFERENCES users (id),      -- null when the system escalates (E6-S4)
    escalated_at    TIMESTAMPTZ   NOT NULL,
    work_item_id    BIGINT,                                   -- Azure DevOps work item; null until synced
    azure_state     VARCHAR(40),                              -- e.g. New, Active, Resolved (read by Member 2)
    azure_revision  INT,                                      -- latest Azure revision seen (Member 2 orders events by it)
    sync_status     VARCHAR(10)   NOT NULL DEFAULT 'PENDING',
    last_synced_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uk_escalation_ticket UNIQUE (ticket_id),
    CONSTRAINT ck_escalation_urgency CHECK (urgency IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_escalation_sync CHECK (sync_status IN ('PENDING', 'SYNCED', 'FAILED'))
);

CREATE INDEX ix_escalation_work_item ON escalation (work_item_id);
CREATE INDEX ix_escalation_sync ON escalation (sync_status);


CREATE TABLE azure_outbox
(
    id               BIGSERIAL     PRIMARY KEY,
    aggregate_type   VARCHAR(10)   NOT NULL,                  -- TICKET or INCIDENT
    aggregate_id     BIGINT        NOT NULL,                  -- tickets.id or incident.id
    idempotency_key  VARCHAR(80)   NOT NULL,                  -- TICKET-2001, INCIDENT-7, TICKET-2001-COMMENT-<uuid>
    operation        VARCHAR(20)   NOT NULL,
    payload          TEXT          NOT NULL,                  -- JSON; never CIF, national ID or mobile
    status           VARCHAR(10)   NOT NULL DEFAULT 'PENDING',
    attempts         INT           NOT NULL DEFAULT 0,
    next_attempt_at  TIMESTAMPTZ   NOT NULL,
    last_error       VARCHAR(1000),
    work_item_id     BIGINT,                                  -- the work item created or commented on
    sent_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    version          BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uk_azure_outbox_key UNIQUE (idempotency_key),
    CONSTRAINT ck_azure_outbox_aggregate CHECK (aggregate_type IN ('TICKET', 'INCIDENT')),
    CONSTRAINT ck_azure_outbox_operation CHECK (operation IN ('CREATE_WORK_ITEM', 'ADD_COMMENT')),
    CONSTRAINT ck_azure_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

-- The worker picks due rows: PENDING and next_attempt_at reached.
CREATE INDEX ix_azure_outbox_due ON azure_outbox (next_attempt_at) WHERE status = 'PENDING';
CREATE INDEX ix_azure_outbox_aggregate ON azure_outbox (aggregate_type, aggregate_id);


-- Audit: E5 actions for all three members, widened once so the members' migrations never fight over the check.
DO $$
DECLARE
    old_values TEXT[];
    new_values TEXT[];
BEGIN
    SELECT array_agg(DISTINCT m[1])
    INTO old_values
    FROM pg_constraint c,
         regexp_matches(pg_get_constraintdef(c.oid), '''([A-Z_]+)''', 'g') AS m
    WHERE c.conname = 'ck_audit_logs_action';

    new_values := ARRAY(SELECT DISTINCT unnest(
            COALESCE(old_values, ARRAY[]::TEXT[]) || ARRAY['TICKET_ESCALATED', 'AZURE_SYNC_FAILED',
                'AZURE_WEBHOOK_RECEIVED', 'SRE_UPDATE_REQUESTED', 'FIX_CONFIRMED', 'FIX_REJECTED']));

    ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS ck_audit_logs_action;
    EXECUTE format('ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_action CHECK (action IN (%s))',
                   (SELECT string_agg(quote_literal(v), ', ' ORDER BY v) FROM unnest(new_values) AS v));
END
$$;
