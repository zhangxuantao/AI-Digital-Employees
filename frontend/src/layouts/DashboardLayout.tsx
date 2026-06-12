import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Button, Typography, Dropdown, theme } from 'antd'
import {
  RobotOutlined,
  BookOutlined,
  TeamOutlined,
  MessageOutlined,
  DashboardOutlined,
  LogoutOutlined,
  UserOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import { useAuthStore } from '../stores/authStore'

const { Header, Sider, Content } = Layout
const { Text } = Typography

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '数据看板', permission: 'dashboard:view' },
  { key: '/ai-employee', icon: <RobotOutlined />, label: 'AI 员工', permission: 'ai_employee:view' },
  { key: '/knowledge', icon: <BookOutlined />, label: '知识库', permission: 'knowledge:view' },
  { key: '/agents', icon: <TeamOutlined />, label: '客服管理', permission: 'agent:view' },
  { key: '/im', icon: <MessageOutlined />, label: 'IM 工作台', permission: 'im:access' },
]

export default function DashboardLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { username, roleCode, permissions, logout } = useAuthStore()
  const { token: themeToken } = theme.useToken()

  const hasPermission = (code: string) => permissions.includes(code)
  const filteredMenu = menuItems.filter((item) => hasPermission(item.permission))

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  const userMenu = {
    items: [
      {
        key: 'role',
        label: `角色: ${roleCode === 'ADMIN' ? '管理员' : roleCode === 'LEADER' ? '主管' : '客服'}`,
        disabled: true,
      },
      { type: 'divider' as const },
      { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: handleLogout },
    ],
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        breakpoint="lg"
        collapsedWidth="0"
        style={{ background: themeToken.colorBgContainer }}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderBottom: `1px solid ${themeToken.colorBorderSecondary}`,
          }}
        >
          <RobotOutlined style={{ fontSize: 24, color: themeToken.colorPrimary, marginRight: 8 }} />
          <Text strong style={{ fontSize: 16 }}>AI 客服</Text>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={filteredMenu}
          onClick={({ key }) => navigate(key)}
          style={{ borderRight: 0 }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 24px',
            background: themeToken.colorBgContainer,
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            borderBottom: `1px solid ${themeToken.colorBorderSecondary}`,
          }}
        >
          <Dropdown menu={userMenu} placement="bottomRight">
            <Button type="text" icon={<UserOutlined />}>
              {username}
            </Button>
          </Dropdown>
        </Header>
        <Content style={{ margin: 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
