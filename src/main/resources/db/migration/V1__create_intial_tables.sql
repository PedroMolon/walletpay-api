CREATE TABLE tb_roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) UNIQUE NOT NULL
);

CREATE TABLE tb_users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES tb_roles(id)
);

CREATE TABLE tb_wallets (
    id BIGSERIAL PRIMARY KEY,
    balance NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES tb_users(id)
);

INSERT INTO tb_roles (name) VALUES ('ROLE_USER') ON CONFLICT DO NOTHING;
INSERT INTO tb_roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT DO NOTHING;

CREATE TABLE tb_transactions (
    id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(10, 2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sender_wallet_id BIGINT,
    recipient_wallet_id BIGINT,
    CONSTRAINT fk_sender_wallet FOREIGN KEY (sender_wallet_id) REFERENCES tb_wallets(id),
    CONSTRAINT fk_recipient_wallet FOREIGN KEY (recipient_wallet_id) REFERENCES tb_wallets(id)
);
