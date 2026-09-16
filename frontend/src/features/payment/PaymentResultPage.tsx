import { isAxiosError } from 'axios'
import { CircleCheck, CircleQuestionMark, CircleX, Clock3, LoaderCircle, RefreshCw, TimerOff, type LucideIcon } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getPaymentStatus, type Payment } from './paymentApi'

type LoadState = { kind: 'loading' } | { kind: 'loaded'; payment: Payment } | { kind: 'error'; message: string }

const POLL_INTERVAL_MS = 3_000

function errorMessage(error: unknown): string {
  if (isAxiosError<{ message?: string }>(error)) return error.response?.data?.message ?? 'Không thể xác nhận trạng thái thanh toán.'
  return 'Không thể kết nối tới hệ thống. Vui lòng thử kiểm tra lại trạng thái giao dịch.'
}

type DisplayState = 'VERIFYING' | 'SUCCESS' | 'FAILED' | 'PAYMENT_REVIEW' | 'REFUND_PENDING' | 'EXPIRED' | 'UNCERTAIN'

function displayState(payment: Payment): DisplayState {
  if (payment.bookingStatus === 'REFUND_PENDING') return 'REFUND_PENDING'
  if (payment.bookingStatus === 'PAYMENT_REVIEW') return 'PAYMENT_REVIEW'
  if (payment.status === 'SUCCESS' && payment.bookingStatus === 'PAID') return 'SUCCESS'
  if (payment.status === 'FAILED') return 'FAILED'
  if (payment.status === 'EXPIRED' || payment.bookingStatus === 'EXPIRED') return 'EXPIRED'
  if (payment.status === 'INITIATED') return 'VERIFYING'
  return 'UNCERTAIN'
}

const copy: Record<DisplayState, { icon: LucideIcon; title: string; description: string; tone: string }> = {
  VERIFYING: { icon: LoaderCircle, title: 'Đang xác nhận thanh toán', description: 'Giao dịch đang được xác nhận. Vui lòng không thanh toán lại.', tone: 'text-primary' },
  SUCCESS: { icon: CircleCheck, title: 'Thanh toán thành công', description: 'Thanh toán đã được backend xác nhận. Vé của bạn đang sẵn sàng.', tone: 'text-emerald-700' },
  FAILED: { icon: CircleX, title: 'Thanh toán chưa thành công', description: 'Giao dịch không được hoàn tất. Hãy xem booking trước khi thực hiện thao tác khác.', tone: 'text-red-700' },
  PAYMENT_REVIEW: { icon: Clock3, title: 'Đang xác nhận giao dịch', description: 'Chúng tôi đang đối soát giao dịch. Không cần thanh toán lại.', tone: 'text-amber-800' },
  REFUND_PENDING: { icon: RefreshCw, title: 'Đang xử lý hoàn tiền', description: 'Thanh toán được ghi nhận sau hạn. Vé sẽ không được phát hành.', tone: 'text-amber-800' },
  EXPIRED: { icon: TimerOff, title: 'Phiên đặt vé đã hết hạn', description: 'Ghế đã được giải phóng. Bạn có thể chọn một suất chiếu khác.', tone: 'text-red-700' },
  UNCERTAIN: { icon: CircleQuestionMark, title: 'Chưa thể xác nhận giao dịch', description: 'Trạng thái chưa đủ rõ ràng. Vui lòng kiểm tra lại thay vì thanh toán lại.', tone: 'text-amber-800' },
}

export function PaymentResultPage() {
  const { paymentId = '' } = useParams()
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    if (!paymentId) return
    let active = true
    async function load() {
      try {
        const payment = await getPaymentStatus(paymentId)
        if (active) setState({ kind: 'loaded', payment })
      } catch (error) {
        if (active) setState({ kind: 'error', message: errorMessage(error) })
      }
    }
    void load()
    return () => { active = false }
  }, [paymentId, attempt])
  useEffect(() => {
    if (state.kind !== 'loaded' || displayState(state.payment) !== 'VERIFYING') return
    const timer = window.setTimeout(() => setAttempt((value) => value + 1), POLL_INTERVAL_MS)
    return () => window.clearTimeout(timer)
  }, [state])

  if (!paymentId) return <main className="grid min-h-screen place-items-center bg-background p-6"><section role="alert" className="max-w-md rounded-xl border border-red-200 bg-surface p-6 text-red-800">Không tìm thấy mã thanh toán.</section></main>
  if (state.kind === 'loading') return <main className="grid min-h-screen place-items-center bg-background p-6" aria-busy="true"><section className="w-full max-w-md rounded-xl border border-border bg-surface p-8 text-center"><LoaderCircle className="mx-auto h-12 w-12 animate-spin text-primary" aria-hidden="true" /><h1 className="mt-4 text-xl font-bold">Đang xác nhận thanh toán</h1></section></main>
  if (state.kind === 'error') return <main className="grid min-h-screen place-items-center bg-background p-6"><section className="w-full max-w-md rounded-xl border border-red-200 bg-surface p-8 text-center"><h1 className="text-xl font-bold">Chưa thể tải trạng thái giao dịch</h1><p role="alert" className="mt-3 text-sm text-text-secondary">{state.message}</p><button onClick={() => setAttempt((value) => value + 1)} className="mt-6 min-h-11 rounded-lg bg-primary px-4 font-semibold text-white">Kiểm tra lại</button></section></main>

  const payment = state.payment
  const display = displayState(payment)
  const content = copy[display]
  const StatusIcon = content.icon
  const bookingLink = `/checkout/${encodeURIComponent(payment.bookingCode)}`
  return <main className="grid min-h-screen place-items-center bg-background p-6"><section className="w-full max-w-md rounded-xl border border-border bg-surface p-8 text-center shadow-sm" aria-live="polite">
    <StatusIcon className={`mx-auto h-12 w-12 ${content.tone}`} aria-hidden="true" />
    <h1 className="mt-4 text-2xl font-bold text-text-primary">{content.title}</h1>
    <p className="mt-3 text-sm leading-6 text-text-secondary">{content.description}</p>
    <p className="mt-5 rounded-lg bg-background px-4 py-3 text-sm text-text-secondary">Booking: <span className="font-semibold text-text-primary">{payment.bookingCode}</span></p>
    <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
      {display === 'SUCCESS' && <Link to={bookingLink} className="min-h-11 rounded-lg bg-primary px-4 py-3 font-semibold text-white">Xem booking</Link>}
      {(display === 'FAILED' || display === 'PAYMENT_REVIEW' || display === 'REFUND_PENDING' || display === 'UNCERTAIN') && <Link to={bookingLink} className="min-h-11 rounded-lg border border-primary px-4 py-3 font-semibold text-primary">Xem booking</Link>}
      {display === 'EXPIRED' && <Link to="/movies" className="min-h-11 rounded-lg bg-primary px-4 py-3 font-semibold text-white">Chọn suất khác</Link>}
      {display === 'VERIFYING' && <p role="status" className="py-3 text-sm font-medium text-text-secondary">Tự động kiểm tra lại sau ít giây</p>}
      <Link to="/" className="min-h-11 rounded-lg border border-border px-4 py-3 font-semibold text-text-primary">Về trang chủ</Link>
    </div>
  </section></main>
}
