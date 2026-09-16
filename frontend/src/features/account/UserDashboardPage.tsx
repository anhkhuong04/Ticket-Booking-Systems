import { isAxiosError } from 'axios'
import { CircleAlert, Film, MapPin, Ticket } from 'lucide-react'
import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { getMovies, type Movie } from '../catalog/catalogApi'
import { getMyBookings, type BookingHistoryItem } from '../ticketing/ticketApi'

type LoadState<T> = { kind: 'loading' } | { kind: 'loaded'; data: T; loadedAt: number } | { kind: 'error'; message: string }

type Attention = { booking: BookingHistoryItem; title: string; description: string; tab: 'upcoming' | 'closed' }

const dateTime = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'medium',
  timeStyle: 'short',
  timeZone: 'Asia/Ho_Chi_Minh',
})

const dateOnly = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'medium',
  timeZone: 'Asia/Ho_Chi_Minh',
})

function errorMessage(error: unknown, fallback: string): string {
  if (isAxiosError<{ message?: string }>(error)) return error.response?.data?.message ?? fallback
  return fallback
}

function attentionFor(booking: BookingHistoryItem): Attention | null {
  if (booking.refundStatus === 'REFUND_FAILED') {
    return { booking, title: 'Hoàn tiền cần được hỗ trợ', description: `Booking ${booking.bookingCode} đang được bộ phận hỗ trợ xử lý.`, tab: 'closed' }
  }
  if (booking.refundStatus === 'REQUESTED' || booking.bookingStatus === 'REFUND_PENDING') {
    return { booking, title: 'Hoàn tiền đang xử lý', description: `Yêu cầu hoàn tiền cho booking ${booking.bookingCode} đã được tiếp nhận.`, tab: 'closed' }
  }
  if (booking.bookingStatus === 'PAYMENT_REVIEW') {
    return { booking, title: 'Thanh toán đang được đối soát', description: `Booking ${booking.bookingCode} đang chờ hệ thống xác minh giao dịch.`, tab: 'upcoming' }
  }
  if (booking.bookingStatus === 'PENDING_PAYMENT') {
    return { booking, title: 'Booking đang chờ thanh toán', description: `Kiểm tra trạng thái booking ${booking.bookingCode} trước khi thực hiện giao dịch mới.`, tab: 'upcoming' }
  }
  return null
}

function Poster({ movieTitle, posterUrl, className }: { movieTitle: string; posterUrl: string | null; className: string }) {
  if (posterUrl) return <img className={className} src={posterUrl} alt={`Poster phim ${movieTitle}`} loading="lazy" />
  return <div className={`${className} grid place-items-center bg-slate-200 p-2 text-center text-xs font-semibold text-text-secondary`}>{movieTitle}</div>
}

function BookingSkeleton() {
  return <div className="h-48 animate-pulse rounded-xl bg-slate-200" aria-hidden="true" />
}

export function UserDashboardPage() {
  const { user } = useAuth()
  const [bookingState, setBookingState] = useState<LoadState<BookingHistoryItem[]>>({ kind: 'loading' })
  const [movieState, setMovieState] = useState<LoadState<Movie[]>>({ kind: 'loading' })
  const [bookingRetry, setBookingRetry] = useState(0)
  const [movieRetry, setMovieRetry] = useState(0)

  useEffect(() => {
    let active = true
    void getMyBookings().then(
      (data) => { if (active) setBookingState({ kind: 'loaded', data, loadedAt: Date.now() }) },
      (error: unknown) => { if (active) setBookingState({ kind: 'error', message: errorMessage(error, 'Không thể tải dữ liệu tài khoản. Vui lòng thử lại.') }) },
    )
    return () => { active = false }
  }, [bookingRetry])

  useEffect(() => {
    let active = true
    void getMovies({ status: 'NOW_SHOWING', size: 4 }).then(
      (page) => { if (active) setMovieState({ kind: 'loaded', data: page.content, loadedAt: Date.now() }) },
      (error: unknown) => { if (active) setMovieState({ kind: 'error', message: errorMessage(error, 'Không thể tải phim đang chiếu.') }) },
    )
    return () => { active = false }
  }, [movieRetry])

  const dashboard = useMemo(() => {
    if (bookingState.kind !== 'loaded') return null
    const now = bookingState.loadedAt
    const upcomingTicket = bookingState.data
      .filter((booking) => booking.ticketStatus === 'VALID' && Date.parse(booking.startAt) > now)
      .sort((left, right) => Date.parse(left.startAt) - Date.parse(right.startAt))[0]
    const attention = bookingState.data.map(attentionFor).filter((value): value is Attention => value !== null)
    const recent = [...bookingState.data]
      .sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt))
      .slice(0, 5)
    return { upcomingTicket, attention, recent }
  }, [bookingState])

  return <main className="min-h-screen bg-background py-8 sm:py-12">
    <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
      <header>
        <p className="text-sm font-semibold uppercase tracking-[0.14em] text-primary">Tài khoản LAK</p>
        <h1 className="mt-2 text-3xl font-bold text-text-primary sm:text-4xl">Xin chào, {user?.fullName ?? 'bạn'}</h1>
        <p className="mt-3 max-w-2xl text-text-secondary">Theo dõi vé sắp xem và các giao dịch cần xử lý của bạn tại một nơi.</p>
      </header>

      {bookingState.kind === 'loading' && <section className="mt-8 grid gap-5 lg:grid-cols-[1.5fr_1fr]" aria-busy="true"><BookingSkeleton /><BookingSkeleton /></section>}
      {bookingState.kind === 'error' && <section role="alert" className="mt-8 rounded-xl border border-red-200 bg-red-50 p-5 text-red-800"><p>{bookingState.message}</p><button className="mt-3 min-h-11 rounded-lg px-3 font-semibold underline" onClick={() => { setBookingState({ kind: 'loading' }); setBookingRetry((value) => value + 1) }}>Thử lại</button></section>}
      {dashboard && <>
        <section className="mt-8" aria-labelledby="attention-title">
          <div className="flex items-center gap-2"><CircleAlert className="text-primary" size={22} aria-hidden="true" /><h2 id="attention-title" className="text-xl font-semibold text-text-primary">Việc cần xử lý</h2></div>
          {dashboard.attention.length === 0 ? <p className="mt-4 rounded-xl border border-border bg-surface p-5 text-text-secondary">Bạn không có giao dịch cần xử lý.</p> : <ul className="mt-4 grid gap-3 lg:grid-cols-2">{dashboard.attention.map((item) => <li key={item.booking.bookingId}><article className="flex h-full flex-col justify-between gap-4 rounded-xl border border-amber-200 bg-amber-50 p-5"><div><h3 className="font-semibold text-text-primary">{item.title}</h3><p className="mt-2 text-sm leading-6 text-text-secondary">{item.description}</p></div><Link className="inline-flex min-h-11 items-center self-start rounded-lg border border-primary px-4 text-sm font-semibold text-primary hover:bg-primary-soft" to={`/me/tickets?tab=${item.tab}`}>Theo dõi trạng thái</Link></article></li>)}</ul>}
        </section>

        <section className="mt-8 grid gap-5 lg:grid-cols-[1.5fr_1fr]">
          <article className="rounded-xl border border-border bg-surface p-5 sm:p-6" aria-labelledby="upcoming-ticket-title">
            <div className="flex items-center gap-2"><Ticket className="text-primary" size={22} aria-hidden="true" /><h2 id="upcoming-ticket-title" className="text-xl font-semibold text-text-primary">Vé sắp xem</h2></div>
            {dashboard.upcomingTicket ? <div className="mt-5 flex flex-col gap-4 sm:flex-row"><Poster movieTitle={dashboard.upcomingTicket.movieTitle} posterUrl={dashboard.upcomingTicket.posterUrl} className="h-40 w-28 shrink-0 rounded-lg object-cover" /><div className="min-w-0 flex-1"><h3 className="text-lg font-bold text-text-primary">{dashboard.upcomingTicket.movieTitle}</h3><p className="mt-2 text-sm text-text-secondary">{dateTime.format(new Date(dashboard.upcomingTicket.startAt))}</p><p className="mt-1 text-sm text-text-secondary">{dashboard.upcomingTicket.cinemaName} · {dashboard.upcomingTicket.auditoriumName}</p><p className="mt-1 text-sm text-text-secondary">Ghế {dashboard.upcomingTicket.seatLabels.join(', ')}</p>{dashboard.upcomingTicket.ticketCode && <Link className="mt-5 inline-flex min-h-11 items-center rounded-lg bg-primary px-4 text-sm font-semibold text-white hover:bg-primary-hover" to={`/tickets/${encodeURIComponent(dashboard.upcomingTicket.ticketCode)}`}>Xem vé</Link>}</div></div> : <div className="mt-5 rounded-lg bg-slate-50 p-5"><p className="text-text-secondary">Bạn chưa có vé sắp xem.</p><Link className="mt-4 inline-flex min-h-11 items-center rounded-lg bg-primary px-4 text-sm font-semibold text-white hover:bg-primary-hover" to="/movies">Khám phá phim</Link></div>}
          </article>

          <aside className="rounded-xl border border-border bg-surface p-5 sm:p-6" aria-labelledby="quick-actions-title">
            <h2 id="quick-actions-title" className="text-xl font-semibold text-text-primary">Thao tác nhanh</h2>
            <nav className="mt-4 grid gap-3" aria-label="Thao tác tài khoản"><QuickAction to="/me/tickets" icon={<Ticket size={20} aria-hidden="true" />} label="Vé của tôi" /><QuickAction to="/movies" icon={<Film size={20} aria-hidden="true" />} label="Đặt vé mới" /><QuickAction to="/cinemas" icon={<MapPin size={20} aria-hidden="true" />} label="Tìm rạp LAK" /></nav>
          </aside>
        </section>

        <section className="mt-10" aria-labelledby="recent-bookings-title">
          <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 id="recent-bookings-title" className="text-2xl font-semibold text-text-primary">Đặt vé gần đây</h2><p className="mt-1 text-sm text-text-secondary">Các booking mới nhất của bạn.</p></div><Link className="inline-flex min-h-11 items-center gap-1 rounded-lg px-3 text-sm font-semibold text-primary hover:bg-primary-soft" to="/me/tickets">Xem tất cả vé <span aria-hidden="true">→</span></Link></div>
          {dashboard.recent.length === 0 ? <p className="mt-5 rounded-xl border border-border bg-surface p-5 text-text-secondary">Bạn chưa có booking nào.</p> : <ul className="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{dashboard.recent.map((booking) => <li key={booking.bookingId}><article className="h-full rounded-xl border border-border bg-surface p-5"><div className="flex items-start justify-between gap-3"><div><p className="text-xs font-semibold uppercase tracking-wide text-primary">{booking.bookingCode}</p><h3 className="mt-2 font-semibold text-text-primary">{booking.movieTitle}</h3></div><BookingStatus booking={booking} /></div><p className="mt-3 text-sm text-text-secondary">{dateTime.format(new Date(booking.startAt))}</p><p className="mt-1 text-sm text-text-secondary">{booking.cinemaName} · Ghế {booking.seatLabels.join(', ')}</p><p className="mt-3 text-xs text-text-muted">Đặt lúc {dateOnly.format(new Date(booking.createdAt))}</p>{booking.ticketCode && <Link className="mt-4 inline-flex min-h-11 items-center rounded-lg border border-primary px-4 text-sm font-semibold text-primary hover:bg-primary-soft" to={`/tickets/${encodeURIComponent(booking.ticketCode)}`}>Xem vé</Link>}</article></li>)}</ul>}
        </section>
      </>}

      <section className="mt-10" aria-labelledby="now-showing-title">
        <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 id="now-showing-title" className="text-2xl font-semibold text-text-primary">Phim đang chiếu</h2><p className="mt-1 text-sm text-text-secondary">Chọn phim để xem suất chiếu phù hợp.</p></div><Link className="inline-flex min-h-11 items-center gap-1 rounded-lg px-3 text-sm font-semibold text-primary hover:bg-primary-soft" to="/movies?status=NOW_SHOWING">Xem tất cả <span aria-hidden="true">→</span></Link></div>
        {movieState.kind === 'loading' && <div className="mt-5 grid grid-cols-2 gap-4 sm:grid-cols-4" aria-busy="true">{[1, 2, 3, 4].map((key) => <div key={key} className="h-64 animate-pulse rounded-xl bg-slate-200" />)}</div>}
        {movieState.kind === 'error' && <div role="alert" className="mt-5 rounded-xl border border-red-200 bg-red-50 p-5 text-red-800"><p>{movieState.message}</p><button className="mt-3 min-h-11 rounded-lg px-3 font-semibold underline" onClick={() => { setMovieState({ kind: 'loading' }); setMovieRetry((value) => value + 1) }}>Thử lại</button></div>}
        {movieState.kind === 'loaded' && (movieState.data.length ? <div className="mt-5 grid grid-cols-2 gap-4 sm:grid-cols-4">{movieState.data.map((movie) => <Link key={movie.id} to={`/movies/${movie.id}`} className="group overflow-hidden rounded-xl border border-border bg-surface focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"><Poster movieTitle={movie.title} posterUrl={movie.posterUrl} className="aspect-[2/3] w-full object-cover transition-transform group-hover:scale-[1.02]" /><div className="p-3"><h3 className="line-clamp-2 font-semibold text-text-primary">{movie.title}</h3><p className="mt-1 text-xs text-text-secondary">{movie.ageRating} · {movie.durationMinutes} phút</p></div></Link>)}</div> : <p className="mt-5 rounded-xl border border-border bg-surface p-5 text-text-secondary">Chưa có phim đang chiếu.</p>)}
      </section>
    </div>
  </main>
}

function QuickAction({ to, icon, label }: { to: string; icon: ReactNode; label: string }) {
  return <Link to={to} className="flex min-h-11 items-center gap-3 rounded-lg border border-border px-4 text-sm font-semibold text-text-primary hover:border-primary hover:bg-primary-soft"><span className="text-primary">{icon}</span>{label}</Link>
}

function BookingStatus({ booking }: { booking: BookingHistoryItem }) {
  const label = booking.refundStatus === 'REFUND_FAILED' ? 'Cần hỗ trợ' : booking.refundStatus === 'REQUESTED' ? 'Đang hoàn tiền' : booking.bookingStatus === 'PAID' ? 'Đã thanh toán' : booking.bookingStatus === 'REFUNDED' ? 'Đã hoàn tiền' : booking.bookingStatus === 'PAYMENT_REVIEW' ? 'Đang đối soát' : booking.bookingStatus === 'PENDING_PAYMENT' ? 'Chờ thanh toán' : booking.bookingStatus === 'CANCELLED' ? 'Đã hủy' : booking.bookingStatus
  return <span className="rounded-full bg-primary-soft px-2.5 py-1 text-xs font-semibold text-primary">{label}</span>
}
