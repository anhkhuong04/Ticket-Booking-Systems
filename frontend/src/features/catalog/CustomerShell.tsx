import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import footerBanner from '../../assets/logo-banners/footbanner.png'
import logo from '../../assets/logo-banners/logo.png'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `text-sm font-semibold ${isActive ? 'text-primary' : 'text-text-secondary hover:text-text-primary'}`

type FooterItemProps = { label: string; to?: string }

function FooterItem({ label, to }: FooterItemProps) {
  const className = 'text-left text-xs leading-5 text-text-secondary transition-colors hover:text-primary'
  return to ? <Link className={className} to={to}>{label}</Link> : <span className={`${className} cursor-default`}>{label}</span>
}

const footerGroups = [
  {
    title: 'Khám phá',
    items: [
      { label: 'Phim đang chiếu', to: '/movies?status=NOW_SHOWING' },
      { label: 'Phim sắp chiếu', to: '/movies?status=COMING_SOON' },
      { label: 'Rạp', to: '/cinemas' },
      { label: 'Ưu đãi' },
    ],
  },
  {
    title: 'Hỗ trợ',
    items: [
      { label: 'Câu hỏi thường gặp' },
      { label: 'Điều khoản sử dụng' },
      { label: 'Chính sách bảo mật' },
      { label: 'Liên hệ' },
    ],
  },
] satisfies Array<{ title: string; items: FooterItemProps[] }>

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
              className="h-12 w-auto object-contain sm:h-14"
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
          className="block h-auto w-full"
        />
      </div>
      <footer className="border-t border-border bg-surface">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:px-6 md:grid-cols-2 lg:grid-cols-[1.25fr_1fr_1fr_1.4fr_1.2fr] lg:gap-10 lg:px-8">
          <div>
            <Link to="/" className="inline-flex items-center" aria-label="LAK Cinema - Trang chủ">
              <img src={logo} alt="LAK Cinema" className="h-16 w-auto object-contain sm:h-20" />
            </Link>
            <p className="mt-3 max-w-[220px] text-xs leading-5 text-text-muted">Điện ảnh kết nối cảm xúc, đặt vé an toàn và rõ ràng.</p>
            <p className="mt-5 text-xs text-text-muted">© 2025 LAK Cinema. Tất cả quyền được bảo lưu.</p>
          </div>

          {footerGroups.map((group) => (
            <div key={group.title}>
              <h2 className="text-sm font-semibold text-text-primary">{group.title}</h2>
              <nav className="mt-3 flex flex-col items-start gap-2" aria-label={group.title}>
                {group.items.map((item) => <FooterItem key={item.label} {...item} />)}
              </nav>
            </div>
          ))}

          <div>
            <h2 className="text-sm font-semibold text-text-primary">Kết nối với chúng tôi</h2>
            <div className="mt-3 flex gap-2" aria-label="Mạng xã hội">
              {['F', 'I', 'Y'].map((label) => <button key={label} type="button" disabled aria-label={`Mạng xã hội ${label} (sắp cập nhật)`} className="grid h-11 w-11 place-items-center rounded-full border border-border text-sm font-semibold text-text-secondary disabled:cursor-not-allowed disabled:opacity-60">{label}</button>)}
            </div>
            <p className="mt-4 text-xs font-semibold text-text-primary">Hotline: <a className="font-normal text-text-secondary hover:text-primary" href="tel:1900636548">1900 636 548</a></p>
            <p className="mt-1 text-xs font-semibold text-text-primary">Email: <a className="font-normal text-text-secondary hover:text-primary" href="mailto:hotro@lak.com">hotro@lak.com</a></p>
          </div>

          <div>
            <h2 className="text-sm font-semibold text-text-primary">Tải ứng dụng LAK</h2>
            <div className="mt-3 flex flex-wrap gap-2 lg:flex-col lg:items-start">
              <button type="button" disabled aria-label="Tải LAK trên App Store (sắp cập nhật)" className="min-h-11 rounded-lg bg-slate-950 px-3 py-2 text-left text-white disabled:cursor-not-allowed disabled:opacity-60"><span className="block text-[9px] uppercase tracking-wide text-slate-300">Download on the</span><span className="block text-sm font-semibold leading-4">App Store</span></button>
              <button type="button" disabled aria-label="Tải LAK trên Google Play (sắp cập nhật)" className="min-h-11 rounded-lg bg-slate-950 px-3 py-2 text-left text-white disabled:cursor-not-allowed disabled:opacity-60"><span className="block text-[9px] uppercase tracking-wide text-slate-300">Get it on</span><span className="block text-sm font-semibold leading-4">Google Play</span></button>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}
