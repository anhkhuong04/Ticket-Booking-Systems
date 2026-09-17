import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AdminDashboardPage } from './AdminDashboardPage'
import { getAdminCinemas, getReportSummary, type ReportSummary } from './adminApi'
import { useAuth } from '../auth/AuthProvider'

vi.mock('./adminApi', () => ({ getAdminCinemas: vi.fn(), getReportSummary: vi.fn() }))
vi.mock('../auth/AuthProvider', () => ({ useAuth: vi.fn() }))

const cinemas = vi.mocked(getAdminCinemas)
const summary = vi.mocked(getReportSummary)
const auth = vi.mocked(useAuth)
const report: ReportSummary = {
  netRevenue: 300000, bookings: 1, seatsSold: 3, occupancyPercent: 60,
  alerts: { totalBookings: 1, paymentReview: 1, overduePayments: 0, overdueRefunds: 0,
    failedRefunds: 0, cancelledShowtimeBookings: 0, paidWithoutTicket: 0 },
  previousPeriod: { netRevenue: 200000, bookings: 1, seatsSold: 2 },
  daily: [{ date: '2026-09-17', netRevenue: 300000, bookings: 1, seatsSold: 3 }],
  topMovies: [{ movieId: 'movie-1', title: 'Phim thử nghiệm', netRevenue: 300000, seatsSold: 3 }],
  asOf: '2026-09-17T03:00:00Z',
}

describe('AdminDashboardPage', () => {
  afterEach(() => { cleanup(); cinemas.mockReset(); summary.mockReset(); auth.mockReset() })

  it('uses seat units, separates current alerts, and displays top movies and period comparison', async () => {
    auth.mockReturnValue({ user: { id: 'admin', fullName: 'Admin', email: 'admin@lak.vn', roles: ['SUPER_ADMIN'] },
      ready: true, login: vi.fn(), register: vi.fn(), logout: vi.fn(), updateDisplayName: vi.fn() })
    cinemas.mockResolvedValue([{ id: 'cinema-1', name: 'LAK Quận 1', address: 'Test', city: 'HCM',
      timezone: 'Asia/Ho_Chi_Minh', status: 'ACTIVE', auditoriumCount: 1 }])
    summary.mockResolvedValue(report)
    render(<MemoryRouter initialEntries={['/admin?from=2026-09-17&to=2026-09-17&cinemaId=cinema-1']}>
      <AdminDashboardPage />
    </MemoryRouter>)

    expect(await screen.findByText('Cảnh báo vận hành')).toBeInTheDocument()
    expect(screen.getByText('Ghế đã bán')).toBeInTheDocument()
    expect(screen.queryByText('Vé đã bán')).not.toBeInTheDocument()
    expect(screen.getAllByText('+50% so với kỳ trước')).toHaveLength(2)
    expect(screen.getByText(/Phim thử nghiệm/)).toBeInTheDocument()
    expect(screen.getByText('Đối soát thanh toán')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Đối soát thanh toán/ })).toHaveAttribute('href',
      '/admin/bookings?exception=PAYMENT_REVIEW&cinemaId=cinema-1')
    expect(summary).toHaveBeenCalledWith({ from: '2026-09-17', to: '2026-09-17', cinemaId: 'cinema-1' })
  })

  it('warns when daily revenue does not reconcile and supports retry after an API error', async () => {
    auth.mockReturnValue({ user: { id: 'admin', fullName: 'Admin', email: 'admin@lak.vn', roles: ['SUPER_ADMIN'] },
      ready: true, login: vi.fn(), register: vi.fn(), logout: vi.fn(), updateDisplayName: vi.fn() })
    cinemas.mockResolvedValue([])
    summary.mockRejectedValueOnce(new Error('offline')).mockResolvedValue({ ...report, netRevenue: 300001 })
    render(<MemoryRouter initialEntries={['/admin?from=2026-09-17&to=2026-09-17']}>
      <AdminDashboardPage />
    </MemoryRouter>)

    expect(await screen.findByRole('alert')).toHaveTextContent('Không thể tải số liệu báo cáo.')
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText(/Số liệu doanh thu ngày chưa khớp tổng kỳ/)).toBeInTheDocument()
    expect(summary).toHaveBeenCalledTimes(2)
  })
})
