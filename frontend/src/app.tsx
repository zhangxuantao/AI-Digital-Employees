import { Routes, Route, Navigate } from 'react-router-dom'
import { App as AntApp } from 'antd'
import LoginPage from './pages/Login/index'
import DashboardLayout from './layouts/DashboardLayout'
import DashboardPage from './pages/Dashboard/index'
import AiEmployeePage from './pages/AiEmployee/index'
import KnowledgePage from './pages/Knowledge/index'
import AgentsPage from './pages/Agents/index'
import TransferRulesPage from './pages/TransferRules/index'
import IMPage from './pages/IM/index'
import SettingsPage from './pages/Settings/index'

function AuthGuard({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem('token')
  if (!token) return <Navigate to="/login" replace />
  return <>{children}</>
}

function App() {
  return (
    <AntApp>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <AuthGuard>
              <DashboardLayout />
            </AuthGuard>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="ai-employee" element={<AiEmployeePage />} />
          <Route path="knowledge" element={<KnowledgePage />} />
          <Route path="agents" element={<AgentsPage />} />
          <Route path="im" element={<IMPage />} />
          <Route path="transfer-rules" element={<TransferRulesPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AntApp>
  )
}

export default App
