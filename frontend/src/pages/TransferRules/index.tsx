import { useState, useEffect, useCallback } from 'react'
import {
  Typography, Table, Button, Drawer, Form, Input, InputNumber, Select, Switch,
  Checkbox, Tag, Popconfirm, message, Space, Empty, Card, Radio,
} from 'antd'
import { PlusOutlined, SwapOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  listTransferRules, createTransferRule, updateTransferRule, deleteTransferRule,
  getAssignmentStrategy, updateAssignmentStrategy,
} from '../../services/transferRule'
import type { TransferRule } from '../../services/transferRule'
import { listEmployees } from '../../services/aiEmployee'
import type { AiEmployee } from '../../services/aiEmployee'

const { Title, Text } = Typography
const { TextArea } = Input

/* ───────── Constants ───────── */

const TRIGGER_TYPE_MAP: Record<string, { color: string; label: string }> = {
  KEYWORD: { color: 'blue', label: '关键词' },
  EMOTION: { color: 'volcano', label: '情感' },
  MANUAL: { color: 'orange', label: '手动' },
  TIMERANGE: { color: 'purple', label: '时段' },
}

const TRIGGER_TYPE_OPTIONS = [
  { value: 'KEYWORD', label: '关键词 (KEYWORD)' },
  { value: 'EMOTION', label: '情感 (EMOTION)' },
  { value: 'MANUAL', label: '手动 (MANUAL)' },
  { value: 'TIMERANGE', label: '时段 (TIMERANGE)' },
]

const ACTION_OPTIONS = [
  { value: 'NOTIFY_AGENT', label: '通知客服 (NOTIFY_AGENT)' },
  { value: 'SEND_MESSAGE', label: '发送消息 (SEND_MESSAGE)' },
  { value: 'AUTO_ACCEPT', label: '自动接入 (AUTO_ACCEPT)' },
  { value: 'CLOSE_SESSION', label: '关闭会话 (CLOSE_SESSION)' },
]

const ASSIGNMENT_OPTIONS = [
  {
    value: 'ROUND_ROBIN',
    label: '轮询分配',
    desc: '按顺序轮流分配给在线客服',
  },
  {
    value: 'DESIGNATED',
    label: '指定分配',
    desc: '分配给指定客服',
  },
  {
    value: 'LOAD_BALANCE',
    label: '负载分配',
    desc: '分配给当前负载最低的客服',
  },
  {
    value: 'HISTORY',
    label: '历史分配',
    desc: '优先分配给该客户历史沟通的客服',
  },
]

/* ───────── Transfer Rule Drawer ───────── */

interface TransferRuleDrawerProps {
  open: boolean
  rule: TransferRule | null
  aiEmployees: AiEmployee[]
  onClose: () => void
  onSuccess: () => void
}

function TransferRuleDrawer({ open, rule, aiEmployees, onClose, onSuccess }: TransferRuleDrawerProps) {
  const [form] = Form.useForm()
  const isEdit = !!rule?.id

  useEffect(() => {
    if (open) {
      if (isEdit) {
        const parsedActions = rule!.actions ? rule!.actions.split(',') : []
        form.setFieldsValue({ ...rule!, actions: parsedActions })
      } else {
        form.resetFields()
        form.setFieldsValue({ priority: 10, enabled: true })
      }
    }
  }, [open, rule, form, isEdit])

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      const data = {
        ...values,
        actions: Array.isArray(values.actions) ? values.actions.join(',') : values.actions,
      }
      if (isEdit) {
        await updateTransferRule(rule!.id!, data)
        message.success('更新成功')
      } else {
        await createTransferRule(data)
        message.success('创建成功')
      }
      onSuccess()
      onClose()
    } catch (e: any) {
      if (e?.errorFields) return
      message.error(e?.message ?? '保存失败')
    }
  }

  return (
    <Drawer
      title={isEdit ? '编辑转人工规则' : '新增转人工规则'}
      open={open}
      onClose={onClose}
      width={520}
      extra={
        <Space>
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" onClick={handleSubmit}>保存</Button>
        </Space>
      }
    >
      <Form form={form} layout="vertical" size="middle">
        <Form.Item name="employeeId" label="AI员工" rules={[{ required: true, message: '请选择AI员工' }]}>
          <Select
            placeholder="选择AI员工"
            showSearch
            filterOption={(input, option) =>
              (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
            }
            options={aiEmployees.map((e) => ({ value: e.id!, label: e.name }))}
            notFoundContent={<Empty description="暂无 AI 员工" image={Empty.PRESENTED_IMAGE_SIMPLE} />}
          />
        </Form.Item>
        <Form.Item name="triggerType" label="触发类型" rules={[{ required: true, message: '请选择触发类型' }]}>
          <Select placeholder="选择触发类型" options={TRIGGER_TYPE_OPTIONS} />
        </Form.Item>
        <Form.Item
          name="conditionJson"
          label="触发条件 JSON"
          rules={[{ required: true, message: '请输入触发条件 JSON' }]}
          extra="JSON 格式的触发条件配置"
        >
          <TextArea rows={4} style={{ fontFamily: 'monospace' }} placeholder='{"keywords":["转人工","人工客服"]}' />
        </Form.Item>
        <Form.Item name="actions" label="执行动作">
          <Checkbox.Group options={ACTION_OPTIONS} />
        </Form.Item>
        <Form.Item name="priority" label="优先级" rules={[{ required: true, message: '请设置优先级' }]}>
          <InputNumber min={1} max={100} style={{ width: '100%' }} placeholder="数值越小优先级越高" />
        </Form.Item>
        <Form.Item name="enabled" label="启用" valuePropName="checked">
          <Switch />
        </Form.Item>
      </Form>
    </Drawer>
  )
}

/* ───────── Main Page Component ───────── */

export default function TransferRulesPage() {
  const [rules, setRules] = useState<TransferRule[]>([])
  const [loading, setLoading] = useState(false)
  const [aiEmployees, setAiEmployees] = useState<AiEmployee[]>([])
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<number | undefined>()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editingRule, setEditingRule] = useState<TransferRule | null>(null)
  const [assignmentStrategy, setAssignmentStrategy] = useState<string>('')
  const [savingStrategy, setSavingStrategy] = useState(false)

  /* ─── Fetch AI employees for filter dropdown and drawer ─── */
  const fetchEmployees = useCallback(async () => {
    try {
      const data = await listEmployees()
      setAiEmployees(data)
    } catch {
      // Silent
    }
  }, [])

  useEffect(() => { fetchEmployees() }, [fetchEmployees])

  /* ─── Fetch transfer rules ─── */
  const fetchRules = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listTransferRules(selectedEmployeeId)
      setRules(data)
    } catch (e: any) {
      message.error(e?.message ?? '获取转人工规则失败')
    } finally {
      setLoading(false)
    }
  }, [selectedEmployeeId])

  useEffect(() => { fetchRules() }, [fetchRules])

  /* ─── Fetch assignment strategy ─── */
  const fetchAssignmentStrategy = useCallback(async () => {
    try {
      const data = await getAssignmentStrategy()
      setAssignmentStrategy(data)
    } catch {
      // Default to first option on error
      setAssignmentStrategy('ROUND_ROBIN')
    }
  }, [])

  useEffect(() => { fetchAssignmentStrategy() }, [fetchAssignmentStrategy])

  /* ─── Handlers ─── */
  const handleCreate = useCallback(() => {
    setEditingRule(null)
    setDrawerOpen(true)
  }, [])

  const handleEdit = useCallback((rule: TransferRule) => {
    setEditingRule(rule)
    setDrawerOpen(true)
  }, [])

  const handleDelete = useCallback(async (id: number) => {
    try {
      await deleteTransferRule(id)
      message.success('已删除')
      fetchRules()
    } catch (e: any) {
      message.error(e?.message ?? '操作失败')
    }
  }, [fetchRules])

  const handleToggleEnabled = useCallback(async (record: TransferRule, checked: boolean) => {
    try {
      await updateTransferRule(record.id!, { enabled: checked })
      message.success(checked ? '已启用' : '已禁用')
      fetchRules()
    } catch (e: any) {
      message.error(e?.message ?? '操作失败')
    }
  }, [fetchRules])

  const handleSaveStrategy = useCallback(async () => {
    if (!assignmentStrategy) {
      message.warning('请选择分配策略')
      return
    }
    setSavingStrategy(true)
    try {
      await updateAssignmentStrategy(assignmentStrategy)
      message.success('分配策略已保存')
    } catch (e: any) {
      message.error(e?.message ?? '保存失败')
    } finally {
      setSavingStrategy(false)
    }
  }, [assignmentStrategy])

  /* ─── Table columns ─── */
  const columns: ColumnsType<TransferRule> = [
    {
      title: '优先级',
      dataIndex: 'priority',
      width: 80,
      sorter: (a, b) => a.priority - b.priority,
      render: (p: number) => (
        <div
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 28,
            height: 28,
            borderRadius: '50%',
            backgroundColor: p <= 10 ? '#ff4d4f' : p <= 30 ? '#fa8c16' : p <= 60 ? '#1677ff' : '#8c8c8c',
            color: '#fff',
            fontSize: 12,
            fontWeight: 600,
          }}
        >
          {p}
        </div>
      ),
    },
    {
      title: 'AI员工',
      dataIndex: 'employeeId',
      width: 120,
      render: (id: number) => {
        const emp = aiEmployees.find((e) => e.id === id)
        return emp?.name ?? `#${id}`
      },
    },
    {
      title: '触发类型',
      dataIndex: 'triggerType',
      width: 100,
      render: (t: string) => {
        const m = TRIGGER_TYPE_MAP[t]
        return <Tag color={m?.color}>{m?.label ?? t}</Tag>
      },
    },
    {
      title: '触发条件',
      dataIndex: 'conditionJson',
      ellipsis: true,
      render: (c: string) => (
        <Text copyable style={{ fontFamily: 'monospace', fontSize: 12 }}>{c}</Text>
      ),
    },
    {
      title: '执行动作',
      dataIndex: 'actions',
      ellipsis: true,
      render: (a: string) => {
        if (!a) return <Text type="secondary">无</Text>
        const items = a.split(',')
        return (
          <Space size={4} wrap>
            {items.map((item) => {
              const label = ACTION_OPTIONS.find((o) => o.value === item)?.label ?? item
              return <Tag key={item}>{label}</Tag>
            })}
          </Space>
        )
      },
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 80,
      render: (_: boolean, record: TransferRule) => (
        <Switch
          checked={record.enabled}
          size="small"
          onChange={(checked) => handleToggleEnabled(record, checked)}
        />
      ),
    },
    {
      title: '操作',
      width: 120,
      render: (_: any, record: TransferRule) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确定删除此规则?" onConfirm={() => handleDelete(record.id!)}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  /* ─── Employee filter options ─── */
  const employeeFilterOptions = aiEmployees.map((e) => ({
    value: e.id!,
    label: e.name,
  }))

  return (
    <div>
      <Title level={4}><SwapOutlined /> 转人工规则</Title>

      {/* ─── Transfer Rules Section ─── */}
      <Card style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
          <Space>
            <Text strong>规则列表</Text>
            <Select
              placeholder="全部 AI 员工"
              value={selectedEmployeeId}
              onChange={setSelectedEmployeeId}
              allowClear
              style={{ width: 200 }}
              options={employeeFilterOptions}
              notFoundContent={<Empty description="暂无 AI 员工" image={Empty.PRESENTED_IMAGE_SIMPLE} />}
            />
          </Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新增规则
          </Button>
        </div>
        <Table
          columns={columns}
          dataSource={rules}
          rowKey={(r) => r.id ?? 0}
          loading={loading}
          size="middle"
          locale={{ emptyText: <Empty description="暂无转人工规则，点击上方按钮新增" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
        />
      </Card>

      {/* ─── Assignment Strategy Section ─── */}
      <Card>
        <div style={{ marginBottom: 16 }}>
          <Text strong style={{ fontSize: 16 }}>分配策略</Text>
        </div>
        <Radio.Group
          value={assignmentStrategy}
          onChange={(e) => setAssignmentStrategy(e.target.value)}
          style={{ width: '100%' }}
        >
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 12 }}>
            {ASSIGNMENT_OPTIONS.map((opt) => (
              <Card
                key={opt.value}
                size="small"
                hoverable
                style={{
                  cursor: 'pointer',
                  border: assignmentStrategy === opt.value ? '2px solid #1677ff' : '1px solid #f0f0f0',
                  background: assignmentStrategy === opt.value ? '#e6f4ff' : '#fff',
                }}
                onClick={() => setAssignmentStrategy(opt.value)}
              >
                <Radio value={opt.value} style={{ display: 'flex', alignItems: 'flex-start' }}>
                  <div>
                    <div style={{ fontWeight: 600, marginBottom: 4 }}>{opt.label}</div>
                    <Text type="secondary" style={{ fontSize: 12 }}>{opt.desc}</Text>
                  </div>
                </Radio>
              </Card>
            ))}
          </div>
        </Radio.Group>
        <div style={{ marginTop: 16, textAlign: 'right' }}>
          <Button type="primary" loading={savingStrategy} onClick={handleSaveStrategy}>
            保存策略
          </Button>
        </div>
      </Card>

      {/* ─── Rule Drawer ─── */}
      <TransferRuleDrawer
        open={drawerOpen}
        rule={editingRule}
        aiEmployees={aiEmployees}
        onClose={() => setDrawerOpen(false)}
        onSuccess={fetchRules}
      />
    </div>
  )
}
