-- Staff accounts for local development. Internal system: there is no sign-up, accounts are seeded.
-- Loaded only on the "local" profile (spring.flyway.locations in application-local.yml), never in prod.
-- Repeatable migration: Flyway re-runs it whenever this file changes; ON CONFLICT keeps existing rows untouched.
-- Password for all three: Password123!  Stored as BCrypt (cost 10) of SHA-256(Password123!), because the
-- browser sends the SHA-256, never the password itself.
INSERT INTO users (username, password_hash, full_name, email, mobile_number, role, created_at, updated_at)
VALUES ('mona',    '$2a$10$m4jvxPe7GKccw/A8eO8Bau7nYzRr2k1My5YNt.NhggDLfMLWIjN/i', 'Mona',    'mona@supportdesk.local',    '+201000000101', 'AGENT',       now(), now()),
       ('youssef', '$2a$10$m4jvxPe7GKccw/A8eO8Bau7nYzRr2k1My5YNt.NhggDLfMLWIjN/i', 'Youssef', 'youssef@supportdesk.local', '+201000000102', 'GUIDE_ADMIN', now(), now()),
       ('nour',    '$2a$10$m4jvxPe7GKccw/A8eO8Bau7nYzRr2k1My5YNt.NhggDLfMLWIjN/i', 'Nour',    'nour@supportdesk.local',    '+201000000103', 'REVIEWER',    now(), now())
ON CONFLICT (username) DO UPDATE SET password_hash = EXCLUDED.password_hash;   -- move old local rows to the new hash
