import { Card, Typography } from 'antd'
import { RobotOutlined } from '@ant-design/icons'

const { Title, Paragraph } = Typography

export default function AiEmployeePage() {
  return (
    <div>
      <Title level={4}>
        <RobotOutlined /> AI 员工管理
      </Title>
      <Card>
        <Paragraph>AI 员工列表和配置功能将在后续版本中实现。</Paragraph>
        <Paragraph type="secondary">
          您可以在这里创建和管理 AI 员工，配置回复策略、知识库关联、服务时段等。
        </Paragraph>
      </Card>
    </div>
  )
}
