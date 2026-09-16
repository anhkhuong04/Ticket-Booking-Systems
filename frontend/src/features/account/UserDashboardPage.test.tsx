import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { UserDashboardPage } from './UserDashboardPage'
import { getMyBookings } from '../ticketing/ticketApi'
import { getMovies } from '../catalog/catalogApi'
import { useAuth } from '../auth/AuthProvider'

vi.mock('../ticketing/ticketApi', () => ({ getMyBookings: vi.fn() }))
vi.mock('../catalog/catalogApi', () => ({ getMovies: vi.fn() }))
vi.mock('../auth/AuthProvider', () => ({ useAuth: vi.fn() }))

const mockedGetMyBookings = vi.mocked(getMyBookings)
const mockedGetMovies = vi.mocked(getMovies)
const mockedUseAuth = vi.mocked(useAuth)

const booking = {
  bookingId: 'booking-1',
  bookingCode: 'LAK-ABC123',
  bookingStatus: 'PAID',
  movieTitle: 'Phim sắp xem',
  posterUrl: null,
  cinemaName: 'LAK Quận 1',
  auditoriumName: 'Phòng 1',
  startAt: '2030-01-01T10:00:00Z',
  seatLabels: ['A1', 'A2'],
  ticketCode: 'TKT-123',
  ticketStatus: 'VALID' as const,
  createdAt: '2029-12-01T10:00:00Z',
  paymentStatus: 'SUCCESS',
  refundStatus: null,
}

describe('UserDashboardPage', () => {
  beforeEach(() => {
    mockedUseAuth.mockReturnValue({ user: { id: 'user-1', email: 'customer@lak.vn', fullName: 'Khách hàng', roles: ['CUSTOMER'] }, ready: true, login: vi.fn(), register: vi.fn(), logout: vi.fn() })
    mockedGetMyBookings.mockResolvedValue([booking, { ...booking, bookingId: 'booking-2', bookingCode: 'LAK-REFUND', ticketCode: null, ticketStatus: null, bookingStatus: 'REFUND_PENDING', refundStatus: 'REFUND_FAILED', createdAt: '2029-12-02T10:00:00Z' }])
    mockedGetMovies.mockResolvedValue({ content: [{ id: 'movie-1', title: 'Phim đang chiếu', durationMinutes: 120, ageRating: 'T13', releaseDate: '2029-01-01', posterUrl: null, status: 'NOW_SHOWING', genres: [] }], page: 0, size: 4, totalElements: 1, totalPages: 1 })
  })

  afterEach(() => {
    cleanup()
    mockedGetMyBookings.mockReset()
    mockedGetMovies.mockReset()
    mockedUseAuth.mockReset()
  })

  it('shows authoritative account work, the next valid ticket, recent bookings and now-showing movies', async () => {
    render(<MemoryRouter><UserDashboardPage /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: 'Xin chào, Khách hàng' })).toBeInTheDocument()
    expect(screen.getByText('Hoàn tiền cần được hỗ trợ')).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: 'Xem vé' })[0]).toHaveAttribute('href', '/tickets/TKT-123')
    expect(screen.getByRole('link', { name: 'Vé của tôi' })).toHaveAttribute('href', '/me/tickets')
    expect(screen.getAllByRole('heading', { name: 'Phim đang chiếu' })[0]).toBeInTheDocument()
    await waitFor(() => expect(mockedGetMovies).toHaveBeenCalledWith({ status: 'NOW_SHOWING', size: 4 }))
    expect(screen.queryByText(/mã qr/i)).not.toBeInTheDocument()
  })
})
