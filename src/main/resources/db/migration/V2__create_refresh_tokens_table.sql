CREATE TABLE refresh_tokens
(
    id          BIGSERIAL    PRIMARY KEY,
    token       VARCHAR(255) NOT NULL,
    expirydate  TIMESTAMPTZ  NOT NULL,
    user_id     BIGINT       NOT NULL,

    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT uk_refresh_tokens_user_id UNIQUE (user_id),
    CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
