import { isAxiosError } from 'axios'
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { bookingStatusLabel, dashboardNoticeFor, refundStatusLabel } from '../ticketing/bookingPresentation'
import { getMyBookings, type BookingHistoryItem } from '../ticketing/ticketApi'

type LoadState = { kind: 'loading' } | { kind: 'loaded'; bookings: BookingHistoryItem[] } | { kind: 'error'; message: string }
type Filter = 'all' | 'action' | 'processing' | 'issue' | 'closed'

const dateTime = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' })
const vietnamDate = new Intl.DateTimeFormat('sv-SE', { year: 'numeric', month: '2-digit', day: '2-digit', timeZone: 'Asia/Ho_Chi_Minh' })

function statusFilter(booking: BookingHistoryItem, filter: Filter): boolean {
  if (filter === 'all') return true
  const category = dashboardNoticeFor(booking)?.category
  if (filter === 'closed') return !category
  return category === filter
}

export function BookingHistoryPage() {
  const [params, setParams] = useSearchParams()
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [retry, setRetry] = useState(0)
  const rawFilter = params.get('status')
  const filter: Filter = rawFilter === 'action' || rawFilter === 'processing' || rawFilter === 'issue' || rawFilter === 'closed' ? rawFilter : 'all'
  const query = (params.get('q') ?? '').trim().toLocaleLowerCase('vi-VN')
  const from = params.get('from') ?? ''
  const to = params.get('to') ?? ''

  useEffect(() => {
    let active = true
    void getMyBookings().then(
      (bookings) => { if (active) setState({ kind: 'loaded', bookings }) },
      (error: unknown) => { if (active) setState({ kind: 'error', message: isAxiosError<{ message?: string }>(error) ? error.response?.data?.message ?? 'Không thể tải lịch sử đặt vé.' : 'Không thể tải lịch sử đặt vé.' }) },
    )
    return () => { active = false }
  }, [retry])

  const items = useMemo(() => {
    if (state.kind !== 'loaded') return []
    return state.bookings.filter((booking) => {
      const bookedDate = vietnamDate.format(new Date(booking.createdAt))
      return statusFilter(booking, filter) && (!query || booking.bookingCode.toLocaleLowerCase('vi-VN').includes(query))
        && (!from || bookedDate >= from) && (!to || bookedDate <= to)
    }).sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt))
  }, [state, filter, query, from, to])

  const apply = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const values = new FormData(event.currentTarget)
    const next = new URLSearchParams()
    for (const key of ['status', 'q', 'from', 'to'] as const) {
      const value = String(values.get(key) ?? '').trim()
      if (value && !(key === 'status' && value === 'all')) next.set(key, value)
    }
    setParams(next)
  }

  return <main className="min-h-screen bg-background py-8 sm:py-12"><div className="mx-auto max-w-5xl px-4 sm:px-6">
    <Link to="/me" className="text-sm font-semibold text-primary hover:underline">← Dashboard</Link>
    <h1 className="mt-4 text-3xl font-bold text-text-primary">Lịch sử đặt vé</h1>
    <p className="mt-2 text-text-secondary">Theo dõi tất cả booking, kể cả giao dịch chưa phát hành vé.</p>

    <form key={params.toString()} onSubmit={apply} className="mt-6 grid gap-3 rounded-xl border border-border bg-surface p-4 sm:grid-cols-2 lg:grid-cols-[1fr_1.2fr_160px_160px_auto]" aria-label="Lọc lịch sử đặt vé">
      <label className="text-sm font-medium">Trạng thái<select name="status" defaultValue={filter} className="control mt-1"><option value="all">Tất cả</option><option value="issue">Có vấn đề</option><option value="action">Cần bạn xử lý</option><option value="processing">Đang được xử lý</option><option value="closed">Hoàn tất / đã đóng</option></select></label>
      <label className="text-sm font-medium">Mã booking<input name="q" defaultValue={params.get('q') ?? ''} placeholder="Tìm mã booking" className="control mt-1" /></label>
      <label className="text-sm font-medium">Từ ngày<input name="from" type="date" defaultValue={from} className="control mt-1" /></label>
      <label className="text-sm font-medium">Đến ngày<input name="to" type="date" defaultValue={to} className="control mt-1" /></label>
      <button className="primary-button self-end">Áp dụng</button>
    </form>
    {(filter !== 'all' || query || from || to) && <button className="mt-3 min-h-11 text-sm font-semibold text-primary underline" onClick={() => setParams({})}>Xóa bộ lọc</button>}

    {state.kind === 'loading' && <div className="mt-6 space-y-4" aria-busy="true">{[1, 2, 3].map((key) => <div key={key} className="h-36 animate-pulse rounded-xl bg-slate-200" />)}</div>}
    {state.kind === 'error' && <div role="alert" className="mt-6 rounded-xl border border-red-200 bg-red-50 p-5 text-red-800"><p>{state.message}</p><button className="mt-3 min-h-11 font-semibold underline" onClick={() => { setState({ kind: 'loading' }); setRetry((value) => value + 1) }}>Thử lại</button></div>}
    {state.kind === 'loaded' && items.length === 0 && <div className="mt-6 rounded-xl border border-border bg-surface p-7 text-center"><p>{state.bookings.length ? 'Không có booking khớp bộ lọc.' : 'Bạn chưa có booking nào.'}</p>{!state.bookings.length && <Link to="/movies" className="mt-4 inline-flex min-h-11 items-center rounded-lg bg-primary px-4 font-semibold text-white">Khám phá phim</Link>}</div>}
    {state.kind === 'loaded' && items.length > 0 && <ul className="mt-6 space-y-4">{items.map((booking) => <li key={booking.bookingId}><article className="rounded-xl border border-border bg-surface p-5">
      <div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-xs font-semibold text-primary">{booking.bookingCode}</p><h2 className="mt-1 text-lg font-bold text-text-primary">{booking.movieTitle}</h2></div><span className="rounded-full bg-primary-soft px-3 py-1 text-sm font-semibold text-primary">{bookingStatusLabel(booking)}</span></div>
      <p className="mt-3 text-sm text-text-secondary">{dateTime.format(new Date(booking.startAt))} · {booking.cinemaName} · {booking.auditoriumName}</p>
      <p className="mt-1 text-sm text-text-secondary">Ghế {booking.seatLabels.join(', ')} · Đặt lúc {dateTime.format(new Date(booking.createdAt))}</p>
      {booking.showtimeStatus === 'CANCELLED' && refundStatusLabel(booking) && <p className="mt-2 text-sm font-semibold text-text-secondary">{refundStatusLabel(booking)}</p>}
      {booking.ticketCode ? <Link to={`/tickets/${encodeURIComponent(booking.ticketCode)}`} className="mt-4 inline-flex min-h-11 items-center rounded-lg border border-primary px-4 text-sm font-semibold text-primary hover:bg-primary-soft">Xem vé</Link>
        : booking.canResumePayment ? <Link to={`/checkout/${encodeURIComponent(booking.bookingCode)}`} className="mt-4 inline-flex min-h-11 items-center rounded-lg border border-primary px-4 text-sm font-semibold text-primary hover:bg-primary-soft">Tiếp tục thanh toán</Link> : null}
    </article></li>)}</ul>}
  </div></main>
}
