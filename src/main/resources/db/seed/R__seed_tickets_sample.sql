-- 30 sample tickets across categories, sub-categories, priorities, statuses and platforms (local profile only).
-- Runs after R__seed_customers, R__seed_error_events (customers CIF-100246..100252), R__seed_staff_users
-- and R__seed_tickets, alphabetically. Numbers #2010..#2039 stay below ticket_number_seq (starts at 2100).
-- SLA targets (application.yml): CRITICAL 2h, HIGH 8h, MEDIUM 24h, LOW 72h; AT_RISK from 75% of the target.
-- Priorities are chosen so the priority suggestion has real history to learn from, e.g. agents usually pick
-- HIGH for OTP Not Received (default MEDIUM) and LOW for App Crashing (default MEDIUM).

-- ---------------------------------------------------------------- more categories and sub-categories
INSERT INTO ticket_categories (code, name, default_priority, created_at, updated_at) VALUES
    ('CARDS',   'Cards',            'MEDIUM', now(), now()),
    ('ACCOUNT', 'Account Services', 'LOW',    now(), now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO ticket_categories (code, name, parent_id, default_priority, created_at, updated_at)
SELECT s.code, s.name, p.id, s.prio, now(), now()
FROM (VALUES
    ('CARD_BLOCKED',      'Card Blocked',          'CARDS',          'HIGH'),
    ('CARD_NOT_RECEIVED', 'Card Not Received',     'CARDS',          'LOW'),
    ('CARD_DECLINED',     'Card Declined Abroad',  'CARDS',          'HIGH'),
    ('STATEMENT_REQUEST', 'Statement Request',     'ACCOUNT',        'LOW'),
    ('ACCOUNT_FROZEN',    'Account Frozen',        'ACCOUNT',        'CRITICAL'),
    ('OTP_EXPIRED',       'OTP Expired Too Fast',  'OTP',            'MEDIUM'),
    ('TRANSFER_DELAYED',  'Transfer Delayed',      'TRANSFERS',      'MEDIUM'),
    ('APP_SLOW',          'App Very Slow',         'MOBILE_APP',     'LOW'),
    ('LOGIN_LOOP',        'Login Loop',            'AUTHENTICATION', 'HIGH')
) AS s (code, name, parent_code, prio)
JOIN ticket_categories p ON p.code = s.parent_code
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------- 30 tickets, all assigned to agent "mona"
INSERT INTO tickets (ticket_number, customer_id, category_id, title, description,
                     priority, status, origin, platform, error_code, reference,
                     assigned_agent_id, created_by_id,
                     sla_started_at, sla_due_at, sla_paused_at, sla_state,
                     resolved_at, closed_at, created_at, updated_at)
SELECT t.num, c.id, cat.id, t.title, t.descr,
       t.prio, t.status, 'MANUAL', t.platform, NULL, t.ref,
       u.id, u.id,
       now() - make_interval(mins => t.mins_ago),
       now() - make_interval(mins => t.mins_ago) + make_interval(mins => tgt.mins),
       CASE WHEN t.status = 'WAITING_ON_CUSTOMER' THEN now() END,
       CASE
           WHEN t.status IN ('RESOLVED', 'CLOSED')      THEN 'MET'
           WHEN t.status = 'WAITING_ON_CUSTOMER'         THEN 'PAUSED'
           WHEN t.mins_ago >= tgt.mins                   THEN 'BREACHED'
           WHEN t.mins_ago >= tgt.mins * 0.75            THEN 'AT_RISK'
           ELSE 'ON_TRACK'
       END,
       CASE WHEN t.status IN ('RESOLVED', 'CLOSED') THEN now() - make_interval(mins => t.mins_ago / 2) END,
       CASE WHEN t.status = 'CLOSED' THEN now() - make_interval(mins => t.mins_ago / 4) END,
       now() - make_interval(mins => t.mins_ago),
       now()
FROM (VALUES
    -- num, cif,        sub-category,          priority,   status,                platform, title, description, reference, minutes ago
    (2010, 'CIF-100245', 'OTP_NOT_RECEIVED',    'HIGH',     'TROUBLESHOOTING',     'MOBILE', 'OTP SMS not arriving',              'Customer waits more than 5 minutes for the login OTP.',            NULL,             120),
    (2011, 'CIF-100311', 'OTP_NOT_RECEIVED',    'HIGH',     'NEW',                 'MOBILE', 'No OTP for transfer approval',      'Transfer stuck because the approval OTP never comes.',             NULL,              60),
    (2012, 'CIF-100101', 'OTP_NOT_RECEIVED',    'HIGH',     'WAITING_ON_CUSTOMER', 'WEB',    'OTP not received on web login',     'Asked the customer to confirm the mobile number on file.',          NULL,             300),
    (2013, 'CIF-100246', 'OTP_NOT_RECEIVED',    'MEDIUM',   'RESOLVED',            'MOBILE', 'OTP delayed by carrier',            'Carrier delay; OTP arrived after 10 minutes.',                      NULL,            1800),
    (2014, 'CIF-100247', 'OTP_EXPIRED',         'MEDIUM',   'NEW',                 'MOBILE', 'OTP expires before it arrives',     'OTP valid for 60 seconds but SMS takes 2 minutes.',                 NULL,             180),
    (2015, 'CIF-100248', 'OTP_EXPIRED',         'LOW',      'CLOSED',              'WEB',    'OTP expired once',                  'Happened once; customer retried successfully.',                     NULL,            4800),
    (2016, 'CIF-100249', 'LOGIN_FAILED',        'HIGH',     'TROUBLESHOOTING',     'MOBILE', 'Cannot sign in after app update',   'Sign-in fails with a generic error since version 5.2.',             NULL,             240),
    (2017, 'CIF-100250', 'LOGIN_FAILED',        'CRITICAL', 'ESCALATED',           'WEB',    'Corporate user locked out',         'Treasury user cannot sign in before payroll cut-off.',              NULL,              90),
    (2018, 'CIF-100251', 'LOGIN_LOOP',          'HIGH',     'RESOLVED',            'MOBILE', 'Login loop on Android',             'App returns to the login screen after every sign-in.',              NULL,            1200),
    (2019, 'CIF-100252', 'TRANSFER_FAILED',     'HIGH',     'NEW',                 'MOBILE', 'Transfer to wallet failed',         'Transfer to a mobile wallet failed with no clear reason.',          'TRX-2026-02001', 120),
    (2020, 'CIF-100245', 'TRANSFER_FAILED',     'CRITICAL', 'TROUBLESHOOTING',     'WEB',    'Salary transfer failed',            'Company salary transfer to the customer failed.',                   'TRX-2026-02002',  60),
    (2021, 'CIF-100311', 'DEBITED_NOT_CREDITED','CRITICAL', 'ESCALATED',           'MOBILE', 'Debited 15,000 EGP not credited',   'Amount left the account but never reached the beneficiary.',        'TRX-2026-02003', 180),
    (2022, 'CIF-100101', 'DEBITED_NOT_CREDITED','HIGH',     'WITH_SRE',            'WEB',    'Bill payment debited twice',        'Electricity bill paid once but debited twice.',                     'TRX-2026-02004', 360),
    (2023, 'CIF-100246', 'TRANSFER_DELAYED',    'MEDIUM',   'WAITING_ON_CUSTOMER', 'MOBILE', 'Instant transfer still pending',    'Waiting for the customer to send the beneficiary IBAN.',            NULL,             600),
    (2024, 'CIF-100247', 'TRANSFER_DELAYED',    'LOW',      'RESOLVED',            'WEB',    'International transfer slow',       'SWIFT transfer took 3 days; within the normal window.',             NULL,            3000),
    (2025, 'CIF-100248', 'PAYMENT_FAILED',      'HIGH',     'TROUBLESHOOTING',     'MOBILE', 'QR payment failed at merchant',     'QR payment shows failed but the merchant was not paid.',            'TRX-2026-02005', 300),
    (2026, 'CIF-100249', 'PAYMENT_FAILED',      'MEDIUM',   'NEW',                 'WEB',    'Online purchase declined',          'Online card purchase declined at checkout.',                        'TRX-2026-02006', 720),
    (2027, 'CIF-100250', 'APP_CRASHING',        'LOW',      'NEW',                 'MOBILE', 'App crashes on iOS 18',             'App closes when opening the home screen on iOS 18.',                NULL,             360),
    (2028, 'CIF-100251', 'APP_CRASHING',        'LOW',      'TROUBLESHOOTING',     'MOBILE', 'Crash when opening cards tab',      'App crashes every time the cards tab is opened.',                   NULL,             900),
    (2029, 'CIF-100252', 'APP_CRASHING',        'LOW',      'CLOSED',              'MOBILE', 'Crash on old Android',              'Android 8 is no longer supported; customer updated the phone.',     NULL,            6000),
    (2030, 'CIF-100245', 'APP_SLOW',            'LOW',      'NEW',                 'MOBILE', 'App takes 30 seconds to load',      'Home screen loads slowly on 4G.',                                   NULL,             480),
    (2031, 'CIF-100311', 'APP_SLOW',            'MEDIUM',   'TROUBLESHOOTING',     'MOBILE', 'Slow statements screen',            'Statements take over a minute to open.',                            NULL,            1200),
    (2032, 'CIF-100101', 'CARD_BLOCKED',        'CRITICAL', 'TROUBLESHOOTING',     'BRANCH', 'Card blocked while travelling',     'VIP customer abroad with the only card blocked.',                   NULL,              60),
    (2033, 'CIF-100246', 'CARD_BLOCKED',        'HIGH',     'NEW',                 'MOBILE', 'Card blocked after wrong PIN',      'Card blocked after 3 wrong PIN attempts.',                          NULL,             120),
    (2034, 'CIF-100247', 'CARD_BLOCKED',        'HIGH',     'RESOLVED',            'BRANCH', 'Card blocked by fraud check',       'Fraud check blocked the card; unblocked after verification.',       NULL,            1800),
    (2035, 'CIF-100248', 'CARD_NOT_RECEIVED',   'LOW',      'WAITING_ON_CUSTOMER', 'BRANCH', 'New card not delivered',            'Waiting for the customer to confirm the delivery address.',         NULL,            2400),
    (2036, 'CIF-100249', 'CARD_DECLINED',       'HIGH',     'NEW',                 'MOBILE', 'Card declined in Dubai',            'Card declined at shops abroad; travel notice was set.',             NULL,             180),
    (2037, 'CIF-100250', 'STATEMENT_REQUEST',   'LOW',      'NEW',                 'WEB',    'Needs 12-month statement',          'Customer needs a stamped 12-month statement for a visa.',           NULL,            1440),
    (2038, 'CIF-100251', 'ACCOUNT_FROZEN',      'CRITICAL', 'ESCALATED',           'BRANCH', 'Account frozen unexpectedly',       'Account frozen with no notice; salary due tomorrow.',               NULL,             120),
    (2039, 'CIF-100252', 'ACCOUNT_FROZEN',      'CRITICAL', 'TROUBLESHOOTING',     'WEB',    'Payroll account frozen',            'Business payroll account frozen during a compliance review.',       NULL,              60)
) AS t (num, cif, sub_code, prio, status, platform, title, descr, ref, mins_ago)
JOIN customers c           ON c.cif = t.cif
JOIN ticket_categories cat ON cat.code = t.sub_code
JOIN (VALUES ('CRITICAL', 120), ('HIGH', 480), ('MEDIUM', 1440), ('LOW', 4320)) AS tgt (prio, mins) ON tgt.prio = t.prio
LEFT JOIN users u          ON u.username = 'mona'
ON CONFLICT (ticket_number) DO NOTHING;
