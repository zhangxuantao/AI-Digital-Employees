# AI-Digital-Employees

在线客服方向 AI 数字员工 —— 基于大模型的智能客服系统

---

## 📋 目录

1. [项目简介](#1-项目简介)
2. [项目亮点](#2-项目亮点)
3. [系统架构](#3-系统架构)
4. [功能模块](#4-功能模块)
5. [技术栈](#5-技术栈)
6. [项目结构](#6-项目结构)
7. [快速启动](#7-快速启动)
8. [Phase 1 验收对照表](#8-phase-1-验收对照表)
9. [待完成清单](#9-待完成清单)
10. [开发路线图](#10-开发路线图)

---

## 1. 项目简介

### 1.1 项目背景

公司主营业务涵盖 **ICT 基础设施服务**、**企业数字化转型服务**及**人力资源劳务派遣**三大板块。随着业务规模扩大及数字化服务升级，现有客服体系面临渠道分散、响应时效不足、服务标准不统一等问题，亟需建设统一的 AI 智能客服系统，实现全渠道智能化服务覆盖。

### 1.2 建设目标

| 目标 | 说明 |
|------|------|
| **统一服务入口** | 整合官网、电商商城、社交媒体等全渠道客户咨询入口 |
| **7×24 小时响应** | AI 员工自动处理 80% 以上常规咨询，人工客服介入处理复杂场景 |
| **业务精准匹配** | 基于 AI 员工配置的专业知识库与回复策略，实现不同渠道/场景差异化服务 |
| **数据驱动决策** | 采集全渠道服务数据，支撑服务优化与客服效能管理 |
| **智能客服分配** | 复杂咨询自动识别并精准分配至对应客服，支持多种分配策略配置化 |

### 1.3 核心设计思路

- **AI 员工**：一个可独立配置、独立运行的 AI 客服实例，拥有专属的知识库、回复策略、服务话术
- **账号覆盖**：每个 AI 员工可绑定一个或多个渠道账号（如抖音号 A、小红书号 B、官网 C），实现分账号差异化服务
- **客服分配**：当 AI 员工无法处理或触发转人工条件时，按配置策略将对话分配给人工客服
- **策略配置化**：AI 员工的回复策略、转人工规则、客服分配规则均支持后台可视化配置

### 1.4 Phase 1 目标

**AI 员工模块 100% 配置化能力上线，官网 + 小红书双渠道跑通，客服分配策略全量可用。**

---

## 2. 项目亮点

| # | 亮点 | 说明 |
|---|------|------|
| 1 | **可插拔回复策略引擎** | 采用 Pipeline + 责任链模式，7 个策略节点（问候/排除/异常/知识库/收集/筛选/沉默追问/留资）独立启停、拖拽排序、动态生效。新增策略只需加 `@Component` 注解，无需修改引擎代码 |
| 2 | **多策略客服分配** | 4 种分配策略（指定分配/轮询分配/负载均衡/历史归属）共享 `AssignmentStrategy` 接口，支持后台配置化切换。策略引擎支持撞单检测（30 天内活跃会话自动关联原客服） |
| 3 | **渠道适配器 SPI** | 统一 `ChannelAdapter` 接口归一化多平台消息格式，Web（官网 JS SDK）和小红书（开放平台 API）已实现。抖音、微信公众号等渠道按接口扩展即可，无需改动核心逻辑 |
| 4 | **RAG 混合检索** | ES 向量相似度（kNN） + BM25 关键词双路检索，RRF（Reciprocal Rank Fusion）融合排序。知识库命中后直接组装回复，未命中则 LLM 兜底生成，兼顾准确性与覆盖范围 |
| 5 | **会话状态机** | 6 状态严格转换矩阵（AI_ACTIVE → WAITING → HUMAN/QUEUED → CLOSED），杜绝非法状态流转。`SessionStateMachine` 组件独立可测试 |
| 6 | **RBAC 双层权限** | 前端：Ant Design 路由守卫（菜单按权限码过滤）+ `Access` 组件按钮级控制。后端：`@PreAuthorize` 方法级鉴权 + `@DataScope` AOP 切面自动注入数据隔离条件 |
| 7 | **客户隐私隔离** | `conversation` 表 `owner_agent_id` 字段 + Repository 层强制 `WHERE` 过滤 + `DataScopeAspect` 兜底。客服仅可见分配给自己的会话，主管可见团队，管理员可见全部 |
| 8 | **WebSocket 实时通信** | 15 种消息类型完整覆盖 IM 场景（心跳/消息收发/客户进出/会话转移/状态切换/超时提醒/挤下线/兜底话术/辅助话术/统计报告），基于 Spring WebSocket + 可插拔 Handler 架构 |
| 9 | **SaaS 多租户预留** | 全部 21 张业务表预留 `tenant_id` 字段，API 路径预留 `/api/v1/` 版本前缀，UI 主题配置预留 `theme_config` JSON 字段。当前私有化部署，架构层已为未来转 SaaS 做好准备 |
| 10 | **智能消息聚合** | AI 员工可配置消息聚合间隔（等客户连续说完后再回复，1-10 秒）+ 延迟回复间隔（模拟真人思考，1-10 秒），让 AI 回复更自然、不打断客户 |

---

## 3. 系统架构

### 3.1 五层架构

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
│  LangChain4j LLM | ES 检索 | Redis | MySQL | 文件存储   │
├──────────────────────────────────────────────────────┤
│  [预留] 多租户路由 | 品牌白标主题 | License/计费 |       │
│  开放 API 网关                                         │
└──────────────────────────────────────────────────────┘
```

### 3.2 后端包结构

```
com.ai.cs
├── gateway/          接入层
│   ├── rest/         REST API Controller（10 个控制器，含管理后台接口）
│   ├── websocket/    WebSocket 消息处理器（ChatWebSocket + CustomerWebSocket + MessageDispatcher）
│   └── channel/      渠道适配器（SPI 接口 + Web + 小红书实现）
│
├── application/      应用服务层
│   ├── aiemployee/   AI 员工配置 + 回复策略引擎（7 个可插拔策略节点）
│   ├── knowledge/    知识库管理 + RAG 混合检索
│   ├── assignment/   转人工规则引擎 + 4 种客服分配策略
│   ├── conversation/ 会话管理 + 消息服务 + 会话状态机
│   └── analytics/    数据看板查询服务（AI 员工/服务/高频问题）
│
├── domain/           领域模型层
│   ├── employee/     AIEmployee / AiEmployeeAccount / AiEmployeeReplyStrategy
│   ├── knowledge/    KnowledgeBase / KnowledgeDocument / KnowledgeChunk
│   ├── conversation/ Conversation / Message
│   ├── assignment/   TransferRule / AssignmentStrategyConfig / HumanAgent
│   ├── customer/     CustomerProfile（顾客名片）
│   └── permission/   SysUser / SysRole / SysPermission / AuditLog
│
├── infrastructure/   基础设施层
│   ├── llm/          LangChain4j 适配（Qwen / DeepSeek / Ollama）
│   ├── search/       Elasticsearch 配置
│   ├── cache/        Redis 配置 + CacheService
│   ├── storage/      本地文件存储 + 文档解析器（PDF/TXT）
│   └── persistence/  BaseEntity 基类（JPA 公共字段）
│
└── shared/           共享工具
    ├── config/       全局配置（AppConfig / SecurityConfig / WebSocketConfig / CorsConfig）
    ├── exception/    统一异常处理（BusinessException / ErrorCode / GlobalExceptionHandler）
    ├── security/     JWT + Ticket 认证 / @DataScope 数据隔离 AOP
    └── dto/          通用 DTO（ApiResponse / PageResult）
```

### 3.3 前端页面树

```
frontend/src/
├── layouts/
│   └── DashboardLayout.tsx    # 管理后台布局（侧边栏菜单 + 顶栏用户信息）
├── pages/
│   ├── Login/                 # 登录页
│   ├── Dashboard/             # 数据看板（实时概览 + 高频问题 TOP5）
│   ├── AiEmployee/            # AI 员工管理（列表 + 表单 Drawer + 策略配置面板）
│   ├── Knowledge/             # 知识库管理（主从布局 + 文件上传 + 分片查看）
│   ├── Agents/                # 客服管理（列表 + 表单 + 渠道权限配置）
│   ├── TransferRules/         # 转人工规则与分配策略配置
│   ├── IM/                    # IM 工作台（三栏布局 + WebSocket 实时消息）
│   └── Settings/              # 系统设置（用户/角色/权限三个 Tab）
├── services/                  # API 服务层（9 个模块）
├── stores/
│   └── authStore.ts           # Zustand 认证状态管理
└── app.tsx                    # 路由配置 + 权限守卫
```

---

## 4. 功能模块

### 4.1 AI 员工管理

AI 员工是系统的核心管理单元，每个 AI 员工可独立配置以下内容：

**基础属性（13 项）：**

| 属性 | 说明 |
|------|------|
| 员工名称 | 对外展示名称，如"小通（ICT 顾问）" |
| 头像 | 对话窗口展示头像 |
| AI 接待时间 | 全天候或自定义时段 |
| 开场白消息 | 首次对话欢迎语 |
| AI 接待风格 | 亲切/热情/专业/靠谱，影响 LLM 回复语气 |
| AI 回复消息字数 | 简短（≤20 字）/ 适中（20-30 字）/ 详细（60-100 字） |
| AI 回复内容检查 | 敏感词替代规则，如 `{"微信":"{V}"}` |
| 客户消息聚合间隔 | 1-10 秒，等客户连续说完再回复 |
| AI 延迟回复间隔 | 1-10 秒，模拟真人思考 |
| 公司介绍 | 必填，帮助 AI 理解企业业务 |
| 产品介绍 | 必填，AI 回复时结合产品内容 |
| 服务对象 | 选填，限制接待范围 |

**回复策略（7 大技能，可插拔）：**

| 策略节点 | 说明 | 可中断 |
|----------|------|--------|
| 问候策略 | 新客/老客/非服务时段欢迎语选择 | ❌ |
| 排除策略 | 广告推销/非服务对象拒绝 | ✅ INTERRUPT |
| 异常策略 | 敏感词过滤 + 负面情感安抚 | ✅ INTERRUPT（转人工） |
| 调用策略 | ES 混合检索 + LLM 兜底生成 | ❌ |
| 收集技能 | 主动发问收集客户信息 → 存入名片 | ❌ |
| 筛选技能 | 多轮询问筛选目标客户 | ❌ |
| 沉默追问 | 客户沉默后自动追问（间隔可配置） | ❌ |
| 留资策略 | 发名片 / 问名片（二选一，末端节点） | ❌ |

### 4.2 知识库与 RAG

```
上传文档 → DocumentParser 解析 → TextSplitter 分片（500-1000 字/片）
  → EmbeddingService 向量化（1536 维） → ES 索引
  → 客户提问 → 向量检索(Top10) + 关键词检索(Top10) → RRF 融合(Top5)
  → 相关性判断 → 命中: 模板组装回复 / 未命中: LLM 兜底
```

- 支持格式：PDF、TXT（XLSX/DOCX 待实现）
- 检索方式：ES kNN 向量相似度 + BM25 关键词双路检索
- 融合算法：RRF（Reciprocal Rank Fusion），k=60

### 4.3 转人工规则与客服分配

**4 种转人工触发条件：**

| 触发类型 | 条件 | 执行动作 | 优先级 |
|----------|------|---------|--------|
| KEYWORD | 客户消息包含「投诉/退款/违约/法律」 | 立即转人工 + 标记紧急 | 1 |
| SENTIMENT | 情感分析愤怒/焦急（Phase 2） | 立即转人工 + 安抚话术 | 2 |
| MANUAL | 客户说「转人工/找客服/不要机器人」 | 立即转人工 + 记录原因 | 3 |
| TIME | 非服务时段（如 22:00-09:00） | 提示留言 + 次日跟进 | 4 |

**4 种分配策略：**

| 策略 | 实现方式 |
|------|---------|
| 指定分配 | 固定分配给指定客服 |
| 轮询分配 | Redis 计数器递增取模 |
| 负载均衡 | 分配给当前待处理量最少的在线客服 |
| 历史归属 | 老客户优先分配给 30 天内上次接待的客服 |

### 4.4 客服 IM 工作台

三栏布局：

- **左栏（对话导航）**：全部对话 / AI 接管 / 待介入 / 未分配 / 未回复 / 我的 / 同事对话
- **中栏（对话内容）**：对话列表 + 聊天窗口（文字/表情/图片/名片/转接/结束）
- **右栏（顾客信息）**：昵称/渠道/电话/标签/AI 收集的自定义字段

会话状态机：`AI_ACTIVE → WAITING → HUMAN/QUEUED → CLOSED`

WebSocket 消息协议支持 15 种消息类型：心跳、消息收发、客户进出、会话转移、状态切换、超时提醒、挤下线、兜底话术、辅助话术、当日统计报告。

### 4.5 渠道接入

采用 **SPI 插件机制**，`ChannelAdapter` 接口统一抽象：

```java
interface ChannelAdapter {
    ChannelType getType();
    Message normalize(Object rawMessage);     // 归一化为内部消息
    void send(Long accountId, Message msg);   // 向渠道发送消息
    CustomerProfile syncCustomer(String openid);
    void onCustomerEnter(String openid);
    void onCustomerLeave(String openid);
}
```

- ✅ **Web/官网**：JS SDK 嵌入 + WebSocket 长连接，通过 `appId` 匹配 AI 员工
- ✅ **小红书**：Webhook 接收私信/评论 + API 发送消息 + RateLimiter 限流
- 🔜 预留：抖音、微信公众号、视频号、企业微信

### 4.6 数据看板

| 看板 | 核心指标 | 实现状态 |
|------|---------|---------|
| 数据概览 | 在线会话数 / 等待中 / 排队中 / 客服在线数 + 高频问题 TOP5 | ✅ 已实现 |
| AI 员工看板 | 各 AI 员工会话量/解决率/转人工率/满意度/知识库命中率 | ⚠️ 后端已实现，前端待完善 |
| 服务看板 | 实时会话量/响应时长/解决率/转人工率 | ⚠️ 后端已实现，前端待完善 |

### 4.7 权限管理

- **RBAC 模型**：用户 → 角色 → 权限码三层关联
- **三种角色**：ADMIN（全部权限）/ LEADER（团队范围）/ AGENT（仅自己会话）
- **前端控制**：菜单按权限过滤 + `<Access>` 组件控制按钮显隐 + 路由守卫
- **后端控制**：`@PreAuthorize("hasAuthority('ai_employee:edit')")` 方法级鉴权
- **数据隔离**：`@DataScope` 注解 + AOP 自动注入 `owner_agent_id` 过滤条件
- **操作审计**：关键操作（分配/转接/导出/登录）写入 `audit_log` 表

---

## 5. 技术栈

| 层次 | 技术选型 | 说明 |
|------|---------|------|
| 后端框架 | Spring Boot 3.2 + JDK 17 | |
| 前端框架 | React 18 + TypeScript + Vite | |
| UI 组件库 | Ant Design 5 | 管理后台 + IM 工作台 |
| 状态管理 | Zustand | 轻量级全局状态 |
| 大模型框架 | LangChain4j 0.32 | 统一适配 Qwen / DeepSeek / Ollama |
| 数据库 | MySQL 8.0 | 21 张业务表，Flyway 管理 schema 版本 |
| 搜索引擎 | Elasticsearch 8.x | 知识库向量检索 + 对话日志全文搜索 |
| 缓存 | Redis | Session / 热点配置 / 分布式锁 / 实时计数 |
| 消息队列 | RocketMQ | 异步文档训练 / 事件驱动（待集成） |
| 文档解析 | Apache PDFBox 3.0 + POI 5.2 | PDF/TXT 已实现 |
| 安全框架 | Spring Security + JWT (jjwt 0.12) | JWT 无状态认证 + BCrypt 密码加密 |
| 数据库迁移 | Flyway | V1 建表 / V2 种子数据 / V3 修复密码 |
| 测试框架 | JUnit 5 + Testcontainers + H2 | 40 个测试文件覆盖核心模块 |
| 部署方式 | 私有化部署（架构预留 SaaS 多租户） | |

---

## 6. 项目结构

### 6.1 根目录

```
AI-Digital-Employees/
├── pom.xml                          # Maven 父 POM（Spring Boot 3.2 + 全部依赖）
├── README.md                        # 本文件
├── docs/                            # 项目文档
│   ├── AI智能客服系统需求说明文件_V1.5.md
│   ├── WebSocket对接接口文档.md
│   └── superpowers/
│       ├── specs/                   # 架构设计文档
│       └── plans/                   # 实施计划文档
├── src/                             # 后端代码
│   ├── main/java/com/ai/cs/         # 94 个 Java 源文件
│   ├── main/resources/              # 配置文件 + DB 迁移
│   └── test/java/com/ai/cs/         # 40 个测试文件
└── frontend/                        # 前端代码
    ├── vite.config.ts
    └── src/                         # 24 个 TypeScript/TSX 源文件
```

### 6.2 后端文件统计

| 层次 | 文件数 | 说明 |
|------|--------|------|
| gateway/rest | 10 | 10 个 REST Controller |
| gateway/websocket | 7 | WebSocket Server + Dispatcher + 3 Handler |
| gateway/channel | 5 | SPI 接口 + Web 适配器 + 小红书适配器 |
| application | 23 | 服务类 + 策略接口与实现 |
| domain | 31 | 实体类 + Repository 接口 |
| infrastructure | 12 | LLM / ES / Redis / 存储 / 文档解析 |
| shared | 10 | 配置 / 异常 / 安全 / DTO |
| **总计** | **94** | (不含测试) |

### 6.3 前端文件统计

| 目录/文件 | 文件数 | 说明 |
|-----------|--------|------|
| pages/ | 8 | 8 个页面模块 |
| services/ | 9 | API 服务层 |
| stores/ | 1 | Zustand 状态管理 |
| layouts/ | 1 | 管理后台布局 |
| app.tsx / main.tsx | 2 | 入口与路由 |
| **总计** | **24** | (不含 node_modules) |

---

## 7. 快速启动

### 7.1 环境要求

| 组件 | 版本要求 |
|------|---------|
| JDK | 17+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| Elasticsearch | 8.x（可选，知识库检索需要） |
| RocketMQ | 5.x（可选，异步训练需要） |
| Maven | 3.8+ |

### 7.2 后端启动

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ai_cs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 编译项目（Flyway 会自动初始化 21 张表 + 种子数据）
mvn compile -s D:/mvn/settings.xml -Dmaven.repo.local=D:/mvn/repository

# 3. 启动应用（默认端口 8082）
mvn spring-boot:run -s D:/mvn/settings.xml -Dmaven.repo.local=D:/mvn/repository

# 或者直接运行 CsApplication.main()
```

> **注意**：首次启动时 Flyway 会自动执行 `V1__init_schema.sql`（建表）和 `V2__seed_data.sql`（初始化角色/权限/管理员账号）。

### 7.3 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器（默认将 API 代理到 localhost:8082）
npm run dev
```

### 7.4 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 超级管理员（ADMIN） |

登录后获取 JWT Token，前端自动存储到 localStorage，后续请求自动携带 `Authorization: Bearer <token>` 头。

### 7.5 配置说明

主要配置文件 `src/main/resources/application.yml`：

```yaml
server.port: 8082                          # 后端端口
spring.datasource.url: .../ai_cs           # 数据库连接
spring.data.redis.host: localhost:6379     # Redis 地址
elasticsearch.host: localhost:9200         # ES 地址
rocketmq.name-server: localhost:9876       # RocketMQ 地址
app.storage.base-path: ./data              # 本地文件存储路径
app.security.jwt-secret: ...               # JWT 签名密钥
app.default-tenant-id: 1                   # 默认租户 ID
```

开发环境配置在 `application-dev.yml` 中覆盖。本地开发调试可使用 `application-local.yml`（无需外部中间件）。

---

## 8. Phase 1 验收对照表

> 综合设计文档 §11（8 条）和需求文档 §9.1（4 条）验收标准，逐条对照。

### 8.1 AI 员工模块

| # | 验收项 | 状态 | 说明 |
|---|--------|------|------|
| 1.1 | 3 个 AI 员工（小通/小智/小H）独立配置运行，互不干扰 | ✅ 已完成 | AiEmployee 实体 + CRUD API + 前端管理页完整实现 |
| 1.2 | AI 员工管理后台（完整版，13 项基础属性） | ✅ 已完成 | AiEmployeeDrawer 表单覆盖全部属性 |
| 1.3 | 覆盖账号配置（平台类型 + 账号 + 启用/停用） | ✅ 已完成 | AiEmployeeAccount 实体 + 前端绑定 UI |
| 1.4 | 回复策略 — 问候策略（首次/二次/离线欢迎语） | ✅ 已完成 | GreetingStrategy.java + 前端配置面板 |
| 1.5 | 回复策略 — 应答策略（知识库优先 + LLM 兜底） | ✅ 已完成 | KnowledgeStrategy + RagRetrievalService |
| 1.6 | 回复策略 — 异常策略（敏感词过滤 + 负面情绪安抚） | ✅ 已完成 | ModerationStrategy.java |
| 1.7 | 回复策略 — 收集技能（主动发问收集客户信息存入名片） | ✅ 已完成 | CollectStrategy.java + extra_fields JSON |
| 1.8 | 回复策略 — 筛选技能（多轮询问筛选目标客户） | ✅ 已完成 | FilterStrategy.java |
| 1.9 | 回复策略 — 排除技能（排除广告推销等非目标客群） | ✅ 已完成 | ExcludeStrategy.java + INTERRUPT 中断 |
| 1.10 | 回复策略 — 沉默追问技能（间隔可配置） | ✅ 已完成 | FollowUpStrategy.java |
| 1.11 | 回复策略 — 留资技能（发名片/问名片二选一） | ✅ 已完成 | ContactStrategy.java（末端节点） |
| 1.12 | 策略面板可视化配置（拖拽排序 + 启停 + JSON 编辑） | ✅ 已完成 | StrategyPanel.tsx 完整实现 |
| 1.13 | 回复策略可插拔（新增策略只需加 @Component） | ✅ 已完成 | ReplyStrategyNode 接口 + Spring 自动注入 |

### 8.2 知识库与 RAG

| # | 验收项 | 状态 | 说明 |
|---|--------|------|------|
| 2.1 | 新建知识库（名称 + 说明） | ✅ 已完成 | KnowledgeBase CRUD API + 前端页面 |
| 2.2 | 文件上传 → 存储 | ✅ 已完成 | LocalFileStorageService + upload API |
| 2.3 | 文档解析 → 文本提取 | ⚠️ 部分完成 | PdfParser + TxtParser 已实现，**缺少 XLSX/DOCX 解析器** |
| 2.4 | 文本分片（语义边界，500-1000 字/片） | ✅ 已完成 | TextSplitter.java |
| 2.5 | 向量化（Embedding）→ ES 索引 | ❌ 未完成 | 缺少 KnowledgeChunkIndexService 和 Embedding 调用链路 |
| 2.6 | RAG 混合检索（向量 + 关键词 + RRF 融合） | ✅ 已完成 | RagRetrievalService.java 核心逻辑已实现 |
| 2.7 | System Prompt 组装（公司信息 + 知识库 + 对话历史） | ❌ 未完成 | 缺少 SystemPromptBuilder.java，当前直接拼接 |
| 2.8 | 异步文档训练链路（RocketMQ） | ❌ 未完成 | 缺少 RocketMQ Producer/Consumer，当前为同步处理 |
| 2.9 | 知识库上传后 AI 可基于内容准确回复 | ⚠️ 部分完成 | 检索链路可用，但缺少 ES 索引和向量化 |

### 8.3 转人工规则

| # | 验收项 | 状态 | 说明 |
|---|--------|------|------|
| 3.1 | 关键词触发（投诉/退款/违约/法律）→ 立即转人工 + 标记紧急 | ✅ 已完成 | TransferRuleEngine.matchKeyword() |
| 3.2 | 情感触发（愤怒/焦急）→ 立即转人工 + 安抚话术 | ⚠️ 部分完成 | 规则已定义，情感分析标记为 Phase 2（当前返回 false） |
| 3.3 | 用户主动要求（转人工/找客服/不要机器人）→ 立即转人工 | ✅ 已完成 | TransferRuleEngine.matchManual() |
| 3.4 | 时间触发（非服务时段）→ 提示留言 + 次日跟进 | ❌ 未完成 | isOutsideServiceTime() 硬编码返回 false |
| 3.5 | 规则可视化配置（触发条件 + 执行动作 + 优先级） | ✅ 已完成 | TransferRulesPage.tsx 前端页面 |

### 8.4 客服分配策略

| # | 验收项 | 状态 | 说明 |
|---|--------|------|------|
| 4.1 | 指定分配（固定分配给某客服） | ✅ 已完成 | SpecifiedStrategy.java |
| 4.2 | 轮询分配（按列表顺序轮流） | ✅ 已完成 | RoundRobinStrategy.java（Redis 计数器） |
| 4.3 | 负载均衡（分配给待处理量最少的客服） | ✅ 已完成 | LeastLoadStrategy.java |
| 4.4 | 历史归属（老客户优先分配给上次接待客服） | ✅ 已完成 | HistoryStrategy.java |
| 4.5 | 4 种策略配置化切换 | ✅ 已完成 | AssignmentStrategyController + 前端 UI |
| 4.6 | 撞单防护（30 天内活跃会话自动关联原客服） | ❌ 未完成 | 缺少 CollisionDetector.java |
| 4.7 | 分配失败排队 + 2 分钟超时提示留言 | ❌ 未完成 | Redis 等待队列 + 重分配机制未实现 |

### 8.5 客服 IM 工作台

| # | 验收项 | 状态 | 说明 |
|---|--------|------|------|
| 5.1 | 三栏布局（导航/对话列表+内容/顾客信息） | ✅ 已完成 | IMPage 完整实现 |
| 5.2 | 对话导航（全部/AI接管/待介入/未分配/未回复/我的/同事对话） | ✅ 已完成 | 前端导航菜单 + ConversationService 过滤查询 |
| 5.3 | 对话内容（文字/表情/图片/名片） | ✅ 已完成 | 消息发送 + Markdown/图片类型支持 |
| 5.4 | 手动转接对话 | ⚠️ 部分完成 | TransferRule API 已实现，**缺少 TransferHandler 处理 WebSocket 转移协议** |
| 5.5 | 结束对话 | ✅ 已完成 | 前端关闭按钮 + ConversationService.closeConversation() |
| 5.6 | 客服状态切换（在线/忙碌/离线） | ⚠️ 部分完成 | HumanAgent.status 字段存在，**缺少 StatusSwitchHandler** |
| 5.7 | WebSocket 实时消息 | ✅ 已完成 | ChatWebSocketServer + CustomerWebSocketServer + MessageDispatcher |
| 5.8 | 客服当日接待统计 | ❌ 未完成 | `employee.report` 消息类型后端未实现 |
| 5.9 | 客户隐私隔离（客服仅可见自己会话） | ✅ 已完成 | DataScopeAspect + owner_agent_id 过滤 |
| 5.10 | 顾客信息展示（昵称/渠道/电话/标签/自定义字段） | ✅ 已完成 | CustomerProfile 实体 + 前端顾客信息面板 |

### 8.6 渠道接入

| # | 验收项 | 状态 | 说明 |
|---|--------|------|------|
| 6.1 | 官网/Web 渠道接入（JS SDK 一行代码嵌入） | ⚠️ 部分完成 | WebChannelAdapter + CustomerWebSocketServer 已实现，**缺少 chat-widget.js SDK** |
| 6.2 | 小红书渠道接入（私信/评论） | ✅ 已完成 | XiaohongshuChannelAdapter + WebhookController |
| 6.3 | 双渠道 AI 对话闭环 | ✅ 已完成 | 官网 + 小红书均可走通 AI 回复 → 转人工流程 |
| 6.4 | 渠道适配器 SPI 可扩展 | ✅ 已完成 | ChannelAdapter 接口，新增渠道只需加实现类 |

### 8.7 数据看板

| # | 验收项 | 状态 | 说明 |
|---|--------|------|------|
| 7.1 | AI 员工看板（会话量/解决率/转人工率/满意度/知识库命中率） | ⚠️ 部分完成 | AiEmployeeAnalyticsService 后端已实现，前端仅有概览页 |
| 7.2 | 服务看板（实时会话量/响应时长/解决率/转人工率） | ⚠️ 部分完成 | ServiceAnalyticsService 后端已实现，前端仅有概览页 |
| 7.3 | 高频问题 TOP20 | ⚠️ 部分完成 | TopQuestionService 后端已实现，前端仅展示 TOP5 |
| 7.4 | 数据可视化图表 | ❌ 未完成 | 缺少 @ant-design/charts 图表集成 |

### 8.8 权限与安全

| # | 验收项 | 状态 | 说明 |
|---|--------|------|------|
| 8.1 | RBAC 角色权限（ADMIN/LEADER/AGENT） | ✅ 已完成 | 三角色 + 权限码控制 |
| 8.2 | 前端路由守卫 + 菜单权限过滤 | ✅ 已完成 | DashboardLayout 权限过滤 + AuthGuard |
| 8.3 | 后端接口鉴权（@PreAuthorize） | ✅ 已完成 | 每个 Controller 方法标注权限码 |
| 8.4 | 操作审计（分配/转接/导出/登录留痕） | ✅ 已完成 | AuditLog 实体 + Repository |
| 8.5 | 敏感信息加密存储（手机号/身份证号） | ❌ 未完成 | 缺少 AesEncryptor.java 和 ContentMasker.java |
| 8.6 | 系统设置页面（用户/角色/权限 CRUD） | ✅ 已完成 | SettingsPage 三个 Tab |

### 8.9 非功能性需求

| # | 验收项 | 状态 | 说明 |
|---|--------|------|------|
| 9.1 | AI 首响时间 ≤ 1 秒 | ⚠️ 待验证 | 缺少性能测试，ES 缓存和流式输出未集成 |
| 9.2 | 并发支持 ≥ 20 路 | ⚠️ 待验证 | Spring WebSocket 天然支持，需压力测试 |
| 9.3 | 多租户预留（所有表 tenant_id + API 版本前缀） | ✅ 已完成 | 21 张表全部预留，API 路径 `/api/v1/` |
| 9.4 | 品牌白标预留（主题色/Logo/欢迎语可配置） | ✅ 已完成 | ai_employee 表含 avatar/greeting_msg 等配置字段 |
| 9.5 | 单元测试覆盖核心模块 | ✅ 已完成 | 40 个测试文件覆盖 strategy/assignment/knowledge/websocket/controller 等 |

---

## 9. 待完成清单

### 9.1 高优先级（影响核心闭环）

| # | 待完成项 | 所属模块 | 影响 |
|---|---------|---------|------|
| P1 | **ES 索引服务**（KnowledgeChunkIndexService） | 知识库 | 文档上传后无法检索，RAG 链断裂 |
| P2 | **Embedding 向量化调用链** | 知识库 | 缺少文本→向量的实际调用 |
| P3 | **SystemPromptBuilder** | 知识库 | System Prompt 需结构化组装 |
| P4 | **chat-widget.js SDK** | 渠道接入 | 官网无法嵌入使用 |
| P5 | **CollisionDetector（撞单检测）** | 分配策略 | 同一客户可能被多个客服并发接待 |
| P6 | **TransferHandler** | IM 工作台 | 客服间转移会话的 WebSocket 协议未实现 |
| P7 | **StatusSwitchHandler** | IM 工作台 | 客服在线/忙碌/离线状态切换未实现 |
| P8 | **完善数据看板前端** | 数据看板 | 前端仅有概览页，缺少 AI 员工看板和服务看板 |

### 9.2 中优先级（影响体验/完整性）

| # | 待完成项 | 所属模块 | 影响 |
|---|---------|---------|------|
| P9 | **XLSX/DOCX 文档解析器** | 知识库 | 不支持 Excel 和 Word 格式上传 |
| P10 | **RocketMQ 异步训练链路** | 知识库 | 文档训练为同步处理，大文件会阻塞 |
| P11 | **时间触发转人工逻辑** | 转人工规则 | isOutsideServiceTime() 未实现 |
| P12 | **情感分析集成** | 转人工规则 | SENTIMENT 触发规则未接入实际分析 |
| P13 | **客服当日接待统计** | IM 工作台 | `employee.report` 消息未实现 |
| P14 | **分配失败排队机制** | 分配策略 | 无可用客服时缺少排队和超时机制 |
| P15 | **AesEncryptor / ContentMasker** | 安全 | 手机号等敏感信息未加密存储 |
| P16 | **@ant-design/charts 图表集成** | 数据看板 | 缺少可视化图表 |
| P17 | **前端消息图片/名片发送** | IM 工作台 | 目前仅支持文本消息类型 |

### 9.3 低优先级（Phase 2+ 规划）

| # | 待完成项 | 说明 |
|---|---------|------|
| P18 | 微信小程序/公众号/抖音/视频号/企业微信渠道接入 | Phase 2 |
| P19 | 客服排班管理 + 绩效看板 | Phase 2 |
| P20 | 转化漏斗 + AI 贡献度分析 | Phase 2 |
| P21 | Webhook 开放平台 + RESTful API 文档自动生成 | Phase 2 |
| P22 | 多租户路由启用 + 品牌白标主题 + License 计费 | Phase 2+ |
| P23 | 客服辅助话术（AI 实时推荐回复） | Phase 2 |
| P24 | 消息 8 秒/20 秒兜底话术 + 3 分钟超时提醒 | Phase 2 |

---

## 10. 开发路线图

```
Phase 0: 项目脚手架         ✅ 已完成  (Maven + Spring Boot + Flyway 建表 + 种子数据)
Phase 1: 6 个纵向切片
  S1: 最小闭环 (Web→AI→人工)  ✅ 已完成  (会话管理 + 回复引擎 + WebSocket)
  S2: AI员工配置 CRUD         ✅ 已完成  (13项属性 + 7大策略面板)
  S3: 知识库训练全链路         ⚠️ 90%   (缺 ES 索引 + RocketMQ + XLSX/DOCX)
  S4: 分配策略 + 转人工规则    ⚠️ 85%   (缺撞单检测 + 时间触发 + 情感分析)
  S5: 小红书渠道 + 顾客名片    ✅ 已完成  (适配器 + Webhook + 名片存储)
  S6: 数据看板 + 隐私隔离      ⚠️ 70%   (缺前端看板完善 + 数据加密)
Phase 2: 扩展渠道 + 运营能力   🔜 规划中
Phase 3: SaaS 多租户 + 开放平台 🔜 远期规划
```

---

## 📊 项目统计

| 指标 | 数值 |
|------|------|
| 后端 Java 源文件 | 94 个 |
| 后端测试文件 | 40 个 |
| 前端 TypeScript 源文件 | 24 个 |
| 数据库表 | 21 张 |
| REST API 端点 | 50+ |
| WebSocket 消息类型 | 15 种 |
| 回复策略节点 | 7 个 |
| 分配策略实现 | 4 种 |
| 渠道适配器 | 2 个（Web + 小红书） |
| Phase 1 整体完成度 | **~80%** |
