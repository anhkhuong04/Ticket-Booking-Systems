import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import footerBanner from '../../assets/logo-banners/footbanner.png'
import logo from '../../assets/logo-banners/logo.png'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `text-sm font-semibold ${isActive ? 'text-primary' : 'text-text-secondary hover:text-text-primary'}`

export function CustomerShell() {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-screen bg-background">
      <header className="sticky top-0 z-10 border-b border-border bg-surface/95 backdrop-blur">
        <nav className="mx-auto flex h-16 max-w-7xl items-center gap-5 px-4 sm:h-[72px] sm:px-6 lg:px-8">
          <Link to="/" className="inline-flex shrink-0 items-center" aria-label="LAK Cinema - Trang chủ">
            <img
              src={logo}
              alt="LAK Cinema"
              className="h-10 w-auto object-contain sm:h-12"
            />
          </Link>
          <div className="flex flex-1 items-center gap-4">
            <NavLink className={linkClass} to="/movies">Phim</NavLink>
            <NavLink className={linkClass} to="/cinemas">Rạp</NavLink>
          </div>
          {user ? (
            <>
              <NavLink className={linkClass} to="/me/tickets">Vé của tôi</NavLink>
              <NavLink className={linkClass} to="/me">Tài khoản</NavLink>
              <button onClick={() => void logout()} className="min-h-11 text-sm font-semibold text-primary">Đăng xuất</button>
            </>
          ) : (
            <Link className="inline-flex min-h-11 items-center rounded-lg bg-primary px-4 text-sm font-semibold text-white hover:bg-primary-hover" to="/login">Đăng nhập</Link>
          )}
        </nav>
      </header>
      <Outlet />
      <div className="border-t border-border bg-surface">
        <img
          src={footerBanner}
          alt="Điện ảnh kết nối cảm xúc - LAK Cinema"
          className="mx-auto h-auto w-full max-w-[978px]"
        />
      </div>
      <footer className="border-t border-border bg-surface">
        <div className="mx-auto max-w-7xl px-4 py-6 text-sm text-text-secondary sm:px-6 lg:px-8">© LAK Cinema · Đặt vé an toàn, rõ ràng.</div>
      </footer>
    </div>
  )
}
