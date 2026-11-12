-- Login now checks BCrypt(SHA-256(password)): the browser sends the SHA-256, never the password.
-- Production was first migrated with the local profile, so it has the seeded staff (Password123!)
-- stored as BCrypt(password). Move exactly those rows to BCrypt(SHA-256(Password123!)).
-- No other row matches this hash, and on a fresh database this updates nothing.
UPDATE users
SET password_hash = '$2a$10$m4jvxPe7GKccw/A8eO8Bau7nYzRr2k1My5YNt.NhggDLfMLWIjN/i',
    updated_at    = now()
WHERE password_hash = '$2a$10$ih4x80TjLMJDdg4EUNBS9OtE1OdHeHHOUFH68IVSgMn9HBK5CkbzO';
