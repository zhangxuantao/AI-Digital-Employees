import { useState, useEffect, useCallback } from 'react'
import {
  Typography, Table, Button, Drawer, Form, Input, InputNumber,
  Popconfirm, message, Badge, Progress, Tag, Space, Select, Empty, Spin,
} from 'antd'
import { PlusOutlined, DeleteOutlined, TeamOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  listAgents, createAgent, updateAgent, deleteAgent,
  listChannelPermissions, addChannelPermission, deleteChannelPermission,
  HumanAgent, AgentChannelPermission,
} from '../../services/agent'
import { listEmployees, AiEmployee } from '../../services/aiEmployee'

const { Title, Text } = Typography

/* ───────── Constants ───────── */

const STATUS_MAP: Record<string, { status: 'success' | 'warning' | 'default'; label: string }> = {
  ONLINE: { status: 'success', label: '在线' },
  BUSY: { status: 'warning', label: '忙碌' },
  OFFLINE: { status: 'default', label: '离线' },
}

const PLATFORM_MAP: Record<string, { color: string; label: string }> = {
  WECHAT: { color: 'green', label: '微信' },
  WEBSITE: { color: 'blue', label: '网站' },
  APP: { color: 'purple', label: 'APP' },
  WECHAT_MP: { color: 'cyan', label: '微信公众号' },
}

const PLATFORM_OPTIONS = [
  { value: 'WECHAT', label: '微信' },
  { value: 'WEBSITE', label: '网站' },
  { value: 'APP', label: 'APP' },
  { value: 'WECHAT_MP', label: '微信公众号' },
]

/* ───────── Channel Permission Section (expandable row) ───────── */

function ChannelPermissionSection({ agentId }: { agentId: number }) {
  const [permissions, setPermissions] = useState<AgentChannelPermission[]>([])
  const [loading, setLoading] = useState(false)
  const [aiEmployees, setAiEmployees] = useState<AiEmployee[]>([])
  const [selectedPlatform, setSelectedPlatform] = useState<string>('WECHAT')
  const [selectedEmployee, setSelectedEmployee] = useState<number | undefined>()
  const [adding, setAdding] = useState(false)

  const fetchPermissions = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listChannelPermissions(agentId)
      setPermissions(data)
    } catch (e: any) {
      message.error(e?.message ?? '获取渠道权限失败')
    } finally {
      setLoading(false)
    }
  }, [agentId])

  const fetchEmployees = useCallback(async () => {
    try {
      const data = await listEmployees()
      setAiEmployees(data)
    } catch {
      // Silently ignore — the dropdown will be empty
    }
  }, [])

  useEffect(() => {
    fetchPermissions()
    fetchEmployees()
  }, [fetchPermissions, fetchEmployees])

  const handleAdd = async () => {
    if (!selectedEmployee) {
      message.warning('请选择 AI 员工')
      return
    }
    setAdding(true)
    try {
      await addChannelPermission(agentId, { platform: selectedPlatform, employeeId: selectedEmployee })
      message.success('添加成功')
      setSelectedEmployee(undefined)
      fetchPermissions()
    } catch (e: any) {
      message.error(e?.message ?? '添加失败')
    } finally {
      setAdding(false)
    }
  }

  const handleDelete = async (permissionId: number) => {
    try {
      await deleteChannelPermission(agentId, permissionId)
      message.success('已删除')
      fetchPermissions()
    } catch (e: any) {
      message.error(e?.message ?? '删除失败')
    }
  }

  const permColumns: ColumnsType<AgentChannelPermission> = [
    {
      title: '平台',
      dataIndex: 'platform',
      width: 120,
      render: (p: string) => {
        const m = PLATFORM_MAP[p]
        return <Tag color={m?.color}>{m?.label ?? p}</Tag>
      },
    },
    {
      title: '关联员工',
      dataIndex: 'employeeName',
      render: (name: string) => name ?? '-',
    },
    {
      title: '操作',
      width: 80,
      render: (_: any, record: AgentChannelPermission) => (
        <Popconfirm title="确定删除此权限?" onConfirm={() => handleDelete(record.id!)}>
          <Button type="link" size="small" danger icon={<DeleteOutlined />} />
        </Popconfirm>
      ),
    },
  ]

  return (
    <div style={{ padding: '8px 0' }}>
      <div style={{ marginBottom: 12, display: 'flex', gap: 8, alignItems: 'center' }}>
        <Select
          value={selectedPlatform}
          onChange={setSelectedPlatform}
          options={PLATFORM_OPTIONS}
          style={{ width: 160 }}
          size="small"
        />
        <Select
          placeholder="选择 AI 员工"
          value={selectedEmployee}
          onChange={setSelectedEmployee}
          style={{ width: 200 }}
          size="small"
          allowClear
          showSearch
          filterOption={(input, option) =>
            (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
          }
          options={aiEmployees.map((e) => ({ value: e.id!, label: e.name }))}
          notFoundContent={<Empty description="暂无 AI 员工" image={Empty.PRESENTED_IMAGE_SIMPLE} />}
        />
        <Button type="primary" size="small" loading={adding} onClick={handleAdd}>
          添加权限
        </Button>
      </div>
      <Spin spinning={loading}>
        {permissions.length === 0 && !loading ? (
          <Empty description="暂无渠道权限" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : (
          <Table
            columns={permColumns}
            dataSource={permissions}
            rowKey="id"
            size="small"
            pagination={false}
            locale={{ emptyText: <Empty description="暂无渠道权限" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
          />
        )}
      </Spin>
    </div>
  )
}

/* ───────── Agent Drawer ───────── */

interface AgentDrawerProps {
  open: boolean
  agent: HumanAgent | null
  onClose: () => void
  onSuccess: () => void
}

function AgentDrawer({ open, agent, onClose, onSuccess }: AgentDrawerProps) {
  const [form] = Form.useForm()
  const isEdit = !!agent?.id

  useEffect(() => {
    if (open) {
      if (isEdit) {
        form.setFieldsValue(agent)
      } else {
        form.resetFields()
        form.setFieldsValue({ maxConcurrent: 10 })
      }
    }
  }, [open, agent, form, isEdit])

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (isEdit) {
        await updateAgent(agent!.id!, values)
        message.success('更新成功')
      } else {
        await createAgent(values)
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
      title={isEdit ? '编辑客服' : '新增客服'}
      open={open}
      onClose={onClose}
      width={480}
      extra={
        <Space>
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" onClick={handleSubmit}>保存</Button>
        </Space>
      }
    >
      <Form form={form} layout="vertical" size="middle">
        <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
          <Input placeholder="请输入客服姓名" />
        </Form.Item>
        <Form.Item name="phone" label="手机号" rules={[{ required: true, message: '请输入手机号' }]}>
          <Input placeholder="请输入手机号" />
        </Form.Item>
        <Form.Item name="maxConcurrent" label="最大并发数" rules={[{ required: true, message: '请设置最大并发数' }]}>
          <InputNumber min={1} max={50} style={{ width: '100%' }} placeholder="建议 10-20" />
        </Form.Item>
      </Form>
    </Drawer>
  )
}

/* ───────── Main Page Component ───────── */

export default function AgentsPage() {
  const [data, setData] = useState<HumanAgent[]>([])
  const [loading, setLoading] = useState(false)
  const [search, setSearch] = useState('')
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editing, setEditing] = useState<HumanAgent | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  const refresh = useCallback(() => setRefreshKey((k) => k + 1), [])

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      setData(await listAgents())
    } catch (e: any) {
      message.error(e?.message ?? '获取客服列表失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchData() }, [refreshKey, fetchData])

  const handleCreate = useCallback(() => {
    setEditing(null)
    setDrawerOpen(true)
  }, [])

  const handleEdit = useCallback((agent: HumanAgent) => {
    setEditing(agent)
    setDrawerOpen(true)
  }, [])

  const handleDelete = useCallback(async (id: number) => {
    try {
      await deleteAgent(id)
      message.success('已删除')
      fetchData()
    } catch (e: any) {
      message.error(e?.message ?? '操作失败')
    }
  }, [fetchData])

  const filtered = data.filter(
    (a) => !search || a.name?.includes(search) || a.phone?.includes(search),
  )

  const columns: ColumnsType<HumanAgent> = [
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (s: string) => {
        const m = STATUS_MAP[s]
        return <Badge status={m?.status ?? 'default'} text={m?.label ?? s} />
      },
    },
    {
      title: '姓名',
      dataIndex: 'name',
      width: 120,
      render: (name: string, record: HumanAgent) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => handleEdit(record)}>
          {name}
        </Button>
      ),
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      width: 140,
    },
    {
      title: '负载',
      width: 160,
      render: (_: any, record: HumanAgent) => (
        <Space>
          <Progress
            percent={Math.round((record.currentLoad / record.maxConcurrent) * 100)}
            size="small"
            style={{ width: 100, margin: 0 }}
          />
          <Text type="secondary" style={{ fontSize: 12 }}>
            {record.currentLoad}/{record.maxConcurrent}
          </Text>
        </Space>
      ),
    },
    {
      title: '渠道权限',
      dataIndex: 'id',
      width: 200,
      render: (_: any, record: HumanAgent) => {
        // Show tags from expandable row data — rendered as inline placeholder
        return <Tag color="default">展开查看</Tag>
      },
    },
    {
      title: '操作',
      width: 80,
      render: (_: any, record: HumanAgent) => (
        <Popconfirm title="确定删除此客服?" onConfirm={() => handleDelete(record.id!)}>
          <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <div>
      <Title level={4}><TeamOutlined /> 客服管理</Title>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Input.Search
          placeholder="搜索姓名或手机号..."
          onSearch={setSearch}
          style={{ width: 240 }}
          allowClear
        />
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新增客服
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={filtered}
        rowKey="id"
        loading={loading}
        size="middle"
        expandable={{
          expandedRowRender: (record) => <ChannelPermissionSection agentId={record.id!} />,
          rowExpandable: (record) => !!record.id,
        }}
        locale={{ emptyText: <Empty description="暂无客服数据" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
      />
      <AgentDrawer
        open={drawerOpen}
        agent={editing}
        onClose={() => setDrawerOpen(false)}
        onSuccess={refresh}
      />
    </div>
  )
}
