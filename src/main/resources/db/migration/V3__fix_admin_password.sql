-- ============================================================
-- V3: 修复 admin 用户密码哈希（admin123）
-- V2 中的 BCrypt 哈希是无效占位符
-- ============================================================
UPDATE sys_user SET password_hash = '$2a$10$LWrTQzjlsL8Ga1XG1soWB.B.SbyCTMWhqcaVlf1/SiZb0/EfsxyoC' WHERE username = 'admin';
