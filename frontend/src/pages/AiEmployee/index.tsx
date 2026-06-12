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
