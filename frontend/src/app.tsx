import { Routes, Route, Navigate } from 'react-router-dom'
import { App as AntApp } from 'antd'
import LoginPage from './pages/Login/index'
import DashboardLayout from './layouts/DashboardLayout'
import DashboardPage from './pages/Dashboard/index'
import AiEmployeePage from './pages/AiEmployee/index'
import PlaceholderPage from './pages/Placeholder/index'

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
          <Route
            path="knowledge"
            element={<PlaceholderPage title="知识库管理" icon="📚" description="上传文档、管理知识库、查看训练状态。" />}
          />
          <Route
            path="agents"
            element={<PlaceholderPage title="客服管理" icon="👥" description="管理人工客服账号、设置渠道权限。" />}
          />
          <Route
            path="im"
            element={<PlaceholderPage title="IM 工作台" icon="💬" description="实时会话列表、消息回复、转接关闭。" />}
          />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AntApp>
  )
}

export default App
