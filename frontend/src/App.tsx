import { Navigate, Route, Routes } from 'react-router-dom'
import { SystemStatusPage } from './features/system-health/SystemStatusPage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<SystemStatusPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
