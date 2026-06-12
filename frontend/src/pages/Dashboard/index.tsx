import { useEffect, useState } from 'react'
import { Row, Col, Card, Statistic, Typography, Table, Spin } from 'antd'
import { TeamOutlined, MessageOutlined, ClockCircleOutlined, CheckCircleOutlined } from '@ant-design/icons'
import api from '../../services/api'

const { Title } = Typography

interface RealtimeStats {
  activeConversations: number
  waitingCount: number
  queuedCount: number
  totalOnline: number
}

interface QuestionRank {
  question: string
  count: number
}

export default function DashboardPage() {
  const [stats, setStats] = useState<RealtimeStats | null>(null)
  const [questions, setQuestions] = useState<QuestionRank[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api
      .get<any, { code: number; data: { realtime: RealtimeStats; topQuestions: QuestionRank[] } }>(
        '/analytics/overview'
      )
      .then((res) => {
        if (res.code === 0) {
          setStats(res.data.realtime)
          setQuestions(res.data.topQuestions)
        }
      })
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />

  return (
    <div>
      <Title level={4} style={{ marginBottom: 24 }}>
        数据概览
      </Title>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic
              title="在线会话"
              value={stats?.totalOnline ?? 0}
              prefix={<MessageOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic
              title="等待中"
              value={stats?.waitingCount ?? 0}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic
              title="排队中"
              value={stats?.queuedCount ?? 0}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic
              title="客服在线"
              value={stats?.totalOnline ?? 0}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      <Card title="高频问题 TOP5">
        <Table
          dataSource={questions.map((q, i) => ({ ...q, key: i, rank: i + 1 }))}
          columns={[
            { title: '排名', dataIndex: 'rank', width: 80 },
            { title: '问题', dataIndex: 'question' },
            { title: '次数', dataIndex: 'count', width: 100 },
          ]}
          pagination={false}
          size="small"
        />
      </Card>
    </div>
  )
}
