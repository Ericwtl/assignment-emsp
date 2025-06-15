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
-- 卡片数据 (使用7字节UID格式)
-- ===========================================
INSERT INTO card (rfid_uid, visible_number, account_email, status) VALUES
-- 激活账户（单卡）- 激活卡 (7字节: 045开头)
('0457E8A1B2C3D0', '1234-5678-9012-3456', 'active_single@emaid.com', 'ACTIVATED'),

-- 激活账户（多卡）- 3张激活卡 (连续7字节UID)
('0457E8A1B2C3D1', '2345-6789-0123-4567', 'active_multi@emaid.com', 'ACTIVATED'),
('0457E8A1B2C3D2', '3456-7890-1234-5678', 'active_multi@emaid.com', 'ACTIVATED'),
('0457E8A1B2C3D3', '4567-8901-2345-6789', 'active_multi@emaid.com', 'ACTIVATED'),

-- 停用账户（单卡）- 停用卡 (新UID序列)
('0457E8A1B2C4E0', '5678-9012-3456-7890', 'deactive_single@emaid.com', 'DEACTIVATED'),

-- 停用账户（多卡）- 3张停用卡 (连续7字节UID)
('0457E8A1B2C4E1', '6789-0123-4567-8901', 'deactive_multi@emaid.com', 'DEACTIVATED'),
('0457E8A1B2C4E2', '7890-1234-5678-9012', 'deactive_multi@emaid.com', 'DEACTIVATED'),
('0457E8A1B2C4E3', '8901-2345-6789-0123', 'deactive_multi@emaid.com', 'DEACTIVATED'),

-- 新建账户（单卡）- 分配卡 (新UID序列)
('0457E8A1B3D5F0', '9012-3456-7890-1234', 'created_single@emaid.com', 'ASSIGNED'),

-- 新建账户（多卡）- 3张分配卡 (连续7字节UID)
('0457E8A1B3D5F1', '0123-4567-8901-2345', 'created_multi@emaid.com', 'ASSIGNED'),
('0457E8A1B3D5F2', '1123-4567-8901-2345', 'created_multi@emaid.com', 'ASSIGNED'),
('0457E8A1B3D5F3', '2123-4567-8901-2345', 'created_multi@emaid.com', 'ASSIGNED'),

-- ======================
-- 边界情况补充数据
-- ======================
-- 1. 新建账户无合同ID但有卡 (新UID序列)
('0457E8A1C4E6G0', '0011-2233-4455-6677', NULL, 'CREATED'),

-- 2. 激活账户含不同状态卡（主卡激活+副卡停用）(同系列UID)
('0457E8A1B2C3D4', '3344-5566-7788-9900', 'active_multi@emaid.com', 'DEACTIVATED'),

-- 3. 停用账户含历史激活卡 (同系列UID)
('0457E8A1B2C4E4', '5566-7788-9900-1122', 'deactive_multi@emaid.com', 'ACTIVATED');