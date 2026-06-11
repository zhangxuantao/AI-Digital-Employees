import { Routes, Route, Navigate } from 'react-router-dom'
import { App as AntApp } from 'antd'

function App() {
  return (
    <AntApp>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<div>Login Page (coming soon)</div>} />
        <Route path="/*" element={<div>404 Not Found</div>} />
      </Routes>
    </AntApp>
  )
}

export default App
