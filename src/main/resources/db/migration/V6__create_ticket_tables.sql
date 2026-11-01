-- E3 ticket core (Member 1): ticket categories and tickets.
-- Numbering: V1 users · V2 token version · V3 refresh tokens · V4 customers · V5 audit log · V6 tickets (this file).
-- Notes and timeline tables come later (Member 3, V7).

-- ---------------------------------------------------------------- categories
CREATE TABLE ticket_categories
(
    id                 BIGSERIAL    PRIMARY KEY,
    code               VARCHAR(50)  NOT NULL,
    name               VARCHAR(100) NOT NULL,
    parent_id          BIGINT       REFERENCES ticket_categories (id),
    default_priority   VARCHAR(10)  NOT NULL,
    reference_required BOOLEAN      NOT NULL DEFAULT FALSE,
    reference_pattern  VARCHAR(100),
    reference_hint     VARCHAR(150),
    default_error_code VARCHAR(40),
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_ticket_categories_code UNIQUE (code),
    CONSTRAINT ck_ticket_categories_priority CHECK (default_priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Top-level categories (what the agent picks first).
INSERT INTO ticket_categories (code, name, default_priority, created_at, updated_at) VALUES
    ('AUTHENTICATION', 'Authentication',     'HIGH',   now(), now()),
    ('OTP',            'OTP',                'MEDIUM', now(), now()),
    ('TRANSFERS',      'Transfers',          'HIGH',   now(), now()),
    ('PAYMENTS',       'Payments',           'HIGH',   now(), now()),
    ('MOBILE_APP',     'Mobile Application', 'MEDIUM', now(), now());

-- Sub-categories (a ticket always uses one of these).
INSERT INTO ticket_categories (code, name, parent_id, default_priority, reference_required, reference_pattern,
                               reference_hint, default_error_code, created_at, updated_at)
SELECT c.code, c.name, p.id, c.prio, c.ref_req, c.ref_pattern, c.ref_hint, c.err, now(), now()
FROM (VALUES
    ('LOGIN_FAILED',         'Login Failed',          'AUTHENTICATION', 'HIGH',   FALSE, NULL, NULL, 'ERR_AUTH_503'),
    ('OTP_NOT_RECEIVED',     'OTP Not Received',      'OTP',            'MEDIUM', FALSE, NULL, NULL, NULL),
    ('TRANSFER_FAILED',      'Transfer Failed',       'TRANSFERS',      'HIGH',   TRUE,  '^TRX-\d{4}-\d{5}$',
                             'Use the format TRX-2026-00981', 'TRX_DEBIT_NO_CREDIT'),
    ('DEBITED_NOT_CREDITED', 'Debited Not Credited',  'TRANSFERS',      'HIGH',   TRUE,  '^TRX-\d{4}-\d{5}$',
                             'Use the format TRX-2026-00981', 'TRX_DEBIT_NO_CREDIT'),
    ('PAYMENT_FAILED',       'Payment Failed',        'PAYMENTS',       'HIGH',   TRUE,  '^TRX-\d{4}-\d{5}$',
                             'Use the format TRX-2026-01111', NULL),
    ('APP_CRASHING',         'App Crashing',          'MOBILE_APP',     'MEDIUM', FALSE, NULL, NULL, NULL)
) AS c (code, name, parent_code, prio, ref_req, ref_pattern, ref_hint, err)
JOIN ticket_categories p ON p.code = c.parent_code;

-- ---------------------------------------------------------------- tickets
-- New tickets get #2100, #2101... (seed tickets use their own numbers like #2001).
CREATE SEQUENCE ticket_number_seq START WITH 2100;

CREATE TABLE tickets
(
    id                       BIGSERIAL     PRIMARY KEY,
    ticket_number            BIGINT        NOT NULL,
    customer_id              BIGINT        NOT NULL REFERENCES customers (id),
    category_id              BIGINT        NOT NULL REFERENCES ticket_categories (id),
    title                    VARCHAR(150)  NOT NULL,
    description              VARCHAR(2000) NOT NULL,
    priority                 VARCHAR(10)   NOT NULL,
    status                   VARCHAR(25)   NOT NULL,
    origin                   VARCHAR(15)   NOT NULL,
    platform                 VARCHAR(20),
    error_code               VARCHAR(40),
    reference                VARCHAR(40),
    assigned_agent_id        BIGINT        REFERENCES users (id),
    created_by_id            BIGINT        REFERENCES users (id),
    sla_started_at           TIMESTAMPTZ   NOT NULL,
    sla_due_at               TIMESTAMPTZ   NOT NULL,
    sla_paused_at            TIMESTAMPTZ,
    sla_state                VARCHAR(15)   NOT NULL,
    resolved_at              TIMESTAMPTZ,
    closed_at                TIMESTAMPTZ,
    reopen_count             INT           NOT NULL DEFAULT 0,
    last_customer_contact_at TIMESTAMPTZ,
    created_at               TIMESTAMPTZ   NOT NULL,
    updated_at               TIMESTAMPTZ   NOT NULL,
    version                  BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uk_tickets_number UNIQUE (ticket_number),
    CONSTRAINT ck_tickets_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_tickets_status CHECK (status IN ('NEW', 'TROUBLESHOOTING', 'WAITING_ON_CUSTOMER', 'ESCALATED',
                                                   'WITH_SRE', 'SRE_FIXED', 'RESOLVED', 'REOPENED', 'CLOSED')),
    CONSTRAINT ck_tickets_origin CHECK (origin IN ('MANUAL', 'AUTO_CREATED', 'FROM_INCIDENT')),
    CONSTRAINT ck_tickets_platform CHECK (platform IS NULL OR platform IN ('MOBILE', 'WEB', 'BRANCH')),
    CONSTRAINT ck_tickets_sla_state CHECK (sla_state IN ('ON_TRACK', 'AT_RISK', 'BREACHED', 'PAUSED', 'MET'))
);
CREATE INDEX ix_tickets_customer_status ON tickets (customer_id, status);
CREATE INDEX ix_tickets_agent_status ON tickets (assigned_agent_id, status);
CREATE INDEX ix_tickets_status_due ON tickets (status, sla_due_at);
