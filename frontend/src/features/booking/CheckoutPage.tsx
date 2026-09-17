import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { displayLocale } from '../../i18n'
import { localizeApiError } from '../../shared/i18n/apiError'
import { createPayment } from '../payment/paymentApi'
import { getBooking, type Booking } from './bookingApi'
import { ProfileAgeAdvisory } from '../account/ageAdvisory'
import { CheckoutBilling } from './CheckoutBilling'

type LoadState = { kind: 'loading' } | { kind: 'loaded'; booking: Booking; serverOffset: number } | { kind: 'error'; message: string }

const copy = {
  vi: { retry: 'Thử lại', otherMovie: 'Chọn phim khác', title: 'Xác nhận đặt vé', bookingCode: 'Mã đặt vé:', expired: 'Đã hết hạn thanh toán', payWithin: 'Thanh toán trong', expiredHint: 'Thời hạn thanh toán đã hết. Trạng thái cuối cùng sẽ được hệ thống xác nhận.', selectedSeats: 'Ghế đã chọn', coupleSeat: 'Ghế đôi', totalPayment: 'Tổng thanh toán', subtotal: 'Tạm tính', discount: 'Giảm giá', serviceFee: 'Phí dịch vụ', total: 'Tổng', gatewayHint: 'Cổng thanh toán sẽ mở ở bước tiếp theo sau khi hệ thống xác nhận yêu cầu.', opening: 'Đang mở cổng thanh toán…', pay: 'Thanh toán' },
  en: { retry: 'Try again', otherMovie: 'Choose another movie', title: 'Confirm booking', bookingCode: 'Booking code:', expired: 'Payment period expired', payWithin: 'Pay within', expiredHint: 'The payment period has expired. The system will confirm the final status.', selectedSeats: 'Selected seats', coupleSeat: 'Couple seat', totalPayment: 'Payment total', subtotal: 'Subtotal', discount: 'Discount', serviceFee: 'Service fee', total: 'Total', gatewayHint: 'The payment gateway will open after the system confirms your request.', opening: 'Opening payment gateway…', pay: 'Pay' },
} as const

function remainingSeconds(booking: Booking, serverOffset: number): number {
  return Math.max(0, Math.ceil((Date.parse(booking.paymentDeadline) - (Date.now() + serverOffset)) / 1_000))
}

function countdown(seconds: number): string {
  return `${Math.floor(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}`
}

export function CheckoutPage() {
  const { i18n } = useTranslation()
  const c = copy[i18n.language === 'en' ? 'en' : 'vi']
  const locale = displayLocale(i18n.language)
  const money = new Intl.NumberFormat(locale, { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
  const showtimeDate = new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' })
  const { bookingId = '' } = useParams()
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [retry, setRetry] = useState(0)
  const [, setTick] = useState(0)
  const [startingPayment, setStartingPayment] = useState(false)
  const [paymentError, setPaymentError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    void getBooking(bookingId).then(
      (booking) => { if (active) setState({ kind: 'loaded', booking, serverOffset: Date.parse(booking.serverNow) - Date.now() }) },
      (error) => { if (active) setState({ kind: 'error', message: localizeApiError(error) }) },
    )
    return () => { active = false }
  }, [bookingId, retry])

  useEffect(() => {
    if (state.kind !== 'loaded') return
    const timer = window.setInterval(() => setTick((value) => value + 1), 1_000)
    return () => window.clearInterval(timer)
  }, [state])

  if (state.kind === 'loading') return <main className="min-h-screen bg-background p-6" aria-busy="true"><div className="mx-auto max-w-5xl animate-pulse space-y-5"><div className="h-10 rounded bg-slate-200" /><div className="h-72 rounded-xl bg-slate-200" /></div></main>
  if (state.kind === 'error') return <main className="grid min-h-screen place-items-center bg-background p-6"><section role="alert" className="max-w-md rounded-xl border border-red-200 bg-red-50 p-6 text-red-800"><p>{state.message}</p><button className="mt-4 min-h-11 font-semibold underline" onClick={() => { setState({ kind: 'loading' }); setRetry((value) => value + 1) }}>{c.retry}</button></section></main>

  const booking = state.booking
  const seconds = remainingSeconds(booking, state.serverOffset)
  const expired = seconds === 0 || booking.status === 'EXPIRED'
  const canStartPayment = booking.status === 'PENDING_PAYMENT' && !expired

  async function startPayment() {
    if (!canStartPayment || startingPayment) return
    setStartingPayment(true)
    setPaymentError(null)
    try {
      const payment = await createPayment(booking.bookingCode)
      if (!payment.paymentUrl) throw new Error('Payment URL is unavailable')
      window.location.assign(payment.paymentUrl)
    } catch (error) {
      setPaymentError(localizeApiError(error))
      setStartingPayment(false)
    }
  }

  return <main className="min-h-screen bg-background pb-28 pt-6 sm:py-10"><div className="mx-auto max-w-5xl px-4 sm:px-6">
    <header className="flex flex-wrap items-start justify-between gap-4 border-b border-border pb-5"><div><Link to="/movies" className="text-sm font-semibold text-primary hover:underline">← {c.otherMovie}</Link><h1 className="mt-2 text-2xl font-bold text-text-primary">{c.title}</h1><p className="mt-1 text-sm text-text-secondary">{booking.movieTitle} · {booking.cinemaName} · {booking.auditoriumName} · {showtimeDate.format(new Date(booking.startAt))}</p><p className="mt-1 text-sm text-text-secondary">{c.bookingCode} {booking.bookingCode}</p></div><p aria-live="polite" className={`rounded-lg px-3 py-2 text-sm font-bold ${expired ? 'bg-red-50 text-red-800' : seconds <= 60 ? 'bg-amber-100 text-amber-900' : 'bg-primary-soft text-primary'}`}>{expired ? c.expired : `${c.payWithin} ${countdown(seconds)}`}</p></header>
    {expired && <p role="alert" className="mt-5 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">{c.expiredHint}</p>}
    <div className="mt-5"><ProfileAgeAdvisory rating={booking.ageRating} at={booking.startAt} compact /></div>
    <CheckoutBilling bookingCode={booking.bookingCode} payable={canStartPayment} />
    <div className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,6fr)_minmax(18rem,4fr)]"><section className="rounded-xl border border-border bg-surface p-5 sm:p-7"><h2 className="text-lg font-bold">{c.selectedSeats}</h2><ul className="mt-5 divide-y divide-border">{booking.items.map((item) => <li key={item.showtimeSeatId} className="flex items-center justify-between gap-4 py-3 text-sm"><span><span className="font-semibold">{item.seatLabel}</span> · {item.seatType === 'COUPLE' ? c.coupleSeat : item.seatType}</span><span>{money.format(item.unitPrice)}</span></li>)}</ul></section>
      <aside className="rounded-xl border border-border bg-surface p-5 lg:sticky lg:top-6 lg:h-fit"><h2 className="text-lg font-bold">{c.totalPayment}</h2><dl className="mt-5 space-y-3 text-sm"><div className="flex justify-between gap-3"><dt>{c.subtotal}</dt><dd>{money.format(booking.subtotal)}</dd></div>{booking.voucherCode && <div className="flex justify-between gap-3"><dt>Voucher</dt><dd>{booking.voucherCode}</dd></div>}<div className="flex justify-between gap-3"><dt>{c.discount}</dt><dd>{money.format(booking.discountAmount)}</dd></div><div className="flex justify-between gap-3"><dt>{c.serviceFee}</dt><dd>{money.format(booking.serviceFee)}</dd></div><div className="flex justify-between gap-3 border-t border-border pt-4 text-base font-bold"><dt>{c.total}</dt><dd>{money.format(booking.totalAmount)}</dd></div></dl><p className="mt-5 text-sm text-text-secondary">{c.gatewayHint}</p>{paymentError && <p role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">{paymentError}</p>}<button disabled={!canStartPayment || startingPayment} onClick={() => void startPayment()} className="mt-5 min-h-11 w-full rounded-lg bg-primary px-4 font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50">{startingPayment ? c.opening : `${c.pay} ${money.format(booking.totalAmount)}`}</button></aside></div>
  </div></main>
}
