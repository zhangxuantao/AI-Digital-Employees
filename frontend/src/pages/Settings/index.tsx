import { useEffect, useState, useCallback } from 'react'
import { Tabs, Table, Tree, Tag, Spin, Typography, Empty, message } from 'antd'
import { SettingOutlined, UserOutlined, TeamOutlined, SafetyOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { DataNode } from 'antd/es/tree'
import { listUsers, listRoles, listPermissions } from '../../services/settings'

const { Title } = Typography

/* ───────── Helpers ───────── */

function buildTree(permissions: any[]): DataNode[] {
  const map = new Map<number, DataNode & { parentId?: number | null }>()
  const roots: DataNode[] = []

  permissions.forEach((p) => {
    const typeLabel: Record<string, string> = { MENU: '菜单', BUTTON: '按钮', API: '接口' }
    map.set(p.id, {
      ...p,
      key: p.id,
      title: (
        <span>
          {p.name}
          {p.type && (
            <Tag style={{ marginLeft: 8 }} color={p.type === 'MENU' ? 'blue' : p.type === 'BUTTON' ? 'green' : 'orange'}>
              {typeLabel[p.type] ?? p.type}
            </Tag>
          )}
          {p.code && <span style={{ marginLeft: 8, color: '#999', fontSize: 12 }}>({p.code})</span>}
        </span>
      ),
      children: [],
      parentId: p.parentId,
    })
  })

  permissions.forEach((p) => {
    const node = map.get(p.id)
    if (node && p.parentId && map.has(p.parentId)) {
      map.get(p.parentId)!.children!.push(node)
    } else if (node && !p.parentId) {
      roots.push(node)
    }
  })

  return roots
}

/* ───────── UserTab ───────── */

function UserTab() {
  const [data, setData] = useState<any[]>([])
  const [loading, setLoading] = useState(false)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      setData(await listUsers())
    } catch (e: any) {
      message.error(e?.message ?? '获取用户列表失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchData() }, [fetchData])

  const columns: ColumnsType<any> = [
    {
      title: '用户名',
      dataIndex: 'username',
      width: 160,
    },
    {
      title: '角色',
      dataIndex: 'roleName',
      width: 140,
      render: (roleName: string) => (roleName ? <Tag color="blue">{roleName}</Tag> : '-'),
    },
    {
      title: '关联客服',
      dataIndex: 'agentName',
      width: 140,
      render: (name: string) => name ?? '-',
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 100,
      render: (enabled: boolean) => (
        <Tag color={enabled ? 'success' : 'error'}>{enabled ? '启用' : '禁用'}</Tag>
      ),
    },
  ]

  return (
    <Spin spinning={loading}>
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        size="middle"
        pagination={{ pageSize: 10 }}
        locale={{ emptyText: <Empty description="暂无用户数据" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
      />
    </Spin>
  )
}

/* ───────── RoleTab ───────── */

function RoleTab() {
  const [data, setData] = useState<any[]>([])
  const [loading, setLoading] = useState(false)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      setData(await listRoles())
    } catch (e: any) {
      message.error(e?.message ?? '获取角色列表失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchData() }, [fetchData])

  const columns: ColumnsType<any> = [
    {
      title: '角色编码',
      dataIndex: 'code',
      width: 160,
    },
    {
      title: '角色名称',
      dataIndex: 'name',
      width: 200,
    },
    {
      title: '描述',
      dataIndex: 'description',
      render: (desc: string) => desc ?? '-',
    },
  ]

  return (
    <Spin spinning={loading}>
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        size="middle"
        pagination={{ pageSize: 10 }}
        locale={{ emptyText: <Empty description="暂无角色数据" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
      />
    </Spin>
  )
}

/* ───────── PermissionTab ───────── */

function PermissionTab() {
  const [treeData, setTreeData] = useState<DataNode[]>([])
  const [loading, setLoading] = useState(false)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const perms = await listPermissions()
      setTreeData(buildTree(perms))
    } catch (e: any) {
      message.error(e?.message ?? '获取权限列表失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchData() }, [fetchData])

  return (
    <Spin spinning={loading}>
      {treeData.length === 0 && !loading ? (
        <Empty description="暂无权限数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      ) : (
        <Tree
          treeData={treeData}
          defaultExpandAll
          showLine
          style={{ maxHeight: 600, overflow: 'auto' }}
        />
      )}
    </Spin>
  )
}

/* ───────── Main Page ───────── */

export default function SettingsPage() {
  return (
    <div>
      <Title level={4}><SettingOutlined /> 系统设置</Title>
      <Tabs
        defaultActiveKey="users"
        items={[
          {
            key: 'users',
            label: (
              <span><UserOutlined /> 用户管理</span>
            ),
            children: <UserTab />,
          },
          {
            key: 'roles',
            label: (
              <span><TeamOutlined /> 角色管理</span>
            ),
            children: <RoleTab />,
          },
          {
            key: 'permissions',
            label: (
              <span><SafetyOutlined /> 菜单权限</span>
            ),
            children: <PermissionTab />,
          },
        ]}
      />
    </div>
  )
}
