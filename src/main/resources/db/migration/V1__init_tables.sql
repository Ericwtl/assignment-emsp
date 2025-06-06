CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- account table
CREATE TABLE account (
    email VARCHAR(255) PRIMARY KEY CHECK (email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    contract_id VARCHAR(14) NULL CHECK (
        contract_id IS NULL OR contract_id ~ '^[A-Z]{2}[0-9A-Z]{3}[0-9A-Z]{9}$'
    ),
    status VARCHAR(20) NOT NULL CHECK (status IN ('CREATED', 'ACTIVATED', 'DEACTIVATED')),
    last_updated TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

--card table
CREATE TABLE card (
    rfid_uid VARCHAR(50) PRIMARY KEY,          -- 物理RFID芯片的唯一标识符（主键）
    visible_number VARCHAR(19) NOT NULL UNIQUE,-- 卡片表面印刷的完整可见编号
    account_email VARCHAR(255) REFERENCES account(email) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('CREATED', 'ASSIGNED', 'ACTIVATED', 'DEACTIVATED')),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 创建外键索引
CREATE INDEX idx_card_account_email ON card(account_email);

-- 合同ID唯一约束
ALTER TABLE account ADD CONSTRAINT uniq_contract_id UNIQUE (contract_id);