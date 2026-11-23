-- E6-S3: later tickets that join an existing incident are audited too (INCIDENT_TICKETS_LINKED).
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
            COALESCE(old_values, ARRAY[]::TEXT[]) || ARRAY['INCIDENT_TICKETS_LINKED']));

    ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS ck_audit_logs_action;
    EXECUTE format('ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_action CHECK (action IN (%s))',
                   (SELECT string_agg(quote_literal(v), ', ' ORDER BY v) FROM unnest(new_values) AS v));
END
$$;
