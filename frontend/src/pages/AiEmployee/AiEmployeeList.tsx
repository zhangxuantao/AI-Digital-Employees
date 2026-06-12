import { useEffect, useState } from 'react'
import { Table, Button, Input, Space, Tag, Popconfirm, message, Avatar } from 'antd'
import { PlusOutlined, EditOutlined, SettingOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { listEmployees, deleteEmployee } from '../../services/aiEmployee'
import type { AiEmployee } from '../../services/aiEmployee'

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
    try {
      await deleteEmployee(id)
      message.success('已禁用')
      fetchData()
    } catch (e: any) {
      message.error(e?.message ?? '操作失败')
    }
  }

  const filtered = data.filter((e) => !search || e.name?.includes(search))

  const columns: ColumnsType<AiEmployee> = [
    {
      title: '名称', dataIndex: 'name', width: 160,
      render: (name: string) => (
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
        <Input.Search placeholder="搜索名称..." onSearch={setSearch} style={{ width: 240 }} allowClear />
        <Button type="primary" icon={<PlusOutlined />} onClick={() => onEdit({} as AiEmployee)}>新建 AI 员工</Button>
      </div>
      <Table columns={columns} dataSource={filtered} rowKey="id" loading={loading} size="middle" />
    </div>
  )
}
