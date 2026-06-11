# AI 智能客服系统 Phase 1 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建 AI 智能客服系统 Phase 1，实现 AI 员工配置化运行、官网+小红书双渠道闭环、客服 IM 工作台、知识库 RAG 全链路、4 种分配策略、3 块数据看板。

**Architecture:** Spring Boot 3.x 单体应用，Maven 单模块 + 5 层包分层（gateway/application/domain/infrastructure/shared），前端 React 18 + Ant Design 5 + Vite，LangChain4j 统一适配大模型，ES 8.x 向量检索，RocketMQ 异步消息，Redis 缓存/分布式锁。

**Tech Stack:** Spring Boot 3.x + JDK 17, React 18 + Ant Design 5 + Vite, LangChain4j, MySQL 8.0, Elasticsearch 8.x, Redis, RocketMQ, Apache PDFBox/POI

---

## 文件结构总览

```
ai-customer-service/                          # Maven 项目根目录
├── pom.xml                                   # 父 POM
├── src/main/java/com/ai/cs/
│   ├── CsApplication.java                    # Spring Boot 入口
│   │
│   ├── gateway/                              # 接入层
│   │   ├── rest/
│   │   │   ├── AiEmployeeController.java
│   │   │   ├── KnowledgeBaseController.java
│   │   │   ├── ConversationController.java
│   │   │   ├── TransferRuleController.java
│   │   │   ├── AssignmentController.java
│   │   │   ├── HumanAgentController.java
│   │   │   ├── CustomerController.java
│   │   │   ├── AnalyticsController.java
│   │   │   ├── AuthController.java
│   │   │   └── SysPermissionController.java
│   │   ├── websocket/
│   │   │   ├── ChatWebSocketServer.java       # @ServerEndpoint("/ws/chat/{ticket}")
│   │   │   ├── CustomerWebSocketServer.java   # @ServerEndpoint("/ws/customer/{appId}")
│   │   │   ├── MessageDispatcher.java         # 消息 type 路由分发
│   │   │   ├── handler/
│   │   │   │   ├── MessageHandler.java        # 接口
│   │   │   │   ├── MessageSendHandler.java
│   │   │   │   ├── TransferHandler.java
│   │   │   │   ├── StatusSwitchHandler.java
│   │   │   │   └── HeartbeatHandler.java
│   │   │   └── SessionRegistry.java           # WebSocket 会话注册表
│   │   └── channel/
│   │       ├── spi/
│   │       │   ├── ChannelAdapter.java        # 渠道适配器 SPI 接口
│   │       │   ├── ChannelType.java
│   │       │   └── Message.java               # 归一化消息模型
│   │       ├── web/
│   │       │   ├── WebChannelAdapter.java
│   │       │   └── WebCustomerHandler.java
│   │       └── xiaohongshu/
│   │           ├── XiaohongshuChannelAdapter.java
│   │           └── XiaohongshuWebhookController.java
│   │
│   ├── application/                           # 应用服务层
│   │   ├── aiemployee/
│   │   │   ├── AiEmployeeService.java
│   │   │   ├── ReplyPipelineService.java      # 回复策略责任链执行器
│   │   │   └── strategy/                      # 7 个可插拔策略节点
│   │   │       ├── ReplyStrategyNode.java     # 策略接口
│   │   │       ├── StrategyResult.java
│   │   │       ├── ConversationContext.java
│   │   │       ├── ReplyBuilder.java
│   │   │       ├── GreetingStrategy.java
│   │   │       ├── ExcludeStrategy.java
│   │   │       ├── ModerationStrategy.java
│   │   │       ├── KnowledgeStrategy.java
│   │   │       ├── CollectStrategy.java
│   │   │       ├── FilterStrategy.java
│   │   │       ├── FollowUpStrategy.java
│   │   │       └── ContactStrategy.java
│   │   ├── knowledge/
│   │   │   ├── KnowledgeBaseService.java
│   │   │   ├── DocumentTrainingService.java
│   │   │   ├── RagRetrievalService.java       # RAG 混合检索 + RRF 融合
│   │   │   └── SystemPromptBuilder.java       # System Prompt 组装
│   │   ├── assignment/
│   │   │   ├── TransferRuleEngine.java
│   │   │   ├── AssignmentEngine.java
│   │   │   ├── strategy/
│   │   │   │   ├── AssignmentStrategy.java    # 分配策略接口
│   │   │   │   ├── SpecifiedStrategy.java
│   │   │   │   ├── RoundRobinStrategy.java
│   │   │   │   ├── LeastLoadStrategy.java
│   │   │   │   └── HistoryStrategy.java
│   │   │   └── CollisionDetector.java         # 撞单检测
│   │   ├── conversation/
│   │   │   ├── ConversationService.java
│   │   │   ├── MessageService.java
│   │   │   └── SessionStateMachine.java       # 会话状态机
│   │   └── analytics/
│   │       ├── AiEmployeeAnalyticsService.java
│   │       ├── ServiceAnalyticsService.java
│   │       └── TopQuestionService.java
│   │
│   ├── domain/                                # 领域模型层
│   │   ├── employee/
│   │   │   ├── AiEmployee.java                # 聚合根
│   │   │   ├── AiEmployeeAccount.java
│   │   │   ├── AiEmployeeReplyStrategy.java
│   │   │   └── repository/
│   │   │       ├── AiEmployeeRepository.java
│   │   │       ├── AiEmployeeAccountRepository.java
│   │   │       └── AiEmployeeReplyStrategyRepository.java
│   │   ├── knowledge/
│   │   │   ├── KnowledgeBase.java
│   │   │   ├── KnowledgeDocument.java
│   │   │   ├── KnowledgeChunk.java
│   │   │   └── repository/
│   │   │       ├── KnowledgeBaseRepository.java
│   │   │       ├── KnowledgeDocumentRepository.java
│   │   │       └── KnowledgeChunkRepository.java
│   │   ├── conversation/
│   │   │   ├── Conversation.java
│   │   │   ├── Message.java
│   │   │   ├── ConversationStatus.java
│   │   │   └── repository/
│   │   │       ├── ConversationRepository.java
│   │   │       └── MessageRepository.java
│   │   ├── customer/
│   │   │   ├── CustomerProfile.java
│   │   │   └── repository/
│   │   │       └── CustomerProfileRepository.java
│   │   ├── assignment/
│   │   │   ├── TransferRule.java
│   │   │   ├── AssignmentStrategyConfig.java
│   │   │   ├── HumanAgent.java
│   │   │   ├── AgentChannelPermission.java
│   │   │   └── repository/
│   │   │       ├── TransferRuleRepository.java
│   │   │       ├── AssignmentStrategyConfigRepository.java
│   │   │       ├── HumanAgentRepository.java
│   │   │       └── AgentChannelPermissionRepository.java
│   │   └── permission/
│   │       ├── SysUser.java
│   │       ├── SysRole.java
│   │       ├── SysPermission.java
│   │       ├── SysUserRole.java
│   │       ├── SysRolePermission.java
│   │       ├── AuditLog.java
│   │       └── repository/
│   │           ├── SysUserRepository.java
│   │           ├── SysRoleRepository.java
│   │           ├── SysPermissionRepository.java
│   │           ├── SysUserRoleRepository.java
│   │           ├── SysRolePermissionRepository.java
│   │           └── AuditLogRepository.java
│   │
│   ├── infrastructure/                        # 基础设施层
│   │   ├── llm/
│   │   │   ├── LangChain4jConfig.java
│   │   │   └── LlmService.java
│   │   ├── search/
│   │   │   ├── ElasticsearchConfig.java
│   │   │   ├── KnowledgeChunkIndexService.java
│   │   │   ├── ConversationLogIndexService.java
│   │   │   └── RagRetriever.java
│   │   ├── mq/
│   │   │   ├── RocketMQConfig.java
│   │   │   ├── DocumentTrainingProducer.java
│   │   │   ├── DocumentTrainingConsumer.java
│   │   │   ├── AssignmentNotificationProducer.java
│   │   │   └── AssignmentNotificationConsumer.java
│   │   ├── cache/
│   │   │   ├── RedisConfig.java
│   │   │   └── CacheService.java
│   │   ├── storage/
│   │   │   └── LocalFileStorageService.java
│   │   └── persistence/
│   │       └── BaseEntity.java                # 公共字段 (id, created_at, updated_at, tenant_id)
│   │
│   └── shared/                                # 共享工具
│       ├── config/
│       │   ├── AppConfig.java
│       │   ├── SecurityConfig.java
│       │   ├── WebSocketConfig.java
│       │   └── CorsConfig.java
│       ├── exception/
│       │   ├── GlobalExceptionHandler.java
│       │   ├── BusinessException.java
│       │   └── ErrorCode.java
│       ├── security/
│       │   ├── JwtTokenProvider.java
│       │   ├── JwtAuthenticationFilter.java
│       │   ├── TicketService.java
│       │   ├── DataScope.java                # @DataScope 注解
│       │   └── DataScopeAspect.java          # 数据隔离 AOP
│       ├── dto/
│       │   ├── ApiResponse.java              # 统一响应 {code, message, data}
│       │   ├── PageResult.java
│       │   └── ...（各模块 DTO 与对应 Controller 同目录或按需创建）
│       └── util/
│           ├── AesEncryptor.java
│           └── ContentMasker.java
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── db/
│       └── migration/
│           └── V1__init_schema.sql           # 全部 DDL
│
├── src/test/java/com/ai/cs/
│   ├── application/aiemployee/
│   │   └── ReplyPipelineServiceTest.java
│   ├── application/knowledge/
│   │   └── RagRetrievalServiceTest.java
│   ├── application/assignment/
│   │   ├── TransferRuleEngineTest.java
│   │   └── AssignmentEngineTest.java
│   ├── application/conversation/
│   │   └── ConversationServiceTest.java
│   └── gateway/websocket/
│       └── MessageDispatcherTest.java
│
└── frontend/                                  # React 前端
    ├── package.json
    ├── vite.config.ts
    ├── src/
    │   ├── layouts/
    │   ├── pages/
    │   │   ├── AiEmployee/
    │   │   ├── KnowledgeBase/
    │   │   ├── IM/
    │   │   ├── AgentManagement/
    │   │   ├── Analytics/
    │   │   └── Login/
    │   ├── components/
    │   ├── services/
    │   ├── stores/
    │   ├── access.ts
    │   └── app.tsx
    └── public/
        └── sdk/
            └── chat-widget.js                 # 官网客户端 SDK
```

---

## Phase 0: 项目脚手架与基础设施

### Task 0.1: Maven 项目初始化

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/ai/cs/CsApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-dev.yml`

**说明**: 创建 Maven 单模块项目，引入所有依赖（Spring Boot 3.x, Spring Data JPA, Spring WebSocket, Spring Security, LangChain4j, Elasticsearch, RocketMQ, Redis, MySQL, Apache PDFBox, Apache POI, Lombok, MapStruct）。

- [ ] **Step 1: 编写 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>
    <groupId>com.ai</groupId>
    <artifactId>ai-customer-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>AI Customer Service</name>
    <description>AI 智能客服系统 Phase 1</description>

    <properties>
        <java.version>17</java.version>
        <langchain4j.version>0.32.0</langchain4j.version>
        <elasticsearch.version>8.13.0</elasticsearch.version>
        <rocketmq.version>2.3.0</rocketmq.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
        </dependency>

        <!-- LangChain4j -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-spring-boot-starter</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-ollama</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>

        <!-- Elasticsearch -->
        <dependency>
            <groupId>co.elastic.clients</groupId>
            <artifactId>elasticsearch-java</artifactId>
            <version>${elasticsearch.version}</version>
        </dependency>

        <!-- RocketMQ -->
        <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-spring-boot-starter</artifactId>
            <version>${rocketmq.version}</version>
        </dependency>

        <!-- Document Parsing -->
        <dependency>
            <groupId>org.apache.pdfbox</groupId>
            <artifactId>pdfbox</artifactId>
            <version>3.0.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.5</version>
        </dependency>

        <!-- Security -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>1.5.5.Final</version>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <version>1.5.5.Final</version>
            <scope>provided</scope>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <version>1.19.7</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <version>1.19.7</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>elasticsearch</artifactId>
            <version>1.19.7</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 验证 pom.xml 依赖解析**

```bash
mvn dependency:resolve -s D:/mvn/settings.xml -Dmaven.repo.local=D:/mvn/repository
```

Expected: BUILD SUCCESS，所有依赖下载成功。

- [ ] **Step 3: 编写 Spring Boot 入口类**

```java
package com.ai.cs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CsApplication.class, args);
    }
}
```

- [ ] **Step 4: 编写 application.yml 主配置**

```yaml
spring:
  application:
    name: ai-customer-service
  profiles:
    active: dev
  datasource:
    url: jdbc:mysql://localhost:3306/ai_cs?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 100MB

server:
  port: 8080

langchain4j:
  providers:
    - id: qwen
      type: openai-compatible
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: ${QWEN_API_KEY:}
      model: qwen-plus
    - id: deepseek
      type: openai-compatible
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY:}
      model: deepseek-chat
    - id: ollama
      type: ollama
      base-url: http://localhost:11434
      model: qwen2.5:7b

elasticsearch:
  host: localhost
  port: 9200
  username: ${ES_USERNAME:}
  password: ${ES_PASSWORD:}

rocketmq:
  name-server: localhost:9876
  producer:
    group: cs-producer-group
  consumer:
    group: cs-consumer-group

app:
  storage:
    base-path: ./data
  security:
    jwt-secret: ${JWT_SECRET:change-me-in-production}
    jwt-expiration: 86400000
  default-tenant-id: 1
```

- [ ] **Step 5: 编写 application-dev.yml**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_cs_dev?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
  jpa:
    show-sql: true

logging:
  level:
    com.ai.cs: DEBUG

app:
  storage:
    base-path: ./data-dev
```

- [ ] **Step 6: 编译验证**

```bash
mvn compile -s D:/mvn/settings.xml -Dmaven.repo.local=D:/mvn/repository
```

Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add pom.xml src/main/java/com/ai/cs/CsApplication.java src/main/resources/
git commit -m "chore(init): 初始化 Maven 项目和 Spring Boot 入口"
```

---

### Task 0.2: 数据库 Schema（全部 21 张表）

**Files:**
- Create: `src/main/resources/db/migration/V1__init_schema.sql`
- Create: `src/main/java/com/ai/cs/infrastructure/persistence/BaseEntity.java`
- Create: `src/main/java/com/ai/cs/shared/config/AppConfig.java`

**说明**: 一次性创建全部 21 张 MySQL 表，使用 Flyway 管理 schema 版本。因为所有表都已经在设计文档中明确定义，且表之间有外键关联，一次性创建更可靠。

- [ ] **Step 1: 编写 BaseEntity 基类**

```java
package com.ai.cs.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId = 1L;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: 编写 AppConfig 配置类**

```java
package com.ai.cs.shared.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private Storage storage = new Storage();
    private Security security = new Security();
    private Long defaultTenantId = 1L;

    @Data
    public static class Storage {
        private String basePath = "./data";
    }

    @Data
    public static class Security {
        private String jwtSecret = "change-me-in-production";
        private long jwtExpiration = 86400000;
    }
}
```

- [ ] **Step 3: 编写完整 DDL (V1__init_schema.sql)**

```sql
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
```

- [ ] **Step 4: 初始化种子数据 (V2__seed_data.sql)**

```sql
-- 默认角色
INSERT INTO sys_role (code, name, description, tenant_id) VALUES
('ADMIN', '超级管理员', '全部菜单 + 全部数据', 1),
('LEADER', '客服主管', 'AI员工管理 / 知识库 / 客服管理 / IM工作台 / 数据看板（团队范围）', 1),
('AGENT', '客服', 'IM工作台（仅自己会话）', 1);

-- 默认权限
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
```

- [ ] **Step 5: 提交**

```bash
git add src/main/resources/db/ src/main/java/com/ai/cs/infrastructure/persistence/BaseEntity.java src/main/java/com/ai/cs/shared/config/AppConfig.java
git commit -m "feat(db): 创建全部21张表DDL和种子数据"
```

---

## Phase 1: S1 — 最小闭环（官网客户 → AI 回复 → 转人工 → 客服 IM → 关闭）

### Task 1.1: 共享层基础设施（统一响应、异常处理、安全）

**Files:**
- Create: `src/main/java/com/ai/cs/shared/dto/ApiResponse.java`
- Create: `src/main/java/com/ai/cs/shared/dto/PageResult.java`
- Create: `src/main/java/com/ai/cs/shared/exception/ErrorCode.java`
- Create: `src/main/java/com/ai/cs/shared/exception/BusinessException.java`
- Create: `src/main/java/com/ai/cs/shared/exception/GlobalExceptionHandler.java`
- Create: `src/main/java/com/ai/cs/shared/security/JwtTokenProvider.java`
- Create: `src/main/java/com/ai/cs/shared/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/ai/cs/shared/security/TicketService.java`
- Create: `src/main/java/com/ai/cs/shared/config/SecurityConfig.java`
- Create: `src/main/java/com/ai/cs/shared/config/CorsConfig.java`
- Create: `src/main/java/com/ai/cs/shared/config/WebSocketConfig.java`

- [ ] **Step 1: 编写 ApiResponse**

```java
package com.ai.cs.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, "success", null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

- [ ] **Step 2: 编写 ErrorCode、BusinessException、GlobalExceptionHandler**

```java
package com.ai.cs.shared.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务错误码
    AI_EMPLOYEE_NOT_FOUND(1001, "AI员工不存在"),
    KNOWLEDGE_BASE_NOT_FOUND(1002, "知识库不存在"),
    CONVERSATION_NOT_FOUND(1003, "会话不存在"),
    AGENT_NOT_FOUND(1004, "客服不存在"),
    AGENT_OFFLINE(1005, "客服不在线"),
    AGENT_OVERLOAD(1006, "客服负载已满"),
    STRATEGY_NOT_FOUND(1007, "策略配置不存在"),
    DOCUMENT_PARSE_FAILED(1008, "文档解析失败"),
    NO_AVAILABLE_AGENT(1009, "无可用客服");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

```java
package com.ai.cs.shared.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
```

```java
package com.ai.cs.shared.exception;

import com.ai.cs.shared.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("未预期异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误"));
    }
}
```

- [ ] **Step 3: 编写 JWT 和 Ticket 安全组件**

```java
package com.ai.cs.shared.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expiration;

    public JwtTokenProvider(@Value("${app.security.jwt-secret}") String secret,
                            @Value("${app.security.jwt-expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String createToken(Long userId, String username, List<String> permissions) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
```

```java
package com.ai.cs.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class TicketService {
    // 简化实现：生产环境应存储到 Redis
    private final Map<String, TicketInfo> ticketStore = new ConcurrentHashMap<>();

    public String createTicket(Long agentId, String username) {
        String ticket = UUID.randomUUID().toString().replace("-", "");
        ticketStore.put(ticket, new TicketInfo(agentId, username, System.currentTimeMillis()));
        return ticket;
    }

    public TicketInfo validate(String ticket) {
        TicketInfo info = ticketStore.get(ticket);
        if (info == null) return null;
        // 2小时过期
        if (System.currentTimeMillis() - info.createdAt > 7200000) {
            ticketStore.remove(ticket);
            return null;
        }
        return info;
    }

    public void revoke(String ticket) {
        ticketStore.remove(ticket);
    }

    public record TicketInfo(Long agentId, String username, long createdAt) {}
}
```

- [ ] **Step 4: 编写 JwtAuthenticationFilter**

```java
package com.ai.cs.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @SuppressWarnings("unchecked")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            var claims = jwtTokenProvider.parseToken(token);
            List<String> permissions = claims.get("permissions", List.class);
            List<SimpleGrantedAuthority> authorities = permissions != null
                    ? permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                    : List.of();

            var auth = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 5: 编写 SecurityConfig、CorsConfig、WebSocketConfig**

```java
package com.ai.cs.shared.config;

import com.ai.cs.shared.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/ws/**", "/sdk/**", "/webhook/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

```java
package com.ai.cs.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

```java
package com.ai.cs.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebSocketConfig {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
```

- [ ] **Step 6: 编译并提交**

```bash
mvn compile -s D:/mvn/settings.xml -Dmaven.repo.local=D:/mvn/repository
git add src/main/java/com/ai/cs/shared/
git commit -m "feat(shared): 添加统一响应、异常处理、JWT安全基础设施"
```

---

### Task 1.2: Domain 层 — 对话相关实体和 Repository

**Files:**
- Create: `src/main/java/com/ai/cs/domain/conversation/ConversationStatus.java`
- Create: `src/main/java/com/ai/cs/domain/conversation/Conversation.java`
- Create: `src/main/java/com/ai/cs/domain/conversation/Message.java`
- Create: `src/main/java/com/ai/cs/domain/conversation/repository/ConversationRepository.java`
- Create: `src/main/java/com/ai/cs/domain/conversation/repository/MessageRepository.java`
- Create: `src/main/java/com/ai/cs/domain/customer/CustomerProfile.java`
- Create: `src/main/java/com/ai/cs/domain/customer/repository/CustomerProfileRepository.java`
- Create: `src/main/java/com/ai/cs/domain/employee/AiEmployee.java`
- Create: `src/main/java/com/ai/cs/domain/employee/AiEmployeeAccount.java`
- Create: `src/main/java/com/ai/cs/domain/employee/AiEmployeeReplyStrategy.java`
- Create: `src/main/java/com/ai/cs/domain/employee/repository/AiEmployeeRepository.java`
- Create: `src/main/java/com/ai/cs/domain/employee/repository/AiEmployeeAccountRepository.java`
- Create: `src/main/java/com/ai/cs/domain/employee/repository/AiEmployeeReplyStrategyRepository.java`
- Create: `src/main/java/com/ai/cs/domain/assignment/HumanAgent.java`
- Create: `src/main/java/com/ai/cs/domain/assignment/TransferRule.java`
- Create: `src/main/java/com/ai/cs/domain/assignment/AssignmentStrategyConfig.java`
- Create: `src/main/java/com/ai/cs/domain/assignment/repository/HumanAgentRepository.java`
- Create: `src/main/java/com/ai/cs/domain/assignment/repository/TransferRuleRepository.java`
- Create: `src/main/java/com/ai/cs/domain/assignment/repository/AssignmentStrategyConfigRepository.java`
- Create: `src/main/java/com/ai/cs/domain/permission/SysUser.java`
- Create: `src/main/java/com/ai/cs/domain/permission/SysRole.java`
- Create: `src/main/java/com/ai/cs/domain/permission/SysPermission.java`
- Create: `src/main/java/com/ai/cs/domain/permission/SysUserRole.java`
- Create: `src/main/java/com/ai/cs/domain/permission/SysRolePermission.java`
- Create: `src/main/java/com/ai/cs/domain/permission/AuditLog.java`
- Create: `src/main/java/com/ai/cs/domain/permission/repository/SysUserRepository.java`
- Create: `src/main/java/com/ai/cs/domain/permission/repository/SysRoleRepository.java`
- Create: `src/main/java/com/ai/cs/domain/permission/repository/SysPermissionRepository.java`

- [ ] **Step 1: 编写 ConversationStatus 枚举和实体类**

```java
package com.ai.cs.domain.conversation;

public enum ConversationStatus {
    AI_ACTIVE,   // AI服务中
    WAITING,     // 等待人工
    HUMAN,       // 人工服务中
    QUEUED,      // 排队中
    CLOSED       // 已关闭
}
```

```java
package com.ai.cs.domain.conversation;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "conversation")
public class Conversation extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "human_agent_id")
    private Long humanAgentId;

    @Column(name = "owner_agent_id")
    private Long ownerAgentId;

    @Column(nullable = false, length = 20)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationStatus status = ConversationStatus.AI_ACTIVE;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime = LocalDateTime.now();

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "close_reason", length = 50)
    private String closeReason;
}
```

```java
package com.ai.cs.domain.conversation;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "message")
public class Message extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_type", nullable = false, length = 10)
    private String senderType;  // CUSTOMER/AI/HUMAN/SYSTEM

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "msg_type", nullable = false, length = 10)
    private String msgType = "text";

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "send_time", nullable = false)
    private LocalDateTime sendTime = LocalDateTime.now();
}
```

- [ ] **Step 2: 编写 Repository 接口**

```java
package com.ai.cs.domain.conversation.repository;

import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByEmployeeIdAndStatusNot(Long employeeId, ConversationStatus status);

    @Query("SELECT c FROM Conversation c WHERE c.customerId = :customerId " +
           "AND c.startTime > :since AND c.status != 'CLOSED' ORDER BY c.startTime DESC")
    List<Conversation> findActiveByCustomerSince(@Param("customerId") Long customerId,
                                                  @Param("since") java.time.LocalDateTime since);

    // 数据隔离查询
    @Query("SELECT c FROM Conversation c WHERE c.ownerAgentId = :agentId")
    List<Conversation> findByOwnerAgentId(@Param("agentId") Long agentId);

    List<Conversation> findByOwnerAgentIdAndStatus(Long agentId, ConversationStatus status);

    Optional<Conversation> findByIdAndOwnerAgentId(Long id, Long agentId);
}
```

```java
package com.ai.cs.domain.conversation.repository;

import com.ai.cs.domain.conversation.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderBySendTimeAsc(Long conversationId);
    List<Message> findTop10ByConversationIdOrderBySendTimeDesc(Long conversationId);
}
```

- [ ] **Step 3: 编写 AiEmployee 和 CustomerProfile 实体**

```java
package com.ai.cs.domain.employee;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter @Setter
@Entity
@Table(name = "ai_employee")
public class AiEmployee extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String avatar;

    @Column(name = "greeting_msg", columnDefinition = "TEXT")
    private String greetingMsg;

    @Column(length = 20)
    private String style = "PROFESSIONAL";

    @Column(name = "reply_length", length = 10)
    private String replyLength = "MEDIUM";

    @Column(name = "content_check", columnDefinition = "JSON")
    private String contentCheck;

    @Column(name = "aggregate_interval")
    private Integer aggregateInterval = 3;

    @Column(name = "delay_interval")
    private Integer delayInterval = 2;

    @Column(name = "service_time_start")
    private LocalTime serviceTimeStart;

    @Column(name = "service_time_end")
    private LocalTime serviceTimeEnd;

    @Column(length = 50)
    private String weekdays;

    @Column(name = "company_intro", columnDefinition = "TEXT", nullable = false)
    private String companyIntro;

    @Column(name = "product_intro", columnDefinition = "TEXT", nullable = false)
    private String productIntro;

    @Column(name = "service_scope", columnDefinition = "TEXT")
    private String serviceScope;

    @Column(length = 20)
    private String status = "ENABLED";
}
```

```java
package com.ai.cs.domain.customer;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "customer_profile")
public class CustomerProfile extends BaseEntity {

    @Column(length = 100)
    private String nickname;

    @Column(nullable = false, length = 20)
    private String platform;

    @Column(nullable = false, length = 200)
    private String openid;

    @Column(length = 500)
    private String avatar;

    @Column(length = 100)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(length = 10)
    private String gender;

    @Column(length = 50)
    private String city;

    @Column(columnDefinition = "JSON")
    private String tags;

    @Column(name = "extra_fields", columnDefinition = "JSON")
    private String extraFields;
}
```

- [ ] **Step 4: 编写 HumanAgent 实体和 TransferRule 实体**

```java
package com.ai.cs.domain.assignment;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "human_agent")
public class HumanAgent extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Column(length = 20)
    private String status = "OFFLINE";  // ONLINE/BUSY/OFFLINE

    @Column(name = "current_load")
    private Integer currentLoad = 0;

    @Column(name = "max_concurrent")
    private Integer maxConcurrent = 5;
}
```

```java
package com.ai.cs.domain.assignment;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "transfer_rule")
public class TransferRule extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType;  // KEYWORD/SENTIMENT/MANUAL/TIME

    @Column(name = "trigger_config", columnDefinition = "JSON", nullable = false)
    private String triggerConfig;

    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;

    @Column(name = "action_config", columnDefinition = "JSON")
    private String actionConfig;

    private Integer priority = 0;

    private Boolean enabled = true;
}
```

- [ ] **Step 5: 编写所有 Repository 接口**

```java
package com.ai.cs.domain.employee.repository;

import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.employee.AiEmployeeAccount;
import com.ai.cs.domain.employee.AiEmployeeReplyStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiEmployeeRepository extends JpaRepository<AiEmployee, Long> {
    List<AiEmployee> findByStatus(String status);
}

@Repository
public interface AiEmployeeAccountRepository extends JpaRepository<AiEmployeeAccount, Long> {
    Optional<AiEmployeeAccount> findByPlatformAndAccountId(String platform, String accountId);
    List<AiEmployeeAccount> findByEmployeeId(Long employeeId);
}

@Repository
public interface AiEmployeeReplyStrategyRepository extends JpaRepository<AiEmployeeReplyStrategy, Long> {
    List<AiEmployeeReplyStrategy> findByEmployeeIdAndEnabledOrderBySortOrderAsc(Long employeeId, Boolean enabled);
}
```

```java
package com.ai.cs.domain.assignment.repository;

import com.ai.cs.domain.assignment.HumanAgent;
import com.ai.cs.domain.assignment.TransferRule;
import com.ai.cs.domain.assignment.AssignmentStrategyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HumanAgentRepository extends JpaRepository<HumanAgent, Long> {
    List<HumanAgent> findByStatusInAndCurrentLoadLessThan(List<String> statuses, Integer maxLoad);
    Optional<HumanAgent> findByPhone(String phone);
}

@Repository
public interface TransferRuleRepository extends JpaRepository<TransferRule, Long> {
    List<TransferRule> findByEmployeeIdAndEnabledOrderByPriorityAsc(Long employeeId, Boolean enabled);
}

@Repository
public interface AssignmentStrategyConfigRepository extends JpaRepository<AssignmentStrategyConfig, Long> {
    Optional<AssignmentStrategyConfig> findByEmployeeIdAndIsActive(Long employeeId, Boolean isActive);
}
```

- [ ] **Step 6: 编写权限管理实体**（sys_user, sys_role, sys_permission, sys_user_role, sys_role_permission, audit_log）

```java
package com.ai.cs.domain.permission;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;
    @Column(name = "agent_id")
    private Long agentId;
    @Column(name = "role_code", nullable = false, length = 20)
    private String roleCode;
    @Column(length = 20)
    private String status = "ENABLED";
}
```

```java
package com.ai.cs.domain.permission.repository;

import com.ai.cs.domain.permission.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);
}
```

其余权限实体（SysRole, SysPermission, SysUserRole, SysRolePermission, AuditLog）和 Repository 按照相同的 JPA 实体模式创建，字段对应 DDL 定义。

- [ ] **Step 7: 补充 CustomerProfileRepository 查询方法**

```java
package com.ai.cs.domain.customer.repository;

import com.ai.cs.domain.customer.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
    Optional<CustomerProfile> findByPlatformAndOpenid(String platform, String openid);
}
```

- [ ] **Step 8: 编译验证并提交**

```bash
mvn compile -s D:/mvn/settings.xml -Dmaven.repo.local=D:/mvn/repository
git add src/main/java/com/ai/cs/domain/
git commit -m "feat(domain): 添加对话/AI员工/客服/客户/权限领域实体和Repository"
```

---

### Task 1.3: 渠道适配器 SPI + Web 渠道实现

**Files:**
- Create: `src/main/java/com/ai/cs/gateway/channel/spi/ChannelType.java`
- Create: `src/main/java/com/ai/cs/gateway/channel/spi/Message.java`（归一化消息）
- Create: `src/main/java/com/ai/cs/gateway/channel/spi/ChannelAdapter.java`
- Create: `src/main/java/com/ai/cs/gateway/channel/web/WebChannelAdapter.java`
- Create: `src/main/java/com/ai/cs/gateway/channel/web/WebCustomerHandler.java`
- Create: `src/main/java/com/ai/cs/gateway/websocket/CustomerWebSocketServer.java`

- [ ] **Step 1: 编写 SPI 接口和归一化消息模型**

```java
package com.ai.cs.gateway.channel.spi;

public enum ChannelType {
    WEB, XIAOHONGSHU, DOUYIN, WECHAT_MP
}
```

```java
package com.ai.cs.gateway.channel.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String msgId;
    private ChannelType channel;
    private String senderOpenid;
    private String senderNickname;
    private String content;
    private String msgType;  // text/image/card
    private LocalDateTime timestamp;
    private Map<String, Object> raw;  // 原始消息
    private String appId;  // 官网的 appId / 小红书的企业号 ID
}
```

```java
package com.ai.cs.gateway.channel.spi;

import com.ai.cs.domain.customer.CustomerProfile;

public interface ChannelAdapter {

    ChannelType getType();

    /** 将渠道原始消息归一化为内部Message */
    Message normalize(Object rawMessage);

    /** 向渠道客户发送消息 */
    void send(Long accountId, Message msg);

    /** 同步客户信息 */
    CustomerProfile syncCustomer(String openid);

    /** 客户进入回调 */
    void onCustomerEnter(String openid);

    /** 客户离开回调 */
    void onCustomerLeave(String openid);
}
```

- [ ] **Step 2: 编写 Web 渠道适配器**

```java
package com.ai.cs.gateway.channel.web;

import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.customer.repository.CustomerProfileRepository;
import com.ai.cs.gateway.channel.spi.ChannelAdapter;
import com.ai.cs.gateway.channel.spi.ChannelType;
import com.ai.cs.gateway.channel.spi.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebChannelAdapter implements ChannelAdapter {

    private final CustomerProfileRepository customerProfileRepository;
    // openid -> WebSocket session (客户连接)
    private final Map<String, jakarta.websocket.Session> customerSessions = new ConcurrentHashMap<>();

    @Override
    public ChannelType getType() {
        return ChannelType.WEB;
    }

    @Override
    public Message normalize(Object rawMessage) {
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = (Map<String, Object>) rawMessage;
        return Message.builder()
                .msgId((String) raw.get("msgId"))
                .channel(ChannelType.WEB)
                .senderOpenid((String) raw.get("openid"))
                .senderNickname((String) raw.getOrDefault("nickname", "匿名用户"))
                .content((String) raw.get("content"))
                .msgType((String) raw.getOrDefault("msgType", "text"))
                .appId((String) raw.get("appId"))
                .raw(raw)
                .build();
    }

    @Override
    public void send(Long accountId, Message msg) {
        jakarta.websocket.Session session = customerSessions.get(msg.getSenderOpenid());
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(toJson(msg));
            } catch (IOException e) {
                log.error("发送消息到客户失败: openid={}", msg.getSenderOpenid(), e);
                customerSessions.remove(msg.getSenderOpenid());
            }
        }
    }

    @Override
    public CustomerProfile syncCustomer(String openid) {
        return customerProfileRepository.findByPlatformAndOpenid("WEB", openid)
                .orElseGet(() -> {
                    CustomerProfile cp = new CustomerProfile();
                    cp.setPlatform("WEB");
                    cp.setOpenid(openid);
                    cp.setNickname("官网访客");
                    return customerProfileRepository.save(cp);
                });
    }

    @Override
    public void onCustomerEnter(String openid) {
        log.info("Web 客户进入: openid={}", openid);
    }

    @Override
    public void onCustomerLeave(String openid) {
        customerSessions.remove(openid);
        log.info("Web 客户离开: openid={}", openid);
    }

    public void registerSession(String openid, jakarta.websocket.Session session) {
        customerSessions.put(openid, session);
    }

    private String toJson(Message msg) {
        // 简化：使用 Jackson，实际注入 ObjectMapper
        return "{\"type\":\"message\",\"content\":\"" + msg.getContent() + "\"}";
    }
}
```

- [ ] **Step 3: 编写客户 WebSocket 服务端**

```java
package com.ai.cs.gateway.websocket;

import com.ai.cs.application.aiemployee.ReplyPipelineService;
import com.ai.cs.application.conversation.ConversationService;
import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.employee.AiEmployeeAccount;
import com.ai.cs.domain.employee.repository.AiEmployeeAccountRepository;
import com.ai.cs.domain.employee.repository.AiEmployeeRepository;
import com.ai.cs.gateway.channel.spi.Message;
import com.ai.cs.gateway.channel.web.WebChannelAdapter;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/ws/customer/{appId}")
public class CustomerWebSocketServer {

    // Spring 注入通过静态 ApplicationContextHolder 实现，此处简化展示核心逻辑
    private static WebChannelAdapter webChannelAdapter;
    private static ConversationService conversationService;
    private static ReplyPipelineService replyPipelineService;
    private static AiEmployeeAccountRepository accountRepo;
    private static AiEmployeeRepository employeeRepo;

    // 暂时用静态方法设置依赖（后续通过 Spring 配置注入）
    public static void setDependencies(WebChannelAdapter adapter, ConversationService convSvc,
                                        ReplyPipelineService pipeline, AiEmployeeAccountRepository accRepo,
                                        AiEmployeeRepository empRepo) {
        webChannelAdapter = adapter;
        conversationService = convSvc;
        replyPipelineService = pipeline;
        accountRepo = accRepo;
        employeeRepo = empRepo;
    }

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private String openid;
    private String appId;

    @OnOpen
    public void onOpen(Session session, @PathParam("appId") String appId) {
        this.appId = appId;
        this.openid = session.getId();  // 简化：实际应通过 ticket 认证
        sessions.put(openid, session);
        webChannelAdapter.registerSession(openid, session);
        log.info("客户 WebSocket 连接: openid={}, appId={}", openid, appId);
    }

    @OnMessage
    public void onMessage(String text, Session session) {
        log.info("收到客户消息: openid={}, content={}", openid, text);
        // 1. 归一化消息
        Map<String, Object> raw = Map.of("msgId", java.util.UUID.randomUUID().toString(),
                "openid", openid, "content", text, "appId", appId, "msgType", "text");
        Message msg = webChannelAdapter.normalize(raw);

        // 2. 查找 AI 员工
        AiEmployeeAccount account = accountRepo.findByPlatformAndAccountId("WEB", appId)
                .orElse(null);
        if (account == null) {
            sendToClient(session, "系统未配置此渠道的AI员工");
            return;
        }
        AiEmployee employee = employeeRepo.findById(account.getEmployeeId()).orElse(null);
        if (employee == null || !"ENABLED".equals(employee.getStatus())) {
            sendToClient(session, "AI员工不在线");
            return;
        }

        // 3. 同步客户信息、创建/查找会话
        CustomerProfile customer = webChannelAdapter.syncCustomer(openid);
        var conv = conversationService.findOrCreateConversation(customer.getId(), employee.getId(), "WEB");

        // 4. 进入回复流水线
        replyPipelineService.process(conv.getId(), employee.getId(), msg.getContent());
    }

    @OnClose
    public void onClose() {
        sessions.remove(openid);
        webChannelAdapter.onCustomerLeave(openid);
    }

    @OnError
    public void onError(Throwable t) {
        log.error("客户 WebSocket 错误: openid={}", openid, t);
    }

    private void sendToClient(Session session, String content) {
        try {
            session.getBasicRemote().sendText("{\"type\":\"message\",\"content\":\"" + content + "\"}");
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/ai/cs/gateway/channel/ src/main/java/com/ai/cs/gateway/websocket/CustomerWebSocketServer.java
git commit -m "feat(channel): 添加渠道适配器SPI和Web渠道实现"
```

---

### Task 1.4: 应用层 — 会话管理 + 回复策略引擎

**Files:**
- Create: `src/main/java/com/ai/cs/application/conversation/ConversationService.java`
- Create: `src/main/java/com/ai/cs/application/conversation/SessionStateMachine.java`
- Create: `src/main/java/com/ai/cs/application/conversation/MessageService.java`
- Create: `src/main/java/com/ai/cs/application/aiemployee/ReplyPipelineService.java`
- Create: `src/main/java/com/ai/cs/application/aiemployee/strategy/StrategyResult.java`
- Create: `src/main/java/com/ai/cs/application/aiemployee/strategy/ConversationContext.java`
- Create: `src/main/java/com/ai/cs/application/aiemployee/strategy/ReplyBuilder.java`
- Create: `src/main/java/com/ai/cs/application/aiemployee/strategy/ReplyStrategyNode.java`
- Create: `src/main/java/com/ai/cs/application/aiemployee/strategy/GreetingStrategy.java`
- Create: `src/main/java/com/ai/cs/application/aiemployee/strategy/ExcludeStrategy.java`
- Create: `src/main/java/com/ai/cs/application/aiemployee/strategy/ModerationStrategy.java`
- Create: `src/main/java/com/ai/cs/application/aiemployee/strategy/KnowledgeStrategy.java`
- Create: `src/main/java/com/ai/cs/application/aiemployee/strategy/ContactStrategy.java`

- [ ] **Step 1: 编写 ConversationService 和 SessionStateMachine**

```java
package com.ai.cs.application.conversation;

import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.ConversationStatus;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final SessionStateMachine stateMachine;

    @Transactional
    public Conversation findOrCreateConversation(Long customerId, Long employeeId, String channel) {
        // 查找最近的活跃会话
        var recent = conversationRepository
                .findActiveByCustomerSince(customerId, LocalDateTime.now().minusDays(30));
        if (!recent.isEmpty()) {
            return recent.get(0);
        }
        Conversation conv = new Conversation();
        conv.setCustomerId(customerId);
        conv.setEmployeeId(employeeId);
        conv.setChannel(channel);
        conv.setStatus(ConversationStatus.AI_ACTIVE);
        conv.setStartTime(LocalDateTime.now());
        return conversationRepository.save(conv);
    }

    @Transactional
    public Conversation transferToHuman(Long conversationId, Long agentId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        stateMachine.transition(conv, ConversationStatus.HUMAN);
        conv.setHumanAgentId(agentId);
        conv.setOwnerAgentId(agentId);
        return conversationRepository.save(conv);
    }

    @Transactional
    public Conversation closeConversation(Long conversationId, String reason) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        stateMachine.transition(conv, ConversationStatus.CLOSED);
        conv.setEndTime(LocalDateTime.now());
        conv.setCloseReason(reason);
        return conversationRepository.save(conv);
    }

    @Transactional
    public Conversation queueConversation(Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        stateMachine.transition(conv, ConversationStatus.QUEUED);
        return conversationRepository.save(conv);
    }

    @Transactional
    public Conversation markWaiting(Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        stateMachine.transition(conv, ConversationStatus.WAITING);
        return conversationRepository.save(conv);
    }
}
```

```java
package com.ai.cs.application.conversation;

import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.ConversationStatus;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class SessionStateMachine {

    private static final Map<ConversationStatus, Set<ConversationStatus>> ALLOWED_TRANSITIONS = Map.of(
        ConversationStatus.AI_ACTIVE, EnumSet.of(ConversationStatus.WAITING, ConversationStatus.CLOSED),
        ConversationStatus.WAITING, EnumSet.of(ConversationStatus.HUMAN, ConversationStatus.QUEUED, ConversationStatus.CLOSED),
        ConversationStatus.HUMAN, EnumSet.of(ConversationStatus.CLOSED),
        ConversationStatus.QUEUED, EnumSet.of(ConversationStatus.HUMAN, ConversationStatus.CLOSED),
        ConversationStatus.CLOSED, EnumSet.noneOf(ConversationStatus.class)
    );

    public void transition(Conversation conv, ConversationStatus target) {
        Set<ConversationStatus> allowed = ALLOWED_TRANSITIONS.get(conv.getStatus());
        if (allowed == null || !allowed.contains(target)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "不允许从 " + conv.getStatus() + " 转换到 " + target);
        }
        conv.setStatus(target);
    }
}
```

- [ ] **Step 2: 编写 MessageService**

```java
package com.ai.cs.application.conversation;

import com.ai.cs.domain.conversation.Message;
import com.ai.cs.domain.conversation.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    @Transactional
    public Message saveMessage(Long conversationId, String senderType, Long senderId,
                                String content, String msgType, String metadata) {
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setSenderType(senderType);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setMsgType(msgType != null ? msgType : "text");
        msg.setMetadata(metadata);
        msg.setSendTime(LocalDateTime.now());
        return messageRepository.save(msg);
    }

    public List<Message> getRecentMessages(Long conversationId, int limit) {
        return messageRepository.findTop10ByConversationIdOrderBySendTimeDesc(conversationId)
                .stream().limit(limit).toList();
    }

    public List<Message> getConversationMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderBySendTimeAsc(conversationId);
    }
}
```

- [ ] **Step 3: 编写回复策略引擎核心**

```java
package com.ai.cs.application.aiemployee.strategy;

public enum StrategyResult {
    CONTINUE,    // 继续流水线
    INTERRUPT,   // 中断流水线（如排除策略拒绝、转人工）
    REPLIED      // 已生成回复，跳过后续节点
}
```

```java
package com.ai.cs.application.aiemployee.strategy;

import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.customer.CustomerProfile;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConversationContext {
    private Long conversationId;
    private Conversation conversation;
    private AiEmployee employee;
    private CustomerProfile customer;
    private String customerMessage;
    private List<String> recentHistory;  // 最近10轮对话
    private List<String> knowledgeChunks; // 检索到的知识库片段
    private String generatedReply;       // AI生成的回复
}
```

```java
package com.ai.cs.application.aiemployee.strategy;

public class ReplyBuilder {
    private final StringBuilder reply = new StringBuilder();
    private boolean hasContent = false;

    public ReplyBuilder append(String text) {
        if (text != null && !text.isEmpty()) {
            if (hasContent) reply.append("\n\n");
            reply.append(text);
            hasContent = true;
        }
        return this;
    }

    public String build() {
        return reply.toString();
    }

    public boolean hasContent() {
        return hasContent;
    }
}
```

```java
package com.ai.cs.application.aiemployee.strategy;

public interface ReplyStrategyNode {
    StrategyResult process(ConversationContext ctx, ReplyBuilder builder);
    int getOrder();
}
```

- [ ] **Step 4: 编写具体策略节点**

```java
package com.ai.cs.application.aiemployee.strategy;

import com.ai.cs.domain.employee.AiEmployeeReplyStrategy;
import com.ai.cs.domain.employee.repository.AiEmployeeReplyStrategyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(10)
@RequiredArgsConstructor
public class GreetingStrategy implements ReplyStrategyNode {

    private final AiEmployeeReplyStrategyRepository strategyRepo;
    private final ObjectMapper objectMapper;

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        List<AiEmployeeReplyStrategy> configs = strategyRepo
                .findByEmployeeIdAndEnabledOrderBySortOrderAsc(ctx.getEmployee().getId(), true);
        var greetingConfig = configs.stream()
                .filter(s -> "GREETING".equals(s.getStrategyType()))
                .findFirst().orElse(null);

        if (greetingConfig == null && ctx.getEmployee().getGreetingMsg() != null) {
            builder.append(ctx.getEmployee().getGreetingMsg());
            return StrategyResult.REPLIED;
        }

        if (greetingConfig != null) {
            try {
                JsonNode cfg = objectMapper.readTree(greetingConfig.getConfigJson());
                // 首次对话用 first_greeting，否则用 return_greeting
                String msg = ctx.getConversation().getStartTime() != null
                        && ctx.getConversation().getStartTime().isAfter(java.time.LocalDateTime.now().minusMinutes(5))
                        ? cfg.path("first_greeting").asText()
                        : cfg.path("return_greeting").asText();
                builder.append(msg);
                return StrategyResult.REPLIED;
            } catch (Exception e) {
                // 配置解析失败，使用默认问候
            }
        }
        return StrategyResult.CONTINUE;
    }

    @Override
    public int getOrder() { return 10; }
}
```

```java
package com.ai.cs.application.aiemployee.strategy;

import com.ai.cs.domain.employee.AiEmployeeReplyStrategy;
import com.ai.cs.domain.employee.repository.AiEmployeeReplyStrategyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
public class ExcludeStrategy implements ReplyStrategyNode {

    private final AiEmployeeReplyStrategyRepository strategyRepo;
    private final ObjectMapper objectMapper;

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        var configs = strategyRepo
                .findByEmployeeIdAndEnabledOrderBySortOrderAsc(ctx.getEmployee().getId(), true);
        var excludeConfig = configs.stream()
                .filter(s -> "EXCLUDE".equals(s.getStrategyType()))
                .findFirst().orElse(null);

        if (excludeConfig == null) return StrategyResult.CONTINUE;

        try {
            JsonNode cfg = objectMapper.readTree(excludeConfig.getConfigJson());
            // 检查消息中是否包含广告/推销关键词
            String msg = ctx.getCustomerMessage();
            // 简化实现：检查关键词
            if (msg.contains("广告") || msg.contains("推广") || msg.contains("合作")) {
                String reply = cfg.path("reject_message").asText("抱歉，我们不接受广告推销，如有业务需求请联系官方渠道。");
                builder.append(reply);
                return StrategyResult.INTERRUPT;
            }
        } catch (Exception ignored) {}
        return StrategyResult.CONTINUE;
    }

    @Override
    public int getOrder() { return 20; }
}
```

```java
package com.ai.cs.application.aiemployee.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
@RequiredArgsConstructor
public class ModerationStrategy implements ReplyStrategyNode {

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        String msg = ctx.getCustomerMessage();
        // 简化：检查敏感词汇
        if (msg.contains("投诉") || msg.contains("退款") || msg.contains("违约")) {
            builder.append("非常抱歉给您带来不便，我马上为您转接人工客服处理。");
            return StrategyResult.INTERRUPT;  // 触发转人工
        }
        return StrategyResult.CONTINUE;
    }

    @Override
    public int getOrder() { return 30; }
}
```

```java
package com.ai.cs.application.aiemployee.strategy;

import com.ai.cs.infrastructure.llm.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(50)
@RequiredArgsConstructor
public class KnowledgeStrategy implements ReplyStrategyNode {

    private final LlmService llmService;

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        // 构建 System Prompt 并调用 LLM
        String systemPrompt = buildSystemPrompt(ctx);
        String reply = llmService.chat(systemPrompt, ctx.getCustomerMessage());
        builder.append(reply);
        return StrategyResult.REPLIED;
    }

    private String buildSystemPrompt(ConversationContext ctx) {
        return String.format("""
            你是%s的AI客服，公司主营业务为：%s
            公司主要产品/服务包括：%s
            %s
            回复风格：%s，回复长度：%s
            ---
            相关知识库内容：
            %s
            ---
            对话历史：
            %s
            ---
            """,
            ctx.getEmployee().getName(),
            ctx.getEmployee().getCompanyIntro(),
            ctx.getEmployee().getProductIntro(),
            ctx.getEmployee().getServiceScope() != null
                    ? "你只接待" + ctx.getEmployee().getServiceScope() + "的客户" : "",
            ctx.getEmployee().getStyle(),
            ctx.getEmployee().getReplyLength(),
            ctx.getKnowledgeChunks() != null ? String.join("\n", ctx.getKnowledgeChunks()) : "暂无相关知识",
            ctx.getRecentHistory() != null ? String.join("\n", ctx.getRecentHistory()) : "无历史对话"
        );
    }

    @Override
    public int getOrder() { return 50; }
}
```

```java
package com.ai.cs.application.aiemployee.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
@RequiredArgsConstructor
public class ContactStrategy implements ReplyStrategyNode {

    @Override
    public StrategyResult process(ConversationContext ctx, ReplyBuilder builder) {
        // 留资策略：在对话末尾附上联系方式提示
        if (!builder.hasContent()) {
            builder.append("如需进一步了解，可以留下您的联系方式，我们的顾问会尽快与您联系～");
        }
        return StrategyResult.REPLIED;
    }

    @Override
    public int getOrder() { return 100; }
}
```

- [ ] **Step 5: 编写 ReplyPipelineService**

```java
package com.ai.cs.application.aiemployee;

import com.ai.cs.application.aiemployee.strategy.ConversationContext;
import com.ai.cs.application.aiemployee.strategy.ReplyBuilder;
import com.ai.cs.application.aiemployee.strategy.ReplyStrategyNode;
import com.ai.cs.application.aiemployee.strategy.StrategyResult;
import com.ai.cs.application.conversation.MessageService;
import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.customer.repository.CustomerProfileRepository;
import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.employee.repository.AiEmployeeRepository;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplyPipelineService {

    private final List<ReplyStrategyNode> strategyNodes;  // Spring 自动注入所有实现
    private final ConversationRepository conversationRepository;
    private final AiEmployeeRepository employeeRepository;
    private final CustomerProfileRepository customerRepository;
    private final MessageService messageService;

    @Transactional
    public String process(Long conversationId, Long employeeId, String customerMessage) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        AiEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_EMPLOYEE_NOT_FOUND));
        CustomerProfile customer = customerRepository.findById(conv.getCustomerId())
                .orElse(null);

        // 1. 保存客户消息
        messageService.saveMessage(conversationId, "CUSTOMER", conv.getCustomerId(),
                customerMessage, "text", null);

        // 2. 获取最近对话历史
        var recentMsgs = messageService.getRecentMessages(conversationId, 10);
        List<String> history = recentMsgs.stream()
                .map(m -> "[" + m.getSenderType() + "]:" + m.getContent())
                .toList();

        // 3. 构建上下文
        ConversationContext ctx = ConversationContext.builder()
                .conversationId(conversationId)
                .conversation(conv)
                .employee(employee)
                .customer(customer)
                .customerMessage(customerMessage)
                .recentHistory(history)
                .build();

        // 4. 按 order 排序执行策略流水线
        ReplyBuilder builder = new ReplyBuilder();
        List<ReplyStrategyNode> sortedNodes = strategyNodes.stream()
                .sorted(Comparator.comparingInt(ReplyStrategyNode::getOrder))
                .toList();

        for (ReplyStrategyNode node : sortedNodes) {
            StrategyResult result = node.process(ctx, builder);
            if (result == StrategyResult.INTERRUPT) {
                // 中断场景：已回复拒绝或触发转人工
                log.info("策略节点 {} 中断流水线", node.getClass().getSimpleName());
                break;
            }
            if (result == StrategyResult.REPLIED && builder.hasContent()) {
                break;  // 已生成回复，停止后续节点
            }
        }

        // 5. 保存 AI 回复
        String reply = builder.build();
        if (reply != null && !reply.isEmpty()) {
            messageService.saveMessage(conversationId, "AI", employeeId, reply, "text", null);
        }

        return reply;
    }
}
```

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/ai/cs/application/
git commit -m "feat(application): 添加会话管理、消息服务和回复策略引擎"
```

---

### Task 1.5: LLM 基础设施 + Elasticsearch 配置

**Files:**
- Create: `src/main/java/com/ai/cs/infrastructure/llm/LangChain4jConfig.java`
- Create: `src/main/java/com/ai/cs/infrastructure/llm/LlmService.java`
- Create: `src/main/java/com/ai/cs/infrastructure/search/ElasticsearchConfig.java`
- Create: `src/main/java/com/ai/cs/infrastructure/cache/RedisConfig.java`
- Create: `src/main/java/com/ai/cs/infrastructure/cache/CacheService.java`

- [ ] **Step 1: 编写 LLM 基础设施**

```java
package com.ai.cs.infrastructure.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // 默认使用 DeepSeek（通过 OpenAI 兼容接口）
        String provider = System.getenv().getOrDefault("LLM_PROVIDER", "deepseek");
        if ("ollama".equals(provider)) {
            return OllamaChatModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName("qwen2.5:7b")
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }
        // deepseek / qwen 都走 OpenAI 兼容接口
        String baseUrl = "deepseek".equals(provider)
                ? "https://api.deepseek.com"
                : "https://dashscope.aliyuncs.com/compatible-mode/v1";
        String apiKey = System.getenv().getOrDefault("LLM_API_KEY", "");
        String model = "deepseek".equals(provider) ? "deepseek-chat" : "qwen-plus";

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(2)
                .build();
    }
}
```

```java
package com.ai.cs.infrastructure.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final ChatLanguageModel chatModel;

    public String chat(String systemPrompt, String userMessage) {
        try {
            String fullPrompt = systemPrompt + "\n客户最新消息：" + userMessage + "\n请直接回复客户：";
            return chatModel.generate(fullPrompt);
        } catch (Exception e) {
            log.error("LLM 调用失败", e);
            return "抱歉，我暂时无法处理您的问题，请稍后再试或联系人工客服。";
        }
    }
}
```

- [ ] **Step 2: 编写 ES 和 Redis 配置**

```java
package com.ai.cs.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.host:localhost}")
    private String host;

    @Value("${elasticsearch.port:9200}")
    private int port;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        var builder = RestClient.builder(new HttpHost(host, port, "http"));
        if (username != null && !username.isEmpty()) {
            CredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(hcb ->
                    hcb.setDefaultCredentialsProvider(cp));
        }
        RestClient restClient = builder.build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
```

```java
package com.ai.cs.infrastructure.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

```java
package com.ai.cs.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/ai/cs/infrastructure/
git commit -m "feat(infra): 添加LangChain4j LLM、Elasticsearch、Redis基础设施"
```

---

### Task 1.6: 客服 WebSocket + IM 消息分发

**Files:**
- Create: `src/main/java/com/ai/cs/gateway/websocket/SessionRegistry.java`
- Create: `src/main/java/com/ai/cs/gateway/websocket/ChatWebSocketServer.java`
- Create: `src/main/java/com/ai/cs/gateway/websocket/MessageDispatcher.java`
- Create: `src/main/java/com/ai/cs/gateway/websocket/handler/MessageHandler.java`
- Create: `src/main/java/com/ai/cs/gateway/websocket/handler/MessageSendHandler.java`
- Create: `src/main/java/com/ai/cs/gateway/websocket/handler/TransferHandler.java`
- Create: `src/main/java/com/ai/cs/gateway/websocket/handler/HeartbeatHandler.java`

- [ ] **Step 1: 编写 SessionRegistry**

```java
package com.ai.cs.gateway.websocket;

import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SessionRegistry {

    // agentId -> Session
    private final Map<Long, Session> agentSessions = new ConcurrentHashMap<>();
    // sessionId -> agentId
    private final Map<String, Long> sessionToAgent = new ConcurrentHashMap<>();
    // agentId -> ticket
    private final Map<Long, String> agentTickets = new ConcurrentHashMap<>();

    public void register(Long agentId, Session session, String ticket) {
        Session old = agentSessions.put(agentId, session);
        if (old != null && old.isOpen()) {
            try { old.close(); } catch (Exception ignored) {}
        }
        sessionToAgent.put(session.getId(), agentId);
        agentTickets.put(agentId, ticket);
        log.info("客服上线: agentId={}", agentId);
    }

    public void unregister(Long agentId) {
        Session session = agentSessions.remove(agentId);
        if (session != null) {
            sessionToAgent.remove(session.getId());
        }
        agentTickets.remove(agentId);
        log.info("客服下线: agentId={}", agentId);
    }

    public Session getSession(Long agentId) {
        return agentSessions.get(agentId);
    }

    public Long getAgentId(String sessionId) {
        return sessionToAgent.get(sessionId);
    }

    public Set<Long> getOnlineAgentIds() {
        return agentSessions.keySet().stream()
                .filter(id -> agentSessions.get(id).isOpen())
                .collect(Collectors.toSet());
    }

    public boolean isOnline(Long agentId) {
        Session session = agentSessions.get(agentId);
        return session != null && session.isOpen();
    }
}
```

- [ ] **Step 2: 编写 ChatWebSocketServer**

```java
package com.ai.cs.gateway.websocket;

import com.ai.cs.shared.security.TicketService;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ServerEndpoint("/ws/chat/{ticket}")
public class ChatWebSocketServer {

    private static SessionRegistry sessionRegistry;
    private static TicketService ticketService;
    private static MessageDispatcher messageDispatcher;

    public static void setDependencies(SessionRegistry registry, TicketService ticketSvc,
                                        MessageDispatcher dispatcher) {
        sessionRegistry = registry;
        ticketService = ticketSvc;
        messageDispatcher = dispatcher;
    }

    private Long agentId;

    @OnOpen
    public void onOpen(Session session, @PathParam("ticket") String ticket) {
        var ticketInfo = ticketService.validate(ticket);
        if (ticketInfo == null) {
            sendAndClose(session, "{\"type\":\"auth.failed\",\"code\":401,\"data\":{\"message\":\"ticket无效或过期\"}}");
            return;
        }
        this.agentId = ticketInfo.agentId();
        sessionRegistry.register(agentId, session, ticket);
        sendMessage(session, "{\"type\":\"auth.success\",\"data\":{\"agentId\":" + agentId + "}}");
        log.info("客服 WebSocket 认证成功: agentId={}", agentId);
    }

    @OnMessage
    public void onMessage(String text, Session session) {
        messageDispatcher.dispatch(text, session, agentId);
    }

    @OnClose
    public void onClose() {
        if (agentId != null) {
            sessionRegistry.unregister(agentId);
        }
    }

    @OnError
    public void onError(Throwable t) {
        log.error("客服 WebSocket 错误: agentId={}", agentId, t);
    }

    private void sendMessage(Session session, String msg) {
        try {
            session.getBasicRemote().sendText(msg);
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    private void sendAndClose(Session session, String msg) {
        try {
            session.getBasicRemote().sendText(msg);
            session.close();
        } catch (Exception ignored) {}
    }
}
```

- [ ] **Step 3: 编写 MessageDispatcher 和 Handler**

```java
package com.ai.cs.gateway.websocket;

import com.ai.cs.gateway.websocket.handler.MessageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDispatcher {

    private final ObjectMapper objectMapper;
    private final Map<String, MessageHandler> handlerMap;

    // Spring 自动注入所有 MessageHandler 实现
    public MessageDispatcher(ObjectMapper objectMapper, List<MessageHandler> handlers) {
        this.objectMapper = objectMapper;
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(MessageHandler::supportedType, Function.identity()));
    }

    public void dispatch(String text, Session session, Long agentId) {
        try {
            JsonNode root = objectMapper.readTree(text);
            String type = root.path("type").asText();
            MessageHandler handler = handlerMap.get(type);
            if (handler != null) {
                handler.handle(root, session, agentId);
            } else {
                log.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("消息分发异常", e);
        }
    }
}
```

```java
package com.ai.cs.gateway.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.websocket.Session;

public interface MessageHandler {
    String supportedType();
    void handle(JsonNode message, Session session, Long agentId);
}
```

```java
package com.ai.cs.gateway.websocket.handler;

import com.ai.cs.application.conversation.MessageService;
import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSendHandler implements MessageHandler {

    private final MessageService messageService;
    private final ConversationRepository conversationRepository;

    @Override
    public String supportedType() {
        return "message.response";
    }

    @Override
    public void handle(JsonNode message, Session session, Long agentId) {
        Long conversationId = message.path("data").path("conversationId").asLong();
        String content = message.path("data").path("content").asText();
        String msgType = message.path("data").path("msgType").asText("text");

        Conversation conv = conversationRepository.findById(conversationId).orElse(null);
        if (conv == null) {
            log.warn("会话不存在: {}", conversationId);
            return;
        }

        messageService.saveMessage(conversationId, "HUMAN", agentId, content, msgType, null);
        log.info("客服发送消息: conversationId={}, agentId={}", conversationId, agentId);
    }
}
```

```java
package com.ai.cs.gateway.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HeartbeatHandler implements MessageHandler {

    @Override
    public String supportedType() {
        return "ping.request";
    }

    @Override
    public void handle(JsonNode message, Session session, Long agentId) {
        try {
            session.getBasicRemote().sendText("{\"type\":\"pong.response\",\"code\":0}");
        } catch (Exception e) {
            log.error("心跳响应失败", e);
        }
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/ai/cs/gateway/websocket/
git commit -m "feat(websocket): 添加客服WebSocket、消息分发和心跳处理"
```

---

### Task 1.7: REST API — 认证 + 会话管理 Controller

**Files:**
- Create: `src/main/java/com/ai/cs/gateway/rest/AuthController.java`
- Create: `src/main/java/com/ai/cs/gateway/rest/ConversationController.java`

- [ ] **Step 1: 编写 AuthController**

```java
package com.ai.cs.gateway.rest;

import com.ai.cs.domain.permission.SysUser;
import com.ai.cs.domain.permission.repository.SysUserRepository;
import com.ai.cs.shared.dto.ApiResponse;
import com.ai.cs.shared.security.JwtTokenProvider;
import com.ai.cs.shared.security.TicketService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TicketService ticketService;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        SysUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        if (!"ENABLED".equals(user.getStatus())) {
            return ApiResponse.error(403, "账号已被禁用");
        }

        List<String> permissions = getUserPermissions(user);
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), permissions);

        return ApiResponse.success(Map.of(
            "token", token,
            "username", user.getUsername(),
            "roleCode", user.getRoleCode(),
            "agentId", user.getAgentId(),
            "permissions", permissions
        ));
    }

    @PostMapping("/ticket")
    public ApiResponse<Map<String, Object>> getWebSocketTicket(@RequestAttribute("userId") Long userId) {
        SysUser user = userRepository.findById(userId).orElseThrow();
        String ticket = ticketService.createTicket(user.getAgentId(), user.getUsername());
        return ApiResponse.success(Map.of("ticket", ticket));
    }

    private List<String> getUserPermissions(SysUser user) {
        // 根据角色返回权限码列表，简化实现
        return switch (user.getRoleCode()) {
            case "ADMIN" -> List.of("ai_employee:view", "ai_employee:edit", "knowledge:view", "knowledge:edit",
                    "agent:view", "agent:edit", "im:access", "dashboard:view");
            case "LEADER" -> List.of("ai_employee:view", "knowledge:view", "agent:view", "im:access", "dashboard:view");
            case "AGENT" -> List.of("im:access");
            default -> List.of();
        };
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
```

- [ ] **Step 2: 编写 ConversationController**

```java
package com.ai.cs.gateway.rest;

import com.ai.cs.application.conversation.ConversationService;
import com.ai.cs.application.conversation.MessageService;
import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.Message;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.ai.cs.shared.dto.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final MessageService messageService;

    @GetMapping
    @PreAuthorize("hasAuthority('im:access')")
    public ApiResponse<List<Conversation>> listConversations(
            @RequestAttribute("userId") Long userId,
            @RequestAttribute(value = "roleCode", required = false) String roleCode,
            @RequestAttribute(value = "agentId", required = false) Long agentId) {

        List<Conversation> conversations;
        if ("ADMIN".equals(roleCode)) {
            conversations = conversationRepository.findAll();
        } else if ("LEADER".equals(roleCode)) {
            // 主管看团队所有客服的会话（简化：看所有非CLOSED）
            conversations = conversationRepository.findAll();
        } else {
            // AGENT 只能看自己的
            conversations = conversationRepository.findByOwnerAgentId(agentId);
        }
        return ApiResponse.success(conversations);
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("hasAuthority('im:access')")
    public ApiResponse<List<Message>> getMessages(@PathVariable Long id) {
        return ApiResponse.success(messageService.getConversationMessages(id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('im:access')")
    public ApiResponse<Conversation> closeConversation(@PathVariable Long id,
                                                        @RequestBody CloseRequest request) {
        return ApiResponse.success(conversationService.closeConversation(id, request.getReason()));
    }

    @Data
    public static class CloseRequest {
        private String reason;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/ai/cs/gateway/rest/
git commit -m "feat(rest): 添加认证登录和会话管理REST API"
```

---

## Phase 2: S2 — AI 员工配置管理

### Task 2.1: AI 员工 CRUD REST API

**Files:**
- Create: `src/main/java/com/ai/cs/application/aiemployee/AiEmployeeService.java`
- Create: `src/main/java/com/ai/cs/gateway/rest/AiEmployeeController.java`

- [ ] **Step 1: 编写 AiEmployeeService**

```java
package com.ai.cs.application.aiemployee;

import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.employee.AiEmployeeReplyStrategy;
import com.ai.cs.domain.employee.repository.AiEmployeeAccountRepository;
import com.ai.cs.domain.employee.repository.AiEmployeeReplyStrategyRepository;
import com.ai.cs.domain.employee.repository.AiEmployeeRepository;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiEmployeeService {

    private final AiEmployeeRepository employeeRepository;
    private final AiEmployeeReplyStrategyRepository strategyRepository;
    private final AiEmployeeAccountRepository accountRepository;

    public List<AiEmployee> listAll() {
        return employeeRepository.findAll();
    }

    public AiEmployee getById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_EMPLOYEE_NOT_FOUND));
    }

    @Transactional
    public AiEmployee create(AiEmployee employee) {
        if (employee.getStatus() == null) employee.setStatus("ENABLED");
        return employeeRepository.save(employee);
    }

    @Transactional
    public AiEmployee update(Long id, AiEmployee update) {
        AiEmployee existing = getById(id);
        // 只更新非空字段
        if (update.getName() != null) existing.setName(update.getName());
        if (update.getAvatar() != null) existing.setAvatar(update.getAvatar());
        if (update.getGreetingMsg() != null) existing.setGreetingMsg(update.getGreetingMsg());
        if (update.getStyle() != null) existing.setStyle(update.getStyle());
        if (update.getReplyLength() != null) existing.setReplyLength(update.getReplyLength());
        if (update.getContentCheck() != null) existing.setContentCheck(update.getContentCheck());
        if (update.getAggregateInterval() != null) existing.setAggregateInterval(update.getAggregateInterval());
        if (update.getDelayInterval() != null) existing.setDelayInterval(update.getDelayInterval());
        if (update.getServiceTimeStart() != null) existing.setServiceTimeStart(update.getServiceTimeStart());
        if (update.getServiceTimeEnd() != null) existing.setServiceTimeEnd(update.getServiceTimeEnd());
        if (update.getWeekdays() != null) existing.setWeekdays(update.getWeekdays());
        if (update.getCompanyIntro() != null) existing.setCompanyIntro(update.getCompanyIntro());
        if (update.getProductIntro() != null) existing.setProductIntro(update.getProductIntro());
        if (update.getServiceScope() != null) existing.setServiceScope(update.getServiceScope());
        if (update.getStatus() != null) existing.setStatus(update.getStatus());
        return employeeRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        AiEmployee e = getById(id);
        e.setStatus("DISABLED");
        employeeRepository.save(e);
    }

    // === 回复策略管理 ===

    public List<AiEmployeeReplyStrategy> getStrategies(Long employeeId) {
        return strategyRepository.findByEmployeeIdAndEnabledOrderBySortOrderAsc(employeeId, true);
    }

    @Transactional
    public AiEmployeeReplyStrategy saveStrategy(Long employeeId, AiEmployeeReplyStrategy strategy) {
        strategy.setEmployeeId(employeeId);
        return strategyRepository.save(strategy);
    }

    @Transactional
    public void deleteStrategy(Long strategyId) {
        AiEmployeeReplyStrategy s = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STRATEGY_NOT_FOUND));
        s.setEnabled(false);
        strategyRepository.save(s);
    }
}
```

- [ ] **Step 2: 编写 AiEmployeeController**

```java
package com.ai.cs.gateway.rest;

import com.ai.cs.application.aiemployee.AiEmployeeService;
import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.employee.AiEmployeeReplyStrategy;
import com.ai.cs.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-employees")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ai_employee:view')")
public class AiEmployeeController {

    private final AiEmployeeService aiEmployeeService;

    @GetMapping
    public ApiResponse<List<AiEmployee>> list() {
        return ApiResponse.success(aiEmployeeService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<AiEmployee> get(@PathVariable Long id) {
        return ApiResponse.success(aiEmployeeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<AiEmployee> create(@RequestBody AiEmployee employee) {
        return ApiResponse.success(aiEmployeeService.create(employee));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<AiEmployee> update(@PathVariable Long id, @RequestBody AiEmployee employee) {
        return ApiResponse.success(aiEmployeeService.update(id, employee));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        aiEmployeeService.delete(id);
        return ApiResponse.success();
    }

    // 回复策略
    @GetMapping("/{employeeId}/strategies")
    public ApiResponse<List<AiEmployeeReplyStrategy>> getStrategies(@PathVariable Long employeeId) {
        return ApiResponse.success(aiEmployeeService.getStrategies(employeeId));
    }

    @PostMapping("/{employeeId}/strategies")
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<AiEmployeeReplyStrategy> saveStrategy(@PathVariable Long employeeId,
                                                               @RequestBody AiEmployeeReplyStrategy strategy) {
        return ApiResponse.success(aiEmployeeService.saveStrategy(employeeId, strategy));
    }

    @DeleteMapping("/{employeeId}/strategies/{strategyId}")
    @PreAuthorize("hasAuthority('ai_employee:edit')")
    public ApiResponse<Void> deleteStrategy(@PathVariable Long employeeId, @PathVariable Long strategyId) {
        aiEmployeeService.deleteStrategy(strategyId);
        return ApiResponse.success();
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/ai/cs/application/aiemployee/AiEmployeeService.java src/main/java/com/ai/cs/gateway/rest/AiEmployeeController.java
git commit -m "feat(ai-employee): 添加AI员工CRUD和回复策略管理API"
```

---

### Task 2.2: 前端项目初始化

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/index.html`
- Create: `frontend/src/app.tsx`
- Create: `frontend/src/main.tsx`

- [ ] **Step 1: 初始化前端项目**

```bash
cd frontend
npm create vite@latest . -- --template react-ts
npm install antd @ant-design/icons @ant-design/pro-layout @ant-design/charts
npm install react-router-dom zustand axios dayjs
npm install -D @types/node
```

- [ ] **Step 2: 编写核心前端文件**（略，具体前端代码见后续前端专项计划）

- [ ] **Step 3: 提交**

```bash
git add frontend/
git commit -m "feat(frontend): 初始化React前端项目和核心依赖"
```

---

## Phase 3-6: S3-S6 任务概要

以下切片的具体实施任务将在 S1-S2 完成后按相同模式展开。每个任务均遵循 TDD 流程：「编写测试 → 验证失败 → 最小实现 → 验证通过 → 提交」。

### S3 — 知识库训练全链路 (Task 3.1-3.4)

| Task | 内容 |
|------|------|
| 3.1 | 文档解析器：PdfParser, ExcelParser, WordParser, TxtParser + DocumentParserService |
| 3.2 | 文档分片：TextSplitter（语义边界，500-1000字/片，重叠50-100字） |
| 3.3 | 向量化 + ES 索引：EmbeddingService → KnowledgeChunkIndexService 批量索引 |
| 3.4 | RocketMQ 异步链路：DocumentTrainingProducer/Consumer 串联全流程 + RAG 混合检索（RRF 融合） |

### S4 — 分配策略与转人工规则 (Task 4.1-4.4)

| Task | 内容 |
|------|------|
| 4.1 | TransferRuleEngine：4 种触发条件评估 + 最高优先级命中 |
| 4.2 | 4 种 AssignmentStrategy 实现：SpecifiedStrategy, RoundRobinStrategy, LeastLoadStrategy, HistoryStrategy |
| 4.3 | AssignmentEngine：撞单检测 → 客服池过滤 → 分配策略 → 等待队列 |
| 4.4 | REST API：TransferRuleController + AssignmentController + HumanAgentController |

### S5 — 小红书渠道 + 顾客名片 (Task 5.1-5.3)

| Task | 内容 |
|------|------|
| 5.1 | XiaohongshuChannelAdapter：Webhook 验签 + 消息 normalize + API 发送（RateLimiter 限流） |
| 5.2 | CollectStrategy / FilterStrategy 完整实现：多轮收集 → 写入 CustomerProfile.extraFields |
| 5.3 | CustomerProfile REST API：名片查询/编辑 + 前端顾客信息展示卡片 |

### S6 — 数据看板 + 隐私隔离 (Task 6.1-6.4)

| Task | 内容 |
|------|------|
| 6.1 | AiEmployeeAnalyticsService：会话量/解决率/转人工率/知识库命中率 聚合查询 |
| 6.2 | ServiceAnalyticsService：Redis 实时计数 + 30s 推送 + 响应时长/解决率 |
| 6.3 | TopQuestionService：ES aggregation 客户问题聚类 + 频次排名 TOP20 |
| 6.4 | 客户隐私隔离：@DataScope 注解 + AOP 切面 + Conversation 查询强制 owner_agent_id 过滤 + 菜单权限控制 |

---

## 附录 A: 前端页面规划

前端作为管理后台 + IM 工作台，按以下页面结构组织：

```
/frontend/src/pages/
├── Login/              # 登录页
├── AiEmployee/         # AI员工列表 + 新建/编辑表单 + 回复策略配置面板
├── KnowledgeBase/      # 知识库列表 + 文档上传 + 训练状态 + 分片预览
├── AgentManagement/    # 客服列表 + 新增/编辑 + 渠道权限配置
├── IM/                 # IM工作台（三栏布局 + 7个导航 + 消息气泡）
├── Analytics/          # 数据看板（3块看板 + 图表）
└── System/             # 系统设置（角色权限/菜单管理/操作日志）
```

前端详细实施将在后端 API 稳定后，按 S1→S6 对应页面分批实现。

---

## 附录 B: 关键技术决策记录

1. **单模块 Maven 项目**：Phase 1 不拆多模块，保持简单，包分层隔离关注点
2. **WebSocket 用 Spring 原生 @ServerEndpoint**：不引入 Netty，减少依赖复杂度
3. **Flyway 管理 Schema**：确保环境一致性，按版本有序迁移
4. **tenant_id 当前写死为 1**：所有查询条件中预留，但 Phase 1 不做多租户路由
5. **AI 回复走同步流水线**：Phase 1 不做异步回复，S1 保证最小闭环的简单性
6. **ES 与 MySQL 双写**：知识库分片同时存 MySQL（元数据）和 ES（检索），保证数据可恢复
