CREATE TABLE users
(
    id                    BIGSERIAL    PRIMARY KEY,
    username              VARCHAR(100) NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    full_name             VARCHAR(150) NOT NULL,
    role                  VARCHAR(30)  NOT NULL,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    last_login_at         TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('AGENT', 'GUIDE_ADMIN', 'REVIEWER')),
    CONSTRAINT ck_users_failed_attempts CHECK (failed_login_attempts >= 0)
);

CREATE UNIQUE INDEX ux_users_username_lower ON users (LOWER(username));