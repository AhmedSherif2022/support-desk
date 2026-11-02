-- Local demo activity (local profile only). The run marker below changes on every start, so Flyway re-runs
-- this file each time and "the last 6 hours" always has data. Run marker: ${flyway:timestamp}
DELETE FROM customer_activity_log
WHERE customer_id IN (SELECT id FROM customers WHERE cif IN ('CIF-100245', 'CIF-100311', 'CIF-100101'));

-- Sara: 3 failed logins with ERR_AUTH_503 in the last few hours (the E2 diagnosis example).
-- Ahmed: no failures at all (the "check device or network" case).
INSERT INTO customer_activity_log (customer_id, occurred_at, event_type, platform, service, result, error_code, created_at)
SELECT c.id, now() - v.ago, v.event_type, v.platform, v.service, v.result, v.error_code, now()
FROM (VALUES
    ('CIF-100245', interval '35 minutes',        'LOGIN',       'MOBILE', 'AUTHENTICATION', 'FAILURE', 'ERR_AUTH_503'),
    ('CIF-100245', interval '1 hour 10 minutes', 'LOGIN',       'MOBILE', 'AUTHENTICATION', 'FAILURE', 'ERR_AUTH_503'),
    ('CIF-100245', interval '2 hours 5 minutes', 'LOGIN',       'MOBILE', 'AUTHENTICATION', 'FAILURE', 'ERR_AUTH_503'),
    ('CIF-100245', interval '3 hours',           'OTP_REQUEST', 'MOBILE', 'OTP',            'SUCCESS', NULL),
    ('CIF-100245', interval '4 hours',           'TRANSFER',    'WEB',    'TRANSFERS',      'SUCCESS', NULL),
    ('CIF-100245', interval '9 hours',           'PAYMENT',     'MOBILE', 'PAYMENTS',       'FAILURE', 'PAY_TIMEOUT'),
    ('CIF-100245', interval '2 days',            'LOGIN',       'WEB',    'AUTHENTICATION', 'SUCCESS', NULL),
    ('CIF-100311', interval '50 minutes',        'TRANSFER',    'MOBILE', 'TRANSFERS',      'FAILURE', 'TRX_DEBIT_NO_CREDIT'),
    ('CIF-100311', interval '20 minutes',        'LOGIN',       'MOBILE', 'AUTHENTICATION', 'SUCCESS', NULL),
    ('CIF-100101', interval '1 hour',            'LOGIN',       'WEB',    'AUTHENTICATION', 'SUCCESS', NULL)
) AS v (cif, ago, event_type, platform, service, result, error_code)
JOIN customers c ON c.cif = v.cif;

-- Mai has not enrolled in digital banking yet.
UPDATE customers SET enrollment_status = 'NOT_ENROLLED' WHERE cif = 'CIF-100311';
