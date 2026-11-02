-- E2 Customer 360 and E3 ticket workspace.

-- E2-S1: search results show the customer's digital-banking enrollment.
ALTER TABLE customers ADD COLUMN enrollment_status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED';
ALTER TABLE customers ADD CONSTRAINT ck_customers_enrollment
    CHECK (enrollment_status IN ('ENROLLED', 'NOT_ENROLLED', 'SUSPENDED'));

-- E2-S4/S5: what the customer did on each channel. Written by the channels, read by Customer 360.
CREATE TABLE customer_activity_log
(
    id          BIGSERIAL   PRIMARY KEY,
    customer_id BIGINT      NOT NULL REFERENCES customers (id),
    occurred_at TIMESTAMPTZ NOT NULL,
    event_type  VARCHAR(50) NOT NULL,
    platform    VARCHAR(20) NOT NULL,
    service     VARCHAR(50) NOT NULL,   -- top-level ticket category code, e.g. AUTHENTICATION
    result      VARCHAR(10) NOT NULL,
    error_code  VARCHAR(40),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_activity_platform CHECK (platform IN ('MOBILE', 'WEB', 'BRANCH')),
    CONSTRAINT ck_activity_result CHECK (result IN ('SUCCESS', 'FAILURE'))
);
-- Every read is "one customer, newest first, within a time range".
CREATE INDEX idx_activity_customer_time ON customer_activity_log (customer_id, occurred_at);

-- E3-S6: what the agent told the customer.
CREATE TABLE ticket_contacts
(
    id         BIGSERIAL     PRIMARY KEY,
    ticket_id  BIGINT        NOT NULL REFERENCES tickets (id),
    author_id  BIGINT        REFERENCES users (id),
    note       VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_ticket_contacts_ticket ON ticket_contacts (ticket_id, created_at);

-- New audit actions for E2 and E3.
ALTER TABLE audit_logs DROP CONSTRAINT ck_audit_logs_action;
ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_action CHECK (action IN (
    'ACCESS_DENIED', 'USER_LOGIN', 'USER_LOCKED', 'CUSTOMER_DATA_VIEWED',
    'CUSTOMER_SEARCHED', 'CALLER_VERIFIED', 'CALLER_VERIFICATION_FAILED', 'CUSTOMER_FIELD_REVEALED',
    'TICKET_CREATED', 'TICKET_STATUS_CHANGED', 'TICKET_TRANSITION_REJECTED', 'CUSTOMER_CONTACTED',
    'SLA_WARNING', 'SLA_BREACHED'));
