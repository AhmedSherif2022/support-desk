-- Test customers for local development only (local profile loads db/seed, prod does not).
-- Repeatable: Flyway re-runs it when this file changes; ON CONFLICT keeps existing rows.
INSERT INTO customers (cif, full_name, national_id, mobile, date_of_birth, segment, preferred_language, created_at, updated_at)
VALUES ('CIF-100245', 'Sara Mahmoud', '29001011234567', '+201001234567', '1990-01-01', 'PREMIUM', 'AR', now(), now()),
       ('CIF-100311', 'Mai Hassan',   '29205051234568', '+201112345678', '1992-05-05', 'RETAIL',  'AR', now(), now()),
       ('CIF-100101', 'Ahmed Ali',    '28803031234569', '+201223456789', '1988-03-03', 'VIP',     'EN', now(), now())
ON CONFLICT DO NOTHING;
