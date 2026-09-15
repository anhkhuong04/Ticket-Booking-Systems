import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SeatSelectionPage } from './SeatSelectionPage'
import { createSeatHold, getSeatHold, releaseSeatHold } from './reservationApi'
import { getShowtimeSeatMap } from '../showtime/showtimeApi'
import { checkout } from '../booking/bookingApi'

vi.mock('./reservationApi', () => ({
  createSeatHold: vi.fn(),
  getSeatHold: vi.fn(),
  releaseSeatHold: vi.fn(),
}))
vi.mock('../showtime/showtimeApi', () => ({ getShowtimeSeatMap: vi.fn() }))
vi.mock('../booking/bookingApi', () => ({ checkout: vi.fn() }))

const mockedSeatMap = vi.mocked(getShowtimeSeatMap)
const mockedCreateHold = vi.mocked(createSeatHold)
const mockedCheckout = vi.mocked(checkout)

describe('SeatSelectionPage', () => {
  beforeEach(() => {
    cleanup()
    sessionStorage.clear()
    mockedCreateHold.mockReset()
    mockedCheckout.mockReset()
    vi.mocked(getSeatHold).mockReset()
    vi.mocked(releaseSeatHold).mockReset()
    mockedSeatMap.mockResolvedValue({
      showtimeId: 'showtime-1', movieId: 'movie-1', movieTitle: 'Demo Movie', cinemaId: 'cinema-1',
      cinemaName: 'LAK Demo', auditoriumName: 'Room 1', startAt: '2026-01-01T10:00:00Z', prices: {},
      seats: [
        { id: 'standard-1', rowLabel: 'A', seatNumber: 1, seatType: 'STANDARD', pairKey: null, status: 'AVAILABLE', price: 90_000 },
        { id: 'couple-1', rowLabel: 'B', seatNumber: 1, seatType: 'COUPLE', pairKey: 'B-1-2', status: 'AVAILABLE', price: 180_000 },
        { id: 'couple-2', rowLabel: 'B', seatNumber: 2, seatType: 'COUPLE', pairKey: 'B-1-2', status: 'AVAILABLE', price: 180_000 },
      ],
    })
  })

  it('selects a couple pair together and sends both seat IDs to the backend', async () => {
    const user = userEvent.setup()
    mockedCreateHold.mockResolvedValue({
      id: 'hold-1', showtimeId: 'showtime-1', status: 'ACTIVE', serverNow: '2026-01-01T10:00:00Z',
      expiresAt: '2026-01-01T10:05:00Z', hardExpiresAt: '2026-01-01T10:05:00Z', showtimeSeatIds: ['couple-1', 'couple-2'],
    })

    render(<MemoryRouter initialEntries={['/showtimes/showtime-1/seats']}><Routes>
      <Route path="/showtimes/:showtimeId/seats" element={<SeatSelectionPage />} />
    </Routes></MemoryRouter>)

    await user.click(await screen.findByRole('button', { name: /B1/i }))
    expect(screen.getByRole('button', { name: /B2.*đang chọn/i })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Giữ ghế' }))
    expect(mockedCreateHold).toHaveBeenCalledWith('showtime-1', ['couple-1', 'couple-2'], expect.any(String))
  })

  it('allows a voucher to be entered and removed before checkout', async () => {
    const user = userEvent.setup()
    mockedCreateHold.mockResolvedValue({
      id: 'hold-1', showtimeId: 'showtime-1', status: 'ACTIVE', serverNow: '2026-01-01T10:00:00Z',
      expiresAt: '2026-01-01T10:05:00Z', hardExpiresAt: '2026-01-01T10:05:00Z', showtimeSeatIds: ['standard-1'],
    })
    render(<MemoryRouter initialEntries={['/showtimes/showtime-1/seats']}><Routes>
      <Route path="/showtimes/:showtimeId/seats" element={<SeatSelectionPage />} />
    </Routes></MemoryRouter>)

    await user.click(await screen.findByRole('button', { name: /A1/i }))
    await user.click(screen.getByRole('button', { name: 'Giữ ghế' }))
    const voucherInput = await screen.findByRole('textbox')
    await user.type(voucherInput, 'save50')
    expect(voucherInput).toHaveValue('save50')
    await user.click(screen.getByRole('button', { name: 'Xóa' }))
    expect(voucherInput).toHaveValue('')
  })

  it('sends the normalized voucher code with the stable checkout request', async () => {
    const user = userEvent.setup()
    mockedCreateHold.mockResolvedValue({
      id: 'hold-1', showtimeId: 'showtime-1', status: 'ACTIVE', serverNow: '2026-01-01T10:00:00Z',
      expiresAt: '2026-01-01T10:05:00Z', hardExpiresAt: '2026-01-01T10:05:00Z', showtimeSeatIds: ['standard-1'],
    })
    mockedCheckout.mockResolvedValue({
      id: 'booking-1', bookingCode: 'LAK-1', holdId: 'hold-1', showtimeId: 'showtime-1', movieTitle: 'Demo Movie',
      cinemaName: 'LAK Demo', auditoriumName: 'Room 1', startAt: '2026-01-01T10:00:00Z', status: 'PENDING_PAYMENT',
      subtotal: 90_000, voucherCode: 'SAVE50', discountAmount: 20_000, serviceFee: 0, totalAmount: 70_000,
      serverNow: new Date().toISOString(), paymentDeadline: new Date(Date.now() + 120_000).toISOString(),
      hardDeadline: new Date(Date.now() + 240_000).toISOString(),
      items: [{ showtimeSeatId: 'standard-1', seatLabel: 'A1', seatType: 'STANDARD', unitPrice: 90_000 }],
    })
    render(<MemoryRouter initialEntries={['/showtimes/showtime-1/seats']}><Routes>
      <Route path="/showtimes/:showtimeId/seats" element={<SeatSelectionPage />} />
      <Route path="/checkout/:bookingId" element={<div>Checkout</div>} />
    </Routes></MemoryRouter>)

    await user.click(await screen.findByRole('button', { name: /A1/i }))
    await user.click(screen.getByRole('button', { name: 'Giữ ghế' }))
    await user.type(await screen.findByRole('textbox'), 'save50')
    await user.click(screen.getByRole('button', { name: 'Tiếp tục xác nhận' }))
    expect(mockedCheckout).toHaveBeenCalledWith('hold-1', expect.any(String), 'SAVE50')
  })
})
