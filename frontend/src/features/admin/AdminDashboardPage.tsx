import { isAxiosError } from 'axios'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getAdminCinemas, getReportSummary, type CinemaAdmin, type ReportSummary } from './adminApi'
import { useAuth } from '../auth/AuthProvider'

const zone = 'Asia/Ho_Chi_Minh'
const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
const number = new Intl.NumberFormat('vi-VN')
const updatedAt = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short', timeZone: zone })

function shiftDate(day: string, offset: number): string {
  const date = new Date(day + 'T00:00:00Z')
  date.setUTCDate(date.getUTCDate() + offset)
  return date.toISOString().slice(0, 10)
}

function errorMessage(error: unknown): string {
  return isAxiosError<{ message?: string }>(error)
    ? error.response?.data?.message ?? 'Không thể tải số liệu báo cáo.'
    : 'Không thể tải số liệu báo cáo.'
}

function Delta({ current, previous }: { current: number; previous: number }) {
  if (previous <= 0) return <span className="text-xs text-text-muted">Kỳ trước: {number.format(previous)}</span>
  const value = Math.round((current - previous) * 100 / previous)
  return <span className="text-xs text-text-secondary">{value > 0 ? '+' : ''}{value}% so với kỳ trước</span>
}

function MetricCard({ label, value, previous }: { label: string; value: string; previous?: { current: number; value: number } }) {
  return <article className="rounded-xl border border-border bg-surface p-5">
    <h2 className="text-sm font-medium text-text-secondary">{label}</h2>
    <p className="mt-2 text-2xl font-bold text-text-primary">{value}</p>
    {previous && <p className="mt-2"><Delta current={previous.current} previous={previous.value} /></p>}
  </article>
}

function RevenueChart({ daily }: { daily: ReportSummary['daily'] }) {
  const shown = daily.slice(-30)
  const max = Math.max(1, ...shown.map((item) => Math.abs(item.netRevenue)))
  return <section className="rounded-xl border border-border bg-surface p-5" aria-labelledby="revenue-chart-title">
    <h2 id="revenue-chart-title" className="text-lg font-semibold">Doanh thu ròng theo ngày</h2>
    <p className="mt-1 text-xs text-text-secondary">{daily.length > 30 ? 'Hiển thị 30 ngày cuối của kỳ đã chọn.' : 'Ngày theo Asia/Ho_Chi_Minh; có thể âm khi hoàn tiền lớn hơn thu trong ngày.'}</p>
    <div className="mt-5 max-h-96 space-y-2 overflow-y-auto">
      {shown.map((item) => <div key={item.date} className="grid grid-cols-[5rem_minmax(0,1fr)_7rem] items-center gap-3 text-xs sm:grid-cols-[6rem_minmax(0,1fr)_9rem]">
        <span>{item.date.slice(5)}</span>
        <div className="h-4 rounded bg-slate-100" aria-hidden="true"><div className={`h-4 rounded ${item.netRevenue < 0 ? 'bg-amber-500' : 'bg-primary'}`} style={{ width: `${Math.abs(item.netRevenue) / max * 100}%` }} /></div>
        <span className="text-right font-semibold">{money.format(item.netRevenue)}<small className="block font-normal text-text-secondary">{number.format(item.seatsSold)} ghế</small></span>
      </div>)}
    </div>
  </section>
}

export function AdminDashboardPage() {
  const { user } = useAuth()
  const [params, setParams] = useSearchParams()
  const today = new Date().toLocaleDateString('sv-SE', { timeZone: zone })
  const from = params.get('from') ?? today
  const to = params.get('to') ?? today
  const cinemaId = params.get('cinemaId') ?? ''
  const filterKey = `${from}|${to}|${cinemaId}`
  const [report, setReport] = useState<ReportSummary | null>(null)
  const [loadedFilter, setLoadedFilter] = useState({ from, to, cinemaId })
  const loadedFilterKey = `${loadedFilter.from}|${loadedFilter.to}|${loadedFilter.cinemaId}`
  const [cinemas, setCinemas] = useState<CinemaAdmin[]>([])
  const [cinemasError, setCinemasError] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [filterError, setFilterError] = useState<string | null>(null)
  const [refreshing, setRefreshing] = useState(true)
  const [retry, setRetry] = useState(0)
  const manager = !user?.roles.includes('SUPER_ADMIN')

  useEffect(() => {
    let active = true
    void getAdminCinemas().then(
      (rows) => { if (active) setCinemas(rows) },
      (reason: unknown) => { if (active) setCinemasError(errorMessage(reason)) },
    )
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    void getReportSummary({ from, to, cinemaId: cinemaId || undefined }).then(
      (value) => { if (active) { setReport(value); setLoadedFilter({ from, to, cinemaId }); setError(null); setRefreshing(false) } },
      (reason: unknown) => { if (active) { setError(errorMessage(reason)); setRefreshing(false) } },
    )
    return () => { active = false }
  }, [from, to, cinemaId, filterKey, retry])

  function apply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const values = new FormData(event.currentTarget)
    const nextFrom = String(values.get('from') ?? '')
    const nextTo = String(values.get('to') ?? '')
    const nextCinema = String(values.get('cinemaId') ?? '')
    if (!nextFrom || !nextTo || nextFrom > nextTo || Date.parse(nextTo) - Date.parse(nextFrom) > 365 * 86_400_000) {
      setFilterError('Khoảng ngày không hợp lệ hoặc vượt quá 366 ngày.')
      return
    }
    setFilterError(null)
    setRefreshing(true)
    setParams({ from: nextFrom, to: nextTo, ...(nextCinema ? { cinemaId: nextCinema } : {}) })
  }

  function preset(nextFrom: string, nextTo = today) {
    setFilterError(null)
    setRefreshing(true)
    setParams({ from: nextFrom, to: nextTo, ...(cinemaId ? { cinemaId } : {}) })
  }

  const reconciled = !report || report.daily.reduce((total, item) => total + item.netRevenue, 0) === report.netRevenue
  const alerts = report?.alerts
  const alertRows = alerts ? [
    { label: 'Đối soát thanh toán', count: alerts.paymentReview, to: '/admin/bookings?exception=PAYMENT_REVIEW' },
    { label: 'Booking quá hạn thanh toán', count: alerts.overduePayments, to: '/admin/bookings?exception=OVERDUE_PAYMENT' },
    { label: 'Hoàn tiền quá SLA', count: alerts.overdueRefunds, to: '/admin/refunds?status=REQUESTED&overdue=true' },
    { label: 'Hoàn tiền lỗi', count: alerts.failedRefunds, to: '/admin/refunds?status=REFUND_FAILED' },
    { label: 'Suất bị hủy còn giao dịch', count: alerts.cancelledShowtimeBookings, to: '/admin/bookings?exception=CANCELLED_SHOWTIME' },
    { label: 'Đã thanh toán nhưng thiếu vé', count: alerts.paidWithoutTicket, to: '/admin/bookings?exception=PAID_WITHOUT_TICKET' },
  ] : []

  return <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
    <header className="flex flex-wrap items-start justify-between gap-3">
      <div><h1 className="text-3xl font-bold text-text-primary">Dashboard</h1>
        <p className="mt-2 text-sm text-text-secondary">Snapshot vận hành và giao dịch theo giờ Việt Nam.</p></div>
      {report && <p className="text-xs text-text-secondary">Cập nhật lúc {updatedAt.format(new Date(report.asOf))}</p>}
    </header>

    <div className="mt-6 flex flex-wrap gap-2" aria-label="Khoảng thời gian nhanh">
      {[{ label: 'Hôm nay', from: today }, { label: '7 ngày', from: shiftDate(today, -6) },
        { label: '30 ngày', from: shiftDate(today, -29) }, { label: 'Tháng này', from: today.slice(0, 7) + '-01' }]
        .map((item) => <button key={item.label} type="button" onClick={() => preset(item.from)}
          className="min-h-11 rounded-lg border border-border bg-surface px-4 text-sm font-semibold hover:border-primary focus-visible:outline-2 focus-visible:outline-primary">{item.label}</button>)}
    </div>
    <form key={params.toString()} onSubmit={apply} className="mt-3 grid gap-3 rounded-xl border border-border bg-surface p-4 sm:grid-cols-2 lg:grid-cols-[1fr_1fr_1.5fr_auto]" aria-label="Bộ lọc Dashboard">
      <label className="text-sm font-medium">Từ ngày<input name="from" type="date" required defaultValue={from} className="control mt-1" /></label>
      <label className="text-sm font-medium">Đến ngày<input name="to" type="date" required defaultValue={to} className="control mt-1" /></label>
      <label className="text-sm font-medium">Chi nhánh<select name="cinemaId" defaultValue={cinemaId} disabled={manager && cinemas.length === 1} className="control mt-1">
        {!(manager && cinemas.length === 1) && <option value="">{manager ? 'Tất cả chi nhánh được phép' : 'Tất cả chi nhánh'}</option>}
        {cinemas.map((cinema) => <option value={cinema.id} key={cinema.id}>{cinema.name}</option>)}
      </select></label>
      <button className="primary-button self-end">Áp dụng</button>
    </form>
    {filterError && <p role="alert" className="mt-2 text-sm text-red-700">{filterError}</p>}
    {cinemasError && <p role="alert" className="mt-2 text-sm text-red-700">Không thể tải danh sách chi nhánh: {cinemasError}</p>}
    {(refreshing || (report !== null && loadedFilterKey !== filterKey)) && <p role="status" className="mt-4 text-sm text-text-secondary">{report ? 'Đang cập nhật số liệu…' : 'Đang tải số liệu…'}</p>}
    {error && <div role="alert" className="mt-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
      <p>{error}{report ? ' Số liệu bên dưới là kết quả tải trước đó.' : ''}</p>
      <button type="button" className="mt-2 min-h-11 font-semibold underline" onClick={() => { setRefreshing(true); setRetry((value) => value + 1) }}>Thử lại</button>
    </div>}

    {report && <>
      {!reconciled && <p role="alert" className="mt-5 rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm font-semibold text-amber-900">Số liệu doanh thu ngày chưa khớp tổng kỳ. Vui lòng thử lại hoặc kiểm tra báo cáo nguồn.</p>}
      <section className="mt-6" aria-label="Chỉ số theo kỳ">
        <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-text-muted">Theo kỳ {loadedFilter.from} — {loadedFilter.to}{loadedFilterKey !== filterKey ? ' · số liệu trước khi cập nhật' : ''}</p>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <MetricCard label="Doanh thu ròng" value={money.format(report.netRevenue)} previous={{ current: report.netRevenue, value: report.previousPeriod.netRevenue }} />
          <MetricCard label="Booking thành công" value={number.format(report.bookings)} previous={{ current: report.bookings, value: report.previousPeriod.bookings }} />
          <MetricCard label="Ghế đã bán" value={number.format(report.seatsSold)} previous={{ current: report.seatsSold, value: report.previousPeriod.seatsSold }} />
          <MetricCard label="Lấp đầy" value={`${report.occupancyPercent}%`} />
        </div>
      </section>

      <section className="mt-8 rounded-xl border border-border bg-surface p-5" aria-labelledby="alerts-title">
        <div className="flex flex-wrap items-center justify-between gap-2"><div>
          <h2 id="alerts-title" className="text-xl font-semibold">Cảnh báo vận hành</h2>
          <p className="mt-1 text-xs text-text-secondary">Hiện tại · {manager ? 'trong chi nhánh được phân quyền' : 'trong phạm vi chi nhánh của lần tải'} · không theo khoảng ngày</p>
        </div><span className="rounded-full bg-amber-100 px-3 py-1 text-sm font-bold text-amber-900">{alerts?.totalBookings ?? 0} booking cần chú ý</span></div>
        {alerts?.totalBookings === 0 ? <p className="mt-5 text-sm text-text-secondary">Chưa có ngoại lệ vận hành cần chú ý.</p>
          : <ul className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{alertRows.filter((item) => item.count > 0).map((item) => <li key={item.label}>
            <Link to={item.to + (loadedFilter.cinemaId ? (item.to.includes('?') ? '&' : '?') + 'cinemaId=' + encodeURIComponent(loadedFilter.cinemaId) : '')}
              className="flex min-h-16 items-center justify-between gap-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 hover:border-primary focus-visible:outline-2 focus-visible:outline-primary">
              <span className="text-sm font-medium">{item.label}</span><span className="text-lg font-bold">{item.count}</span>
            </Link>
          </li>)}</ul>}
      </section>

      <div className="mt-8 grid gap-5 lg:grid-cols-[1.4fr_1fr]">
        <RevenueChart daily={report.daily} />
        <section className="rounded-xl border border-border bg-surface p-5" aria-labelledby="top-movies-title">
          <h2 id="top-movies-title" className="text-lg font-semibold">Top Movies</h2>
          <p className="mt-1 text-xs text-text-secondary">Xếp theo doanh thu ròng trong kỳ; số ghế bán gộp.</p>
          {report.topMovies.length === 0 ? <p className="mt-5 text-sm text-text-secondary">Chưa có giao dịch trong kỳ.</p>
            : <ol className="mt-4 divide-y divide-border">{report.topMovies.map((movie, index) => <li className="flex justify-between gap-3 py-3 text-sm" key={movie.movieId}>
              <span className="min-w-0"><span className="font-semibold">{index + 1}. {movie.title}</span><small className="block text-text-secondary">{number.format(movie.seatsSold)} ghế đã bán</small></span>
              <span className="shrink-0 font-semibold">{money.format(movie.netRevenue)}</span>
            </li>)}</ol>}
        </section>
      </div>
    </>}
  </main>
}
