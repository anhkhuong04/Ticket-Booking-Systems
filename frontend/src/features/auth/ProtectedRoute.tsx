import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthProvider'

export function ProtectedRoute({ roles }: { roles?: string[] }) {
  const { user, ready } = useAuth(); const location = useLocation()
  if (!ready) return <main className="grid min-h-screen place-items-center text-text-secondary" aria-busy="true">Đang khôi phục phiên đăng nhập…</main>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (roles && !roles.some((role) => user.roles.includes(role))) return <Navigate to="/forbidden" replace />
  return <Outlet />
}
