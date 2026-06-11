-- ============================================================
-- AI 智能客服系统 Phase1 - 数据库初始化脚本
-- 描述: 创建全部 19 张表及其索引、外键约束
-- ============================================================

-- ============================================================
-- AI 员工表组
-- ============================================================

CREATE TABLE ai_employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '员工名称',
    avatar VARCHAR(500) COMMENT '头像路径',
    greeting_msg TEXT COMMENT '开场白消息',
    style VARCHAR(20) NOT NULL DEFAULT 'PROFESSIONAL' COMMENT '接待风格: PROFESSIONAL/WARM/ENTHUSIASTIC/RELIABLE',
    reply_length VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' COMMENT '回复字数: SHORT/MEDIUM/DETAIL',
    content_check JSON COMMENT '敏感词替代规则',
    aggregate_interval INT NOT NULL DEFAULT 3 COMMENT '客户消息聚合间隔(秒)',
    delay_interval INT NOT NULL DEFAULT 2 COMMENT 'AI延迟回复间隔(秒)',
    service_time_start TIME COMMENT '服务开始时间',
    service_time_end TIME COMMENT '服务结束时间',
    weekdays VARCHAR(50) COMMENT '服务日，如 "1,2,3,4,5"',
    company_intro TEXT NOT NULL COMMENT '公司介绍',
    product_intro TEXT NOT NULL COMMENT '产品介绍',
    service_scope TEXT COMMENT '服务对象描述',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    INDEX idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI员工基础属性';

CREATE TABLE ai_employee_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL COMMENT '关联 ai_employee.id',
    platform VARCHAR(20) NOT NULL COMMENT '平台: WEB/XIAOHONGSHU/DOUYIN/WECHAT_MP',
    account_id VARCHAR(100) NOT NULL COMMENT '平台内唯一账号标识',
    access_config JSON COMMENT '平台接入配置',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_platform_account (platform, account_id, tenant_id),
    INDEX idx_employee (employee_id),
    CONSTRAINT fk_account_employee FOREIGN KEY (employee_id) REFERENCES ai_employee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI员工覆盖账号绑定';

CREATE TABLE ai_employee_reply_strategy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL COMMENT '关联 ai_employee.id',
    strategy_type VARCHAR(30) NOT NULL COMMENT 'GREETING/RESPONSE/MODERATION/COLLECT/FILTER/EXCLUDE/FOLLOWUP/CONTACT',
    config_json JSON NOT NULL COMMENT '策略配置JSON',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '流水线执行顺序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    INDEX idx_employee_type (employee_id, strategy_type),
    CONSTRAINT fk_strategy_employee FOREIGN KEY (employee_id) REFERENCES ai_employee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI员工回复策略配置';

-- ============================================================
-- 知识库表组
-- ============================================================

CREATE TABLE knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '知识库名称',
    description TEXT COMMENT '知识库说明',
    employee_id BIGINT COMMENT '关联的AI员工',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库定义';

CREATE TABLE knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id BIGINT NOT NULL COMMENT '关联 knowledge_base.id',
    file_name VARCHAR(500) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(20) NOT NULL COMMENT 'PDF/XLSX/DOCX/MD/TXT',
    file_size BIGINT COMMENT '文件大小(bytes)',
    file_path VARCHAR(500) COMMENT '本地存储路径',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/DONE/PARTIAL_FAILED',
    chunk_count INT DEFAULT 0 COMMENT '分片数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    INDEX idx_kb (kb_id),
    INDEX idx_status (status),
    CONSTRAINT fk_doc_kb FOREIGN KEY (kb_id) REFERENCES knowledge_base(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档元数据';

CREATE TABLE knowledge_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id BIGINT NOT NULL COMMENT '关联 knowledge_document.id',
    kb_id BIGINT NOT NULL COMMENT '关联 knowledge_base.id',
    content MEDIUMTEXT NOT NULL COMMENT '分片文本内容',
    chunk_index INT NOT NULL COMMENT '分片序号',
    es_doc_id VARCHAR(100) COMMENT 'ES中对应文档ID',
    embedding_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/EMBEDDED/FAILED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    INDEX idx_doc (doc_id),
    INDEX idx_kb (kb_id),
    CONSTRAINT fk_chunk_doc FOREIGN KEY (doc_id) REFERENCES knowledge_document(id),
    CONSTRAINT fk_chunk_kb FOREIGN KEY (kb_id) REFERENCES knowledge_base(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档分片';

-- ============================================================
-- 会话表组
-- ============================================================

CREATE TABLE customer_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(100) COMMENT '昵称',
    platform VARCHAR(20) NOT NULL COMMENT '来源平台',
    openid VARCHAR(200) NOT NULL COMMENT '平台客户唯一标识',
    avatar VARCHAR(500) COMMENT '头像',
    phone VARCHAR(100) COMMENT '电话（加密存储）',
    email VARCHAR(200) COMMENT '邮箱',
    gender VARCHAR(10) COMMENT '性别',
    city VARCHAR(50) COMMENT '城市',
    tags JSON COMMENT '标签数组',
    extra_fields JSON COMMENT 'AI收集的自定义字段',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_platform_openid (platform, openid, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='顾客名片';

CREATE TABLE conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL COMMENT '关联 customer_profile.id',
    employee_id BIGINT NOT NULL COMMENT '关联 ai_employee.id',
    human_agent_id BIGINT COMMENT '转人工后关联 human_agent.id',
    owner_agent_id BIGINT COMMENT '负责该会话的客服ID（数据隔离）',
    channel VARCHAR(20) NOT NULL COMMENT '渠道: WEB/XIAOHONGSHU',
    status VARCHAR(20) NOT NULL DEFAULT 'AI_ACTIVE' COMMENT 'AI_ACTIVE/WAITING/HUMAN/QUEUED/CLOSED',
    start_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time DATETIME COMMENT '会话结束时间',
    close_reason VARCHAR(50) COMMENT '关闭原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    INDEX idx_customer (customer_id),
    INDEX idx_employee (employee_id),
    INDEX idx_owner_agent (owner_agent_id),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    CONSTRAINT fk_conv_customer FOREIGN KEY (customer_id) REFERENCES customer_profile(id),
    CONSTRAINT fk_conv_employee FOREIGN KEY (employee_id) REFERENCES ai_employee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务会话主表';

CREATE TABLE message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL COMMENT '关联 conversation.id',
    sender_type VARCHAR(10) NOT NULL COMMENT 'CUSTOMER/AI/HUMAN/SYSTEM',
    sender_id BIGINT COMMENT '发送者ID',
    msg_type VARCHAR(10) NOT NULL DEFAULT 'text' COMMENT 'text/image/card',
    content TEXT COMMENT '消息内容',
    metadata JSON COMMENT '扩展信息',
    send_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    INDEX idx_conversation (conversation_id),
    INDEX idx_send_time (send_time),
    CONSTRAINT fk_msg_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息记录';

-- ============================================================
-- 分配表组
-- ============================================================

CREATE TABLE transfer_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL COMMENT '关联 ai_employee.id',
    trigger_type VARCHAR(20) NOT NULL COMMENT 'KEYWORD/SENTIMENT/MANUAL/TIME',
    trigger_config JSON NOT NULL COMMENT '触发条件配置',
    action_type VARCHAR(30) NOT NULL COMMENT 'IMMEDIATE_TRANSFER/MARK_URGENT/SOOTHE_MESSAGE/LEAVE_MESSAGE',
    action_config JSON COMMENT '执行动作附加参数',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级',
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    INDEX idx_employee_enabled (employee_id, enabled),
    CONSTRAINT fk_rule_employee FOREIGN KEY (employee_id) REFERENCES ai_employee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转人工规则';

CREATE TABLE assignment_strategy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL COMMENT '关联 ai_employee.id',
    strategy_type VARCHAR(20) NOT NULL COMMENT 'SPECIFIED/ROUND_ROBIN/LEAST_LOAD/HISTORY',
    config_json JSON NOT NULL COMMENT '策略配置',
    is_active TINYINT NOT NULL DEFAULT 0 COMMENT '当前是否生效',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    INDEX idx_employee_active (employee_id, is_active),
    CONSTRAINT fk_asg_employee FOREIGN KEY (employee_id) REFERENCES ai_employee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分配策略配置';

-- ============================================================
-- 客服表组
-- ============================================================

CREATE TABLE human_agent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '客服姓名',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    password_hash VARCHAR(200) NOT NULL COMMENT '密码哈希',
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE' COMMENT 'ONLINE/BUSY/OFFLINE',
    current_load INT NOT NULL DEFAULT 0 COMMENT '当前待处理会话数',
    max_concurrent INT NOT NULL DEFAULT 5 COMMENT '最大并发接待数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_phone_tenant (phone, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工客服';

CREATE TABLE agent_channel_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL COMMENT '关联 human_agent.id',
    platform VARCHAR(20) NOT NULL COMMENT '有权限的平台',
    employee_id BIGINT NOT NULL COMMENT '有权限服务的AI员工',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    INDEX idx_agent (agent_id),
    UNIQUE KEY uk_agent_platform_employee (agent_id, platform, employee_id, tenant_id),
    CONSTRAINT fk_perm_agent FOREIGN KEY (agent_id) REFERENCES human_agent(id),
    CONSTRAINT fk_perm_employee FOREIGN KEY (employee_id) REFERENCES ai_employee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服渠道权限';

-- ============================================================
-- 权限管理表组
-- ============================================================

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL COMMENT '登录用户名',
    password_hash VARCHAR(200) NOT NULL COMMENT '密码哈希',
    agent_id BIGINT COMMENT '关联 human_agent.id',
    role_code VARCHAR(20) NOT NULL COMMENT '角色: ADMIN/LEADER/AGENT',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_username_tenant (username, tenant_id),
    INDEX idx_agent (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL COMMENT '角色编码: ADMIN/LEADER/AGENT',
    name VARCHAR(100) NOT NULL COMMENT '角色名称',
    description VARCHAR(500) COMMENT '角色描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_code_tenant (code, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色定义';

CREATE TABLE sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT COMMENT '父级权限ID（树形结构）',
    name VARCHAR(100) NOT NULL COMMENT '权限名称',
    type VARCHAR(10) NOT NULL COMMENT 'MENU/BUTTON/API',
    path VARCHAR(200) COMMENT '前端路由路径或后端API路径',
    permission_code VARCHAR(100) NOT NULL COMMENT '权限码',
    icon VARCHAR(50) COMMENT '菜单图标',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_code_tenant (permission_code, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单/权限定义';

CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '关联 sys_user.id',
    role_id BIGINT NOT NULL COMMENT '关联 sys_role.id',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_user_role (user_id, role_id, tenant_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

CREATE TABLE sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL COMMENT '关联 sys_role.id',
    permission_id BIGINT NOT NULL COMMENT '关联 sys_permission.id',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_role_perm (role_id, permission_id, tenant_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联';

-- ============================================================
-- 审计日志表
-- ============================================================

CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '操作人ID',
    action VARCHAR(50) NOT NULL COMMENT '操作类型: ASSIGN/TRANSFER/EXPORT/LOGIN/LOGOUT',
    target_type VARCHAR(50) COMMENT '目标类型: CONVERSATION/CUSTOMER/EMPLOYEE',
    target_id BIGINT COMMENT '目标ID',
    detail JSON COMMENT '操作详情JSON',
    ip_address VARCHAR(50) COMMENT '操作IP',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    INDEX idx_user (user_id),
    INDEX idx_action_time (action, created_at),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';
