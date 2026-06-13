-- V4: 为缺失 updated_at 字段的表添加该列（BaseEntity 要求）
ALTER TABLE agent_channel_permission ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;
