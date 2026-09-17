import { Link, NavLink, Outlet } from 'react-router-dom'
import { ChevronDown, LayoutDashboard, LogOut, Search, UserRound, ReceiptText } from 'lucide-react'
import { useAuth } from '../auth/AuthProvider'
import footerBanner from '../../assets/logo-banners/footbanner.png'
import logo from '../../assets/logo-banners/logo.png'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `text-sm font-semibold ${isActive ? 'text-primary' : 'text-text-secondary hover:text-text-primary'}`

function dashboardPath(roles: string[]): string {
  if (roles.includes('SUPER_ADMIN') || roles.includes('CINEMA_MANAGER')) return '/admin'
  if (roles.includes('TICKET_STAFF')) return '/staff'
  return '/me'
}

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
          <div className="relative hidden min-w-0 flex-1 md:block md:max-w-md" role="search">
            <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" size={18} aria-hidden="true" />
            <input type="search" aria-label="Tìm phim hoặc rạp" placeholder="Tìm phim, rạp..." className="min-h-10 w-full rounded-full border border-border bg-background py-2 pl-10 pr-4 text-sm text-text-primary outline-none transition-colors placeholder:text-text-muted focus:border-primary focus:ring-2 focus:ring-primary/15" />
          </div>
          {user ? (
            <>
              <NavLink className={linkClass} to="/me/tickets">Vé của tôi</NavLink>
              <details className="group relative">
                <summary className="flex min-h-11 cursor-pointer list-none items-center gap-2 rounded-lg px-2 text-sm font-semibold text-text-primary hover:bg-primary-soft focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary [&::-webkit-details-marker]:hidden">
                  <span className="grid size-7 place-items-center rounded-full bg-primary text-white" aria-hidden="true"><UserRound size={17} /></span>
                  <span>Tài khoản</span>
                  <ChevronDown className="transition-transform group-open:rotate-180" size={17} aria-hidden="true" />
                </summary>
                <div className="absolute right-0 top-full z-20 mt-2 min-w-44 rounded-xl border border-border bg-surface p-1.5 shadow-lg shadow-slate-900/10">
                  <Link className="flex min-h-11 items-center gap-2 rounded-lg px-3 text-sm font-medium text-text-primary hover:bg-primary-soft" to={dashboardPath(user.roles)}><LayoutDashboard size={18} aria-hidden="true" />Dashboard</Link>
                  {user.roles.includes('CUSTOMER') && <><Link className="flex min-h-11 items-center gap-2 rounded-lg px-3 text-sm font-medium text-text-primary hover:bg-primary-soft" to="/me/profile"><UserRound size={18} aria-hidden="true" />Hồ sơ</Link><Link className="flex min-h-11 items-center gap-2 rounded-lg px-3 text-sm font-medium text-text-primary hover:bg-primary-soft" to="/me/billing"><ReceiptText size={18} aria-hidden="true" />Thông tin hóa đơn</Link></>}
                  <button onClick={() => void logout()} className="flex min-h-11 w-full items-center gap-2 rounded-lg px-3 text-left text-sm font-medium text-text-primary hover:bg-primary-soft"><LogOut size={18} aria-hidden="true" />Đăng xuất</button>
                </div>
              </details>
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
