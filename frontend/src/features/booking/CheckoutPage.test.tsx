import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CheckoutPage } from './CheckoutPage'
import { getBooking } from './bookingApi'

vi.mock('./bookingApi', () => ({ getBooking: vi.fn() }))

const mockedGetBooking = vi.mocked(getBooking)

describe('CheckoutPage', () => {
  beforeEach(() => mockedGetBooking.mockReset())

  it('renders the backend booking snapshot and payment deadline', async () => {
    mockedGetBooking.mockResolvedValue({
      id: 'booking-1', bookingCode: 'LAK-1', holdId: 'hold-1', showtimeId: 'showtime-1', movieTitle: 'Demo Movie',
      cinemaName: 'LAK Demo', auditoriumName: 'Room 1', startAt: '2026-01-01T10:00:00Z', status: 'PENDING_PAYMENT',
      subtotal: 180_000, voucherCode: null, discountAmount: 0, serviceFee: 0, totalAmount: 180_000,
      serverNow: new Date().toISOString(), paymentDeadline: new Date(Date.now() + 120_000).toISOString(),
      hardDeadline: new Date(Date.now() + 240_000).toISOString(),
      items: [{ showtimeSeatId: 'seat-1', seatLabel: 'A1', seatType: 'VIP', unitPrice: 180_000 }],
    })

    render(<MemoryRouter initialEntries={['/checkout/LAK-1']}><Routes>
      <Route path="/checkout/:bookingId" element={<CheckoutPage />} />
    </Routes></MemoryRouter>)

    expect(await screen.findByText(/Mã đặt vé: LAK-1/)).toBeInTheDocument()
    expect(screen.getByText('A1')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Thanh toán 180\.000/ })).toBeDisabled()
  })
})
