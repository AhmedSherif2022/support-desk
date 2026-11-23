-- E6-S3 (Member 2): incidents, the incident <-> ticket link and the configurable grouping thresholds.
-- E5 (Azure DevOps) is postponed: no work item columns here; they can be added by E5 without touching this layout.

-- ---------------------------------------------------------------- incidents
-- One grouped outage: the same error code on the same platform hit by many different customers in a short window.
CREATE TABLE incident
(
    id                      BIGSERIAL    PRIMARY KEY,
    error_code              VARCHAR(40)  NOT NULL,
    platform                VARCHAR(10)  NOT NULL,
    service                 VARCHAR(40)  NOT NULL,
    status                  VARCHAR(15)  NOT NULL DEFAULT 'OPEN',
    affected_customer_count INT          NOT NULL DEFAULT 0,
    first_occurred_at       TIMESTAMPTZ  NOT NULL,                             -- earliest linked error
    last_occurred_at        TIMESTAMPTZ  NOT NULL,                             -- latest linked error
    resolved_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,
    version                 BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT ck_incident_platform CHECK (platform IN ('MOBILE', 'WEB', 'BRANCH')),
    CONSTRAINT ck_incident_status CHECK (status IN ('OPEN', 'SRE_FIXED', 'RESOLVED')),
    CONSTRAINT ck_incident_affected CHECK (affected_customer_count >= 0)
);

-- At most one OPEN incident per error code and platform: two monitor runs can never both open one for the same outage.
CREATE UNIQUE INDEX uk_incident_open_group ON incident (error_code, platform) WHERE status = 'OPEN';
CREATE INDEX ix_incident_status_created ON incident (status, created_at DESC);


-- ---------------------------------------------------------------- incident <-> ticket
CREATE TABLE incident_ticket
(
    id          BIGSERIAL   PRIMARY KEY,
    incident_id BIGINT      NOT NULL REFERENCES incident (id),
    ticket_id   BIGINT      NOT NULL REFERENCES tickets (id),
    linked_at   TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_incident_ticket_ticket UNIQUE (ticket_id)                   -- a ticket belongs to one incident only
);
CREATE INDEX ix_incident_ticket_incident ON incident_ticket (incident_id);

-- The grouping query walks open automatic tickets that are not linked yet.
CREATE INDEX ix_tickets_origin_status ON tickets (origin, status);


-- ---------------------------------------------------------------- settings
-- Key/value store for values the Guide Admin tunes at runtime. Defaults are inserted once; later runs of the
-- application never overwrite what an administrator changed (the row is only read, and written through the API).
CREATE TABLE app_settings
(
    setting_key   VARCHAR(80)  PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

INSERT INTO app_settings (setting_key, setting_value, updated_at) VALUES
    ('incident.grouping.min-customers',  '5',  now()),
    ('incident.grouping.window-minutes', '15', now())
ON CONFLICT (setting_key) DO NOTHING;


-- ---------------------------------------------------------------- audit actions
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
            COALESCE(old_values, ARRAY[]::TEXT[]) || ARRAY['INCIDENT_CREATED', 'SETTING_CHANGED']));

    ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS ck_audit_logs_action;
    EXECUTE format('ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_action CHECK (action IN (%s))',
                   (SELECT string_agg(quote_literal(v), ', ' ORDER BY v) FROM unnest(new_values) AS v));
END
$$;
