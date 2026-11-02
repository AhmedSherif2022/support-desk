-- Local queue samples (local profile only).
-- Runs after R__seed_customers.sql and R__seed_staff_users.sql alphabetically.
-- Seeded numbers #2001..#2004 stay below ticket_number_seq (starts at 2100).
-- SLA targets from application.yml: CRITICAL = 2h, HIGH = 8h, MEDIUM = 24h, LOW = 72h (warning at 75%).

-- #2001: HIGH (8h target), 6h30m elapsed (~81%) -> AT_RISK
INSERT INTO tickets (ticket_number, customer_id, category_id, title, description,
                     priority, status, origin, platform, error_code, reference,
                     assigned_agent_id, created_by_id,
                     sla_started_at, sla_due_at, sla_paused_at, sla_state,
                     created_at, updated_at)
SELECT 2001, c.id, cat.id,
       'Card payment declined',
       'Customer reported a declined merchant payment despite sufficient balance.',
       'HIGH', 'TROUBLESHOOTING', 'MANUAL', 'MOBILE', NULL, 'TRX-2026-01111',
       u.id, u.id,
       now() - interval '6 hours 30 minutes',
       now() + interval '1 hour 30 minutes',
       NULL, 'AT_RISK',
       now(), now()
FROM customers c
JOIN ticket_categories cat ON cat.code = 'PAYMENT_FAILED'
LEFT JOIN users u ON u.username = 'mona'
WHERE c.cif = 'CIF-100245'
ON CONFLICT (ticket_number) DO NOTHING;

-- #2002: CRITICAL (2h target), 3h elapsed (150%) -> BREACHED
INSERT INTO tickets (ticket_number, customer_id, category_id, title, description,
                     priority, status, origin, platform, error_code, reference,
                     assigned_agent_id, created_by_id,
                     sla_started_at, sla_due_at, sla_paused_at, sla_state,
                     created_at, updated_at)
SELECT 2002, c.id, cat.id,
       'Customer locked out of mobile banking',
       'VIP customer unable to sign in after ERR_AUTH_503 on mobile.',
       'CRITICAL', 'NEW', 'MANUAL', 'MOBILE', 'ERR_AUTH_503', NULL,
       u.id, u.id,
       now() - interval '3 hours',
       now() - interval '1 hour',
       NULL, 'BREACHED',
       now(), now()
FROM customers c
JOIN ticket_categories cat ON cat.code = 'LOGIN_FAILED'
LEFT JOIN users u ON u.username = 'mona'
WHERE c.cif = 'CIF-100101'
ON CONFLICT (ticket_number) DO NOTHING;

-- #2003: LOW (72h target), 3h elapsed (4%) -> ON_TRACK
INSERT INTO tickets (ticket_number, customer_id, category_id, title, description,
                     priority, status, origin, platform, error_code, reference,
                     assigned_agent_id, created_by_id,
                     sla_started_at, sla_due_at, sla_paused_at, sla_state,
                     created_at, updated_at)
SELECT 2003, c.id, cat.id,
       'Mobile app crashing on statement view',
       'App closes unexpectedly when opening monthly statement PDF.',
       'LOW', 'NEW', 'MANUAL', 'MOBILE', NULL, NULL,
       u.id, u.id,
       now() - interval '3 hours',
       now() + interval '69 hours',
       NULL, 'ON_TRACK',
       now(), now()
FROM customers c
JOIN ticket_categories cat ON cat.code = 'APP_CRASHING'
LEFT JOIN users u ON u.username = 'mona'
WHERE c.cif = 'CIF-100311'
ON CONFLICT (ticket_number) DO NOTHING;

-- #2004: MEDIUM (24h target), paused after 2h of work (8%) -> PAUSED
INSERT INTO tickets (ticket_number, customer_id, category_id, title, description,
                     priority, status, origin, platform, error_code, reference,
                     assigned_agent_id, created_by_id,
                     sla_started_at, sla_due_at, sla_paused_at, sla_state,
                     created_at, updated_at)
SELECT 2004, c.id, cat.id,
       'Waiting on customer for transfer receipt',
       'Transfer debited without beneficiary credit; awaiting customer screenshot.',
       'MEDIUM', 'WAITING_ON_CUSTOMER', 'MANUAL', 'WEB', 'TRX_DEBIT_NO_CREDIT', 'TRX-2026-00981',
       u.id, u.id,
       now() - interval '10 hours',
       now() + interval '14 hours',
       now() - interval '8 hours', 'PAUSED',
       now(), now()
FROM customers c
JOIN ticket_categories cat ON cat.code = 'TRANSFER_FAILED'
LEFT JOIN users u ON u.username = 'mona'
WHERE c.cif = 'CIF-100245'
ON CONFLICT (ticket_number) DO NOTHING;
