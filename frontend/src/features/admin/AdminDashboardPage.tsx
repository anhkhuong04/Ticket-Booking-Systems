import { isAxiosError } from 'axios'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import i18n, { displayLocale } from '../../i18n'
import { getAdminCinemas, getReportSummary, type CinemaAdmin, type ReportSummary } from './adminApi'
import { useAuth } from '../auth/AuthProvider'

const zone = 'Asia/Ho_Chi_Minh'
const copy = {
  vi: { previous: 'Kỳ trước', vsPrevious: 'so với kỳ trước', revenueDaily: 'Doanh thu ròng theo ngày', last30: 'Hiển thị 30 ngày cuối của kỳ đã chọn.', dailyNote: 'Ngày theo Asia/Ho_Chi_Minh; có thể âm khi hoàn tiền lớn hơn thu trong ngày.', seats: 'ghế', intro: 'Snapshot vận hành và giao dịch theo giờ Việt Nam.', updated: 'Cập nhật lúc', quickRange: 'Khoảng thời gian nhanh', today: 'Hôm nay', days7: '7 ngày', days30: '30 ngày', thisMonth: 'Tháng này', filter: 'Bộ lọc Dashboard', from: 'Từ ngày', to: 'Đến ngày', branch: 'Chi nhánh', allAllowed: 'Tất cả chi nhánh được phép', allBranches: 'Tất cả chi nhánh', apply: 'Áp dụng', loading: 'Đang tải số liệu…', refreshing: 'Đang cập nhật số liệu…', retry: 'Thử lại', period: 'Theo kỳ', stale: 'số liệu trước khi cập nhật', netRevenue: 'Doanh thu ròng', bookings: 'Booking thành công', seatsSold: 'Ghế đã bán', occupancy: 'Lấp đầy', alerts: 'Cảnh báo vận hành', noAlerts: 'Chưa có ngoại lệ vận hành cần chú ý.', topMovies: 'Top Movies', noTransactions: 'Chưa có giao dịch trong kỳ.' },
  en: { previous: 'Previous period', vsPrevious: 'vs previous period', revenueDaily: 'Daily net revenue', last30: 'Showing the last 30 days of the selected period.', dailyNote: 'Dates use Asia/Ho_Chi_Minh; values may be negative when refunds exceed daily revenue.', seats: 'seats', intro: 'Operations and transaction snapshot in Vietnam time.', updated: 'Updated at', quickRange: 'Quick date ranges', today: 'Today', days7: '7 days', days30: '30 days', thisMonth: 'This month', filter: 'Dashboard filters', from: 'From', to: 'To', branch: 'Cinema', allAllowed: 'All assigned cinemas', allBranches: 'All cinemas', apply: 'Apply', loading: 'Loading report…', refreshing: 'Refreshing report…', retry: 'Try again', period: 'Period', stale: 'data from before refresh', netRevenue: 'Net revenue', bookings: 'Successful bookings', seatsSold: 'Seats sold', occupancy: 'Occupancy', alerts: 'Operational alerts', noAlerts: 'No operational exceptions need attention.', topMovies: 'Top movies', noTransactions: 'No transactions in this period.' },
} as const
const detailCopy = {
  vi: { invalidRange: 'Khoảng ngày không hợp lệ hoặc vượt quá 366 ngày.', reportError: 'Không thể tải số liệu báo cáo.', cinemasError: 'Không thể tải danh sách chi nhánh:', staleReport: ' Số liệu bên dưới là kết quả tải trước đó.', mismatch: 'Số liệu doanh thu ngày chưa khớp tổng kỳ. Vui lòng thử lại hoặc kiểm tra báo cáo nguồn.', metrics: 'Chỉ số theo kỳ', currentScope: 'Hiện tại', assignedScope: 'trong chi nhánh được phân quyền', loadedScope: 'trong phạm vi chi nhánh của lần tải', noDateScope: 'không theo khoảng ngày', needsAttention: 'booking cần chú ý', topHint: 'Xếp theo doanh thu ròng trong kỳ; số ghế bán gộp.', paymentReview: 'Đối soát thanh toán', overduePayments: 'Booking quá hạn thanh toán', overdueRefunds: 'Hoàn tiền quá SLA', failedRefunds: 'Hoàn tiền lỗi', cancelledShowtime: 'Suất bị hủy còn giao dịch', missingTicket: 'Đã thanh toán nhưng thiếu vé' },
  en: { invalidRange: 'The date range is invalid or exceeds 366 days.', reportError: 'Unable to load report data.', cinemasError: 'Unable to load cinemas:', staleReport: ' The data below is from the previous load.', mismatch: 'Daily revenue does not match the period total. Retry or check the source report.', metrics: 'Period metrics', currentScope: 'Current', assignedScope: 'within assigned cinemas', loadedScope: 'within the loaded cinema scope', noDateScope: 'independent of the date range', needsAttention: 'bookings need attention', topHint: 'Ranked by period net revenue; seats sold are combined.', paymentReview: 'Payment review', overduePayments: 'Overdue payments', overdueRefunds: 'Refunds past SLA', failedRefunds: 'Failed refunds', cancelledShowtime: 'Cancelled showtimes with transactions', missingTicket: 'Paid bookings without tickets' },
} as const
type DashboardCopy = Record<keyof typeof copy.vi, string>

function shiftDate(day: string, offset: number): string {
  const date = new Date(day + 'T00:00:00Z')
  date.setUTCDate(date.getUTCDate() + offset)
  return date.toISOString().slice(0, 10)
}

function errorMessage(error: unknown): string {
  if (i18n.language === 'en') return detailCopy.en.reportError
  return isAxiosError<{ message?: string }>(error)
    ? error.response?.data?.message ?? detailCopy.vi.reportError
    : detailCopy.vi.reportError
}

function Delta({ current, previous, c, number }: { current: number; previous: number; c: DashboardCopy; number: Intl.NumberFormat }) {
  if (previous <= 0) return <span className="text-xs text-text-muted">{c.previous}: {number.format(previous)}</span>
  const value = Math.round((current - previous) * 100 / previous)
  return <span className="text-xs text-text-secondary">{value > 0 ? '+' : ''}{value}% {c.vsPrevious}</span>
}

function MetricCard({ label, value, previous, c, number }: { label: string; value: string; previous?: { current: number; value: number }; c: DashboardCopy; number: Intl.NumberFormat }) {
  return <article className="rounded-xl border border-border bg-surface p-5">
    <h2 className="text-sm font-medium text-text-secondary">{label}</h2>
    <p className="mt-2 text-2xl font-bold text-text-primary">{value}</p>
    {previous && <p className="mt-2"><Delta current={previous.current} previous={previous.value} c={c} number={number} /></p>}
  </article>
}

function RevenueChart({ daily, c, money, number }: { daily: ReportSummary['daily']; c: DashboardCopy; money: Intl.NumberFormat; number: Intl.NumberFormat }) {
  const shown = daily.slice(-30)
  const max = Math.max(1, ...shown.map((item) => Math.abs(item.netRevenue)))
  return <section className="rounded-xl border border-border bg-surface p-5" aria-labelledby="revenue-chart-title">
    <h2 id="revenue-chart-title" className="text-lg font-semibold">{c.revenueDaily}</h2>
    <p className="mt-1 text-xs text-text-secondary">{daily.length > 30 ? c.last30 : c.dailyNote}</p>
    <div className="mt-5 max-h-96 space-y-2 overflow-y-auto">
      {shown.map((item) => <div key={item.date} className="grid grid-cols-[5rem_minmax(0,1fr)_7rem] items-center gap-3 text-xs sm:grid-cols-[6rem_minmax(0,1fr)_9rem]">
        <span>{item.date.slice(5)}</span>
        <div className="h-4 rounded bg-slate-100" aria-hidden="true"><div className={`h-4 rounded ${item.netRevenue < 0 ? 'bg-amber-500' : 'bg-primary'}`} style={{ width: `${Math.abs(item.netRevenue) / max * 100}%` }} /></div>
        <span className="text-right font-semibold">{money.format(item.netRevenue)}<small className="block font-normal text-text-secondary">{number.format(item.seatsSold)} {c.seats}</small></span>
      </div>)}
    </div>
  </section>
}

export function AdminDashboardPage() {
  const { i18n } = useTranslation()
  const c = copy[i18n.language === 'en' ? 'en' : 'vi']
  const d = detailCopy[i18n.language === 'en' ? 'en' : 'vi']
  const locale = displayLocale(i18n.language)
  const money = new Intl.NumberFormat(locale, { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
  const number = new Intl.NumberFormat(locale)
  const updatedAt = new Intl.DateTimeFormat(locale, { dateStyle: 'short', timeStyle: 'short', timeZone: zone })
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
      setFilterError(d.invalidRange)
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
    { label: d.paymentReview, count: alerts.paymentReview, to: '/admin/bookings?exception=PAYMENT_REVIEW' },
    { label: d.overduePayments, count: alerts.overduePayments, to: '/admin/bookings?exception=OVERDUE_PAYMENT' },
    { label: d.overdueRefunds, count: alerts.overdueRefunds, to: '/admin/refunds?status=REQUESTED&overdue=true' },
    { label: d.failedRefunds, count: alerts.failedRefunds, to: '/admin/refunds?status=REFUND_FAILED' },
    { label: d.cancelledShowtime, count: alerts.cancelledShowtimeBookings, to: '/admin/bookings?exception=CANCELLED_SHOWTIME' },
    { label: d.missingTicket, count: alerts.paidWithoutTicket, to: '/admin/bookings?exception=PAID_WITHOUT_TICKET' },
  ] : []

  return <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
    <header className="flex flex-wrap items-start justify-between gap-3">
      <div><h1 className="text-3xl font-bold text-text-primary">Dashboard</h1>
        <p className="mt-2 text-sm text-text-secondary">{c.intro}</p></div>
      {report && <p className="text-xs text-text-secondary">{c.updated} {updatedAt.format(new Date(report.asOf))}</p>}
    </header>

    <div className="mt-6 flex flex-wrap gap-2" aria-label={c.quickRange}>
      {[{ label: c.today, from: today }, { label: c.days7, from: shiftDate(today, -6) },
        { label: c.days30, from: shiftDate(today, -29) }, { label: c.thisMonth, from: today.slice(0, 7) + '-01' }]
        .map((item) => <button key={item.label} type="button" onClick={() => preset(item.from)}
          className="min-h-11 rounded-lg border border-border bg-surface px-4 text-sm font-semibold hover:border-primary focus-visible:outline-2 focus-visible:outline-primary">{item.label}</button>)}
    </div>
    <form key={params.toString()} onSubmit={apply} className="mt-3 grid gap-3 rounded-xl border border-border bg-surface p-4 sm:grid-cols-2 lg:grid-cols-[1fr_1fr_1.5fr_auto]" aria-label={c.filter}>
      <label className="text-sm font-medium">{c.from}<input name="from" type="date" required defaultValue={from} className="control mt-1" /></label>
      <label className="text-sm font-medium">{c.to}<input name="to" type="date" required defaultValue={to} className="control mt-1" /></label>
      <label className="text-sm font-medium">{c.branch}<select name="cinemaId" defaultValue={cinemaId} disabled={manager && cinemas.length === 1} className="control mt-1">
        {!(manager && cinemas.length === 1) && <option value="">{manager ? c.allAllowed : c.allBranches}</option>}
        {cinemas.map((cinema) => <option value={cinema.id} key={cinema.id}>{cinema.name}</option>)}
      </select></label>
      <button className="primary-button self-end">{c.apply}</button>
    </form>
    {filterError && <p role="alert" className="mt-2 text-sm text-red-700">{filterError}</p>}
    {cinemasError && <p role="alert" className="mt-2 text-sm text-red-700">{d.cinemasError} {cinemasError}</p>}
    {(refreshing || (report !== null && loadedFilterKey !== filterKey)) && <p role="status" className="mt-4 text-sm text-text-secondary">{report ? c.refreshing : c.loading}</p>}
    {error && <div role="alert" className="mt-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
      <p>{error}{report ? d.staleReport : ''}</p>
      <button type="button" className="mt-2 min-h-11 font-semibold underline" onClick={() => { setRefreshing(true); setRetry((value) => value + 1) }}>{c.retry}</button>
    </div>}

    {report && <>
      {!reconciled && <p role="alert" className="mt-5 rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm font-semibold text-amber-900">{d.mismatch}</p>}
      <section className="mt-6" aria-label={d.metrics}>
        <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-text-muted">{c.period} {loadedFilter.from} — {loadedFilter.to}{loadedFilterKey !== filterKey ? ` · ${c.stale}` : ''}</p>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <MetricCard label={c.netRevenue} value={money.format(report.netRevenue)} previous={{ current: report.netRevenue, value: report.previousPeriod.netRevenue }} c={c} number={number} />
          <MetricCard label={c.bookings} value={number.format(report.bookings)} previous={{ current: report.bookings, value: report.previousPeriod.bookings }} c={c} number={number} />
          <MetricCard label={c.seatsSold} value={number.format(report.seatsSold)} previous={{ current: report.seatsSold, value: report.previousPeriod.seatsSold }} c={c} number={number} />
          <MetricCard label={c.occupancy} value={`${report.occupancyPercent}%`} c={c} number={number} />
        </div>
      </section>

      <section className="mt-8 rounded-xl border border-border bg-surface p-5" aria-labelledby="alerts-title">
        <div className="flex flex-wrap items-center justify-between gap-2"><div>
          <h2 id="alerts-title" className="text-xl font-semibold">{c.alerts}</h2>
          <p className="mt-1 text-xs text-text-secondary">{d.currentScope} · {manager ? d.assignedScope : d.loadedScope} · {d.noDateScope}</p>
        </div><span className="rounded-full bg-amber-100 px-3 py-1 text-sm font-bold text-amber-900">{alerts?.totalBookings ?? 0} {d.needsAttention}</span></div>
        {alerts?.totalBookings === 0 ? <p className="mt-5 text-sm text-text-secondary">{c.noAlerts}</p>
          : <ul className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{alertRows.filter((item) => item.count > 0).map((item) => <li key={item.label}>
            <Link to={item.to + (loadedFilter.cinemaId ? (item.to.includes('?') ? '&' : '?') + 'cinemaId=' + encodeURIComponent(loadedFilter.cinemaId) : '')}
              className="flex min-h-16 items-center justify-between gap-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 hover:border-primary focus-visible:outline-2 focus-visible:outline-primary">
              <span className="text-sm font-medium">{item.label}</span><span className="text-lg font-bold">{item.count}</span>
            </Link>
          </li>)}</ul>}
      </section>

      <div className="mt-8 grid gap-5 lg:grid-cols-[1.4fr_1fr]">
        <RevenueChart daily={report.daily} c={c} money={money} number={number} />
        <section className="rounded-xl border border-border bg-surface p-5" aria-labelledby="top-movies-title">
          <h2 id="top-movies-title" className="text-lg font-semibold">{c.topMovies}</h2>
          <p className="mt-1 text-xs text-text-secondary">{d.topHint}</p>
          {report.topMovies.length === 0 ? <p className="mt-5 text-sm text-text-secondary">{c.noTransactions}</p>
            : <ol className="mt-4 divide-y divide-border">{report.topMovies.map((movie, index) => <li className="flex justify-between gap-3 py-3 text-sm" key={movie.movieId}>
              <span className="min-w-0"><span className="font-semibold">{index + 1}. {movie.title}</span><small className="block text-text-secondary">{number.format(movie.seatsSold)} {c.seatsSold.toLowerCase()}</small></span>
              <span className="shrink-0 font-semibold">{money.format(movie.netRevenue)}</span>
            </li>)}</ol>}
        </section>
      </div>
    </>}
  </main>
}
