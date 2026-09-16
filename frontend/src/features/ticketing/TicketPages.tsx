import { isAxiosError } from 'axios'
import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { toDataURL } from 'qrcode'
import { getMyBookings, getTicket, resendTicketEmail, type BookingHistoryItem, type Ticket } from './ticketApi'

type LoadState<T> = { kind: 'loading' } | { kind: 'loaded'; data: T } | { kind: 'error'; message: string }
type TicketTab = 'upcoming' | 'past' | 'closed'

const dateTime = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'medium',
  timeStyle: 'short',
  timeZone: 'Asia/Ho_Chi_Minh',
})

function errorMessage(error: unknown): string {
  if (isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message ?? 'Không thể tải dữ liệu vé.'
  }
  return 'Không thể kết nối tới hệ thống. Vui lòng thử lại.'
}

function ticketLabel(status: BookingHistoryItem['ticketStatus']): string {
  if (status === 'VALID') return 'Hợp lệ'
  if (status === 'USED') return 'Đã sử dụng'
  if (status === 'CANCELLED') return 'Đã hủy'
  return 'Chưa phát hành'
}

function TicketStatus({ status }: { status: BookingHistoryItem['ticketStatus'] }) {
  return <span className="rounded-full bg-primary-soft px-2 py-1 text-xs font-semibold text-primary">{ticketLabel(status)}</span>
}

function isInTab(item: BookingHistoryItem, tab: TicketTab): boolean {
  if (item.ticketStatus === 'CANCELLED' || item.bookingStatus === 'REFUND_PENDING' || item.bookingStatus === 'REFUNDED') {
    return tab === 'closed'
  }
  const isFuture = Date.parse(item.startAt) >= Date.now()
  return tab === 'upcoming' ? isFuture : !isFuture
}

export function MyTicketsPage() {
  const [params, setParams] = useSearchParams()
  const selected = params.get('tab')
  const tab: TicketTab = selected === 'past' || selected === 'closed' ? selected : 'upcoming'
  const [state, setState] = useState<LoadState<BookingHistoryItem[]>>({ kind: 'loading' })
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    let active = true
    void getMyBookings().then(
      (data) => { if (active) setState({ kind: 'loaded', data }) },
      (error: unknown) => { if (active) setState({ kind: 'error', message: errorMessage(error) }) },
    )
    return () => { active = false }
  }, [retry])

  const items = state.kind === 'loaded' ? state.data.filter((item) => isInTab(item, tab)) : []
  return (
    <main className="min-h-screen bg-background py-8 sm:py-12">
      <section className="mx-auto max-w-5xl px-4 sm:px-6">
        <h1 className="text-3xl font-bold text-text-primary">Vé của tôi</h1>
        <p className="mt-2 text-sm text-text-secondary">Vé và lịch sử đặt chỗ được đồng bộ trực tiếp từ hệ thống.</p>
        <div className="mt-6 flex gap-2 border-b border-border" role="tablist" aria-label="Lọc vé">
          <Tab current={tab} value="upcoming" onSelect={(next) => setParams({ tab: next })}>Sắp xem</Tab>
          <Tab current={tab} value="past" onSelect={(next) => setParams({ tab: next })}>Đã xem</Tab>
          <Tab current={tab} value="closed" onSelect={(next) => setParams({ tab: next })}>Đã hủy / hoàn</Tab>
        </div>
        {state.kind === 'loading' && (
          <div className="mt-6 space-y-4" aria-busy="true">
            {[1, 2].map((key) => <div key={key} className="h-44 animate-pulse rounded-xl bg-slate-200" />)}
          </div>
        )}
        {state.kind === 'error' && (
          <div role="alert" className="mt-6 rounded-xl border border-red-200 bg-red-50 p-5 text-red-800">
            <p>{state.message}</p>
            <button className="mt-3 min-h-11 font-semibold underline" onClick={() => { setState({ kind: 'loading' }); setRetry((value) => value + 1) }}>Thử lại</button>
          </div>
        )}
        {state.kind === 'loaded' && items.length === 0 && (
          <div className="mt-8 rounded-xl border border-border bg-surface p-8 text-center">
            <h2 className="text-lg font-semibold">Bạn chưa có vé trong mục này</h2>
            <Link to="/movies" className="mt-4 inline-flex min-h-11 items-center rounded-lg bg-primary px-4 font-semibold text-white">Khám phá phim</Link>
          </div>
        )}
        {state.kind === 'loaded' && (
          <ul className="mt-6 space-y-4">
            {items.map((item) => (
              <li key={item.bookingId}>
                <article className="flex flex-col gap-4 rounded-xl border border-border bg-surface p-4 sm:flex-row sm:p-5">
                  <Poster item={item} />
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <h2 className="text-lg font-bold text-text-primary">{item.movieTitle}</h2>
                        <p className="mt-1 text-sm text-text-secondary">{dateTime.format(new Date(item.startAt))}</p>
                      </div>
                      <TicketStatus status={item.ticketStatus} />
                    </div>
                    <p className="mt-3 text-sm text-text-secondary">{item.cinemaName} · {item.auditoriumName}</p>
                    <p className="mt-1 text-sm text-text-secondary">Ghế {item.seatLabels.join(', ')} · Booking {item.bookingCode}</p>
                    <div className="mt-4 flex flex-wrap gap-3">
                      {item.ticketCode && <Link to={`/tickets/${encodeURIComponent(item.ticketCode)}`} className="inline-flex min-h-11 items-center rounded-lg border border-primary px-4 font-semibold text-primary">Xem vé</Link>}
                      {item.bookingStatus === 'PAID' && item.ticketStatus === 'VALID' && <Link to={`/me/bookings/${encodeURIComponent(item.bookingId)}/refund`} className="inline-flex min-h-11 items-center rounded-lg border border-primary px-4 font-semibold text-primary">Yêu cầu hoàn vé</Link>}
                    </div>
                  </div>
                </article>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  )
}

function Tab({ current, value, onSelect, children }: {
  current: TicketTab
  value: TicketTab
  onSelect: (tab: TicketTab) => void
  children: string
}) {
  const active = current === value
  return <button role="tab" aria-selected={active} onClick={() => onSelect(value)} className={`min-h-11 border-b-2 px-3 text-sm font-semibold ${active ? 'border-primary text-primary' : 'border-transparent text-text-secondary'}`}>{children}</button>
}

function Poster({ item }: { item: BookingHistoryItem }) {
  if (item.posterUrl) {
    return <img className="h-32 w-24 rounded-lg object-cover" src={item.posterUrl} alt={`Poster phim ${item.movieTitle}`} />
  }
  return <div className="grid h-32 w-24 place-items-center rounded-lg bg-slate-200 p-2 text-center text-xs font-semibold text-text-secondary">{item.movieTitle}</div>
}

function TicketQr({ payload, ticketCode }: { payload: string; ticketCode: string }) {
  const [image, setImage] = useState<string>()
  useEffect(() => {
    let active = true
    void toDataURL(payload, { width: 280, margin: 1, errorCorrectionLevel: 'M' }).then((value) => {
      if (active) setImage(value)
    })
    return () => { active = false }
  }, [payload])

  if (!image) return <div className="mx-auto mt-6 h-[304px] w-[304px] animate-pulse rounded-xl bg-slate-100" aria-busy="true" />
  return <div className="mx-auto mt-6 inline-block rounded-xl bg-white p-3"><img src={image} alt={`Mã QR vé ${ticketCode}`} width="280" height="280" /></div>
}

export function TicketDetailPage() {
  const { ticketCode = '' } = useParams()
  const [state, setState] = useState<LoadState<Ticket>>({ kind: 'loading' })
  const [retry, setRetry] = useState(0)
  const [emailState, setEmailState] = useState<'idle' | 'sending' | 'sent' | 'error'>('idle')

  useEffect(() => {
    let active = true
    void getTicket(ticketCode).then(
      (data) => { if (active) setState({ kind: 'loaded', data }) },
      (error: unknown) => { if (active) setState({ kind: 'error', message: errorMessage(error) }) },
    )
    return () => { active = false }
  }, [ticketCode, retry])

  if (state.kind === 'loading') {
    return <main className="grid min-h-screen place-items-center bg-background p-6" aria-busy="true"><div className="h-80 w-full max-w-md animate-pulse rounded-xl bg-slate-200" /></main>
  }
  if (state.kind === 'error') {
    return <main className="grid min-h-screen place-items-center bg-background p-6"><section role="alert" className="max-w-md rounded-xl border border-red-200 bg-red-50 p-6 text-red-800"><p>{state.message}</p><button className="mt-4 min-h-11 font-semibold underline" onClick={() => { setState({ kind: 'loading' }); setRetry((value) => value + 1) }}>Thử lại</button></section></main>
  }

  const ticket = state.data
  const resendEmail = () => {
    setEmailState('sending')
    void resendTicketEmail(ticket.id).then(
      () => setEmailState('sent'),
      () => setEmailState('error'),
    )
  }
  return (
    <main className="min-h-screen bg-background py-8 sm:py-12">
      <section className="mx-auto max-w-lg px-4 sm:px-6">
        <Link className="text-sm font-semibold text-primary hover:underline" to="/me/tickets">← Vé của tôi</Link>
        <article className="mt-4 rounded-xl border border-border bg-surface p-6 text-center">
          <p className="text-sm font-semibold text-primary">{ticket.status === 'VALID' ? 'VÉ HỢP LỆ' : ticketLabel(ticket.status)}</p>
          <h1 className="mt-3 text-2xl font-bold text-text-primary">{ticket.movieTitle}</h1>
          <p className="mt-2 text-sm text-text-secondary">{dateTime.format(new Date(ticket.startAt))}</p>
          <p className="mt-1 text-sm text-text-secondary">{ticket.cinemaName} · {ticket.auditoriumName}</p>
          <p className="mt-4 text-sm font-semibold text-text-primary">Ghế {ticket.seatLabels.join(', ')}</p>
          <p className="mt-1 text-sm text-text-secondary">Booking {ticket.bookingCode}</p>
          {ticket.qrPayload
            ? <TicketQr key={ticket.ticketCode} payload={ticket.qrPayload} ticketCode={ticket.ticketCode} />
            : <p className="mt-6 rounded-lg bg-amber-50 p-4 text-sm text-amber-900">Mã QR chỉ hiển thị cho đúng chủ tài khoản đặt vé.</p>}
          <p className="mt-4 font-mono text-xs text-text-secondary">{ticket.ticketCode}</p>
          {ticket.qrPayload && (
            <div className="mt-5">
              <button disabled={emailState === 'sending' || emailState === 'sent'} onClick={resendEmail} className="min-h-11 rounded-lg border border-primary px-4 text-sm font-semibold text-primary disabled:cursor-not-allowed disabled:opacity-60">
                {emailState === 'sending' ? 'Đang gửi…' : emailState === 'sent' ? 'Đã gửi lại email' : 'Gửi lại email vé'}
              </button>
              {emailState === 'error' && <p role="alert" className="mt-2 text-sm text-error">Không thể gửi email lúc này. Vui lòng thử lại sau.</p>}
            </div>
          )}
        </article>
      </section>
    </main>
  )
}
