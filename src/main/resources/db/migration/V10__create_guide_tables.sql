-- E4 guided troubleshooting: guides, their versions, and the agent's runs through them.

CREATE TABLE guides
(
    id          BIGSERIAL    PRIMARY KEY,
    title       VARCHAR(150) NOT NULL,
    category_id BIGINT       NOT NULL REFERENCES ticket_categories (id),   -- the sub-category it troubleshoots
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_guides_title UNIQUE (title)
);

-- E4-S1: a guide is recommended only on its platforms, and ahead of category matches for its error codes.
CREATE TABLE guide_platforms
(
    guide_id BIGINT      NOT NULL REFERENCES guides (id),
    platform VARCHAR(20) NOT NULL,

    PRIMARY KEY (guide_id, platform),
    CONSTRAINT ck_guide_platforms_platform CHECK (platform IN ('MOBILE', 'WEB', 'BRANCH'))
);

CREATE TABLE guide_error_codes
(
    guide_id   BIGINT      NOT NULL REFERENCES guides (id),
    error_code VARCHAR(40) NOT NULL,

    PRIMARY KEY (guide_id, error_code)
);

CREATE TABLE guide_versions
(
    id             BIGSERIAL   PRIMARY KEY,
    guide_id       BIGINT      NOT NULL REFERENCES guides (id),
    version_number INT         NOT NULL,
    state          VARCHAR(15) NOT NULL,
    tree           JSONB       NOT NULL,
    published_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    version        BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT uk_guide_versions_number UNIQUE (guide_id, version_number),
    CONSTRAINT ck_guide_versions_state CHECK (state IN ('DRAFT', 'IN_REVIEW', 'LIVE', 'RETIRED'))
);
CREATE UNIQUE INDEX uk_guide_versions_one_live ON guide_versions (guide_id) WHERE state = 'LIVE';

-- E4-S2: a run keeps the version it started on (E4-S4), so a publish mid-call never changes its questions.
CREATE TABLE troubleshooting_sessions
(
    id               BIGSERIAL   PRIMARY KEY,
    ticket_id        BIGINT      NOT NULL REFERENCES tickets (id),
    guide_version_id BIGINT      NOT NULL REFERENCES guide_versions (id),
    started_by_id    BIGINT      NOT NULL REFERENCES users (id),
    started_at       TIMESTAMPTZ NOT NULL,
    completed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    version          BIGINT      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_troubleshooting_sessions_active ON troubleshooting_sessions (ticket_id) WHERE completed_at IS NULL;

-- The answers in the order given. The run's current step is replayed from them against the pinned tree.
CREATE TABLE troubleshooting_answers
(
    id          BIGSERIAL   PRIMARY KEY,
    session_id  BIGINT      NOT NULL REFERENCES troubleshooting_sessions (id),
    position    INT         NOT NULL,
    step_key    VARCHAR(60) NOT NULL,
    answer_key  VARCHAR(60) NOT NULL,
    answered_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_troubleshooting_answers_position UNIQUE (session_id, position)
);

-- A category's usual error code is not one the customer hit, so guides neither match nor hint on it.
ALTER TABLE tickets ADD COLUMN error_code_from_category BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE audit_logs DROP CONSTRAINT ck_audit_logs_action;
ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_action CHECK (action IN (
    'ACCESS_DENIED', 'USER_LOGIN', 'USER_LOCKED', 'CUSTOMER_DATA_VIEWED',
    'CUSTOMER_SEARCHED', 'CALLER_VERIFIED', 'CALLER_VERIFICATION_FAILED', 'CUSTOMER_FIELD_REVEALED',
    'TICKET_CREATED', 'TICKET_STATUS_CHANGED', 'TICKET_TRANSITION_REJECTED', 'CUSTOMER_CONTACTED',
    'SLA_WARNING', 'SLA_BREACHED', 'GUIDE_STARTED'));
