# AI 智能客服系统 — 架构设计与 Phase 1 详细设计

**版本**: V1.0  
**日期**: 2026-06-11  
**状态**: 已确认，待实施

---

## 1. 项目概述与目标

### 1.1 项目背景

基于《AI智能客服系统需求说明文件 V1.5》，构建统一的 AI 智能客服系统。Phase 1（2-2.5 个月）目标：**AI 员工模块 100% 配置化能力上线，官网 + 小红书双渠道跑通，客服分配策略全量可用**。

### 1.2 技术栈

| 层次 | 技术选型 | 说明 |
|------|---------|------|
| 后端框架 | Spring Boot 3.x + JDK 17 | |
| 前端框架 | React 18 + Ant Design 5 + Vite | 管理后台（IM 工作台作为菜单项集成） |
| 大模型框架 | LangChain4j | 统一适配 Qwen / DeepSeek / Ollama |
| 数据库 | MySQL 8.0 | 结构化业务数据 |
| 搜索引擎 | Elasticsearch 8.x | 知识库向量检索 + 对话日志全文搜索 |
| 缓存 | Redis | Session / 热点配置 / 分布式锁 / 实时计数 |
| 消息队列 | RocketMQ | 异步消息分发 / 知识库训练 / 事件驱动 |
| 文件存储 | 本地文件系统 | `{config.storage.base-path}` 下按类型分目录 |
| 项目结构 | Maven 单模块 + 包分层 | 包名 `com.ai.cs` |
| 部署方式 | 私有化部署 | 架构预留 SaaS 多租户路由能力 |

### 1.3 开发策略

**纵向切片（按用户故事）**：每条切片独立可演示，优先级从高到低：

| 切片 | 内容 | 预计周期 |
|------|------|---------|
| S1 | 官网客户发消息 → AI 基于知识库检索回复 → 转人工触发 → 客服 IM 回复 → 会话关闭（最小闭环） | 4 周 |
| S2 | 管理后台：AI 员工配置 CRUD + 回复策略 7 大技能可视化配置 | 2 周 |
| S3 | 知识库训练：文件上传 → 解析 → 向量化 → 索引全链路 | 1.5 周 |
| S4 | 客服分配策略 4 种全覆盖 + 转人工规则 4 种全覆盖 | 1 周 |
| S5 | 小红书渠道接入 + 顾客名片（收集/筛选/排除技能数据沉淀） | 1 周 |
| S6 | 基础数据看板（3 块）+ 撞单防护 + 客户隐私隔离 | 1 周 |

---

## 2. 系统架构

### 2.1 五层架构

```
┌──────────────────────────────────────────────────────┐
│  接入层 (Gateway)                                     │
│  REST Controller | WebSocket Handler | Channel Adapter │
├──────────────────────────────────────────────────────┤
│  应用层 (Application)                                 │
│  AI员工服务 | 知识库服务 | 回复策略引擎 | 转人工规则引擎  │
│  客服分配引擎 | 会话管理服务 | 数据看板服务              │
├──────────────────────────────────────────────────────┤
│  领域层 (Domain)                                      │
│  AIEmployee | KnowledgeBase | Conversation |          │
│  Assignment | Customer 聚合根与实体                    │
├──────────────────────────────────────────────────────┤
│  基础设施层 (Infrastructure)                           │
│  LangChain4j LLM | ES 检索 | RocketMQ | Redis |       │
│  MySQL 持久化 | 本地文件存储                           │
├──────────────────────────────────────────────────────┤
│  [预留] 多租户路由 | 品牌白标主题 | License/计费 |       │
│  开放 API 网关                                         │
└──────────────────────────────────────────────────────┘
```

### 2.2 项目包结构

```
com.ai.cs
├── gateway/          接入层
│   ├── rest/         REST API Controller（管理后台接口）
│   ├── websocket/    WebSocket 消息处理器
│   └── channel/      渠道适配器实现
│       ├── spi/      ChannelAdapter 接口
│       ├── web/      官网 JS SDK 适配
│       └── xiaohongshu/  小红书 API 适配
│
├── application/      应用服务层
│   ├── aiemployee/   AI员工配置 + 回复策略应用服务
│   ├── knowledge/    知识库管理 + 训练应用服务
│   ├── assignment/   转人工规则 + 客服分配应用服务
│   ├── conversation/ 会话管理 + 消息路由应用服务
│   └── analytics/    数据看板查询服务
│
├── domain/           领域模型层
│   ├── employee/     AIEmployee 聚合根
│   ├── knowledge/    KnowledgeBase 聚合根
│   ├── conversation/ Conversation 聚合根
│   ├── assignment/   Assignment 聚合根
│   └── customer/     CustomerProfile 实体
│
├── infrastructure/   基础设施层
│   ├── llm/          LangChain4j 适配 (Qwen/DeepSeek/Ollama)
│   ├── search/       ES 向量检索 + 全文搜索
│   ├── mq/           RocketMQ Producer/Consumer
│   ├── cache/        Redis 缓存配置
│   ├── storage/      本地文件存储服务
│   └── persistence/  JPA Repository
│
└── shared/           共享工具
    ├── config/       全局配置
    ├── exception/    统一异常处理
    └── dto/          通用 DTO
```

---

## 3. 数据模型设计

### 3.1 MySQL 核心表（18 张）

#### AI 员工表组

**ai_employee** — AI 员工基础属性

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| name | VARCHAR(100) | 员工名称 |
| avatar | VARCHAR(500) | 头像路径 |
| greeting_msg | TEXT | 开场白消息 |
| style | VARCHAR(20) | 接待风格: PROFESSIONAL/WARM/ENTHUSIASTIC/RELIABLE |
| reply_length | VARCHAR(10) | 回复字数: SHORT/MEDIUM/DETAIL |
| content_check | JSON | 敏感词替代规则，如 `{"微信":"{V}","QQ":"{Q}"}` |
| aggregate_interval | INT | 客户消息聚合间隔(秒)，1-10 |
| delay_interval | INT | AI延迟回复间隔(秒)，1-10 |
| service_time_start | TIME | 服务开始时间 |
| service_time_end | TIME | 服务结束时间 |
| weekdays | VARCHAR(50) | 服务日，如 "1,2,3,4,5" |
| company_intro | TEXT | 公司介绍（必填） |
| product_intro | TEXT | 产品介绍（必填） |
| service_scope | TEXT | 服务对象描述（选填） |
| status | VARCHAR(20) | ENABLED/DISABLED |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| tenant_id | BIGINT | [预留] 租户ID |

**ai_employee_account** — 覆盖账号绑定

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| employee_id | BIGINT FK | 关联 ai_employee.id |
| platform | VARCHAR(20) | 平台: WEB/XIAOHONGSHU/DOUYIN/WECHAT_MP |
| account_id | VARCHAR(100) | 平台内唯一账号标识 |
| access_config | JSON | 平台接入配置（如 access_token 加密存储） |
| status | VARCHAR(20) | ENABLED/DISABLED |
| tenant_id | BIGINT | [预留] |

**ai_employee_reply_strategy** — 回复策略配置

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| employee_id | BIGINT FK | 关联 ai_employee.id |
| strategy_type | VARCHAR(30) | GREETING/RESPONSE/MODERATION/COLLECT/FILTER/EXCLUDE/FOLLOWUP/CONTACT |
| config_json | JSON | 策略配置（JSON格式，每种策略结构不同） |
| enabled | TINYINT | 是否启用 |
| sort_order | INT | 流水线执行顺序 |
| tenant_id | BIGINT | [预留] |

#### 知识库表组

**knowledge_base** — 知识库定义

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| name | VARCHAR(200) | 知识库名称 |
| description | TEXT | 知识库说明 |
| employee_id | BIGINT FK | 关联的 AI 员工（可多个员工共享） |
| created_at | DATETIME | |
| tenant_id | BIGINT | [预留] |

**knowledge_document** — 文档元数据

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| kb_id | BIGINT FK | 关联 knowledge_base.id |
| file_name | VARCHAR(500) | 原始文件名 |
| file_type | VARCHAR(20) | PDF/XLSX/DOCX/MD/TXT |
| file_size | BIGINT | 文件大小(bytes) |
| file_path | VARCHAR(500) | 本地存储路径 |
| status | VARCHAR(20) | PENDING/PROCESSING/DONE/PARTIAL_FAILED |
| chunk_count | INT | 分片数量 |
| created_at | DATETIME | |
| tenant_id | BIGINT | [预留] |

**knowledge_chunk** — 文档分片

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| doc_id | BIGINT FK | 关联 knowledge_document.id |
| kb_id | BIGINT FK | 关联 knowledge_base.id |
| content | MEDIUMTEXT | 分片文本内容 |
| chunk_index | INT | 分片序号 |
| es_doc_id | VARCHAR(100) | ES 中对应文档 ID |
| embedding_status | VARCHAR(20) | PENDING/EMBEDDED/FAILED |
| created_at | DATETIME | |
| tenant_id | BIGINT | [预留] |

#### 会话表组

**conversation** — 服务会话主表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| customer_id | BIGINT FK | 关联 customer_profile.id |
| employee_id | BIGINT FK | 关联 ai_employee.id |
| human_agent_id | BIGINT FK | 转人工后关联 human_agent.id（可为空） |
| channel | VARCHAR(20) | 渠道: WEB/XIAOHONGSHU |
| status | VARCHAR(20) | AI_ACTIVE/WAITING/HUMAN/QUEUED/CLOSED |
| start_time | DATETIME | 会话开始时间 |
| end_time | DATETIME | 会话结束时间 |
| close_reason | VARCHAR(50) | 关闭原因 |
| tenant_id | BIGINT | [预留] |

**message** — 消息记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| conversation_id | BIGINT FK | 关联 conversation.id |
| sender_type | VARCHAR(10) | CUSTOMER/AI/HUMAN/SYSTEM |
| sender_id | BIGINT | 发送者 ID |
| msg_type | VARCHAR(10) | text/image/card |
| content | TEXT | 消息内容 |
| metadata | JSON | 扩展信息（引用知识库来源等） |
| send_time | DATETIME | 发送时间 |
| tenant_id | BIGINT | [预留] |

**customer_profile** — 顾客名片

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| nickname | VARCHAR(100) | 昵称 |
| platform | VARCHAR(20) | 来源平台 |
| openid | VARCHAR(200) | 平台客户唯一标识 |
| avatar | VARCHAR(500) | 头像 |
| phone | VARCHAR(20) | 电话（加密存储） |
| email | VARCHAR(200) | 邮箱 |
| gender | VARCHAR(10) | 性别 |
| city | VARCHAR(50) | 城市 |
| tags | JSON | 标签数组 |
| extra_fields | JSON | AI 收集的自定义字段（如招聘岗位、期望薪资） |
| created_at | DATETIME | |
| tenant_id | BIGINT | [预留] |

#### 分配表组

**transfer_rule** — 转人工规则

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| employee_id | BIGINT FK | 关联 ai_employee.id |
| trigger_type | VARCHAR(20) | KEYWORD/SENTIMENT/MANUAL/TIME |
| trigger_config | JSON | 触发条件配置 |
| action_type | VARCHAR(30) | IMMEDIATE_TRANSFER/MARK_URGENT/SOOTHE_MESSAGE/LEAVE_MESSAGE |
| action_config | JSON | 执行动作附加参数 |
| priority | INT | 优先级（数字越小越高） |
| enabled | TINYINT | 是否启用 |
| tenant_id | BIGINT | [预留] |

**assignment_strategy** — 分配策略配置

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| employee_id | BIGINT FK | 关联 ai_employee.id |
| strategy_type | VARCHAR(20) | SPECIFIED/ROUND_ROBIN/LEAST_LOAD/HISTORY |
| config_json | JSON | 策略配置 |
| is_active | TINYINT | 当前是否生效（每个AI员工仅一条生效） |
| tenant_id | BIGINT | [预留] |

#### 客服表组

**human_agent** — 人工客服

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| name | VARCHAR(100) | 客服姓名 |
| phone | VARCHAR(20) | 手机号 |
| password_hash | VARCHAR(200) | 密码哈希 |
| status | VARCHAR(20) | ONLINE/BUSY/OFFLINE |
| current_load | INT | 当前待处理会话数 |
| max_concurrent | INT | 最大并发接待数 |
| tenant_id | BIGINT | [预留] |

**agent_channel_permission** — 客服渠道权限

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| agent_id | BIGINT FK | 关联 human_agent.id |
| platform | VARCHAR(20) | 有权限的平台 |
| employee_id | BIGINT FK | 有权限服务的 AI 员工 |
| tenant_id | BIGINT | [预留] |

### 3.2 Elasticsearch 索引

**knowledge_chunk_index** — 知识库检索

```json
{
  "chunk_id": { "type": "long" },
  "kb_id": { "type": "long" },
  "doc_id": { "type": "long" },
  "content": { "type": "text", "analyzer": "ik_smart" },
  "content_vector": { "type": "dense_vector", "dims": 1536 },
  "source_file": { "type": "keyword" },
  "tenant_id": { "type": "long" },
  "created_at": { "type": "date" }
}
```

**conversation_log_index** — 对话日志搜索

```json
{
  "conversation_id": { "type": "long" },
  "customer_id": { "type": "long" },
  "employee_id": { "type": "long" },
  "message": { "type": "text", "analyzer": "ik_smart" },
  "channel": { "type": "keyword" },
  "sender_type": { "type": "keyword" },
  "timestamp": { "type": "date" },
  "tenant_id": { "type": "long" }
}
```

### 3.3 本地文件存储

```
{config.storage.base-path}/
├── knowledge/         知识库文档
│   └── {kb_id}/
│       ├── {doc_id}.pdf
│       └── {doc_id}.xlsx
├── avatars/           头像
│   ├── employee/
│   └── customer/
├── images/            聊天图片
│   └── {yyyy-MM}/
│       └── {msg_id}.jpg
└── temp/              临时文件（定时清理）
```

---

## 4. AI 员工模块设计

### 4.1 AIEmployee 聚合根

AI 员工是系统的核心管理单元，包含：

- **基础属性**：名称、头像、开场白、接待风格、回复字数、敏感词替代、消息聚合间隔、延迟回复间隔
- **能力配置**：回复策略配置、知识库关联、转人工规则、客服分配策略
- **服务时段**：每周服务日 + 每日服务时间段
- **覆盖账号**：绑定的渠道平台账号列表

### 4.2 回复策略引擎（Pipeline Pattern）

采用 **策略模式 + 责任链**，每个技能独立实现 `ReplyStrategyNode` 接口，可插拔：

```java
interface ReplyStrategyNode {
    StrategyResult process(ConversationContext ctx, ReplyBuilder builder);
    int getOrder();  // 执行顺序
}
```

**7 个策略节点**（6 大技能 + 留资末端处理）：

| 节点 | 类型 | 说明 | 可中断 |
|------|------|------|--------|
| GreetingStrategy | 问候策略 | 新客/老客欢迎语选择 | ❌ |
| ExcludeStrategy | 排除策略 | 广告推销/非服务对象拒绝 | ✅ INTERRUPT |
| ModerationStrategy | 异常策略 | 敏感词过滤 + 情感安抚 | ✅ INTERRUPT(转人工) |
| KnowledgeStrategy | 调用策略 | ES 混合检索 + LLM 兜底 | ❌ |
| CollectStrategy | 收集技能 | 主动发问收集客户信息→名片 | ❌ |
| FilterStrategy | 筛选技能 | 多轮询问筛选目标客户 | ❌ |
| FollowUpStrategy | 沉默追问 | 超时自动追问 | ❌ |
| ContactStrategy | 留资策略 | 发名片/问名片（二选一） | ❌ |

**可插拔保证**：
1. Spring 自动注入 `List<ReplyStrategyNode>`，新增策略只需加 `@Component` 实现类
2. `@Order` 注解控制优先级
3. `ai_employee_reply_strategy.config_json.enabled` 控制动态启停

### 4.3 回复策略 JSON 配置示例

```json
// 问候策略
{"first_greeting":"您好，我是ICT顾问小倩，请问有什么可以帮您？","return_greeting":"欢迎回来！","offline_hint":"当前非服务时段，请留言，明天9:00后客服会联系您"}

// 收集技能
{"fields":[{"name":"招聘岗位","question":"请问您想应聘什么岗位？"},{"name":"期望薪资","question":"您的期望薪资范围是？"}]}

// 沉默追问
{"intervals":[{"seconds":30,"fixed":"老板，还在吗？"},{"seconds":60,"ai_generated":true}]}

// 留资策略
{"mode":"ASK_CARD","card_id":null,"ask_message":"方便留个联系方式吗？我可以给您发详细的配置表和最新报价单～"}
```

---

## 5. 知识库与 RAG 设计

### 5.1 文档训练链路（异步）

```
用户上传文件 → REST API 保存 → knowledge_document(status=PENDING)
                                    │
                            RocketMQ 消息通知
                                    │
                                    ▼
                            DocumentParser（按文件类型选解析器）
                            ├── PdfParser (Apache PDFBox)
                            ├── ExcelParser (Apache POI)
                            ├── WordParser (Apache POI)
                            └── TxtParser (JDK IO)
                                    │
                                    ▼
                            TextSplitter（语义边界分片，500~1000字/片，重叠50~100字）
                                    │
                                    ▼
                            EmbeddingService（LangChain4j EmbeddingModel → 1536维向量）
                                    │
                                    ▼
                            ES 批量索引 (knowledge_chunk_index)
                                    │
                                    ▼
                            knowledge_chunk(status=INDEXED) + knowledge_document(status=DONE)
```

### 5.2 RAG 混合检索

客户问题同时走两条检索路径，结果用 RRF (Reciprocal Rank Fusion) 融合：

1. **向量检索**：ES kNN 基于 dense_vector 余弦相似度，Top 10
2. **关键词检索**：ES BM25 + ik_smart 分词，Top 10
3. **RRF 融合**：`score = Σ 1/(k + rank_i)`，k=60，去重排序返回 Top 5
4. **相关性判断**：最高分 > 0.75 → 模板组装回复；≤ 0.75 → LLM 兜底

### 5.3 System Prompt 组装

```text
你是{公司名称}的AI客服，公司主营业务为：{公司介绍}
公司主要产品/服务包括：{产品介绍}
你只接待{服务对象}的客户，其他客户请礼貌告知不在服务范围。
{接待风格提示}
{回复字数限制}
{敏感词替换规则}
---
相关知识库内容：
{Top5 chunks}
---
对话历史：
{最近10轮对话}
---
客户最新消息：{message}
请用{接待风格}的语气回复。
```

### 5.4 LangChain4j LLM 配置

```yaml
langchain4j:
  providers:
    - id: qwen
      type: openai-compatible
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: ${QWEN_API_KEY}
      model: qwen-plus
    - id: deepseek
      type: openai-compatible
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY}
      model: deepseek-chat
    - id: ollama
      type: ollama
      base-url: http://localhost:11434
      model: qwen2.5:7b
```

---

## 6. 转人工规则与分配引擎

### 6.1 转人工规则引擎

4 种触发条件，规则独立评估，取最高优先级命中：

| 触发类型 | 条件 | 执行动作 | 优先级 |
|----------|------|---------|--------|
| KEYWORD | 客户消息包含「投诉/退款/违约/法律」 | 立即转人工 + 标记紧急 | 1 |
| SENTIMENT | 情感分析愤怒/焦急 | 立即转人工 + 安抚话术 | 2 |
| MANUAL | 客户说「转人工/找客服/不要机器人」 | 立即转人工 + 记录原因 | 3 |
| TIME | 非服务时段（如 22:00-09:00） | 提示留言 + 次日跟进（不转人工） | 4 |

### 6.2 客服分配引擎

4 种分配策略，共享 `AssignmentStrategy` 接口：

```java
interface AssignmentStrategy {
    HumanAgent assign(AssignmentContext ctx);
    StrategyType getType();
}
```

| 策略 | 实现方式 |
|------|---------|
| 指定分配 | 直接查 `config_json.targetAgentId` |
| 轮询分配 | Redis 计数器 `rr:{employeeId}` 递增取模 |
| 负载均衡 | 在线客服中 `currentLoad < maxConcurrent` 的最小负载者 |
| 历史归属 | 查该客户 30 天内上次接待客服，在线则分配；否则走轮询兜底 |

### 6.3 分配流程

```
TransferRuleEngine 返回 TransferDecision(shouldTransfer=true)
  → ① 撞单检查（30天活跃会话 → 原客服在线 → 直接分配）
  → ② 获取可用客服池（ONLINE + 负载未满 + 排班+权限过滤）
  → ③ 执行分配策略
  → ④ 成功：更新会话状态 + agent.current_load + RocketMQ 通知 
  → ⑤ 失败：进入 Redis 等待队列，定时重分配，2分钟超时提示留言
```

---

## 7. 客服 IM 工作台

### 7.1 前端布局

IM 工作台作为管理后台的一个菜单项，使用 Ant Design Pro 标准布局：

- **左侧**：标准管理后台菜单（AI员工/知识库/客服管理/数据看板/IM工作台）
- **IM 区域三栏**：
  - 左栏：对话导航（全部/AI接管/待介入/未分配/未回复/我的/同事对话）
  - 中栏：对话列表（头像/昵称/最后消息/时间）+ 对话内容区（消息气泡/输入框/转接/结束）
  - 右上角：顾客信息（昵称/渠道/电话/标签/AI收集的自定义字段）

### 7.2 会话状态机

```
AI_ACTIVE → WAITING → HUMAN/QUEUED → CLOSED
                ↑                        ↑
                └──── 超时/离开 ──────────┘
```

6 个状态：`AI_ACTIVE` / `WAITING` / `HUMAN` / `QUEUED` / `CLOSED`

### 7.3 WebSocket 消息协议

基于参考文档 `WebSocket对接接口文档.md` 的核心消息类型精简实现：

**消息格式**：
```json
{
    "type": "message.request",
    "id": "uuid",
    "code": 0,
    "data": { ... }
}
```

**核心消息类型**：

| 类型 | 方向 | 说明 |
|------|------|------|
| `ping.request` / `pong.response` | C↔S | 心跳 |
| `message.request` | S→C | 客户新消息 |
| `message.response` | C→S | 客服/AI 回复 |
| `subscriber.open` | S→C | 新客户进入 |
| `subscriber.close` | S→C | 客户离开 |
| `session.close` | C→S | 客服主动关闭 |
| `transfer.request` | C↔S | 会话转移发起/通知 |
| `transfer.accept` / `transfer.reject` | C→S | 接受/拒绝转移 |
| `transfer.accepted` / `transfer.rejected` | S→C | 转移结果通知 |
| `employee.status` | C↔S | 客服状态切换/通知 |
| `employee.report` | S→C | 当日接待统计 |
| `auth.failed` / `system.logout` | S→C | 认证失败/退出登录 |

### 7.4 后端 WebSocket 架构

- 使用 Spring 原生 `@ServerEndpoint("/ws/chat/{ticket}")`
- ticket 认证 → 建立客服身份 → 注册到 SessionRegistry
- 消息 type 分发到对应 Handler（MessageSendHandler / TransferHandler / StatusSwitchHandler 等）
- Redis 存储 `ws:agent:{agentId} → nodeId` 映射，支持跨节点消息推送

### 7.5 客户端聊天 SDK

官网一行代码接入：
```html
<script src="/sdk/chat-widget.js"></script>
<script>
  AIChat.init({ appId: 'site_main', position: 'bottom-right', primaryColor: '#1890ff' });
</script>
```

技术选型：Preact 渲染聊天窗口（~40KB gzip），Shadow DOM 样式隔离。

---

## 8. 渠道接入

### 8.1 渠道适配器 SPI

```java
interface ChannelAdapter {
    ChannelType getType();
    Message normalize(Object rawMessage);
    void send(Long subId, Message msg);
    CustomerProfile syncCustomer(String openid);
    void onCustomerEnter(String openid);
    void onCustomerLeave(String openid);
}
```

Phase 1 实现：`WebChannelAdapter` + `XiaohongshuChannelAdapter`

### 8.2 官网/Web 渠道

- 客户侧：WebSocket 长连接 → Gateway → 查找 AI 员工 → 创建会话 → 发送开场白 → 进入回复流水线
- `appId` → `ai_employee_account` 表查找对应 AI 员工

### 8.3 小红书渠道

- 配置 Webhook URL 接收消息（私信/评论），验签后 normalize 为内部 Message
- 主动调用小红书 API 发送消息，使用 Guava RateLimiter 限流
- access_token 在 Redis 中缓存，过期前自动刷新

---

## 9. 数据看板

Phase 1 三块看板（读操作，不做实时流计算）：

| 看板 | 核心指标 | 数据来源 | 刷新策略 |
|------|---------|---------|---------|
| AI员工看板 | 会话量/解决率/转人工率/满意度/知识库命中率 | conversation + message 聚合 | Redis 缓存 5min |
| 服务看板 | 实时会话量/响应时长/解决率/转人工率 | Redis 实时计数 + MySQL 定时聚合 | WebSocket 30s 推送 |
| 高频问题 TOP20 | 客户问题聚类 + 频次排名 | ES aggregation | 按需查询 |

前端图表：`@ant-design/charts` (G2Plot)，看板布局用 Ant Design Pro 的 ProCard。

---

## 10. 安全与非功能性设计

### 10.1 安全

- **认证**：Spring Security + JWT（管理后台登录），WebSocket 通过 ticket 认证
- **权限**：RBAC 细粒度控制（客服仅见自己会话，主管见团队，管理员见全部）
- **数据加密**：敏感信息（手机号、身份证号）AES-256 加密存储，展示脱敏
- **操作审计**：关键操作（分配/转接/数据导出）写入 `audit_log` 表完整留痕
- **客户隐私**：`conversation` + `message` 查询按 `agent_id` 过滤

### 10.2 性能

| 指标 | 目标 | 实现方式 |
|------|------|---------|
| AI 首响 ≤ 1s | 达标 | ES 检索缓存 + LLM 流式输出 |
| 常规查询 ≤ 2s | 达标 | Redis 热点配置缓存 |
| 客服分配通知 ≤ 10s | 达标 | RocketMQ 异步推送 + WebSocket |
| 并发 ≥ 20 路 | 达标 | Spring WebSocket 天然支持 |

### 10.3 扩展性预留

所有业务表预留 `tenant_id` 字段，当前阶段写死为 1。API 路径预留版本前缀 `/api/v1/`。配置表（UI 主题色/Logo）预留 `theme_config` JSON 字段。

---

## 11. Phase 1 验收标准

1. ✅ 3 个 AI 员工（小通/小智/小 H）独立配置运行，互不干扰
2. ✅ 回复策略 7 大技能全部可配置生效（含可插拔支持）
3. ✅ 4 种分配策略配置化切换
4. ✅ 4 种转人工规则触发条件全覆盖
5. ✅ 知识库文档上传 → 解析 → 向量化 → 索引 → 检索回复全链路跑通
6. ✅ 官网 + 小红书双渠道 AI 对话闭环
7. ✅ 转人工后客服可在 IM 工作台完成接待闭环（含转接/结束/顾客名片）
8. ✅ 3 块基础数据看板数据准确展示

---

## 12. 不实施内容（Phase 2+）

- 微信小程序/公众号/抖音/视频号/企业微信渠道接入
- 客服排班管理 + 绩效看板
- 转化漏斗 + AI 贡献度分析
- 客服分配策略效果分析
- Webhook 开放平台 + RESTful API 文档自动生成
- 多租户路由启用 + 品牌白标主题 + License 计费
- 客服辅助话术（AI 实时推荐回复）
- 消息 8 秒/20 秒兜底话术 + 3 分钟超时提醒
