-- E1-S3: every record carries who (user + role), what (action), which (entity + id), and before/after.
ALTER TABLE audit_logs
    ADD COLUMN role         VARCHAR(30),
    ADD COLUMN entity       VARCHAR(50),
    ADD COLUMN entity_id    VARCHAR(100),
    ADD COLUMN before_state TEXT,
    ADD COLUMN after_state  TEXT,
    DROP COLUMN updated_at,
    DROP COLUMN version;

-- Business events (a login, a data view) are not always tied to a URL.
ALTER TABLE audit_logs
    ALTER COLUMN endpoint DROP NOT NULL,
    ALTER COLUMN http_method DROP NOT NULL;

ALTER TABLE audit_logs DROP CONSTRAINT ck_audit_logs_action;
ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_action
    CHECK (action IN ('ACCESS_DENIED', 'USER_LOGIN', 'USER_LOCKED', 'CUSTOMER_DATA_VIEWED'));

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity, entity_id, created_at);

-- Append-only: no row may be changed or removed, whichever code or user connects.
CREATE FUNCTION audit_logs_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only: % is not allowed', TG_OP;
END;
$$;

CREATE TRIGGER audit_logs_no_update_delete
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit_logs_append_only();

CREATE TRIGGER audit_logs_no_truncate
    BEFORE TRUNCATE ON audit_logs
    FOR EACH STATEMENT EXECUTE FUNCTION audit_logs_append_only();
