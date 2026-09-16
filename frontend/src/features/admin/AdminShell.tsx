import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'

const links = [
  { to: '/admin', label: 'Dashboard' },
  { to: '/admin/movies', label: 'Phim' },
  { to: '/admin/cinemas', label: 'Rạp & phòng' },
  { to: '/admin/showtimes', label: 'Suất chiếu' },
  { to: '/admin/pricing', label: 'Bảng giá' },
  { to: '/admin/bookings', label: 'Booking' },
  { to: '/admin/payments', label: 'Thanh toán' },
  { to: '/admin/refunds', label: 'Hoàn tiền' },
  { to: '/admin/users', label: 'Người dùng' },
]

export function AdminShell() {
  const { user, logout } = useAuth()
  return <div className="min-h-screen bg-background lg:grid lg:grid-cols-[240px_1fr]">
    <aside className="border-b border-border bg-surface lg:min-h-screen lg:border-b-0 lg:border-r"><div className="flex h-16 items-center px-5 text-lg font-bold text-primary">LAK ADMIN</div><nav className="flex gap-1 overflow-x-auto px-3 pb-3 lg:block lg:space-y-1">{links.map((link) => <NavLink key={link.to} to={link.to} className={({ isActive }) => `inline-flex min-h-11 items-center rounded-lg px-3 text-sm font-semibold ${isActive ? 'bg-primary-soft text-primary' : 'text-text-secondary hover:bg-slate-50'}`}>{link.label}</NavLink>)}</nav></aside>
    <div><header className="flex min-h-16 items-center justify-between border-b border-border bg-surface px-4 sm:px-6"><p className="text-sm text-text-secondary">{user?.fullName}</p><button onClick={() => void logout()} className="min-h-11 text-sm font-semibold text-primary">Đăng xuất</button></header><Outlet /></div>
  </div>
}
