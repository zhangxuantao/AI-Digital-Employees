# 基础设施补全设计 — RocketMQ + ES 索引 + 文档训练全链路

> **状态：** 已确认  
> **日期：** 2026-06-13  
> **范围：** 在 ES/Redis/MySQL/RocketMQ 全部本地就绪后，补全 Phase 1 计划中因基础设施缺失而延后的模块

---

## 1. 背景

Phase 1 核心功能已全部完成（AI员工管理、知识库 CRUD、回复策略引擎、4种分配策略、客服 IM 工作台、渠道适配器、3块数据看板、前端6页面）。以下能力因之前 ES/Redis/RocketMQ 未部署而暂缓，现在全部就绪，按自底向上顺序推进。

## 2. 总体架构

```
RocketMQ 集成层 (infrastructure/mq/)
    ↓
ES 索引服务层 (infrastructure/search/) + EmbeddingService
    ↓
文档训练全链路 (application/knowledge/DocumentTrainingService)
    ↓
碰撞检测 (application/assignment/CollisionDetector)
    ↓
WebSocket 处理器 (TransferHandler / StatusSwitchHandler)
    ↓
工具类 (AesEncryptor / ContentMasker / ExcelParser / WordParser)
```

## 3. 模块设计

### 3.1 RocketMQ 集成层

**文件清单：**

```
src/main/java/com/ai/cs/infrastructure/mq/
├── RocketMQConfig.java
├── DocumentTrainingProducer.java
├── DocumentTrainingConsumer.java
├── AssignmentNotificationProducer.java
└── AssignmentNotificationConsumer.java
```

**RocketMQConfig** — 配置类，读取 `rocketmq.*` 配置：
- name-server: `localhost:9876`
- producer group: `cs-producer-group`
- consumer group: `cs-consumer-group`
- Topic: `doc-training-topic`（文档训练）、`assignment-notify-topic`（分配通知）
- 使用 `rocketmq-spring-boot-starter` 自动配置，补充自定义 Bean

**DocumentTrainingProducer** — 文档上传后发送异步训练消息：
- 入参：`documentId`
- 消息体：`{"documentId": 123, "timestamp": "..."}`
- 发送到 `doc-training-topic`
- 调用点：`KnowledgeBaseController` 文档上传成功后

**DocumentTrainingConsumer** — 消费训练消息，串联全流程：
- 监听 `doc-training-topic`，消费模式 CLUSTERING
- 流程：PROCESSING → 解析 → 分片 → 向量化 → ES索引 → DONE/FAILED
- 重试3次，超过标记 PARTIAL_FAILED

**AssignmentNotificationProducer/Consumer** — 分配通知：
- Producer：`AssignmentEngine.assign()` 完成后发送
- Consumer：通过 WebSocket `SessionRegistry` 推送给客服端

### 3.2 ES 索引服务层

**文件清单：**

```
src/main/java/com/ai/cs/infrastructure/search/
├── ElasticsearchConfig.java          # 已存在
├── KnowledgeChunkIndexService.java   # 新增
├── ConversationLogIndexService.java  # 新增
└── RagRetriever.java                # 新增

src/main/java/com/ai/cs/infrastructure/llm/
└── EmbeddingService.java            # 新增
```

**KnowledgeChunkIndexService：**
- 索引 `knowledge_chunks`，mapping: kb_id, doc_id, chunk_id, content(text), embedding(1024维dense_vector), created_at
- `bulkIndex(List<KnowledgeChunk>)`：批量写入 ES，回写 es_doc_id 到 MySQL
- `deleteByDocId(Long)`：文档重新训练前清除旧索引
- `searchByKb(Long kbId, String query, int topK)`：BM25 文本检索

**ConversationLogIndexService：**
- 索引 `conversation_logs`，供 Analytics 聚合查询
- `indexConversation(Long convId, List<Message>)`：会话关闭后批量索引
- `searchTopQuestions(int days, int topN)`：ES aggregation 高频问题 TOP20

**RagRetriever：**
- `hybridSearch(Long kbId, String query, int topK)`：BM25 + 向量混合检索 + RRF 融合排序
- 依赖 `KnowledgeChunkIndexService` + `EmbeddingService`

**EmbeddingService：**
- `embed(String text)`：调用 LLM Embedding API（OpenAI 兼容接口，1024维）
- `embedBatch(List<String>)`：批量向量化

### 3.3 文档训练全链路

**文件清单：**

```
src/main/java/com/ai/cs/application/knowledge/
├── DocumentTrainingService.java   # 新增：训练流程编排
└── SystemPromptBuilder.java       # 新增：System Prompt 组装

src/main/java/com/ai/cs/infrastructure/storage/
├── ExcelParser.java               # 新增
└── WordParser.java                # 新增
```

**DocumentTrainingService：**
```
train(documentId):
  1. KnowledgeDocument → PROCESSING
  2. DocumentParserService.parse() → 文本
  3. TextSplitter.split() → 分片列表
  4. 每个分片建 KnowledgeChunk（MySQL）
  5. EmbeddingService.embedBatch() → 向量
  6. KnowledgeChunkIndexService.bulkIndex() → ES
  7. 回写 chunk.es_doc_id
  8. document.status = DONE
  9. 异常 → PARTIAL_FAILED
```

**SystemPromptBuilder：**
- 从 `KnowledgeStrategy` 中提取出来，独立可测试
- `build(employeeId, knowledgeChunks, history)` → 完整 System Prompt 字符串

**RagRetrievalService 改造：**
- 注入 `RagRetriever`，`retrieve()` 调用 `hybridSearch()`
- ES 不可用时 fallback MySQL LIKE（保留现有逻辑）

**ExcelParser / WordParser：**
- ExcelParser：Apache POI 读 `.xls/.xlsx`，按行拼接
- WordParser：Apache POI 读 `.doc/.docx`，提取段落
- 注册到 `DocumentParserService` 解析器映射

### 3.4 碰撞检测

**文件：** `src/main/java/com/ai/cs/application/assignment/CollisionDetector.java`

```
detect(customerId):
  1. ConversationRepository.findActiveByCustomerSince(customerId, now-30天)
  2. 有活跃会话 → 返回 ownerAgentId
  3. 无 → 返回 null
```

调用点：`AssignmentEngine.assign()` 分配前先检测，命中则直接分配给原客服。

### 3.5 WebSocket 处理器

**文件清单：**

```
src/main/java/com/ai/cs/gateway/websocket/handler/
├── TransferHandler.java      # 新增
└── StatusSwitchHandler.java  # 新增
```

都实现已有 `MessageHandler` 接口，由 `MessageDispatcher` 按 type 路由。

**TransferHandler：**
- type: `transfer`，payload: `conversationId`, `targetAgentId`
- 权限验证 → `ConversationService.transferToHuman()` → WS 通知双方 → 写 AuditLog

**StatusSwitchHandler：**
- type: `status_switch`，payload: `status`（ONLINE/BUSY/OFFLINE）
- 更新 `HumanAgent.status` → 广播同组客服 → 离线触发负载重分配

### 3.6 工具类

**文件清单：**

```
src/main/java/com/ai/cs/shared/util/
├── AesEncryptor.java     # 新增
└── ContentMasker.java    # 新增
```

**AesEncryptor：**
- `encrypt(plainText)` / `decrypt(cipherText)` — AES-256-CBC
- 用于 `CustomerProfile.phone` / `email` 加密存储

**ContentMasker：**
- `maskPhone("13812341234")` → `"138****1234"`
- `maskIdCard("310...1234")` → `"310***********1234"`
- 前端展示和日志脱敏

## 4. 与现有代码衔接

| 新模块 | 调用方（已有代码） |
|--------|-------------------|
| DocumentTrainingProducer | KnowledgeBaseController.uploadDocument() — 文档上传后加一行 send |
| DocumentTrainingConsumer | 独立运行，调用 DocumentTrainingService |
| KnowledgeChunkIndexService | DocumentTrainingService, RagRetriever |
| RagRetriever | RagRetrievalService.retrieve() — 替换内部实现 |
| EmbeddingService | DocumentTrainingService, RagRetriever |
| CollisionDetector | AssignmentEngine.assign() — 分配前置检测 |
| TransferHandler | MessageDispatcher.dispatch() — 按 type 路由 |
| StatusSwitchHandler | MessageDispatcher.dispatch() — 按 type 路由 |
| SystemPromptBuilder | KnowledgeStrategy — 替换内部 String.format |
| AesEncryptor | CustomerProfile 的 get/set phone/email |
| ContentMasker | 前端 DTO 序列化和日志 |

## 5. 验证计划

每个模块完成后执行：

1. `mvn compile` 编译通过
2. 启动 Spring Boot 应用（`mvn spring-boot:run`）
3. 对涉及的新增/修改 API 端点执行 curl 验证（GET/POST/PUT/DELETE + 中文数据 + 带 Token / 不带 Token）
4. RocketMQ Consumer 启动后确认消费正常
5. ES 索引创建并确认数据可检索
6. 前端联调确认功能可用

## 6. 不在范围内

- 多租户路由（Phase 1 已预留 tenant_id 字段，写死为 1）
- SaaS 计费 / License
- 品牌白标
- 抖音、视频号、企业微信渠道适配（留到后续 Phase）
- 排班管理
- 转化漏斗分析
- 文档中 Markdown 解析（仅支持 PDF/XLSX/DOCX/TXT）
