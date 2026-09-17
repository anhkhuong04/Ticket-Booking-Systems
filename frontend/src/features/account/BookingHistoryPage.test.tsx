import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { BookingHistoryPage } from './BookingHistoryPage'
import { getMyBookings, type BookingHistoryItem } from '../ticketing/ticketApi'

vi.mock('../ticketing/ticketApi', () => ({ getMyBookings: vi.fn() }))

const myBookings = vi.mocked(getMyBookings)

const booking: BookingHistoryItem = {
  bookingId: 'booking-1', bookingCode: 'LAK-PAID', bookingStatus: 'PAID', movieTitle: 'Phim A',
  posterUrl: null, cinemaName: 'LAK Quận 1', auditoriumName: 'Phòng 1', startAt: '2030-01-01T10:00:00Z',
  seatLabels: ['A1'], ticketCode: 'TKT-1', ticketStatus: 'VALID', createdAt: '2029-12-01T10:00:00Z',
  paymentStatus: 'SUCCESS', refundStatus: null, showtimeStatus: 'SCHEDULED', canResumePayment: false,
}

describe('BookingHistoryPage', () => {
  afterEach(() => { cleanup(); myBookings.mockReset() })

  it('shows customer-friendly statuses and never offers payment for a cancelled showtime', async () => {
    myBookings.mockResolvedValue([
      booking,
      { ...booking, bookingId: 'booking-2', bookingCode: 'LAK-CANCEL', movieTitle: 'Phim B',
        bookingStatus: 'PENDING_PAYMENT', ticketCode: null, ticketStatus: null,
        paymentStatus: null, showtimeStatus: 'CANCELLED', canResumePayment: false },
    ])
    render(<MemoryRouter initialEntries={['/me/bookings?status=issue']}><BookingHistoryPage /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: 'Phim B' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Phim A' })).not.toBeInTheDocument()
    expect(screen.getByText('Suất chiếu đã hủy')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Tiếp tục thanh toán' })).not.toBeInTheDocument()
    expect(screen.queryByText('PENDING_PAYMENT')).not.toBeInTheDocument()
  })
})
