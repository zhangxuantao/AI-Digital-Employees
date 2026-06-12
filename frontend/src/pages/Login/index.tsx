import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Form, Input, Button, Card, Typography, message, Space } from 'antd'
import { UserOutlined, LockOutlined, RobotOutlined } from '@ant-design/icons'
import { login } from '../../services/auth'
import { useAuthStore } from '../../stores/authStore'

const { Title, Text } = Typography

export default function LoginPage() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true)
    try {
      const data = await login(values)
      setAuth({
        token: data.token,
        username: data.username,
        roleCode: data.roleCode,
        agentId: data.agentId,
        permissions: data.permissions,
      })
      message.success(`欢迎回来，${data.username}！`)
      navigate('/dashboard', { replace: true })
    } catch {
      message.error('用户名或密码错误')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      }}
    >
      <Card
        style={{ width: 400, borderRadius: 12, boxShadow: '0 8px 40px rgba(0,0,0,0.2)' }}
        bodyStyle={{ padding: 40 }}
      >
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div style={{ textAlign: 'center' }}>
            <RobotOutlined
              style={{ fontSize: 48, color: '#667eea', marginBottom: 16 }}
            />
            <Title level={3} style={{ margin: 0 }}>
              AI 智能客服系统
            </Title>
            <Text type="secondary">登录管理后台</Text>
          </div>

          <Form
            name="login"
            onFinish={onFinish}
            initialValues={{ username: 'admin', password: 'admin123' }}
            size="large"
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input prefix={<UserOutlined />} placeholder="用户名" />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="密码" />
            </Form.Item>

            <Form.Item style={{ marginBottom: 0 }}>
              <Button type="primary" htmlType="submit" loading={loading} block>
                登录
              </Button>
            </Form.Item>
          </Form>

          <Text
            type="secondary"
            style={{ display: 'block', textAlign: 'center', fontSize: 12 }}
          >
            默认账号：admin / admin123
          </Text>
        </Space>
      </Card>
    </div>
  )
}
