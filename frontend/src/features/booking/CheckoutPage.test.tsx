import { act, cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CheckoutPage } from './CheckoutPage'
import { getBooking } from './bookingApi'

vi.mock('./bookingApi', () => ({ getBooking: vi.fn(), getBookingBilling: vi.fn().mockResolvedValue(null) }))
vi.mock('../account/profileApi', () => ({ getProfile: vi.fn().mockResolvedValue({ birthDate: null, fullName: 'Demo', email: 'demo@example.com', phone: null }), getBillingPreferences: vi.fn().mockResolvedValue(null) }))
vi.mock('../auth/AuthProvider', () => ({ useAuth: () => ({ user: { id: 'user-1' } }) }))

const mockedGetBooking = vi.mocked(getBooking)

describe('CheckoutPage', () => {
  beforeEach(() => { cleanup(); vi.useRealTimers(); mockedGetBooking.mockReset() })

  it('renders the backend booking snapshot and enables payment only for a payable booking', async () => {
    mockedGetBooking.mockResolvedValue({
      id: 'booking-1', bookingCode: 'LAK-1', holdId: 'hold-1', showtimeId: 'showtime-1', movieTitle: 'Demo Movie', ageRating: 'T16',
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
    expect(screen.getByRole('button', { name: /Thanh toán 180.000/ })).toBeEnabled()
  })

  it('continues counting down from the received server deadline', async () => {
    vi.useFakeTimers()
    const now = new Date('2026-01-01T10:00:00Z')
    vi.setSystemTime(now)
    mockedGetBooking.mockResolvedValue({
      id: 'booking-1', bookingCode: 'LAK-1', holdId: 'hold-1', showtimeId: 'showtime-1', movieTitle: 'Demo Movie', ageRating: 'T16',
      cinemaName: 'LAK Demo', auditoriumName: 'Room 1', startAt: now.toISOString(), status: 'PENDING_PAYMENT',
      subtotal: 180_000, voucherCode: null, discountAmount: 0, serviceFee: 0, totalAmount: 180_000,
      serverNow: now.toISOString(), paymentDeadline: new Date(now.getTime() + 120_000).toISOString(),
      hardDeadline: new Date(now.getTime() + 240_000).toISOString(),
      items: [{ showtimeSeatId: 'seat-1', seatLabel: 'A1', seatType: 'VIP', unitPrice: 180_000 }],
    })

    render(<MemoryRouter initialEntries={['/checkout/LAK-1']}><Routes>
      <Route path="/checkout/:bookingId" element={<CheckoutPage />} />
    </Routes></MemoryRouter>)

    await act(async () => { await Promise.resolve() })
    expect(screen.getByText(/02:00/)).toBeInTheDocument()
    await act(async () => { await vi.advanceTimersByTimeAsync(1_000) })
    expect(screen.getByText(/01:59/)).toBeInTheDocument()
    vi.useRealTimers()
  })
})
