-- Staff accounts for local development. Internal system: there is no sign-up, accounts are seeded.
-- Loaded only on the "local" profile (spring.flyway.locations in application-local.yml), never in prod.
-- Repeatable migration: Flyway re-runs it whenever this file changes; ON CONFLICT keeps existing rows untouched.
-- Password for all three: Password123!  (BCrypt hash, cost 10)
INSERT INTO users (username, password_hash, full_name, email, mobile_number, role, created_at, updated_at)
VALUES ('mona',    '$2a$10$ih4x80TjLMJDdg4EUNBS9OtE1OdHeHHOUFH68IVSgMn9HBK5CkbzO', 'Mona',    'mona@supportdesk.local',    '+201000000101', 'AGENT',       now(), now()),
       ('youssef', '$2a$10$ih4x80TjLMJDdg4EUNBS9OtE1OdHeHHOUFH68IVSgMn9HBK5CkbzO', 'Youssef', 'youssef@supportdesk.local', '+201000000102', 'GUIDE_ADMIN', now(), now()),
       ('nour',    '$2a$10$ih4x80TjLMJDdg4EUNBS9OtE1OdHeHHOUFH68IVSgMn9HBK5CkbzO', 'Nour',    'nour@supportdesk.local',    '+201000000103', 'REVIEWER',    now(), now())
ON CONFLICT DO NOTHING;
