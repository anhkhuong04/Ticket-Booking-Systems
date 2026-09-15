import { isAxiosError } from 'axios'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { createSeatHold, getSeatHold, releaseSeatHold, type SeatHold } from './reservationApi'
import { getShowtimeSeatMap, type ShowtimeSeat, type ShowtimeSeatMap } from '../showtime/showtimeApi'
import { checkout } from '../booking/bookingApi'

type LoadState = { kind: 'loading' } | { kind: 'loaded'; data: ShowtimeSeatMap } | { kind: 'error' }
type Notice = { kind: 'conflict' | 'network'; message: string } | null

const formatter = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
const dateFormatter = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' })

function storageKey(showtimeId: string) { return `lak:seat-hold:${showtimeId}` }
function checkoutStorageKey(holdId: string) { return `lak:booking-checkout:${holdId}` }
function checkoutVoucherStorageKey(holdId: string) { return `lak:booking-checkout-voucher:${holdId}` }

function websocketUrl(): string {
  const configured = import.meta.env.VITE_API_URL?.replace(/\/$/, '')
  const apiUrl = configured ? new URL(configured) : new URL(window.location.origin)
  apiUrl.protocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:'
  apiUrl.pathname = `${apiUrl.pathname.replace(/\/$/, '')}/ws/seats`
  return apiUrl.toString()
}

function idempotencyKey(): string {
  return crypto.randomUUID()
}

function remainingSeconds(hold: SeatHold): number {
  const serverOffset = Date.parse(hold.serverNow) - Date.now()
  return Math.max(0, Math.ceil((Date.parse(hold.expiresAt) - (Date.now() + serverOffset)) / 1000))
}

function countdown(seconds: number): string {
  return `${Math.floor(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}`
}

function messageFor(error: unknown): string {
  if (isAxiosError<{ message?: string }>(error)) return error.response?.data?.message ?? 'Không thể cập nhật trạng thái ghế.'
  return 'Không thể kết nối tới hệ thống. Vui lòng thử lại.'
}

export function SeatSelectionPage() {
  const { showtimeId = '' } = useParams()
  const navigate = useNavigate()
  const [seatMap, setSeatMap] = useState<LoadState>({ kind: 'loading' })
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [hold, setHold] = useState<SeatHold | null>(null)
  const [pending, setPending] = useState(false)
  const [holdRequestKey, setHoldRequestKey] = useState<string | null>(null)
  const [voucherCode, setVoucherCode] = useState('')
  const [voucherError, setVoucherError] = useState<string | null>(null)
  const [checkoutUncertain, setCheckoutUncertain] = useState(false)
  const [notice, setNotice] = useState<Notice>(null)
  const [expired, setExpired] = useState(false)
  const [realtimeDisconnected, setRealtimeDisconnected] = useState(false)
  const [, setTick] = useState(0)

  const load = useCallback(() => {
    setSeatMap((current) => current.kind === 'loaded' ? current : { kind: 'loading' })
    return getShowtimeSeatMap(showtimeId).then(
      (data) => setSeatMap({ kind: 'loaded', data }),
      () => setSeatMap({ kind: 'error' }),
    )
  }, [showtimeId])

  const clearHold = useCallback(() => {
    sessionStorage.removeItem(storageKey(showtimeId))
    setHold(null)
    setSelectedIds([])
    setHoldRequestKey(null)
    setVoucherCode('')
    setVoucherError(null)
    setCheckoutUncertain(false)
  }, [showtimeId])

  useEffect(() => { void Promise.resolve().then(load) }, [load])
  useEffect(() => {
    const savedHoldId = sessionStorage.getItem(storageKey(showtimeId))
    if (!savedHoldId) return
    void getSeatHold(savedHoldId).then(
      (saved) => {
        if (saved.status === 'ACTIVE') {
          setHold(saved); setSelectedIds(saved.showtimeSeatIds)
          setVoucherCode(sessionStorage.getItem(checkoutVoucherStorageKey(saved.id)) ?? '')
          setCheckoutUncertain(sessionStorage.getItem(checkoutStorageKey(saved.id)) !== null)
        } else { clearHold() }
      },
      (error) => {
        if (isAxiosError(error) && error.response?.status === 410) setExpired(true)
        else setNotice({ kind: 'network', message: messageFor(error) })
        clearHold()
      },
    )
  }, [clearHold, showtimeId])
  useEffect(() => {
    if (!hold) return
    const timer = window.setInterval(() => {
      setTick((value) => value + 1)
      if (remainingSeconds(hold) === 0) {
        setExpired(true)
        clearHold()
        void load()
      }
    }, 1_000)
    return () => window.clearInterval(timer)
  }, [clearHold, hold, load])
  useEffect(() => {
    let socket: WebSocket | undefined
    let reconnect: number | undefined
    let closed = false
    const connect = () => {
      try {
        socket = new WebSocket(websocketUrl())
        socket.onopen = () => setRealtimeDisconnected(false)
        socket.onmessage = (event) => {
          try {
            const data: unknown = JSON.parse(String(event.data))
            if (typeof data === 'object' && data !== null && 'showtimeId' in data && data.showtimeId === showtimeId) void load()
          } catch {
            setRealtimeDisconnected(true)
          }
        }
        socket.onclose = () => {
          if (!closed) {
            setRealtimeDisconnected(true)
            reconnect = window.setTimeout(connect, 3_000)
          }
        }
      } catch {
        setRealtimeDisconnected(true)
        reconnect = window.setTimeout(connect, 3_000)
      }
    }
    connect()
    return () => { closed = true; if (reconnect) window.clearTimeout(reconnect); socket?.close() }
  }, [load, showtimeId])
  useEffect(() => {
    if (!expired) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setExpired(false)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [expired])

  const selectedSeats = useMemo(() => seatMap.kind === 'loaded'
    ? seatMap.data.seats.filter((seat) => selectedIds.includes(seat.id)) : [], [seatMap, selectedIds])
  const total = selectedSeats.reduce((sum, seat) => sum + seat.price, 0)
  const seconds = hold ? remainingSeconds(hold) : null

  const toggleSeat = (seat: ShowtimeSeat) => {
    if (hold || seat.status !== 'AVAILABLE' || seatMap.kind !== 'loaded') return
    const linked = seat.pairKey ? seatMap.data.seats.filter((candidate) => candidate.pairKey === seat.pairKey) : [seat]
    if (linked.some((candidate) => candidate.status !== 'AVAILABLE')) return
    const ids = linked.map((candidate) => candidate.id)
    setHoldRequestKey(null)
    setSelectedIds((current) => current.some((id) => ids.includes(id))
      ? current.filter((id) => !ids.includes(id))
      : [...new Set([...current, ...ids])])
  }

  const holdSeats = async () => {
    if (!selectedIds.length || pending || hold) return
    setPending(true); setNotice(null)
    const requestKey = holdRequestKey ?? idempotencyKey()
    setHoldRequestKey(requestKey)
    try {
      const created = await createSeatHold(showtimeId, selectedIds, requestKey)
      sessionStorage.setItem(storageKey(showtimeId), created.id)
      setHold(created)
      setSelectedIds(created.showtimeSeatIds)
      setHoldRequestKey(null)
      setVoucherCode('')
      setVoucherError(null)
      setCheckoutUncertain(false)
      await load()
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 409) {
        setNotice({ kind: 'conflict', message: 'Một hoặc nhiều ghế vừa được người khác giữ. Sơ đồ ghế đã được cập nhật.' })
        setSelectedIds([])
        setHoldRequestKey(null)
        await load()
      } else if (isAxiosError(error) && error.response?.status === 410) {
        setHoldRequestKey(null); setExpired(true); clearHold(); await load()
      } else setNotice({ kind: 'network', message: messageFor(error) })
    } finally { setPending(false) }
  }

  const cancelHold = async () => {
    if (!hold || pending) return
    setPending(true)
    try {
      await releaseSeatHold(hold.id)
      sessionStorage.removeItem(checkoutStorageKey(hold.id))
      sessionStorage.removeItem(checkoutVoucherStorageKey(hold.id))
      clearHold(); await load()
    }
    catch (error) { setNotice({ kind: 'network', message: messageFor(error) }) }
    finally { setPending(false) }
  }

  const confirmCheckout = async () => {
    if (!hold || pending) return
    const normalizedVoucher = voucherCode.trim().toUpperCase()
    const storedKey = sessionStorage.getItem(checkoutStorageKey(hold.id))
    const storedVoucher = sessionStorage.getItem(checkoutVoucherStorageKey(hold.id))
    if (storedKey && storedVoucher !== normalizedVoucher) {
      setVoucherError('Yêu cầu trước đang chờ xác nhận. Vui lòng thử lại với cùng mã giảm giá.')
      return
    }
    setPending(true); setNotice(null)
    const key = storedKey ?? idempotencyKey()
    sessionStorage.setItem(checkoutStorageKey(hold.id), key)
    sessionStorage.setItem(checkoutVoucherStorageKey(hold.id), normalizedVoucher)
    try {
      const booking = await checkout(hold.id, key, normalizedVoucher || undefined)
      sessionStorage.removeItem(checkoutStorageKey(hold.id))
      sessionStorage.removeItem(checkoutVoucherStorageKey(hold.id))
      sessionStorage.removeItem(storageKey(showtimeId))
      navigate(`/checkout/${booking.bookingCode}`)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 410) {
        sessionStorage.removeItem(checkoutStorageKey(hold.id)); sessionStorage.removeItem(checkoutVoucherStorageKey(hold.id)); setExpired(true); clearHold(); await load()
      } else if (isAxiosError(error) && error.response?.status === 422) {
        sessionStorage.removeItem(checkoutStorageKey(hold.id)); sessionStorage.removeItem(checkoutVoucherStorageKey(hold.id))
        setVoucherError(error.response.data?.message ?? 'Mã giảm giá không hợp lệ hoặc không còn áp dụng được.')
      } else {
        setCheckoutUncertain(true)
        setNotice({ kind: 'network', message: messageFor(error) })
      }
    } finally { setPending(false) }
  }

  if (seatMap.kind === 'loading') return <main className="min-h-screen bg-background p-6" aria-busy="true"><div className="mx-auto max-w-6xl animate-pulse space-y-5"><div className="h-12 rounded bg-slate-200" /><div className="h-96 rounded-xl bg-slate-200" /></div></main>
  if (seatMap.kind === 'error') return <main className="grid min-h-screen place-items-center bg-background p-6"><section role="alert" className="max-w-md rounded-xl border border-red-200 bg-red-50 p-6 text-red-800">Không thể tải sơ đồ ghế. <button onClick={() => void load()} className="font-semibold underline">Thử lại</button></section></main>

  const data = seatMap.data
  const rows = Object.values(data.seats.reduce<Record<string, ShowtimeSeat[]>>((result, seat) => ({ ...result, [seat.rowLabel]: [...(result[seat.rowLabel] ?? []), seat] }), {}))
  return <main className="min-h-screen bg-background pb-32 pt-6 sm:py-10"><div className="mx-auto max-w-6xl px-4 sm:px-6">{hold && <section className="mb-4 rounded-xl border border-border bg-surface p-4 sm:flex sm:items-end sm:gap-3"><div className="min-w-0 flex-1"><label htmlFor="voucher-code" className="text-sm font-semibold text-text-primary">Mã giảm giá</label><div className="mt-2 flex gap-2"><input id="voucher-code" value={voucherCode} disabled={pending || checkoutUncertain} maxLength={64} onChange={(event) => { setVoucherCode(event.target.value); setVoucherError(null) }} aria-describedby={voucherError ? 'voucher-error' : 'voucher-help'} className="min-h-11 min-w-0 flex-1 rounded-lg border border-border px-3 uppercase outline-none focus:border-primary focus:ring-2 focus:ring-primary-soft disabled:bg-slate-100" placeholder="Nhập mã" /><button type="button" onClick={() => { setVoucherCode(''); setVoucherError(null) }} disabled={!voucherCode || pending || checkoutUncertain} className="min-h-11 rounded-lg border border-border px-3 text-sm font-semibold text-text-primary disabled:cursor-not-allowed disabled:opacity-50">Xóa</button></div>{voucherError ? <p id="voucher-error" role="alert" className="mt-2 text-sm text-red-700">{voucherError}</p> : <p id="voucher-help" className="mt-2 text-xs text-text-secondary">Mã được backend kiểm tra khi xác nhận đơn.</p>}{checkoutUncertain && <p role="status" className="mt-2 text-xs text-amber-800">Yêu cầu đang chờ xác nhận. Hãy thử lại với cùng mã giảm giá.</p>}</div></section>}
    <header className="flex flex-wrap items-start justify-between gap-3 border-b border-border pb-5"><div><Link to={`/movies/${data.movieId}/showtimes`} className="text-sm font-semibold text-primary hover:underline">← Chọn suất chiếu</Link><h1 className="mt-2 text-2xl font-bold text-text-primary">{data.movieTitle}</h1><p className="mt-1 text-sm text-text-secondary">{data.cinemaName} · {data.auditoriumName} · {dateFormatter.format(new Date(data.startAt))}</p></div>{hold && seconds !== null && <p className={`rounded-lg px-3 py-2 text-sm font-bold ${seconds <= 60 ? 'bg-amber-100 text-amber-900' : 'bg-primary-soft text-primary'}`}>Giữ ghế {countdown(seconds)}</p>}</header>
    {realtimeDisconnected && <p role="status" className="mt-4 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">Kết nối realtime bị gián đoạn. Trạng thái ghế vẫn được đồng bộ lại bằng API.</p>}
    {notice && <p role="alert" className={`mt-4 rounded-lg p-3 text-sm ${notice.kind === 'conflict' ? 'border border-amber-200 bg-amber-50 text-amber-900' : 'border border-red-200 bg-red-50 text-red-800'}`}>{notice.message}</p>}
    <div className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,7fr)_minmax(17rem,3fr)]"><section aria-label="Sơ đồ ghế" className="rounded-xl border border-border bg-surface p-4 sm:p-7"><p className="mx-auto mb-8 max-w-md rounded-b-full bg-slate-800 py-2 text-center text-xs font-bold tracking-[0.2em] text-white">MÀN HÌNH</p><div className="space-y-3 overflow-x-auto">{rows.map((row) => <div key={row[0].rowLabel} className="flex min-w-max items-center gap-2"><span className="w-6 text-sm font-bold text-text-secondary">{row[0].rowLabel}</span>{row.map((seat) => <SeatButton key={seat.id} seat={seat} selected={selectedIds.includes(seat.id)} disabled={Boolean(hold)} onClick={() => toggleSeat(seat)} />)}</div>)}</div><div className="mt-8 flex flex-wrap gap-3 text-xs text-text-secondary"><span>□ Trống</span><span>✓ Đang chọn</span><span>◷ Đang giữ</span><span>▣ Đã bán</span><span>╱ Bị khóa</span><span>═ Ghế đôi</span></div></section>
      <aside className="rounded-xl border border-border bg-surface p-5 lg:sticky lg:top-6 lg:h-fit"><h2 className="text-lg font-bold">Tóm tắt đặt vé</h2><p className="mt-1 text-sm text-text-secondary">{data.cinemaName} · {data.auditoriumName}</p><ul className="mt-5 space-y-3">{selectedSeats.length ? selectedSeats.map((seat) => <li key={seat.id} className="flex justify-between gap-3 text-sm"><span>{seat.rowLabel}{seat.seatNumber} · {seat.seatType === 'COUPLE' ? 'Ghế đôi' : seat.seatType}</span><span>{formatter.format(seat.price)}</span></li>) : <li className="text-sm text-text-secondary">Chọn ghế để xem tạm tính.</li>}</ul><div className="mt-5 flex justify-between border-t border-border pt-4 font-bold"><span>Tổng</span><span>{formatter.format(total)}</span></div>{hold ? <><p className="mt-5 text-sm text-emerald-700">Ghế đã được backend xác nhận giữ.</p><button onClick={() => void confirmCheckout()} disabled={pending} className="mt-4 min-h-11 w-full rounded-lg bg-primary px-4 font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50">{pending ? 'Đang xác nhận…' : 'Tiếp tục xác nhận'}</button><button onClick={() => void cancelHold()} disabled={pending} className="mt-3 min-h-11 w-full rounded-lg border border-primary px-4 font-semibold text-primary disabled:opacity-50">Hủy giữ ghế</button></> : <button onClick={() => void holdSeats()} disabled={!selectedIds.length || pending} className="mt-5 min-h-11 w-full rounded-lg bg-primary px-4 font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50">{pending ? 'Đang giữ ghế…' : 'Giữ ghế'}</button>}</aside></div>
  </div>{expired && <div role="dialog" aria-modal="true" aria-labelledby="expired-title" aria-describedby="expired-description" className="fixed inset-0 grid place-items-center bg-slate-950/40 p-5"><section className="w-full max-w-sm rounded-2xl bg-surface p-6 shadow-xl"><h2 id="expired-title" className="text-xl font-bold">Thời gian giữ ghế đã hết</h2><p id="expired-description" className="mt-2 text-sm text-text-secondary">Ghế của bạn đã được giải phóng. Vui lòng chọn lại ghế.</p><button autoFocus onClick={() => { setExpired(false); void load() }} className="mt-6 min-h-11 rounded-lg bg-primary px-4 font-semibold text-white">Chọn lại</button></section></div>}</main>
}

function SeatButton({ seat, selected, disabled, onClick }: { seat: ShowtimeSeat; selected: boolean; disabled: boolean; onClick: () => void }) {
  const unavailable = seat.status !== 'AVAILABLE'
  const state = selected ? 'selected' : seat.status.toLowerCase()
  const classes: Record<string, string> = { selected: 'border-primary bg-primary text-white', available: 'border-slate-300 bg-white text-slate-800 hover:border-primary', held: 'border-amber-300 bg-amber-100 text-amber-900', sold: 'border-slate-300 bg-slate-200 text-slate-500', blocked: 'border-slate-300 bg-slate-100 text-slate-500', payment_pending: 'border-amber-300 bg-amber-100 text-amber-900' }
  const label = `${seat.rowLabel}${seat.seatNumber}, ${seat.seatType === 'COUPLE' ? 'ghế đôi' : seat.seatType}, ${selected ? 'đang chọn' : seat.status.toLowerCase()}`
  return <button type="button" onClick={onClick} disabled={disabled || unavailable} aria-label={label} className={`grid min-h-11 min-w-11 place-items-center rounded-lg border text-xs font-bold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:cursor-not-allowed ${classes[state]} ${seat.seatType === 'COUPLE' ? 'min-w-24' : ''}`}>{seat.rowLabel}{seat.seatNumber}</button>
}
