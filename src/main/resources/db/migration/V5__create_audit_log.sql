CREATE TABLE audit_logs
(
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT,
    username    VARCHAR(100),
    action      VARCHAR(50)  NOT NULL,
    endpoint    VARCHAR(500) NOT NULL,
    http_method VARCHAR(10)  NOT NULL,
    ip_address  VARCHAR(45),
    timestamp   TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT ck_audit_logs_action CHECK (action IN ('ACCESS_DENIED'))
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs (timestamp);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
