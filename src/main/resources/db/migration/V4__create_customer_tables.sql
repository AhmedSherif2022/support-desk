-- E2 customer core: customers and caller verification.

CREATE TABLE customers
(
    id                 BIGSERIAL    PRIMARY KEY,
    cif                VARCHAR(30)  NOT NULL,
    full_name          VARCHAR(150) NOT NULL,
    national_id        VARCHAR(20)  NOT NULL,
    mobile             VARCHAR(20)  NOT NULL,
    date_of_birth      DATE,
    segment            VARCHAR(20)  NOT NULL,
    preferred_language VARCHAR(2)   NOT NULL DEFAULT 'AR',
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_customers_cif UNIQUE (cif),
    CONSTRAINT uk_customers_national_id UNIQUE (national_id),
    CONSTRAINT ck_customers_segment CHECK (segment IN ('RETAIL', 'PREMIUM', 'VIP', 'BUSINESS'))
);
CREATE UNIQUE INDEX ux_customers_cif_upper ON customers (UPPER(cif));   -- fast case-insensitive CIF lookup
CREATE INDEX ix_customers_mobile ON customers (mobile);

CREATE TABLE caller_verifications
(
    id          BIGSERIAL   PRIMARY KEY,
    customer_id BIGINT      NOT NULL REFERENCES customers (id),
    agent_id    BIGINT      NOT NULL REFERENCES users (id),
    verified_at TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    ended_at    TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0
);
-- Fast lookup of the open verification for an agent + customer.
CREATE INDEX ix_verification_open ON caller_verifications (agent_id, customer_id) WHERE ended_at IS NULL;

CREATE TABLE caller_verification_checks
(
    verification_id BIGINT      NOT NULL REFERENCES caller_verifications (id) ON DELETE CASCADE,
    check_name      VARCHAR(40) NOT NULL,
    PRIMARY KEY (verification_id, check_name),
    CONSTRAINT ck_verification_check CHECK (check_name IN
        ('DATE_OF_BIRTH', 'NATIONAL_ID_LAST4', 'MOBILE_ON_FILE', 'RECENT_TRANSACTION'))
);
