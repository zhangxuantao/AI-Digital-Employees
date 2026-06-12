import { Card, Typography } from 'antd'

const { Title, Paragraph } = Typography

interface Props {
  title: string
  icon?: string
  description?: string
}

export default function PlaceholderPage({ title, icon, description }: Props) {
  return (
    <div>
      <Title level={4}>
        {icon} {title}
      </Title>
      <Card>
        <Paragraph>此页面功能将在后续版本中实现。</Paragraph>
        {description && <Paragraph type="secondary">{description}</Paragraph>}
      </Card>
    </div>
  )
}
