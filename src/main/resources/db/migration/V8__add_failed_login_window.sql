-- E1-S1: lock only after 5 failures within 15 minutes, not 5 failures ever.
ALTER TABLE users ADD COLUMN first_failed_login_at TIMESTAMPTZ;
