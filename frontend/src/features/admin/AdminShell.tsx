import { useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import {
  Building2,
  BarChart3,
  BadgePercent,
  CalendarDays,
  ChevronDown,
  Clapperboard,
  CreditCard,
  ExternalLink,
  LayoutDashboard,
  LogOut,
  Menu,
  ReceiptText,
  Settings2,
  Tags,
  Ticket,
  UsersRound,
  X,
  type LucideIcon,
} from 'lucide-react'
import { useAuth } from '../auth/AuthProvider'
import { useTranslation } from 'react-i18next'
import i18n from '../../i18n'
import { LanguageSwitcher } from '../../shared/i18n/LanguageSwitcher'
import logo from '../../assets/logo-banners/logo.png'

type AdminNavItem = {
  to: string
  label: string
  icon: LucideIcon
  end?: boolean
  disabled?: boolean
}

const navItems: AdminNavItem[] = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/admin/movies', label: 'movies', icon: Clapperboard },
  { to: '/admin/cinemas', label: 'adminCinemas', icon: Building2 },
  { to: '/admin/showtimes', label: 'adminShowtimes', icon: CalendarDays },
  { to: '/admin/pricing', label: 'adminPricing', icon: Tags },
  { to: '/admin/bookings', label: 'adminBookings', icon: Ticket },
  { to: '/admin/payments', label: 'adminPayments', icon: CreditCard },
  { to: '/admin/refunds', label: 'adminRefunds', icon: ReceiptText },
  { to: '/admin/vouchers', label: 'Voucher', icon: BadgePercent, disabled: true },
  { to: '/admin/users', label: 'adminUsers', icon: UsersRound },
  { to: '/admin/reports', label: 'adminReports', icon: BarChart3, disabled: true },
]

const pageTitles = new Map(navItems.map((item) => [item.to, item.label]))

function roleLabel(roles: string[]): string {
  if (roles.includes('SUPER_ADMIN')) return 'Super Admin'
  if (roles.includes('CINEMA_MANAGER')) return 'Cinema Manager'
  return i18n.t('adminRole')
}

function initials(name: string | undefined): string {
  if (!name?.trim()) return 'LA'
  const parts = name.trim().split(/\s+/)
  return parts.slice(-2).map((part) => part[0]?.toUpperCase() ?? '').join('') || 'LA'
}

function getPageTitle(pathname: string): string {
  const exact = pageTitles.get(pathname)
  if (exact) return i18n.t(exact)
  if (pathname.startsWith('/admin/cinemas/')) return i18n.t('adminSeatMap')
  return i18n.t('adminTitle')
}

function SidebarNav({ onNavigate }: { onNavigate?: () => void }) {
  const { t } = useTranslation()
  return (
    <nav className="flex flex-col gap-1" aria-label={t('adminNavigation')}>
      {navItems.map(({ to, label, icon: Icon, end, disabled }) => (
        disabled ? (
          <span
            key={to}
            title={t('adminUnavailable')}
            aria-disabled="true"
            className="group flex min-h-11 w-full cursor-not-allowed items-center gap-3 rounded-xl px-3.5 text-sm font-semibold text-text-muted/70"
          >
            <Icon size={19} aria-hidden="true" />
            <span className="truncate">{label === 'Dashboard' || label === 'Voucher' ? label : t(label)}</span>
          </span>
        ) : (
        <NavLink
          key={to}
          to={to}
          end={end}
          onClick={onNavigate}
          className={({ isActive }) => [
            'group flex min-h-11 w-full items-center gap-3 rounded-xl px-3.5 text-sm font-semibold transition-[background-color,color,transform,box-shadow] duration-150',
            'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary',
            isActive
              ? 'bg-primary-soft text-primary shadow-sm shadow-rose-100'
              : 'text-text-secondary hover:-translate-y-px hover:bg-slate-50 hover:text-text-primary',
          ].join(' ')}
        >
          {({ isActive }) => (
            <>
              <Icon
                size={19}
                strokeWidth={isActive ? 2.4 : 2}
                aria-hidden="true"
                className={isActive ? 'text-primary' : 'text-slate-400 transition-colors group-hover:text-primary'}
              />
              <span className="truncate">{label === 'Dashboard' || label === 'Voucher' ? label : t(label)}</span>
              {isActive && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary" aria-hidden="true" />}
            </>
          )}
        </NavLink>
        )
      ))}
    </nav>
  )
}

function AccountMenu({ onLogout }: { onLogout: () => void }) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const name = user?.fullName ?? t('adminAccount')
  const role = roleLabel(user?.roles ?? [])

  return (
    <details className="group relative">
      <summary className="flex min-h-11 cursor-pointer list-none items-center gap-2.5 rounded-xl px-2.5 py-1.5 text-left hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary [&::-webkit-details-marker]:hidden">
        <span className="grid size-9 shrink-0 place-items-center rounded-full bg-primary text-xs font-bold text-white" aria-hidden="true">{initials(user?.fullName)}</span>
        <span className="hidden min-w-0 sm:block">
          <span className="block max-w-40 truncate text-sm font-semibold text-text-primary">{name}</span>
          <span className="block text-xs text-text-muted">{role}</span>
        </span>
        <ChevronDown size={17} aria-hidden="true" className="text-text-secondary transition-transform group-open:rotate-180" />
      </summary>
      <div className="absolute right-0 top-full z-30 mt-2 w-64 rounded-2xl border border-border bg-surface p-2 shadow-xl shadow-slate-900/10">
        <div className="border-b border-border px-3 py-2.5">
          <p className="truncate text-sm font-semibold text-text-primary">{name}</p>
          <p className="mt-0.5 text-xs text-text-secondary">{role}</p>
        </div>
        <Link to="/" className="mt-1 flex min-h-11 items-center gap-2.5 rounded-xl px-3 text-sm font-medium text-text-primary hover:bg-primary-soft hover:text-primary focus-visible:outline-2 focus-visible:outline-primary">
          <ExternalLink size={17} aria-hidden="true" />
          {t('adminViewWebsite')}
        </Link>
        <button type="button" onClick={onLogout} className="flex min-h-11 w-full items-center gap-2.5 rounded-xl px-3 text-left text-sm font-medium text-text-primary hover:bg-primary-soft hover:text-primary focus-visible:outline-2 focus-visible:outline-primary">
          <LogOut size={17} aria-hidden="true" />
          {t('signOut')}
        </button>
      </div>
    </details>
  )
}

function Sidebar({ mobile = false, onClose }: { mobile?: boolean; onClose?: () => void }) {
  const { t } = useTranslation()
  return (
    <aside className={mobile ? 'flex h-full w-[min(86vw,300px)] flex-col bg-surface shadow-2xl' : 'sticky top-0 hidden h-screen w-60 shrink-0 self-start flex-col border-r border-border bg-surface lg:flex'}>
      <div className="flex h-[72px] shrink-0 items-center justify-between border-b border-border px-5">
        <Link to="/" onClick={onClose} className="flex items-center gap-2.5 rounded-lg text-text-primary focus-visible:outline-2 focus-visible:outline-primary" aria-label={`LAK Cinema - ${t('home')}`}>
          <img src={logo} alt="LAK" className="h-10 w-24 object-contain object-left" />
          <span className="text-sm font-bold tracking-[0.18em] text-text-primary">ADMIN</span>
        </Link>
        {mobile && <button type="button" onClick={onClose} className="grid size-11 place-items-center rounded-xl text-text-secondary hover:bg-slate-50 hover:text-text-primary focus-visible:outline-2 focus-visible:outline-primary" aria-label={t('adminCloseMenu')}><X size={20} aria-hidden="true" /></button>}
      </div>
      <div className="flex-1 overflow-y-auto px-3 py-5">
        <p className="mb-4 px-3 text-sm font-bold uppercase tracking-[0.22em] text-text-muted">{t('adminManagement')}</p>
        <SidebarNav onNavigate={onClose} />
      </div>
      <div className="border-t border-border px-4 py-4">
        <p className="flex items-center gap-2 text-xs text-text-muted"><Settings2 size={15} aria-hidden="true" /> {t('adminScopedData')}</p>
      </div>
    </aside>
  )
}

export function AdminShell() {
  const { t } = useTranslation()
  const { logout } = useAuth()
  const { pathname } = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const title = getPageTitle(pathname)

  return (
    <div className="flex min-h-screen bg-background">
      <Sidebar />
      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label={t('adminMenu')}>
          <button type="button" onClick={() => setMobileOpen(false)} className="absolute inset-0 bg-slate-950/30" aria-label={t('adminCloseMenu')} />
          <div className="relative h-full"><Sidebar mobile onClose={() => setMobileOpen(false)} /></div>
        </div>
      )}
      <div className="min-w-0 flex-1">
        <header className="sticky top-0 z-20 border-b border-border bg-surface/95 backdrop-blur">
          <div className="flex min-h-[72px] items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
            <div className="flex min-w-0 items-center gap-3">
              <button type="button" onClick={() => setMobileOpen(true)} className="grid size-11 shrink-0 place-items-center rounded-xl border border-border text-text-secondary hover:border-primary hover:text-primary focus-visible:outline-2 focus-visible:outline-primary lg:hidden" aria-label={t('adminOpenMenu')}><Menu size={20} aria-hidden="true" /></button>
              <div className="min-w-0">
                <p className="hidden text-xs font-medium text-text-muted sm:block">LAK Admin / {title}</p>
                <h1 className="truncate text-lg font-bold text-text-primary sm:text-xl">{title}</h1>
              </div>
            </div>
            <div className="flex shrink-0 items-center gap-2 sm:gap-4">
              <LanguageSwitcher />
              <AccountMenu onLogout={() => void logout()} />
            </div>
          </div>
        </header>
        <Outlet />
      </div>
    </div>
  )
}
