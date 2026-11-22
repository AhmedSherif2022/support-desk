
CREATE TABLE error_rule
(
    id             BIGSERIAL    PRIMARY KEY,
    error_code     VARCHAR(40)  NOT NULL,
    category_id    BIGINT       NOT NULL REFERENCES ticket_categories (id),
    issue          VARCHAR(150) NOT NULL,                                     -- becomes the ticket title
    priority       VARCHAR(15)  NOT NULL,
    auto_escalate  BOOLEAN      NOT NULL DEFAULT FALSE,                       -- read by Member 2 (S4)
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_error_rule_code UNIQUE (error_code),
    CONSTRAINT ck_error_rule_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);


CREATE TABLE detected_error_event
(
    id              BIGSERIAL     PRIMARY KEY,
    customer_id     BIGINT        NOT NULL REFERENCES customers (id),
    error_code      VARCHAR(40)   NOT NULL,
    platform        VARCHAR(10)   NOT NULL,
    service         VARCHAR(40)   NOT NULL,
    message         VARCHAR(500),
    correlation_id  VARCHAR(80),
    occurred_at     TIMESTAMPTZ   NOT NULL,
    processed_at    TIMESTAMPTZ,                                              -- NULL = not processed yet
    outcome         VARCHAR(20),                                              -- what the job did with it
    ticket_id       BIGINT        REFERENCES tickets (id),                    -- the ticket it created or joined

    CONSTRAINT ck_detected_error_event_platform CHECK (platform IN ('MOBILE', 'WEB')),
    CONSTRAINT ck_detected_error_event_outcome CHECK (outcome IS NULL OR outcome IN
        ('CREATED', 'DUPLICATE', 'GROUPED', 'UNKNOWN_CODE', 'FAILED')),
    -- processed_at and outcome are set together: a row is either untouched or fully handled
    CONSTRAINT ck_detected_error_event_processed CHECK ((processed_at IS NULL) = (outcome IS NULL))
);


CREATE INDEX ix_detected_error_event_unprocessed ON detected_error_event (occurred_at, id) WHERE processed_at IS NULL;

CREATE INDEX ix_detected_error_event_code_time ON detected_error_event (error_code, platform, occurred_at);

CREATE INDEX ix_detected_error_event_ticket ON detected_error_event (ticket_id);


CREATE TABLE monitor_job_run
(
    id            BIGSERIAL    PRIMARY KEY,
    trigger       VARCHAR(10)  NOT NULL,                                      -- SCHEDULE or MANUAL ("Run job now")
    started_at    TIMESTAMPTZ  NOT NULL,
    finished_at   TIMESTAMPTZ,                                                -- NULL while running
    rows_read     INT          NOT NULL DEFAULT 0,
    created       INT          NOT NULL DEFAULT 0,
    duplicates    INT          NOT NULL DEFAULT 0,
    grouped       INT          NOT NULL DEFAULT 0,
    unknown_codes INT          NOT NULL DEFAULT 0,
    failed        INT          NOT NULL DEFAULT 0,                            -- rows that threw (outcome FAILED)
    error         VARCHAR(1000),                                              -- set when the run failed

    CONSTRAINT ck_monitor_job_run_trigger CHECK (trigger IN ('SCHEDULE', 'MANUAL'))
);
CREATE INDEX ix_monitor_job_run_started ON monitor_job_run (started_at DESC);


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
            COALESCE(old_values, ARRAY[]::TEXT[]) || ARRAY['MONITOR_JOB_RUN', 'ERROR_EVENT_JOINED']));

    ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS ck_audit_logs_action;
    EXECUTE format('ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_action CHECK (action IN (%s))',
                   (SELECT string_agg(quote_literal(v), ', ' ORDER BY v) FROM unnest(new_values) AS v));
END
$$;