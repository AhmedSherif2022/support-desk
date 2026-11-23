-- Guide Admin can add ticket categories and sub-categories through the API (audited as CATEGORY_CREATED).
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
            COALESCE(old_values, ARRAY[]::TEXT[]) || ARRAY['CATEGORY_CREATED']));

    ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS ck_audit_logs_action;
    EXECUTE format('ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_action CHECK (action IN (%s))',
                   (SELECT string_agg(quote_literal(v), ', ' ORDER BY v) FROM unnest(new_values) AS v));
END
$$;
