-- 启用必要扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ======================
-- 账户数据 (6种状态组合)
-- ======================
INSERT INTO account (email, contract_id, status) VALUES
-- 激活账户（单卡）
('active_single@emaid.com', 'DE5X9ABCD12345', 'ACTIVATED'),
-- 激活账户（多卡）
('active_multi@emaid.com', 'FR8Y2Z9KLMN789', 'ACTIVATED'),
-- 停用账户（单卡）
('deactive_single@emaid.com', 'NL3A8BCDE45678', 'DEACTIVATED'),
-- 停用账户（多卡）
('deactive_multi@emaid.com', 'ES2B4FGHI01234', 'DEACTIVATED'),
-- 新建账户（单卡）
('created_single@emaid.com', NULL, 'CREATED'),
-- 新建账户（多卡）
('created_multi@emaid.com', NULL, 'CREATED');

-- ===========================================
-- 卡片数据 (覆盖所有状态组合和边界情况)
-- ===========================================
INSERT INTO card (uid, account_email, encrypted_card, last_four_digits, status) VALUES
-- 激活账户（单卡）- 激活卡
(uuid_generate_v4(), 'active_single@emaid.com', gen_random_bytes(128), '1234', 'ACTIVATED'),

-- 激活账户（多卡）- 3张激活卡
(uuid_generate_v4(), 'active_multi@emaid.com', gen_random_bytes(128), '5678', 'ACTIVATED'),
(uuid_generate_v4(), 'active_multi@emaid.com', gen_random_bytes(128), '9012', 'ACTIVATED'),
(uuid_generate_v4(), 'active_multi@emaid.com', gen_random_bytes(128), '3456', 'ACTIVATED'),

-- 停用账户（单卡）- 停用卡
(uuid_generate_v4(), 'deactive_single@emaid.com', gen_random_bytes(128), '7890', 'DEACTIVATED'),

-- 停用账户（多卡）- 3张停用卡
(uuid_generate_v4(), 'deactive_multi@emaid.com', gen_random_bytes(128), '2345', 'DEACTIVATED'),
(uuid_generate_v4(), 'deactive_multi@emaid.com', gen_random_bytes(128), '6789', 'DEACTIVATED'),
(uuid_generate_v4(), 'deactive_multi@emaid.com', gen_random_bytes(128), '0123', 'DEACTIVATED'),

-- 新建账户（单卡）- 分配卡
(uuid_generate_v4(), 'created_single@emaid.com', gen_random_bytes(128), '4567', 'ASSIGNED'),

-- 新建账户（多卡）- 3张分配卡
(uuid_generate_v4(), 'created_multi@emaid.com', gen_random_bytes(128), '8901', 'ASSIGNED'),
(uuid_generate_v4(), 'created_multi@emaid.com', gen_random_bytes(128), '2345', 'ASSIGNED'),
(uuid_generate_v4(), 'created_multi@emaid.com', gen_random_bytes(128), '6789', 'ASSIGNED'),

-- ======================
-- 边界情况补充数据
-- ======================
-- 1. 新建账户无合同ID但有卡
(uuid_generate_v4(), 'created_single@emaid.com', gen_random_bytes(128), '1357', 'CREATED'),

-- 2. 激活账户含不同状态卡（主卡激活+副卡停用）
(uuid_generate_v4(), 'active_multi@emaid.com', gen_random_bytes(128), '2468', 'DEACTIVATED'),

-- 3. 停用账户含历史激活卡
(uuid_generate_v4(), 'deactive_multi@emaid.com', gen_random_bytes(128), '8080', 'ACTIVATED');