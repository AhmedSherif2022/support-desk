-- E6 test data (local profile only). The 9 error rows below need these customers;
-- without them the JOIN on customers silently drops the rows.
INSERT INTO customers (cif, full_name, national_id, mobile, date_of_birth, segment, preferred_language, created_at, updated_at)
VALUES ('CIF-100246', 'Omar Fathy',    '29102021234570', '+201005550246', '1991-02-02', 'RETAIL',   'AR', now(), now()),
       ('CIF-100247', 'Nour Adel',     '29304041234571', '+201005550247', '1993-04-04', 'RETAIL',   'EN', now(), now()),
       ('CIF-100248', 'Karim Samir',   '28706061234572', '+201005550248', '1987-06-06', 'PREMIUM',  'AR', now(), now()),
       ('CIF-100249', 'Laila Hany',    '29508081234573', '+201005550249', '1995-08-08', 'RETAIL',   'AR', now(), now()),
       ('CIF-100250', 'Youssef Nabil', '28910101234574', '+201005550250', '1989-10-10', 'BUSINESS', 'EN', now(), now()),
       ('CIF-100251', 'Hana Tarek',    '29612121234575', '+201005550251', '1996-12-12', 'RETAIL',   'AR', now(), now()),
       ('CIF-100252', 'Mostafa Reda',  '29001151234576', '+201005550252', '1990-01-15', 'VIP',      'AR', now(), now())
ON CONFLICT DO NOTHING;


INSERT INTO error_rule (error_code, category_id, issue, priority, auto_escalate, active, created_at, updated_at)
SELECT r.code, c.id, r.issue, r.priority, r.auto_escalate, TRUE, now(), now()
FROM (VALUES
        ('ERR_AUTH_503',        'LOGIN_FAILED',    'Login failed (auth service unavailable)', 'HIGH',     FALSE),
        ('PAY_TIMEOUT',         'PAYMENT_FAILED',  'Card payment timed out',                  'HIGH',     FALSE),
        ('TRX_DEBIT_NO_CREDIT', 'TRANSFER_FAILED', 'Debited but not credited',                'CRITICAL', TRUE)
     ) AS r(code, category_code, issue, priority, auto_escalate)
JOIN ticket_categories c ON c.code = r.category_code
ON CONFLICT (error_code) DO NOTHING;


INSERT INTO detected_error_event (customer_id, error_code, platform, service, message, correlation_id, occurred_at)
SELECT cu.id, e.code, e.platform, e.service, e.message, e.corr, now() - e.ago
FROM (VALUES

        ('CIF-100245', 'ERR_AUTH_503',        'MOBILE', 'AUTH',      'Login failed: 503 from auth',   'SEED-E6-01', INTERVAL '20 minutes'),

        ('CIF-100245', 'ERR_AUTH_503',        'MOBILE', 'AUTH',      'Login failed again',            'SEED-E6-02', INTERVAL '10 minutes'),

        ('CIF-100246', 'PAY_TIMEOUT',         'WEB',    'PAYMENTS',  'Card payment timed out',        'SEED-E6-03', INTERVAL '30 minutes'),

        ('CIF-100247', 'ERR_NEW_999',         'MOBILE', 'CARDS',     'Unexpected card error',         'SEED-E6-04', INTERVAL '25 minutes'),

        ('CIF-100248', 'TRX_DEBIT_NO_CREDIT', 'MOBILE', 'TRANSFERS', 'Debited, not credited',         'SEED-E6-05', INTERVAL '15 minutes'),

        ('CIF-100249', 'ERR_AUTH_503',        'MOBILE', 'AUTH',      'Login failed: 503 from auth',   'SEED-E6-06', INTERVAL '12 minutes'),
        ('CIF-100250', 'ERR_AUTH_503',        'MOBILE', 'AUTH',      'Login failed: 503 from auth',   'SEED-E6-07', INTERVAL '9 minutes'),
        ('CIF-100251', 'ERR_AUTH_503',        'MOBILE', 'AUTH',      'Login failed: 503 from auth',   'SEED-E6-08', INTERVAL '7 minutes'),
        ('CIF-100252', 'ERR_AUTH_503',        'MOBILE', 'AUTH',      'Login failed: 503 from auth',   'SEED-E6-09', INTERVAL '5 minutes')
     ) AS e(cif, code, platform, service, message, corr, ago)
JOIN customers cu ON cu.cif = e.cif
WHERE NOT EXISTS (SELECT 1 FROM detected_error_event d WHERE d.correlation_id = e.corr);