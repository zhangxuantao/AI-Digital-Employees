# 前端管理后台页面 — 分阶段实施设计

**日期**: 2026-06-12
**状态**: 已确认，待实施
**依赖**: Phase 1 后端 API 已全部就绪

---

## 1. 概述

基于 Phase 1 已实现的后端 API，将前端占位页面全部实现为可交互的管理后台页面。按依赖顺序分 4 个阶段实施。

### 技术栈
- React 18 + TypeScript + Vite
- Ant Design 5 (antd, @ant-design/icons, @ant-design/pro-layout)
- @ant-design/charts 图表
- Zustand 状态管理
- Axios HTTP 客户端
- WebSocket 实时通信
- @dnd-kit/core (拖拽排序)

---

## 2. 页面清单与阶段划分

| 阶段 | 页面 | 核心功能 | 预计 |
|------|------|---------|------|
| **P1** | AI 员工管理 | 列表 + 表单(Drawer) + 策略面板(拖拽排序+Switch开关) | 2 天 |
| **P2** | 知识库管理 | 主从布局 + 文档上传(进度条) + 分片预览 | 1.5 天 |
| **P3** | 客服管理 + 转人工规则 | 客服 CRUD + 渠道权限 + 4种规则表格 + 分配策略卡片 | 1.5 天 |
| **P4** | IM 工作台 + 系统设置 | 三栏布局 + WebSocket 消息 + 用户/角色/权限 Tab | 2 天 |

---

## 3. P1 — AI 员工管理

### 3.1 组件树

```
AiEmployeePage/
├── AiEmployeeList.tsx          # 表格列表（Ant Table）
│   ├── 搜索框 (Input.Search)
│   └── 新建按钮 → 打开 Drawer
├── AiEmployeeDrawer.tsx        # 右侧 Drawer（Ant Drawer）
│   ├── 基础信息区
│   │   ├── 名称 (Input)
│   │   ├── 头像上传 (Upload)
│   │   ├── 接待风格 (Radio.Group: 专业/温暖/热情/可靠)
│   │   ├── 回复字数 (Radio.Group: 简短/中等/详细)
│   │   ├── 开场白 (TextArea)
│   │   ├── 公司介绍 (TextArea)
│   │   ├── 产品介绍 (TextArea)
│   │   └── 服务对象 (Input)
│   └── 服务配置区
│       ├── 服务日 (Checkbox.Group: 一二三四五六日)
│       ├── 服务时段 (TimePicker × 2)
│       ├── 聚合间隔 (InputNumber: 1-10)
│       ├── 延迟间隔 (InputNumber: 1-10)
│       └── 敏感词规则 (TextArea, JSON 格式)
├── StrategyPanel.tsx           # 策略配置面板（Modal）
│   ├── 策略卡片列表 (dnd-kit 可拖拽)
│   │   ├── 拖拽手柄 (⋮⋮)
│   │   ├── 序号
│   │   ├── 策略名 + 类型标签
│   │   ├── 配置摘要
│   │   └── Switch 开关 (调用 POST/DELETE API)
│   └── 策略编辑弹窗 (不同策略不同 JSON Schema)
└── services/aiEmployee.ts      # API 调用封装
```

### 3.2 API 对接

| 操作 | 方法 | 路径 |
|------|------|------|
| 列表 | GET | `/api/v1/ai-employees` |
| 详情 | GET | `/api/v1/ai-employees/{id}` |
| 创建 | POST | `/api/v1/ai-employees` |
| 更新 | PUT | `/api/v1/ai-employees/{id}` |
| 软删除 | DELETE | `/api/v1/ai-employees/{id}` |
| 策略列表 | GET | `/api/v1/ai-employees/{id}/strategies` |
| 保存策略 | POST | `/api/v1/ai-employees/{id}/strategies` |
| 删除策略 | DELETE | `/api/v1/ai-employees/{id}/strategies/{sid}` |
| 批量排序 | PATCH | `/api/v1/ai-employees/{id}/strategies/batch-sort` **(新增)** |

### 3.3 新增后端端点

```java
// AiEmployeeController.java
@PatchMapping("/{employeeId}/strategies/batch-sort")
@PreAuthorize("hasAuthority('ai_employee:edit')")
public ApiResponse<Void> batchSort(@PathVariable Long employeeId,
                                    @RequestBody List<SortOrderItem> items) {
    aiEmployeeService.batchUpdateSortOrder(employeeId, items);
    return ApiResponse.success();
}

record SortOrderItem(Long id, Integer sortOrder) {}
```

---

## 4. P2 — 知识库管理

### 4.1 组件树

```
KnowledgePage/
├── KnowledgeLayout.tsx         # 主从布局（左侧列表 + 右侧详情）
│   ├── KnowledgeList.tsx       # 左侧知识库列表
│   │   ├── 新建按钮
│   │   └── 列表项（名称 + 文档数 + 关联AI员工）
│   └── DocumentList.tsx        # 右侧文档表格（Ant Table）
│       ├── 搜索框
│       ├── 上传按钮
│       └── 表格列：文件名、类型、大小、状态、分片数、时间
├── UploadModal.tsx             # 上传弹窗（Ant Modal）
│   ├── Upload.Dragger (拖拽上传区)
│   └── 训练进度（Steps 组件：解析→分片→向量化→索引）
└── ChunkPanel.tsx              # 分片预览（展开面板）
    ├── 状态统计标签
    ├── 分片卡片列表（内容摘要 + ES ID + 字数）
    └── 失败分片重试按钮
```

### 4.2 API 对接

| 操作 | 方法 | 路径 |
|------|------|------|
| 列表 | GET | `/api/v1/knowledge-bases` |
| 创建 | POST | `/api/v1/knowledge-bases` |
| 上传文档 | POST | `/api/v1/knowledge-bases/{kbId}/documents` (multipart) |
| 文档列表 | GET | `/api/v1/knowledge-bases/{kbId}/documents` |
| 分片列表 | GET | `/api/v1/knowledge-bases/{kbId}/documents/{docId}/chunks` |

---

## 5. P3 — 客服管理 + 转人工规则

### 5.1 组件树

```
AgentPage/
├── AgentList.tsx               # 客服列表（Ant Table）
│   ├── 状态指示灯（在线/忙碌/离线）
│   ├── 负载进度条
│   └── 渠道标签
├── AgentDrawer.tsx             # 新建/编辑 Drawer
│   ├── 姓名、手机号、最大并发数
│   └── 渠道权限面板
│       ├── 已有权限表格
│       └── 添加权限下拉框（渠道 + AI员工）
└── TransferRulePage/
    ├── TransferRuleList.tsx    # 转人工规则表格
    │   ├── AI员工筛选下拉
    │   ├── 优先级序号（彩色圆形）
    │   ├── 触发类型标签
    │   ├── 触发条件 + 执行动作
    │   └── Switch 启停
    ├── TransferRuleDrawer.tsx  # 新建/编辑 Drawer
    │   ├── 触发类型 (Select)
    │   ├── 触发条件 JSON (TextArea)
    │   ├── 执行动作 (Checkbox.Group)
    │   └── 优先级 (InputNumber)
    └── AssignmentCard.tsx      # 分配策略 4 选 1 卡片
```

### 5.2 API 对接

| 操作 | 方法 | 路径 |
|------|------|------|
| 客服列表 | GET | `/api/v1/agents` |
| 创建客服 | POST | `/api/v1/agents` |
| 删除客服 | DELETE | `/api/v1/agents/{id}` |
| 规则列表 | GET | `/api/v1/transfer-rules?employeeId=` |
| 创建规则 | POST | `/api/v1/transfer-rules` |
| 更新规则 | PUT | `/api/v1/transfer-rules/{id}` |
| 删除规则 | DELETE | `/api/v1/transfer-rules/{id}` |
| 渠道权限 | - | **(新增 Controller)** |

---

## 6. P4 — IM 工作台 + 系统设置

### 6.1 组件树

```
IMPage/
├── IMLayout.tsx                # 三栏布局（可拖拽调整宽度）
│   ├── NavPanel.tsx            # 左栏：7 个导航项 + 数量徽标
│   ├── ConversationPanel.tsx   # 中栏上：会话列表
│   ├── MessagePanel.tsx        # 中栏下：消息气泡（Bubble）+ 输入区
│   │   ├── 客户气泡（灰色左侧）
│   │   ├── AI 气泡（蓝色右侧）
│   │   ├── 人工气泡（绿色右侧）
│   │   ├── 系统消息（居中黄色）
│   │   └── 输入区（TextArea + 发送/结束/转接按钮）
│   └── CustomerCard.tsx        # 右栏：顾客信息卡片
│       ├── 头像 + 昵称
│       ├── 基本信息（渠道/来源/时间/状态）
│       ├── 标签
│       ├── AI 收集字段
│       └── 历史会话列表
├── WebSocketService.ts         # WebSocket 连接管理
│   ├── connect(ticket)
│   ├── send(type, data)
│   ├── onMessage(callback)
│   └── disconnect()
└── SystemSettings/
    ├── UserTab.tsx             # 用户管理（Ant Table + Form Modal）
    ├── RoleTab.tsx             # 角色管理（只读 Ant Table）
    └── PermissionTab.tsx       # 菜单权限（Ant Tree）
```

### 6.2 菜单结构更新

```typescript
// DashboardLayout.tsx
const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '数据看板', permission: 'dashboard:view' },
  { key: '/ai-employee', icon: <RobotOutlined />, label: 'AI 员工', permission: 'ai_employee:view' },
  { key: '/knowledge', icon: <BookOutlined />, label: '知识库', permission: 'knowledge:view' },
  { key: '/agents', icon: <TeamOutlined />, label: '客服管理', permission: 'agent:view' },
  { key: '/transfer-rules', icon: <SwapOutlined />, label: '转人工规则', permission: 'ai_employee:view' },
  { key: '/im', icon: <MessageOutlined />, label: 'IM 工作台', permission: 'im:access' },
  { key: '/settings', icon: <SettingOutlined />, label: '系统设置', permission: 'ai_employee:edit', adminOnly: true },
];
```

### 6.3 WebSocket 消息类型

| 类型 | 方向 | 说明 |
|------|------|------|
| `message.request` | S→C | 客户新消息 |
| `message.response` | C→S | 客服回复 |
| `subscriber.open` | S→C | 新客户进入 |
| `subscriber.close` | S→C | 客户离开 |
| `session.close` | C→S | 客服关闭会话 |
| `transfer.request` | C↔S | 转移请求 |
| `employee.status` | C↔S | 状态切换 |
| `ping.request` / `pong.response` | C↔S | 心跳 |

### 6.4 API 对接

| 操作 | 方法 | 路径 |
|------|------|------|
| 会话列表 | GET | `/api/v1/conversations` |
| 会话消息 | GET | `/api/v1/conversations/{id}/messages` |
| 关闭会话 | POST | `/api/v1/conversations/{id}/close` |
| 获取 Ticket | POST | `/api/v1/auth/ticket` |
| 顾客详情 | GET | `/api/v1/customers/{id}` |
| 用户管理 | - | **(新增 Controller)** |

---

## 7. 需要新增的后端端点

| 端点 | 说明 | 优先级 |
|------|------|--------|
| `PATCH /ai-employees/{id}/strategies/batch-sort` | 批量更新策略排序 | P1 |
| `CRUD /api/v1/agent-permissions` | 客服渠道权限管理 | P3 |
| `GET/PUT /api/v1/settings/users` | 系统用户管理 | P4 |
| `GET /api/v1/settings/roles` | 角色列表 | P4 |
| `GET /api/v1/settings/permissions` | 权限树 | P4 |

---

## 8. 验收标准

1. ✅ AI 员工列表可正常 CRUD，表单字段与后端实体一一对应
2. ✅ 回复策略可拖拽排序、Switch 启停、点击编辑 JSON 配置
3. ✅ 知识库可创建、上传文档、查看训练进度和分片内容
4. ✅ 客服可创建/删除、配置渠道权限
5. ✅ 转人工规则 4 种类型可 CRUD、分配策略 4 选 1
6. ✅ IM 工作台三栏布局可正常展示、WebSocket 实时收发消息
7. ✅ 系统设置用户/角色/权限三个 Tab 正常展示
8. ✅ 菜单根据角色权限码自动过滤
