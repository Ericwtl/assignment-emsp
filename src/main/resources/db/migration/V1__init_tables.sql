CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 账户表
CREATE TABLE account (
    email VARCHAR(255) PRIMARY KEY CHECK (email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    contract_id VARCHAR(14) NULL CHECK (
        contract_id IS NULL OR contract_id ~ '^[A-Z]{2}[0-9A-Z]{3}[0-9A-Z]{9}$'
    ),
    status VARCHAR(20) NOT NULL CHECK (status IN ('Created', 'Activated', 'Deactivated')),
    last_updated TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 卡片表
CREATE TABLE card (
    uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_email VARCHAR(255) NOT NULL REFERENCES account(email) ON DELETE CASCADE,
    encrypted_card BYTEA NOT NULL,  -- 修正为BYTEA类型
    last_four_digits CHAR(4) NOT NULL,
    visible_number VARCHAR(19) GENERATED ALWAYS AS
        ('****-****-****-' || last_four_digits) STORED,  -- 修正拼接语法
    status VARCHAR(20) NOT NULL CHECK (status IN ('Created', 'Assigned', 'Activated', 'Deactivated')),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 添加注释
COMMENT ON COLUMN card.encrypted_card IS 'AES-256 encrypted card number';
COMMENT ON COLUMN card.last_four_digits IS 'Last 4 digits of card number';
COMMENT ON COLUMN card.visible_number IS 'Desensitized display number';

-- 创建外键索引
CREATE INDEX idx_card_account_email ON card(account_email);

-- 合同ID唯一约束
ALTER TABLE account ADD CONSTRAINT uniq_contract_id UNIQUE (contract_id);