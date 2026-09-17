import { CircleAlert, Clock3, History, Plus, Ticket, UserRound } from 'lucide-react'
import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { displayLocale } from '../../i18n'
import { localizeApiError } from '../../shared/i18n/apiError'
import { useAuth } from '../auth/AuthProvider'
import { bookingStatusLabel, dashboardNoticeFor, refundStatusLabel, type DashboardNotice } from '../ticketing/bookingPresentation'
import { getMyBookings, type BookingHistoryItem } from '../ticketing/ticketApi'

type LoadState = { kind: 'loading' } | { kind: 'loaded'; data: BookingHistoryItem[]; loadedAt: number } | { kind: 'error'; message: string }
type NoticeCategory = DashboardNotice['category']

function errorMessage(error: unknown): string { return localizeApiError(error, 'accountLoadError') }

function Poster({ movieTitle, posterUrl }: { movieTitle: string; posterUrl: string | null }) {
  if (posterUrl) return <img className="h-40 w-28 shrink-0 rounded-lg object-cover" src={posterUrl} alt={`Poster ${movieTitle}`} loading="lazy" />
  return <div className="grid h-40 w-28 shrink-0 place-items-center rounded-lg bg-slate-200 p-2 text-center text-xs font-semibold text-text-secondary">{movieTitle}</div>
}

function QuickAction({ to, icon, label }: { to: string; icon: ReactNode; label: string }) {
  return <Link to={to} className="flex min-h-11 items-center gap-3 rounded-lg border border-border px-4 text-sm font-semibold text-text-primary hover:border-primary hover:bg-primary-soft focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"><span className="text-primary">{icon}</span>{label}</Link>
}

function NoticeSection({ category, notices }: { category: NoticeCategory; notices: DashboardNotice[] }) {
  const { t } = useTranslation()
  if (notices.length === 0) return null
  const config = {
    issue: { title: t('noticeIssue'), icon: <CircleAlert size={22} aria-hidden="true" />, tone: 'border-red-200 bg-red-50' },
    action: { title: t('noticeAction'), icon: <CircleAlert size={22} aria-hidden="true" />, tone: 'border-amber-200 bg-amber-50' },
    processing: { title: t('noticeProcessing'), icon: <Clock3 size={22} aria-hidden="true" />, tone: 'border-border bg-surface' },
  }[category]
  return <section className="mt-8" aria-label={config.title}>
    <h2 className="flex items-center gap-2 text-xl font-semibold text-text-primary"><span className="text-primary">{config.icon}</span>{config.title}</h2>
    <ul className="mt-4 grid gap-3 lg:grid-cols-2">{notices.map((item) => <li key={item.booking.bookingId}>
      <article className={`flex h-full flex-col justify-between gap-4 rounded-xl border p-5 ${config.tone}`}>
        <div><h3 className="font-semibold text-text-primary">{item.title}</h3><p className="mt-2 text-sm leading-6 text-text-secondary">{item.description}</p></div>
        <Link className={`inline-flex min-h-11 items-center self-start rounded-lg px-4 text-sm font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${category === 'action' ? 'bg-primary text-white hover:bg-primary-hover' : 'border border-primary text-primary hover:bg-primary-soft'}`} to={item.actionTo}>{item.actionLabel}</Link>
      </article>
    </li>)}</ul>
  </section>
}

export function UserDashboardPage() {
  const { t, i18n } = useTranslation()
  const dateTime = useMemo(() => new Intl.DateTimeFormat(displayLocale(i18n.language), { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' }), [i18n.language])
  const dateOnly = useMemo(() => new Intl.DateTimeFormat(displayLocale(i18n.language), { dateStyle: 'medium', timeZone: 'Asia/Ho_Chi_Minh' }), [i18n.language])
  const { user } = useAuth()
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    let active = true
    void getMyBookings().then(
      (data) => { if (active) setState({ kind: 'loaded', data, loadedAt: Date.now() }) },
      (error: unknown) => { if (active) setState({ kind: 'error', message: errorMessage(error) }) },
    )
    return () => { active = false }
  }, [retry])

  useEffect(() => {
    const refresh = () => setRetry((value) => value + 1)
    window.addEventListener('focus', refresh)
    return () => window.removeEventListener('focus', refresh)
  }, [])

  const dashboard = (() => {
    if (state.kind !== 'loaded') return null
    const upcomingTicket = state.data
      .filter((booking) => booking.ticketStatus === 'VALID' && booking.bookingStatus === 'PAID'
        && booking.showtimeStatus === 'SCHEDULED' && Date.parse(booking.startAt) > state.loadedAt)
      .sort((left, right) => Date.parse(left.startAt) - Date.parse(right.startAt))[0]
    const notices = state.data.map(dashboardNoticeFor).filter((item): item is DashboardNotice => item !== null)
    const recent = [...state.data].sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt)).slice(0, 5)
    return { upcomingTicket, notices, recent }
  })()

  return <main className="min-h-screen bg-background py-8 sm:py-12"><div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
    <header><p className="text-sm font-semibold uppercase tracking-[0.14em] text-primary">{t('accountTitle')}</p>
      <h1 className="mt-2 text-3xl font-bold text-text-primary sm:text-4xl">{t('greeting', { name: user?.fullName ?? t('you') })}</h1>
      <p className="mt-3 max-w-2xl text-text-secondary">{t('dashboardHint')}</p>
    </header>

    {state.kind === 'loading' && <section className="mt-8 grid gap-5 lg:grid-cols-[1.5fr_1fr]" aria-busy="true"><div className="h-48 animate-pulse rounded-xl bg-slate-200" /><div className="h-48 animate-pulse rounded-xl bg-slate-200" /></section>}
    {state.kind === 'error' && <section role="alert" className="mt-8 rounded-xl border border-red-200 bg-red-50 p-5 text-red-800"><p>{state.message}</p><button className="mt-3 min-h-11 rounded-lg px-3 font-semibold underline" onClick={() => { setState({ kind: 'loading' }); setRetry((value) => value + 1) }}>{t('retry')}</button></section>}

    {dashboard && <>
      <NoticeSection category="issue" notices={dashboard.notices.filter((item) => item.category === 'issue')} />
      <NoticeSection category="action" notices={dashboard.notices.filter((item) => item.category === 'action')} />
      <NoticeSection category="processing" notices={dashboard.notices.filter((item) => item.category === 'processing')} />

      <section className="mt-8 grid gap-5 lg:grid-cols-[1.5fr_1fr]">
        <article className="rounded-xl border border-border bg-surface p-5 sm:p-6" aria-labelledby="upcoming-ticket-title">
          <h2 id="upcoming-ticket-title" className="flex items-center gap-2 text-xl font-semibold text-text-primary"><Ticket className="text-primary" size={22} aria-hidden="true" />{t('upcomingTicket')}</h2>
          {dashboard.upcomingTicket ? <div className="mt-5 flex flex-col gap-4 sm:flex-row"><Poster movieTitle={dashboard.upcomingTicket.movieTitle} posterUrl={dashboard.upcomingTicket.posterUrl} /><div className="min-w-0 flex-1"><h3 className="text-lg font-bold text-text-primary">{dashboard.upcomingTicket.movieTitle}</h3><p className="mt-2 text-sm text-text-secondary">{dateTime.format(new Date(dashboard.upcomingTicket.startAt))}</p><p className="mt-1 text-sm text-text-secondary">{dashboard.upcomingTicket.cinemaName} · {dashboard.upcomingTicket.auditoriumName}</p><p className="mt-1 text-sm text-text-secondary">{t('seats')} {dashboard.upcomingTicket.seatLabels.join(', ')}</p>{dashboard.upcomingTicket.ticketCode && <Link className="mt-5 inline-flex min-h-11 items-center rounded-lg bg-primary px-4 font-semibold text-white hover:bg-primary-hover" to={`/tickets/${encodeURIComponent(dashboard.upcomingTicket.ticketCode)}`}>{t('viewTicket')}</Link>}</div></div>
            : <div className="mt-5 rounded-lg bg-slate-50 p-5"><p className="text-text-secondary">{t('noUpcomingTicket')}</p><Link className="mt-4 inline-flex min-h-11 items-center rounded-lg bg-primary px-4 text-sm font-semibold text-white hover:bg-primary-hover" to="/movies">{t('exploreMovies')}</Link></div>}
        </article>
        <aside className="rounded-xl border border-border bg-surface p-5 sm:p-6" aria-labelledby="quick-actions-title">
          <h2 id="quick-actions-title" className="text-xl font-semibold text-text-primary">{t('quickActions')}</h2>
          <nav className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-1" aria-label={t('accountActions')}>
            <QuickAction to="/me/tickets" icon={<Ticket size={20} aria-hidden="true" />} label={t('myTickets')} />
            <QuickAction to="/me/bookings" icon={<History size={20} aria-hidden="true" />} label={t('bookingHistory')} />
            <QuickAction to="/movies" icon={<Plus size={20} aria-hidden="true" />} label={t('newBooking')} />
            <QuickAction to="/me/profile" icon={<UserRound size={20} aria-hidden="true" />} label={t('profile')} />
          </nav>
        </aside>
      </section>

      <section className="mt-10" aria-labelledby="recent-bookings-title">
        <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 id="recent-bookings-title" className="text-2xl font-semibold text-text-primary">{t('recentBookings')}</h2><p className="mt-1 text-sm text-text-secondary">{t('recentHint')}</p></div><Link className="inline-flex min-h-11 items-center gap-1 rounded-lg px-3 text-sm font-semibold text-primary hover:bg-primary-soft" to="/me/bookings">{t('viewHistory')} <span aria-hidden="true">→</span></Link></div>
        {dashboard.recent.length === 0 ? <p className="mt-5 rounded-xl border border-border bg-surface p-5 text-text-secondary">{t('noBookings')}</p> : <ul className="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{dashboard.recent.map((booking) => <li key={booking.bookingId}><article className="h-full rounded-xl border border-border bg-surface p-5"><div className="flex items-start justify-between gap-3"><div><p className="text-xs font-semibold uppercase tracking-wide text-primary">{booking.bookingCode}</p><h3 className="mt-2 font-semibold text-text-primary">{booking.movieTitle}</h3></div><span className="rounded-full bg-primary-soft px-2.5 py-1 text-xs font-semibold text-primary">{bookingStatusLabel(booking)}</span></div><p className="mt-3 text-sm text-text-secondary">{dateTime.format(new Date(booking.startAt))}</p><p className="mt-1 text-sm text-text-secondary">{booking.cinemaName} · {t('seats')} {booking.seatLabels.join(', ')}</p>{booking.showtimeStatus === 'CANCELLED' && refundStatusLabel(booking) && <p className="mt-2 text-sm font-semibold text-text-secondary">{refundStatusLabel(booking)}</p>}<p className="mt-3 text-xs text-text-muted">{t('bookedOn', { date: dateOnly.format(new Date(booking.createdAt)) })}</p><Link className="mt-4 inline-flex min-h-11 items-center rounded-lg border border-primary px-4 text-sm font-semibold text-primary hover:bg-primary-soft" to={`/me/bookings?q=${encodeURIComponent(booking.bookingCode)}`}>{t('viewBooking')}</Link></article></li>)}</ul>}
      </section>
    </>}
  </div></main>
}
