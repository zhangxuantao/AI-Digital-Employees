-- ============================================================
-- AI 智能客服系统 Phase1 - 种子数据脚本
-- 描述: 创建默认角色、权限、管理员用户
-- ============================================================

-- 默认角色
INSERT INTO sys_role (code, name, description, tenant_id) VALUES
('ADMIN', '超级管理员', '全部菜单 + 全部数据', 1),
('LEADER', '客服主管', 'AI员工管理 / 知识库 / 客服管理 / IM工作台 / 数据看板（团队范围）', 1),
('AGENT', '客服', 'IM工作台（仅自己会话）', 1);

-- 默认权限（树形结构：parent_id关联）
INSERT INTO sys_permission (id, parent_id, name, type, path, permission_code, icon, sort_order, tenant_id) VALUES
(1, NULL, 'AI员工管理', 'MENU', '/ai-employee', 'ai_employee:menu', 'RobotOutlined', 1, 1),
(2, 1, '查看AI员工', 'BUTTON', NULL, 'ai_employee:view', NULL, 1, 1),
(3, 1, '编辑AI员工', 'BUTTON', NULL, 'ai_employee:edit', NULL, 2, 1),
(4, NULL, '知识库管理', 'MENU', '/knowledge', 'knowledge:menu', 'BookOutlined', 2, 1),
(5, 4, '查看知识库', 'BUTTON', NULL, 'knowledge:view', NULL, 1, 1),
(6, 4, '编辑知识库', 'BUTTON', NULL, 'knowledge:edit', NULL, 2, 1),
(7, NULL, '客服管理', 'MENU', '/agents', 'agent:menu', 'TeamOutlined', 3, 1),
(8, 7, '查看客服', 'BUTTON', NULL, 'agent:view', NULL, 1, 1),
(9, 7, '编辑客服', 'BUTTON', NULL, 'agent:edit', NULL, 2, 1),
(10, NULL, 'IM工作台', 'MENU', '/im', 'im:access', 'MessageOutlined', 4, 1),
(11, NULL, '数据看板', 'MENU', '/analytics', 'dashboard:view', 'DashboardOutlined', 5, 1);

-- 默认管理员用户 (密码: admin123, BCrypt)
INSERT INTO sys_user (username, password_hash, role_code, status, tenant_id) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', 'ADMIN', 'ENABLED', 1);

INSERT INTO sys_user_role (user_id, role_id, tenant_id) VALUES (1, 1, 1);

-- 角色权限关联：ADMIN 拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id, tenant_id)
SELECT 1, id, 1 FROM sys_permission;
