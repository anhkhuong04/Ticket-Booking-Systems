import { isAxiosError } from 'axios'
import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { getBooking, type Booking } from '../booking/bookingApi'
import { getMyBookings, type BookingHistoryItem } from '../ticketing/ticketApi'
import { getCustomerRefund, requestCustomerRefund, type Refund } from './refundApi'

type BookingState =
  | { kind: 'loading' }
  | { kind: 'loaded'; booking: Booking; history: BookingHistoryItem }
  | { kind: 'error'; message: string }

type RefundState =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'loaded'; refund: Refund }
  | { kind: 'error'; message: string }

const POLL_INTERVAL_MS = 3_000
const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
const dateTime = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' })

function refundErrorMessage(error: unknown): string {
  if (!isAxiosError<{ code?: string }>(error)) return 'Không thể gửi yêu cầu hoàn tiền lúc này. Vui lòng thử lại.'
  switch (error.response?.data?.code) {
    case 'REFUND_WINDOW_CLOSED':
      return 'Yêu cầu hoàn cần được gửi ít nhất 45 phút trước giờ chiếu.'
    case 'REFUND_ALREADY_REQUESTED':
      return 'Booking này đã có yêu cầu hoàn tiền hoặc không còn đủ điều kiện hoàn.'
    case 'REFUND_FORBIDDEN':
    case 'REFUND_NOT_FOUND':
      return 'Bạn không thể thực hiện thao tác này với booking đã chọn.'
    case 'IDEMPOTENCY_REQUEST_UNAVAILABLE':
      return 'Yêu cầu đang được tiếp nhận. Vui lòng kiểm tra lại trạng thái sau ít phút.'
    default:
      return 'Không thể gửi yêu cầu hoàn tiền lúc này. Vui lòng thử lại.'
  }
}

function createIdempotencyKey(): string {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  return `refund-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function isAtLeastFortyFiveMinutesAway(booking: Booking): boolean | null {
  const startAt = Date.parse(booking.startAt)
  const serverNow = Date.parse(booking.serverNow)
  if (Number.isNaN(startAt) || Number.isNaN(serverNow)) return null
  return startAt - serverNow >= 45 * 60 * 1_000
}

function EligibilityItem({ passed, pending, children }: { passed: boolean; pending?: boolean; children: string }) {
  const icon = pending ? '•' : passed ? '✓' : '!'
  const tone = pending ? 'text-text-secondary' : passed ? 'text-success' : 'text-error'
  return <li className="flex gap-3 text-sm text-text-primary"><span aria-hidden="true" className={`font-bold ${tone}`}>{icon}</span><span>{children}</span></li>
}

function RefundTracking({ refund, onRefresh }: { refund: Refund; onRefresh: () => void }) {
  const content = refund.status === 'REFUNDED'
    ? { label: 'Đã hoàn', title: 'Đã hoàn tiền', description: 'Khoản hoàn đã được xác nhận. Vé của booking này không còn hiệu lực.', tone: 'bg-emerald-50 border-emerald-200 text-emerald-900' }
    : refund.status === 'REFUND_FAILED'
      ? { label: 'Cần hỗ trợ', title: 'Hoàn tiền cần xử lý thủ công', description: 'Yêu cầu hoàn chưa thể hoàn tất tự động. Vui lòng liên hệ rạp để được hỗ trợ; không tạo thêm yêu cầu hoàn mới.', tone: 'bg-amber-50 border-amber-200 text-amber-950' }
      : { label: 'Đã tiếp nhận', title: 'Đang xử lý hoàn tiền', description: 'Yêu cầu đã được tiếp nhận. Hệ thống đang xử lý khoản hoàn; bạn không cần gửi lại yêu cầu.', tone: 'bg-primary-soft border-rose-200 text-text-primary' }

  return <section className={`rounded-xl border p-5 sm:p-7 ${content.tone}`} aria-live="polite">
    <p className="text-sm font-semibold">{content.label}</p>
    <h1 className="mt-2 text-2xl font-bold">{content.title}</h1>
    <p className="mt-3 text-sm leading-6">{content.description}</p>
    <dl className="mt-5 grid gap-3 rounded-lg bg-white/70 p-4 text-sm sm:grid-cols-2">
      <div><dt className="text-text-secondary">Số tiền hoàn</dt><dd className="mt-1 font-bold text-text-primary">{money.format(refund.amount)}</dd></div>
      <div><dt className="text-text-secondary">Yêu cầu lúc</dt><dd className="mt-1 font-semibold text-text-primary">{dateTime.format(new Date(refund.requestedAt))}</dd></div>
    </dl>
    {refund.status === 'REQUESTED' && <button className="secondary-button mt-5" onClick={onRefresh}>Cập nhật trạng thái</button>}
  </section>
}

export function RefundRequestPage() {
  const { bookingId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const refundId = searchParams.get('refundId')
  const [bookingState, setBookingState] = useState<BookingState>({ kind: 'loading' })
  const [refundState, setRefundState] = useState<RefundState>(refundId ? { kind: 'loading' } : { kind: 'idle' })
  const [bookingAttempt, setBookingAttempt] = useState(0)
  const [refundAttempt, setRefundAttempt] = useState(0)
  const [confirming, setConfirming] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [requestError, setRequestError] = useState<string | null>(null)
  const [idempotencyKey, setIdempotencyKey] = useState<string | null>(null)

  useEffect(() => {
    if (!bookingId) return
    let active = true
    async function load() {
      try {
        const history = await getMyBookings()
        const item = history.find((entry) => entry.bookingId === bookingId || entry.bookingCode === bookingId)
        if (!item) throw new Error('Booking was not found')
        const booking = await getBooking(item.bookingCode)
        if (active) setBookingState({ kind: 'loaded', booking, history: item })
      } catch {
        if (active) setBookingState({ kind: 'error', message: 'Không thể tải thông tin booking. Vui lòng thử lại.' })
      }
    }
    void load()
    return () => { active = false }
  }, [bookingId, bookingAttempt])

  useEffect(() => {
    if (!refundId) return
    let active = true
    void getCustomerRefund(refundId).then(
      (refund) => { if (active) setRefundState({ kind: 'loaded', refund }) },
      () => { if (active) setRefundState({ kind: 'error', message: 'Không thể tải trạng thái hoàn tiền. Vui lòng thử lại.' }) },
    )
    return () => { active = false }
  }, [refundId, refundAttempt])

  useEffect(() => {
    if (refundState.kind !== 'loaded' || refundState.refund.status !== 'REQUESTED') return
    const timer = window.setTimeout(() => setRefundAttempt((value) => value + 1), POLL_INTERVAL_MS)
    return () => window.clearTimeout(timer)
  }, [refundState])

  if (!bookingId) return <main className="grid min-h-screen place-items-center bg-background p-6"><section role="alert" className="max-w-md rounded-xl border border-red-200 bg-red-50 p-6 text-red-800">Không tìm thấy booking cần hoàn tiền.</section></main>
  if (bookingState.kind === 'loading') return <main className="min-h-screen bg-background p-6" aria-busy="true"><div className="mx-auto max-w-3xl animate-pulse space-y-5"><div className="h-10 rounded bg-slate-200" /><div className="h-96 rounded-xl bg-slate-200" /></div></main>
  if (bookingState.kind === 'error') return <main className="grid min-h-screen place-items-center bg-background p-6"><section role="alert" className="max-w-md rounded-xl border border-red-200 bg-red-50 p-6 text-red-800"><p>{bookingState.message}</p><button className="mt-4 min-h-11 font-semibold underline" onClick={() => { setBookingState({ kind: 'loading' }); setBookingAttempt((value) => value + 1) }}>Thử lại</button></section></main>

  const { booking, history } = bookingState
  const withinWindow = isAtLeastFortyFiveMinutesAway(booking)
  const isEligible = booking.status === 'PAID' && history.ticketStatus === 'VALID' && withinWindow === true

  async function submitRefund() {
    if (!isEligible || submitting) return
    setSubmitting(true)
    setRequestError(null)
    const key = idempotencyKey ?? createIdempotencyKey()
    setIdempotencyKey(key)
    try {
      const refund = await requestCustomerRefund(history.bookingId, key)
      setRefundState({ kind: 'loaded', refund })
      setSearchParams({ refundId: refund.id }, { replace: true })
      setConfirming(false)
    } catch (error) {
      setRequestError(refundErrorMessage(error))
    } finally {
      setSubmitting(false)
    }
  }

  return <main className="min-h-screen bg-background py-8 sm:py-12"><section className="mx-auto max-w-3xl px-4 sm:px-6">
    <Link className="text-sm font-semibold text-primary hover:underline" to="/me/tickets">← Vé của tôi</Link>
    {refundState.kind === 'loading' && <section className="mt-5 rounded-xl border border-border bg-surface p-6" aria-busy="true"><p className="font-semibold">Đang tải trạng thái hoàn tiền…</p></section>}
    {refundState.kind === 'error' && <section role="alert" className="mt-5 rounded-xl border border-red-200 bg-red-50 p-5 text-red-800"><p>{refundState.message}</p><button className="mt-3 min-h-11 font-semibold underline" onClick={() => { setRefundState({ kind: 'loading' }); setRefundAttempt((value) => value + 1) }}>Thử lại</button></section>}
    {refundState.kind === 'loaded' && <div className="mt-5"><RefundTracking refund={refundState.refund} onRefresh={() => setRefundAttempt((value) => value + 1)} /></div>}
    {refundState.kind === 'idle' && <>
      <header className="mt-5"><p className="text-sm font-semibold text-primary">Yêu cầu hoàn vé</p><h1 className="mt-2 text-3xl font-bold text-text-primary">Kiểm tra điều kiện hoàn tiền</h1><p className="mt-2 text-sm leading-6 text-text-secondary">Điều kiện và số tiền được xác nhận lại bởi hệ thống khi bạn gửi yêu cầu.</p></header>
      <section className="mt-6 rounded-xl border border-border bg-surface p-5 sm:p-7"><h2 className="text-lg font-bold text-text-primary">{booking.movieTitle}</h2><p className="mt-2 text-sm text-text-secondary">Booking {booking.bookingCode} · {dateTime.format(new Date(booking.startAt))}</p><p className="mt-1 text-sm text-text-secondary">{booking.cinemaName} · {booking.auditoriumName} · Ghế {booking.items.map((item) => item.seatLabel).join(', ')}</p><ul className="mt-6 space-y-3 rounded-lg bg-background p-4"><EligibilityItem passed={withinWindow === true} pending={withinWindow === null}>{withinWindow === false ? 'Không còn tối thiểu 45 phút trước giờ chiếu' : 'Còn tối thiểu 45 phút trước giờ chiếu'}</EligibilityItem><EligibilityItem passed={history.ticketStatus === 'VALID'}>{history.ticketStatus === 'VALID' ? 'Vé chưa được sử dụng' : 'Vé không còn ở trạng thái có thể hoàn'}</EligibilityItem><EligibilityItem passed={booking.status === 'PAID'}>{booking.status === 'PAID' ? 'Booking đã thanh toán và chưa có yêu cầu hoàn' : 'Booking không còn ở trạng thái có thể hoàn'}</EligibilityItem></ul></section>
      <section className="mt-5 rounded-xl border border-border bg-surface p-5 sm:p-7"><h2 className="text-lg font-bold text-text-primary">Xem trước khoản hoàn</h2><dl className="mt-5 space-y-3 text-sm"><div className="flex justify-between gap-4"><dt className="text-text-secondary">Hoàn toàn bộ booking</dt><dd className="font-bold text-text-primary">{money.format(booking.totalAmount)}</dd></div><div className="border-t border-border pt-3"><dt className="text-text-secondary">Voucher</dt><dd className="mt-1 text-text-primary">{booking.voucherCode ? `Voucher ${booking.voucherCode} sẽ được khôi phục nếu vẫn còn hiệu lực.` : 'Booking không dùng voucher.'}</dd></div></dl>{!isEligible && <p role="alert" className="mt-5 rounded-lg bg-amber-50 p-4 text-sm text-amber-950">Booking hiện chưa đáp ứng điều kiện hiển thị. Bạn có thể quay lại Vé của tôi hoặc tải lại để nhận trạng thái mới nhất.</p>}{requestError && <p role="alert" className="mt-5 rounded-lg bg-red-50 p-4 text-sm text-red-800">{requestError}</p>}{!confirming ? <button className="primary-button mt-6 w-full sm:w-auto" onClick={() => setConfirming(true)} disabled={!isEligible}>Tiếp tục</button> : <section className="mt-6 rounded-xl border border-primary bg-primary-soft p-5"><h2 className="text-lg font-bold text-text-primary">Xác nhận yêu cầu hoàn</h2><p className="mt-3 text-sm leading-6 text-text-secondary">Booking sẽ bị hủy và vé không còn hiệu lực sau khi yêu cầu được xử lý. Khoản hoàn dự kiến là <strong className="text-text-primary">{money.format(booking.totalAmount)}</strong>.</p><div className="mt-5 flex flex-col gap-3 sm:flex-row"><button className="secondary-button" onClick={() => setConfirming(false)} disabled={submitting}>Không</button><button className="primary-button" onClick={() => void submitRefund()} disabled={submitting}>{submitting ? 'Đang gửi yêu cầu…' : 'Xác nhận hoàn'}</button></div></section>}</section>
    </>}
  </section></main>
}
