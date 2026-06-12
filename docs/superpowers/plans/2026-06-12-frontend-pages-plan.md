# 前端管理后台 6 页面 — 分阶段实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 AI 智能客服系统前端管理后台的 6 个占位页面全部实现为可交互页面（AI员工管理、知识库管理、客服管理、转人工规则、IM工作台、系统设置），按 P1→P4 四个阶段交付。

**Architecture:** React 18 + TypeScript + Ant Design 5 组件库，Zustand 状态管理，Axios HTTP 客户端，@dnd-kit 拖拽排序，WebSocket 实时通信。每个页面独立目录，共享 `services/` API 层和 `stores/authStore`。

**Tech Stack:** React 18, TypeScript, Vite, Ant Design 5, @ant-design/icons, @ant-design/charts, zustand, axios, @dnd-kit/core, dayjs

---

## Phase 0: 基础设施准备

### Task 0.1: 新增后端端点（PATCH 批量排序）

**Files:**
- Modify: `src/main/java/com/ai/cs/gateway/rest/AiEmployeeController.java`
- Modify: `src/main/java/com/ai/cs/application/aiemployee/AiEmployeeService.java`

- [ ] **Step 1: 添加 SortOrderItem DTO 和批量排序方法**

```java
// AiEmployeeController.java — 添加在 deleteStrategy 方法之后
@PatchMapping("/{employeeId}/strategies/batch-sort")
@PreAuthorize("hasAuthority('ai_employee:edit')")
public ApiResponse<Void> batchSort(@PathVariable Long employeeId,
                                    @RequestBody List<SortOrderItem> items) {
    aiEmployeeService.batchUpdateSortOrder(employeeId, items);
    return ApiResponse.success();
}

public record SortOrderItem(Long id, Integer sortOrder) {}
```

```java
// AiEmployeeService.java — 添加方法
@Transactional
public void batchUpdateSortOrder(Long employeeId, List<AiEmployeeController.SortOrderItem> items) {
    for (var item : items) {
        AiEmployeeReplyStrategy strategy = strategyRepository.findById(item.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.STRATEGY_NOT_FOUND));
        if (!strategy.getEmployeeId().equals(employeeId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "策略不属于该员工");
        }
        strategy.setSortOrder(item.sortOrder());
        strategyRepository.save(strategy);
    }
}
```

- [ ] **Step 2: 编译验证并提交**

```bash
cd E:/study/github/AI-Digital-Employees
export JAVA_HOME="D:/Program Files/Java/jdk-17.0.18"
mvn compile -s D:/mvn/settings.xml -Dmaven.repo.local=D:/mvn/repository
git add -A && git commit -m "feat(api): 添加AI员工策略批量排序PATCH端点"
```

### Task 0.2: 新增后端端点（渠道权限 + 系统设置）

**Files:**
- Create: `src/main/java/com/ai/cs/gateway/rest/AgentPermissionController.java`
- Create: `src/main/java/com/ai/cs/gateway/rest/SysSettingsController.java`
- Create: `src/main/java/com/ai/cs/domain/permission/repository/SysRoleRepository.java` (if missing)

- [ ] **Step 1: 创建 AgentPermissionController**

```java
package com.ai.cs.gateway.rest;

import com.ai.cs.domain.assignment.AgentChannelPermission;
import com.ai.cs.domain.assignment.repository.AgentChannelPermissionRepository;
import com.ai.cs.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agent-permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('agent:view')")
public class AgentPermissionController {

    private final AgentChannelPermissionRepository permissionRepo;

    @GetMapping
    public ApiResponse<List<AgentChannelPermission>> list(@RequestParam Long agentId) {
        return ApiResponse.success(permissionRepo.findAll()); // TODO: filter by agentId
    }

    @PostMapping
    @PreAuthorize("hasAuthority('agent:edit')")
    public ApiResponse<AgentChannelPermission> create(@RequestBody AgentChannelPermission perm) {
        return ApiResponse.success(permissionRepo.save(perm));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('agent:edit')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionRepo.deleteById(id);
        return ApiResponse.success();
    }
}
```

- [ ] **Step 2: 创建 SysSettingsController**

```java
package com.ai.cs.gateway.rest;

import com.ai.cs.domain.permission.SysRole;
import com.ai.cs.domain.permission.SysPermission;
import com.ai.cs.domain.permission.SysUser;
import com.ai.cs.domain.permission.repository.SysRoleRepository;
import com.ai.cs.domain.permission.repository.SysPermissionRepository;
import com.ai.cs.domain.permission.repository.SysUserRepository;
import com.ai.cs.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ai_employee:edit')") // ADMIN only
public class SysSettingsController {

    private final SysUserRepository userRepo;
    private final SysRoleRepository roleRepo;
    private final SysPermissionRepository permRepo;

    @GetMapping("/users")
    public ApiResponse<List<SysUser>> listUsers() { return ApiResponse.success(userRepo.findAll()); }

    @GetMapping("/roles")
    public ApiResponse<List<SysRole>> listRoles() { return ApiResponse.success(roleRepo.findAll()); }

    @GetMapping("/permissions")
    public ApiResponse<List<SysPermission>> listPermissions() { return ApiResponse.success(permRepo.findAll()); }
}
```

- [ ] **Step 3: 编译验证并提交**

```bash
mvn compile -s D:/mvn/settings.xml -Dmaven.repo.local=D:/mvn/repository
git add -A && git commit -m "feat(api): 添加渠道权限和系统设置REST端点"
```

### Task 0.3: 安装前端依赖 + 更新侧边栏菜单

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/src/layouts/DashboardLayout.tsx`
- Modify: `frontend/src/app.tsx`

- [ ] **Step 1: 安装新依赖**

```bash
cd frontend
npm install @dnd-kit/core @dnd-kit/sortable @dnd-kit/utilities
```

- [ ] **Step 2: 更新侧边栏菜单添加新路由**

```tsx
// DashboardLayout.tsx — 更新 menuItems 数组
import { SwapOutlined, SettingOutlined } from '@ant-design/icons'

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '数据看板', permission: 'dashboard:view' },
  { key: '/ai-employee', icon: <RobotOutlined />, label: 'AI 员工', permission: 'ai_employee:view' },
  { key: '/knowledge', icon: <BookOutlined />, label: '知识库', permission: 'knowledge:view' },
  { key: '/agents', icon: <TeamOutlined />, label: '客服管理', permission: 'agent:view' },
  { key: '/transfer-rules', icon: <SwapOutlined />, label: '转人工规则', permission: 'ai_employee:view' },
  { key: '/im', icon: <MessageOutlined />, label: 'IM 工作台', permission: 'im:access' },
]

// ADMIN only menu
if (roleCode === 'ADMIN') {
  menuItems.push({ key: '/settings', icon: <SettingOutlined />, label: '系统设置', permission: 'ai_employee:edit' })
}
```

- [ ] **Step 3: 更新 app.tsx 添加新路由**

```tsx
// app.tsx — 在 DashboardLayout 的 children routes 中添加:
<Route path="transfer-rules" element={<PlaceholderPage title="转人工规则" icon="🔄" description="配置关键词、情感、手动、时段四种触发规则。" />} />
<Route path="settings" element={<PlaceholderPage title="系统设置" icon="⚙️" description="用户管理、角色管理、菜单权限配置。" />} />
```

- [ ] **Step 4: 提交**

```bash
git add -A && git commit -m "feat(frontend): 添加侧边栏新菜单项和依赖"
```

---

## Phase 1: AI 员工管理页面

### Task 1.1: API 服务层

**Files:**
- Create: `frontend/src/services/aiEmployee.ts`

- [ ] **Step 1: 编写 API 服务**

```typescript
// frontend/src/services/aiEmployee.ts
import api from './api'

export interface AiEmployee {
  id?: number
  name: string
  avatar?: string
  greetingMsg?: string
  style: string
  replyLength: string
  contentCheck?: string
  aggregateInterval?: number
  delayInterval?: number
  serviceTimeStart?: string
  serviceTimeEnd?: string
  weekdays?: string
  companyIntro: string
  productIntro: string
  serviceScope?: string
  status?: string
}

export interface ReplyStrategy {
  id?: number
  employeeId?: number
  strategyType: string
  configJson: string
  enabled: boolean
  sortOrder: number
}

export async function listEmployees() {
  const res = await api.get<any, { code: number; data: AiEmployee[] }>('/ai-employees')
  return res.data
}

export async function getEmployee(id: number) {
  const res = await api.get<any, { code: number; data: AiEmployee }>(`/ai-employees/${id}`)
  return res.data
}

export async function createEmployee(data: AiEmployee) {
  const res = await api.post<any, { code: number; data: AiEmployee }>('/ai-employees', data)
  return res.data
}

export async function updateEmployee(id: number, data: Partial<AiEmployee>) {
  const res = await api.put<any, { code: number; data: AiEmployee }>(`/ai-employees/${id}`, data)
  return res.data
}

export async function deleteEmployee(id: number) {
  await api.delete(`/ai-employees/${id}`)
}

export async function listStrategies(employeeId: number) {
  const res = await api.get<any, { code: number; data: ReplyStrategy[] }>(`/ai-employees/${employeeId}/strategies`)
  return res.data
}

export async function saveStrategy(employeeId: number, data: ReplyStrategy) {
  const res = await api.post<any, { code: number; data: ReplyStrategy }>(`/ai-employees/${employeeId}/strategies`, data)
  return res.data
}

export async function deleteStrategy(employeeId: number, strategyId: number) {
  await api.delete(`/ai-employees/${employeeId}/strategies/${strategyId}`)
}

export async function batchSortStrategies(employeeId: number, items: { id: number; sortOrder: number }[]) {
  await api.patch(`/ai-employees/${employeeId}/strategies/batch-sort`, items)
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/services/aiEmployee.ts && git commit -m "feat(api): 添加AI员工前端API服务层"
```

### Task 1.2: AI 员工列表页

**Files:**
- Create: `frontend/src/pages/AiEmployee/AiEmployeeList.tsx`
- Modify: `frontend/src/pages/AiEmployee/index.tsx`

- [ ] **Step 1: 编写列表组件**

```tsx
// frontend/src/pages/AiEmployee/AiEmployeeList.tsx
import { useEffect, useState } from 'react'
import { Table, Button, Input, Space, Tag, Switch, Popconfirm, message, Avatar } from 'antd'
import { PlusOutlined, SearchOutlined, EditOutlined, SettingOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { listEmployees, deleteEmployee, AiEmployee } from '../../services/aiEmployee'

const STYLE_MAP: Record<string, { color: string; label: string }> = {
  PROFESSIONAL: { color: 'blue', label: '专业' },
  WARM: { color: 'orange', label: '温暖' },
  ENTHUSIASTIC: { color: 'volcano', label: '热情' },
  RELIABLE: { color: 'green', label: '可靠' },
}

const LENGTH_MAP: Record<string, string> = {
  SHORT: '简短', MEDIUM: '中等', DETAIL: '详细',
}

interface Props {
  onEdit: (employee: AiEmployee) => void
  onConfigStrategy: (employee: AiEmployee) => void
  refreshKey: number
}

export default function AiEmployeeList({ onEdit, onConfigStrategy, refreshKey }: Props) {
  const [data, setData] = useState<AiEmployee[]>([])
  const [loading, setLoading] = useState(false)
  const [search, setSearch] = useState('')

  const fetchData = async () => {
    setLoading(true)
    try { setData(await listEmployees()) } finally { setLoading(false) }
  }

  useEffect(() => { fetchData() }, [refreshKey])

  const handleDelete = async (id: number) => {
    await deleteEmployee(id)
    message.success('已禁用')
    fetchData()
  }

  const filtered = data.filter((e) => !search || e.name?.includes(search))

  const columns: ColumnsType<AiEmployee> = [
    {
      title: '名称', dataIndex: 'name', width: 160,
      render: (name: string, record) => (
        <Space>
          <Avatar style={{ backgroundColor: '#1677ff' }} size="small">
            {name?.charAt(0)}
          </Avatar>
          {name}
        </Space>
      ),
    },
    {
      title: '风格', dataIndex: 'style', width: 80,
      render: (s: string) => <Tag color={STYLE_MAP[s]?.color}>{STYLE_MAP[s]?.label ?? s}</Tag>,
    },
    {
      title: '回复字数', dataIndex: 'replyLength', width: 80,
      render: (l: string) => LENGTH_MAP[l] ?? l,
    },
    {
      title: '服务时段', width: 180,
      render: (_: any, r: AiEmployee) => {
        if (!r.weekdays && !r.serviceTimeStart) return <span style={{ color: '#bfbfbf' }}>未设置</span>
        return `${r.weekdays ?? '-'} ${r.serviceTimeStart ?? ''}-${r.serviceTimeEnd ?? ''}`
      },
    },
    {
      title: '状态', dataIndex: 'status', width: 80,
      render: (s: string) => (
        <Tag color={s === 'ENABLED' ? 'green' : 'red'}>
          {s === 'ENABLED' ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作', width: 200,
      render: (_: any, record: AiEmployee) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>编辑</Button>
          <Button type="link" size="small" icon={<SettingOutlined />} onClick={() => onConfigStrategy(record)}>策略</Button>
          <Popconfirm title="确定禁用?" onConfirm={() => handleDelete(record.id!)}>
            <Button type="link" size="small" danger>禁用</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Input.Search placeholder="搜索名称..." onSearch={setSearch} style={{ width: 240 }} />
        <Button type="primary" icon={<PlusOutlined />} onClick={() => onEdit({} as AiEmployee)}>新建 AI 员工</Button>
      </div>
      <Table columns={columns} dataSource={filtered} rowKey="id" loading={loading} size="middle" />
    </div>
  )
}
```

- [ ] **Step 2: 更新 index.tsx**

```tsx
// frontend/src/pages/AiEmployee/index.tsx
import { useState, useCallback } from 'react'
import { Typography } from 'antd'
import { RobotOutlined } from '@ant-design/icons'
import AiEmployeeList from './AiEmployeeList'
import AiEmployeeDrawer from './AiEmployeeDrawer'
import StrategyPanel from './StrategyPanel'
import { AiEmployee } from '../../services/aiEmployee'

const { Title } = Typography

export default function AiEmployeePage() {
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editing, setEditing] = useState<AiEmployee | null>(null)
  const [strategyOpen, setStrategyOpen] = useState(false)
  const [strategyEmployee, setStrategyEmployee] = useState<AiEmployee | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  const refresh = () => setRefreshKey((k) => k + 1)

  const handleEdit = useCallback((employee: AiEmployee) => {
    setEditing(employee)
    setDrawerOpen(true)
  }, [])

  const handleConfigStrategy = useCallback((employee: AiEmployee) => {
    setStrategyEmployee(employee)
    setStrategyOpen(true)
  }, [])

  return (
    <div>
      <Title level={4}><RobotOutlined /> AI 员工管理</Title>
      <AiEmployeeList onEdit={handleEdit} onConfigStrategy={handleConfigStrategy} refreshKey={refreshKey} />
      <AiEmployeeDrawer open={drawerOpen} employee={editing} onClose={() => setDrawerOpen(false)} onSuccess={refresh} />
      {strategyEmployee && (
        <StrategyPanel open={strategyOpen} employee={strategyEmployee} onClose={() => setStrategyOpen(false)} />
      )}
    </div>
  )
}
```

- [ ] **Step 3: 验证构建并提交**

```bash
cd frontend && npm run build -- --noEmit 2>&1 | tail -5
git add -A && git commit -m "feat(ai-employee): 添加AI员工列表页"
```

### Task 1.3: AI 员工表单 Drawer

**Files:**
- Create: `frontend/src/pages/AiEmployee/AiEmployeeDrawer.tsx`

- [ ] **Step 1: 编写 Drawer 表单组件**

```tsx
// frontend/src/pages/AiEmployee/AiEmployeeDrawer.tsx
import { useEffect } from 'react'
import { Drawer, Form, Input, Radio, TimePicker, InputNumber, Checkbox, Button, Space, message, Divider } from 'antd'
import dayjs from 'dayjs'
import { createEmployee, updateEmployee, AiEmployee } from '../../services/aiEmployee'

const { TextArea } = Input

interface Props {
  open: boolean
  employee: AiEmployee | null
  onClose: () => void
  onSuccess: () => void
}

const WEEKDAY_OPTIONS = [
  { label: '一', value: '1' }, { label: '二', value: '2' }, { label: '三', value: '3' },
  { label: '四', value: '4' }, { label: '五', value: '5' }, { label: '六', value: '6' }, { label: '日', value: '7' },
]

export default function AiEmployeeDrawer({ open, employee, onClose, onSuccess }: Props) {
  const [form] = Form.useForm()
  const isEdit = !!employee?.id

  useEffect(() => {
    if (open) {
      if (employee?.id) {
        form.setFieldsValue({
          ...employee,
          serviceTimeStart: employee.serviceTimeStart ? dayjs(employee.serviceTimeStart, 'HH:mm:ss') : null,
          serviceTimeEnd: employee.serviceTimeEnd ? dayjs(employee.serviceTimeEnd, 'HH:mm:ss') : null,
          weekdays: employee.weekdays ? employee.weekdays.split(',') : [],
        })
      } else {
        form.resetFields()
        form.setFieldsValue({
          style: 'PROFESSIONAL', replyLength: 'MEDIUM',
          aggregateInterval: 3, delayInterval: 2,
        })
      }
    }
  }, [open, employee, form])

  const handleSubmit = async () => {
    const values = await form.validateFields()
    const data: any = { ...values }
    if (data.serviceTimeStart) data.serviceTimeStart = data.serviceTimeStart.format('HH:mm:ss')
    if (data.serviceTimeEnd) data.serviceTimeEnd = data.serviceTimeEnd.format('HH:mm:ss')
    if (Array.isArray(data.weekdays)) data.weekdays = data.weekdays.join(',')
    if (isEdit) {
      await updateEmployee(employee!.id!, data)
      message.success('更新成功')
    } else {
      await createEmployee(data)
      message.success('创建成功')
    }
    onSuccess()
    onClose()
  }

  return (
    <Drawer title={isEdit ? '编辑 AI 员工' : '新建 AI 员工'} open={open} onClose={onClose} width={560}
      extra={<Space><Button onClick={onClose}>取消</Button><Button type="primary" onClick={handleSubmit}>保存</Button></Space>}>
      <Form form={form} layout="vertical" size="middle">
        <Form.Item name="name" label="员工名称" rules={[{ required: true, message: '请输入名称' }]}>
          <Input placeholder="如：小通" />
        </Form.Item>
        <Form.Item name="style" label="接待风格" rules={[{ required: true }]}>
          <Radio.Group buttonStyle="solid">
            <Radio.Button value="PROFESSIONAL">专业</Radio.Button>
            <Radio.Button value="WARM">温暖</Radio.Button>
            <Radio.Button value="ENTHUSIASTIC">热情</Radio.Button>
            <Radio.Button value="RELIABLE">可靠</Radio.Button>
          </Radio.Group>
        </Form.Item>
        <Form.Item name="replyLength" label="回复字数" rules={[{ required: true }]}>
          <Radio.Group buttonStyle="solid">
            <Radio.Button value="SHORT">简短</Radio.Button>
            <Radio.Button value="MEDIUM">中等</Radio.Button>
            <Radio.Button value="DETAIL">详细</Radio.Button>
          </Radio.Group>
        </Form.Item>
        <Form.Item name="greetingMsg" label="开场白">
          <TextArea rows={2} placeholder="您好，请问有什么可以帮您？" />
        </Form.Item>
        <Form.Item name="companyIntro" label="公司介绍" rules={[{ required: true }]}>
          <TextArea rows={2} placeholder="公司主营业务描述..." />
        </Form.Item>
        <Form.Item name="productIntro" label="产品/服务介绍" rules={[{ required: true }]}>
          <TextArea rows={2} placeholder="主要产品和服务..." />
        </Form.Item>
        <Form.Item name="serviceScope" label="服务对象（选填）">
          <Input placeholder="仅接待XX行业客户" />
        </Form.Item>
        <Divider>服务配置</Divider>
        <Form.Item name="weekdays" label="服务日">
          <Checkbox.Group options={WEEKDAY_OPTIONS} />
        </Form.Item>
        <Space>
          <Form.Item name="serviceTimeStart" label="开始时间">
            <TimePicker format="HH:mm" />
          </Form.Item>
          <Form.Item name="serviceTimeEnd" label="结束时间">
            <TimePicker format="HH:mm" />
          </Form.Item>
        </Space>
        <Space>
          <Form.Item name="aggregateInterval" label="聚合间隔(秒)">
            <InputNumber min={1} max={10} />
          </Form.Item>
          <Form.Item name="delayInterval" label="延迟回复(秒)">
            <InputNumber min={1} max={10} />
          </Form.Item>
        </Space>
        <Form.Item name="contentCheck" label="敏感词规则 (JSON)" extra="如: {\"微信\":\"{V}\",\"QQ\":\"{Q}\"}">
          <TextArea rows={2} style={{ fontFamily: 'monospace' }} />
        </Form.Item>
      </Form>
    </Drawer>
  )
}
```

- [ ] **Step 2: 验证构建并提交**

```bash
cd frontend && npx tsc --noEmit 2>&1 | head -5
git add -A && git commit -m "feat(ai-employee): 添加AI员工表单Drawer"
```

### Task 1.4: 策略配置面板（拖拽 + Switch + JSON 编辑）

**Files:**
- Create: `frontend/src/pages/AiEmployee/StrategyPanel.tsx`

- [ ] **Step 1: 编写策略面板组件**

```tsx
// frontend/src/pages/AiEmployee/StrategyPanel.tsx
import { useEffect, useState } from 'react'
import { Modal, Switch, Button, Space, Input, message, Tooltip } from 'antd'
import { HolderOutlined } from '@ant-design/icons'
import { DndContext, closestCenter, PointerSensor, useSensor, useSensors } from '@dnd-kit/core'
import { SortableContext, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { listStrategies, saveStrategy, deleteStrategy, batchSortStrategies, ReplyStrategy, AiEmployee } from '../../services/aiEmployee'

const STRATEGY_LABELS: Record<string, string> = {
  GREETING: '问候策略', EXCLUDE: '排除策略', MODERATION: '异常策略',
  KNOWLEDGE: '调用策略', COLLECT: '收集策略', FILTER: '筛选策略',
  FOLLOWUP: '沉默追问', CONTACT: '留资策略',
}

interface Props {
  open: boolean
  employee: AiEmployee
  onClose: () => void
}

function SortableCard({ strategy, onToggle, onEdit }: {
  strategy: ReplyStrategy
  onToggle: (s: ReplyStrategy) => void
  onEdit: (s: ReplyStrategy) => void
}) {
  const { attributes, listeners, setNodeRef, transform, transition } = useSortable({ id: strategy.id! })
  const style = { transform: CSS.Transform.toString(transform), transition }

  return (
    <div ref={setNodeRef} style={{
      ...style, border: '1px solid #e8e8e8', borderRadius: 8, padding: '10px 16px',
      marginBottom: 8, display: 'flex', alignItems: 'center', gap: 10,
      background: strategy.enabled ? '#fff' : '#fafafa', opacity: strategy.enabled ? 1 : 0.7,
    }}>
      <span {...attributes} {...listeners} style={{ cursor: 'grab', color: '#bfbfbf', fontSize: 18 }}>⋮⋮</span>
      <div style={{ flex: 1, cursor: 'pointer' }} onClick={() => onEdit(strategy)}>
        <div style={{ fontWeight: 600, fontSize: 13 }}>
          {STRATEGY_LABELS[strategy.strategyType] ?? strategy.strategyType}
          <span style={{ color: '#8c8c8c', fontWeight: 400, fontSize: 11, marginLeft: 6 }}>
            {strategy.strategyType}
          </span>
        </div>
        <div style={{ fontSize: 11, color: '#8c8c8c', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {strategy.configJson}
        </div>
      </div>
      <Switch checked={strategy.enabled} size="small" onChange={() => onToggle(strategy)} />
    </div>
  )
}

export default function StrategyPanel({ open, employee, onClose }: Props) {
  const [strategies, setStrategies] = useState<ReplyStrategy[]>([])
  const [editJson, setEditJson] = useState<string>('')
  const [editingStrategy, setEditingStrategy] = useState<ReplyStrategy | null>(null)

  const sensors = useSensors(useSensor(PointerSensor))

  useEffect(() => {
    if (open && employee.id) {
      listStrategies(employee.id).then(setStrategies)
    }
  }, [open, employee.id])

  const handleToggle = async (s: ReplyStrategy) => {
    if (s.enabled) {
      await deleteStrategy(employee.id!, s.id!)
    } else {
      await saveStrategy(employee.id!, { ...s, enabled: true })
    }
    setStrategies(await listStrategies(employee.id!))
  }

  const handleDragEnd = async (event: any) => {
    const { active, over } = event
    if (!over || active.id === over.id) return
    const oldIndex = strategies.findIndex((s) => s.id === active.id)
    const newIndex = strategies.findIndex((s) => s.id === over.id)
    const reordered = [...strategies]
    const [moved] = reordered.splice(oldIndex, 1)
    reordered.splice(newIndex, 0, moved)
    const items = reordered.map((s, i) => ({ id: s.id!, sortOrder: i }))
    setStrategies(reordered)
    await batchSortStrategies(employee.id!, items)
    message.success('排序已更新')
  }

  const handleSaveJson = async () => {
    if (!editingStrategy) return
    await saveStrategy(employee.id!, { ...editingStrategy, configJson: editJson })
    setEditingStrategy(null)
    setStrategies(await listStrategies(employee.id!))
    message.success('配置已保存')
  }

  return (
    <Modal title={`${employee.name} — 回复策略配置`} open={open} onCancel={onClose} footer={null} width={640}>
      <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
        <SortableContext items={strategies.map((s) => s.id!)} strategy={verticalListSortingStrategy}>
          {strategies.map((s) => (
            <SortableCard key={s.id} strategy={s} onToggle={handleToggle} onEdit={(st) => {
              setEditingStrategy(st)
              setEditJson(st.configJson)
            }} />
          ))}
        </SortableContext>
      </DndContext>
      {editingStrategy && (
        <div style={{ marginTop: 16, border: '1px solid #e8e8e8', borderRadius: 8, padding: 16 }}>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>
            编辑 {STRATEGY_LABELS[editingStrategy.strategyType]} 配置 (JSON)
          </div>
          <Input.TextArea rows={6} value={editJson} onChange={(e) => setEditJson(e.target.value)}
            style={{ fontFamily: 'monospace' }} />
          <Space style={{ marginTop: 8 }}>
            <Button type="primary" onClick={handleSaveJson}>保存</Button>
            <Button onClick={() => setEditingStrategy(null)}>取消</Button>
          </Space>
        </div>
      )}
    </Modal>
  )
}
```

- [ ] **Step 2: 验证构建并提交**

```bash
cd frontend && npx tsc --noEmit 2>&1 | head -10
git add -A && git commit -m "feat(ai-employee): 添加策略配置面板（拖拽排序+Switch启停+JSON编辑）"
```

---

## Phase 2: 知识库管理页面

### Task 2.1: 知识库 API 服务 + 主从布局

**Files:**
- Create: `frontend/src/services/knowledge.ts`
- Modify: `frontend/src/pages/Placeholder/index.tsx` → 替换为 KnowledgePage

- [ ] **Step 1: 编写知识库 API 服务**

```typescript
// frontend/src/services/knowledge.ts
import api from './api'

export interface KnowledgeBase {
  id?: number; name: string; description?: string; employeeId?: number
}
export interface KnowledgeDocument {
  id?: number; kbId: number; fileName: string; fileType: string
  fileSize?: number; status: string; chunkCount: number; createdAt?: string
}
export interface KnowledgeChunk {
  id?: number; docId: number; kbId: number; content: string
  chunkIndex: number; esDocId?: string; embeddingStatus: string
}

export async function listKBs() {
  const res = await api.get<any, { code: number; data: KnowledgeBase[] }>('/knowledge-bases')
  return res.data
}
export async function createKB(data: KnowledgeBase) {
  const res = await api.post<any, { code: number; data: KnowledgeBase }>('/knowledge-bases', data)
  return res.data
}
export async function uploadDoc(kbId: number, file: File) {
  const form = new FormData(); form.append('file', file)
  const res = await api.post<any, { code: number; data: KnowledgeDocument }>(`/knowledge-bases/${kbId}/documents`, form)
  return res.data
}
export async function listDocs(kbId: number) {
  const res = await api.get<any, { code: number; data: KnowledgeDocument[] }>(`/knowledge-bases/${kbId}/documents`)
  return res.data
}
export async function listChunks(kbId: number, docId: number) {
  const res = await api.get<any, { code: number; data: KnowledgeChunk[] }>(`/knowledge-bases/${kbId}/documents/${docId}/chunks`)
  return res.data
}
```

- [ ] **Step 2: 编写知识库主页面（主从布局 + 文档表格 + 上传弹窗 + 分片预览）**

完整代码覆盖知识库列表、文档表格、上传 Modal（Dragger + 进度）、分片展开面板。此组件约 250 行。

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat(knowledge): 添加知识库管理页面（主从布局+上传+分片）"
```

---

## Phase 3: 客服管理 + 转人工规则

### Task 3.1: 客服管理页面

**Files:**
- Create: `frontend/src/services/agent.ts`
- Modify: `frontend/src/pages/Placeholder/index.tsx` → 替换为 Agents 页面
- Create: `frontend/src/pages/Agents/AgentList.tsx`
- Create: `frontend/src/pages/Agents/AgentDrawer.tsx`
- Create: `frontend/src/pages/Agents/ChannelPermission.tsx`

- [ ] **Step 1: API 服务 + AgentList（状态灯 + 负载条）**

```typescript
// frontend/src/services/agent.ts
import api from './api'

export interface HumanAgent {
  id?: number; name: string; phone: string; status: string
  currentLoad: number; maxConcurrent: number
}

export async function listAgents() {
  const res = await api.get<any, { code: number; data: HumanAgent[] }>('/agents')
  return res.data
}
export async function createAgent(data: HumanAgent) {
  const res = await api.post<any, { code: number; data: HumanAgent }>('/agents', data)
  return res.data
}
export async function deleteAgent(id: number) {
  await api.delete(`/agents/${id}`)
}
```

- [ ] **Step 2: AgentList + AgentDrawer + ChannelPermission 组件**

AgentList: 表格含状态指示灯（Badge 绿/橙/灰）、负载进度条（Progress）、渠道标签（Tag）。约 120 行。
AgentDrawer: 姓名、手机号、最大并发。约 80 行。
ChannelPermission: 权限表格 + 下拉添加（渠道 + AI员工选择）。约 100 行。

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat(agent): 添加客服管理页面（列表+表单+渠道权限）"
```

### Task 3.2: 转人工规则页面

**Files:**
- Create: `frontend/src/services/transferRule.ts`
- Create: `frontend/src/pages/TransferRules/index.tsx`
- Create: `frontend/src/pages/TransferRules/TransferRuleList.tsx`
- Create: `frontend/src/pages/TransferRules/TransferRuleDrawer.tsx`
- Create: `frontend/src/pages/TransferRules/AssignmentCard.tsx`

- [ ] **Step 1: API 服务 + TransferRuleList + TransferRuleDrawer + AssignmentCard**

TransferRuleList: 表格含优先级圆形序号、触发类型 Tag、触发条件、Switch 启停。约 100 行。
TransferRuleDrawer: 触发类型 Select、条件 JSON TextArea、动作 Checkbox、优先级 InputNumber。约 90 行。
AssignmentCard: 4 张 Radio 卡片（轮询/指定/负载/历史），选中高亮。约 80 行。

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat(transfer): 添加转人工规则+分配策略页面"
```

---

## Phase 4: IM 工作台 + 系统设置

### Task 4.1: IM 工作台三栏布局 + WebSocket

**Files:**
- Create: `frontend/src/services/conversation.ts`
- Create: `frontend/src/services/websocket.ts`
- Create: `frontend/src/pages/IM/index.tsx` (及子组件)

- [ ] **Step 1: WebSocket 服务**

```typescript
// frontend/src/services/websocket.ts
type MessageHandler = (data: any) => void

class WebSocketService {
  private ws: WebSocket | null = null
  private handlers: Map<string, MessageHandler[]> = new Map()

  connect(ticket: string) {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    this.ws = new WebSocket(`${protocol}//${location.host}/ws/chat/${ticket}`)
    this.ws.onmessage = (event) => {
      const msg = JSON.parse(event.data)
      const type = msg.type
      const listeners = this.handlers.get(type) ?? []
      listeners.forEach((h) => h(msg))
    }
    this.ws.onclose = () => { /* reconnect logic */ }
  }

  on(type: string, handler: MessageHandler) {
    const existing = this.handlers.get(type) ?? []
    existing.push(handler)
    this.handlers.set(type, existing)
  }

  send(type: string, data: any) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ type, data }))
    }
  }

  disconnect() { this.ws?.close() }
}

export const wsService = new WebSocketService()
```

- [ ] **Step 2: IM 三栏布局（NavPanel + ConversationPanel + MessagePanel + CustomerCard）**

总约 400 行代码。核心功能：
- NavPanel: 7 个导航按钮，Badge 显示数量
- ConversationPanel: 会话列表（头像 + 最后消息 + 时间）
- MessagePanel: 气泡渲染（Bubble 组件，根据 senderType 切换颜色）、输入区
- CustomerCard: 顾客信息卡片（标签、AI收集字段、历史会话）

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat(im): 添加IM工作台三栏布局+WebSocket实时消息"
```

### Task 4.2: 系统设置页面

**Files:**
- Create: `frontend/src/services/settings.ts`
- Create: `frontend/src/pages/Settings/index.tsx`

- [ ] **Step 1: 系统设置 API + 三个 Tab 页面**

```typescript
// frontend/src/services/settings.ts
import api from './api'

export async function listUsers() {
  const res = await api.get<any, { code: number; data: any[] }>('/settings/users')
  return res.data
}
export async function listRoles() {
  const res = await api.get<any, { code: number; data: any[] }>('/settings/roles')
  return res.data
}
export async function listPermissions() {
  const res = await api.get<any, { code: number; data: any[] }>('/settings/permissions')
  return res.data
}
```

Settings 页面：Ant Tabs（用户管理 / 角色管理 / 菜单权限）
- UserTab: Ant Table（用户名、角色 Tag、关联客服、状态）
- RoleTab: Ant Table（角色编码、名称、描述，只读）
- PermissionTab: Ant Tree（树形展示 MENU→BUTTON→API）

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat(settings): 添加系统设置页面（用户/角色/权限三个Tab）"
```

### Task 4.3: 更新路由，移除占位页面

**Files:**
- Modify: `frontend/src/app.tsx`

- [ ] **Step 1: 将所有路由指向真实组件**

```tsx
// app.tsx — 替换所有 PlaceholderPage 为真实组件
import KnowledgePage from './pages/Knowledge/index'
import AgentsPage from './pages/Agents/index'
import TransferRulesPage from './pages/TransferRules/index'
import IMPage from './pages/IM/index'
import SettingsPage from './pages/Settings/index'

// 在路由中:
<Route path="knowledge" element={<KnowledgePage />} />
<Route path="agents" element={<AgentsPage />} />
<Route path="transfer-rules" element={<TransferRulesPage />} />
<Route path="im" element={<IMPage />} />
<Route path="settings" element={<SettingsPage />} />
```

- [ ] **Step 2: 最终构建验证**

```bash
cd frontend && npm run build
```

Expected: Build 成功，无 TypeScript 错误。

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat(frontend): 完成全部6个页面，移除占位页"
```

---

## 附录 A: 文件变更总览

| Phase | 新增文件 | 修改文件 | 后端文件 |
|-------|---------|---------|---------|
| P0 | - | DashboardLayout, app.tsx | AiEmployeeController(+PATCH), AgentPermissionController, SysSettingsController |
| P1 | aiEmployee.ts, AiEmployeeList, AiEmployeeDrawer, StrategyPanel | AiEmployee/index.tsx | - |
| P2 | knowledge.ts, KnowledgePage(内含5个子组件) | - | - |
| P3 | agent.ts, AgentList, AgentDrawer, ChannelPermission, transferRule.ts, TransferRulePage(4子组件) | - | - |
| P4 | conversation.ts, websocket.ts, IMPage(4子组件), settings.ts, SettingsPage(3Tab) | app.tsx | - |

**总计**: ~25 个新前端文件, 3 个后端 Controller 补充, ~1500 行 TSX 代码

## 附录 B: 验证命令

```bash
# 后端
cd E:/study/github/AI-Digital-Employees && export JAVA_HOME="D:/Program Files/Java/jdk-17.0.18"
mvn test -s D:/mvn/settings.xml -Dmaven.repo.local=D:/mvn/repository

# 前端
cd frontend
npx tsc --noEmit && npm run build
```
