import { Link, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './features/auth/AuthProvider'
import { ForgotPasswordPage, LoginPage, RegisterPage, ResetPasswordPage } from './features/auth/AuthPages'
import { ProtectedRoute } from './features/auth/ProtectedRoute'
import { CinemasPage, HomePage, MovieDetailPage, MoviesPage } from './features/catalog/CatalogPages'
import { CustomerShell } from './features/catalog/CustomerShell'

function AccountPage() {
  const { user, logout } = useAuth()
  return <main className="min-h-screen bg-background p-6"><div className="mx-auto max-w-3xl rounded-xl border border-border bg-surface p-6"><h1 className="text-2xl font-bold">Tài khoản</h1><p className="mt-3 text-text-secondary">{user?.fullName} · {user?.email}</p><button onClick={() => void logout()} className="mt-6 min-h-11 rounded-lg bg-primary px-4 font-semibold text-white">Đăng xuất</button></div></main>
}

function RoleShell({ title }: { title: string }) {
  const { user, logout } = useAuth()
  return <main className="min-h-screen bg-background p-6"><header className="mx-auto flex max-w-5xl items-center justify-between"><Link className="font-bold text-primary" to="/">LAK</Link><button onClick={() => void logout()} className="min-h-11 text-sm font-semibold text-primary">Đăng xuất</button></header><section className="mx-auto mt-12 max-w-5xl rounded-xl border border-border bg-surface p-8"><h1 className="text-2xl font-bold">{title}</h1><p className="mt-2 text-text-secondary">Đăng nhập với vai trò: {user?.roles.join(', ')}</p></section></main>
}

function ForbiddenPage() {
  return <main className="grid min-h-screen place-items-center bg-background p-6"><section className="max-w-md text-center"><h1 className="text-2xl font-bold">Bạn không có quyền truy cập</h1><Link className="mt-4 inline-block text-primary underline" to="/">Quay lại</Link></section></main>
}

function App() {
  return <AuthProvider><Routes>
    <Route element={<CustomerShell />}>
      <Route path="/" element={<HomePage />} />
      <Route path="/movies" element={<MoviesPage />} />
      <Route path="/movies/:movieId" element={<MovieDetailPage />} />
      <Route path="/cinemas" element={<CinemasPage />} />
    </Route>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />
    <Route path="/forgot-password" element={<ForgotPasswordPage />} />
    <Route path="/reset-password" element={<ResetPasswordPage />} />
    <Route element={<ProtectedRoute roles={['CUSTOMER']} />}><Route path="/me" element={<AccountPage />} /></Route>
    <Route element={<ProtectedRoute roles={['TICKET_STAFF', 'SUPER_ADMIN']} />}><Route path="/staff" element={<RoleShell title="Khu vực nhân viên" />} /></Route>
    <Route element={<ProtectedRoute roles={['CINEMA_MANAGER', 'SUPER_ADMIN']} />}><Route path="/admin" element={<RoleShell title="Khu vực quản trị" />} /></Route>
    <Route path="/forbidden" element={<ForbiddenPage />} />
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes></AuthProvider>
}

export default App
