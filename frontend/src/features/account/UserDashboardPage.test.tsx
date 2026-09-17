import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { UserDashboardPage } from './UserDashboardPage'
import { getMyBookings, type BookingHistoryItem } from '../ticketing/ticketApi'
import { useAuth } from '../auth/AuthProvider'

vi.mock('../ticketing/ticketApi', () => ({ getMyBookings: vi.fn() }))
vi.mock('../auth/AuthProvider', () => ({ useAuth: vi.fn() }))

const myBookings = vi.mocked(getMyBookings)
const useCurrentAuth = vi.mocked(useAuth)

const booking: BookingHistoryItem = {
  bookingId: 'booking-1', bookingCode: 'LAK-ABC123', bookingStatus: 'PAID', movieTitle: 'Phim sắp xem',
  posterUrl: null, cinemaName: 'LAK Quận 1', auditoriumName: 'Phòng 1', startAt: '2030-01-01T10:00:00Z',
  seatLabels: ['A1', 'A2'], ticketCode: 'TKT-123', ticketStatus: 'VALID', createdAt: '2029-12-01T10:00:00Z',
  paymentStatus: 'SUCCESS', refundStatus: null, showtimeStatus: 'SCHEDULED', canResumePayment: false,
}

describe('UserDashboardPage', () => {
  beforeEach(() => {
    useCurrentAuth.mockReturnValue({ user: { id: 'user-1', email: 'customer@lak.vn', fullName: 'Khách hàng', roles: ['CUSTOMER'] }, ready: true, login: vi.fn(), register: vi.fn(), logout: vi.fn(), updateDisplayName: vi.fn() })
  })
  afterEach(() => { cleanup(); myBookings.mockReset(); useCurrentAuth.mockReset() })

  it('separates issues, customer actions and system processing, and excludes cancelled tickets', async () => {
    myBookings.mockResolvedValue([
      booking,
      { ...booking, bookingId: 'cancelled', bookingCode: 'LAK-CANCEL', showtimeStatus: 'CANCELLED', refundStatus: 'REQUESTED' },
      { ...booking, bookingId: 'payment', bookingCode: 'LAK-PAY', bookingStatus: 'PENDING_PAYMENT', ticketCode: null, ticketStatus: null, paymentStatus: null, canResumePayment: true },
      { ...booking, bookingId: 'review', bookingCode: 'LAK-REVIEW', bookingStatus: 'PAYMENT_REVIEW', ticketCode: null, ticketStatus: null, paymentStatus: 'SUCCESS' },
    ])
    render(<MemoryRouter><UserDashboardPage /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: 'Có vấn đề' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Cần bạn xử lý' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Đang được xử lý' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Tiếp tục thanh toán' })).toHaveAttribute('href', '/checkout/LAK-PAY')
    expect(screen.getByRole('link', { name: 'Lịch sử đặt vé' })).toHaveAttribute('href', '/me/bookings')
    expect(screen.getAllByRole('link', { name: 'Xem vé' })).toHaveLength(1)
    expect(screen.queryByText('PAID')).not.toBeInTheDocument()
    expect(screen.queryByText('REFUND_PENDING')).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Phim đang chiếu' })).not.toBeInTheDocument()
  })

  it('does not show a payment action when the backend disallows it', async () => {
    myBookings.mockResolvedValue([{ ...booking, bookingStatus: 'PENDING_PAYMENT', ticketCode: null, ticketStatus: null, paymentStatus: 'INITIATED', canResumePayment: false }])
    render(<MemoryRouter><UserDashboardPage /></MemoryRouter>)
    expect(await screen.findByRole('heading', { name: 'Đang được xử lý' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Tiếp tục thanh toán' })).not.toBeInTheDocument()
  })
})
