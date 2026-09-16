import { cleanup, render, screen, waitFor } from '@testing-library/react'
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
const mockedGetHold = vi.mocked(getSeatHold)

class FakeWebSocket {
  static instances: FakeWebSocket[] = []
  onopen: (() => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onclose: (() => void) | null = null

  constructor(_url: string) { FakeWebSocket.instances.push(this) }

  close() { this.onclose?.() }
  emitMessage(payload: unknown) { this.onmessage?.({ data: JSON.stringify(payload) } as MessageEvent) }
  emitClose() { this.onclose?.() }
}

function activeHold() {
  const now = new Date()
  return {
    id: 'hold-1', showtimeId: 'showtime-1', status: 'ACTIVE' as const, serverNow: now.toISOString(),
    expiresAt: new Date(now.getTime() + 300_000).toISOString(), hardExpiresAt: new Date(now.getTime() + 300_000).toISOString(),
    showtimeSeatIds: ['standard-1'],
  }
}

function axiosFailure(status: number, message = 'Request failed') {
  return Object.assign(new Error(message), { isAxiosError: true, response: { status, data: { message } } })
}

describe('SeatSelectionPage', () => {
  beforeEach(() => {
    cleanup()
    sessionStorage.clear()
    mockedCreateHold.mockReset()
    mockedCheckout.mockReset()
    mockedGetHold.mockReset()
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
    FakeWebSocket.instances = []
    vi.stubGlobal('WebSocket', FakeWebSocket)
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
      id: 'booking-1', bookingCode: 'LAK-1', holdId: 'hold-1', showtimeId: 'showtime-1', movieTitle: 'Demo Movie', ageRating: 'T16',
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

  it('reconciles the authoritative seat map after a realtime event and warns on disconnect', async () => {
    render(<MemoryRouter initialEntries={['/showtimes/showtime-1/seats']}><Routes>
      <Route path="/showtimes/:showtimeId/seats" element={<SeatSelectionPage />} />
    </Routes></MemoryRouter>)

    await screen.findByRole('button', { name: /A1/i })
    const socket = FakeWebSocket.instances[0]
    socket.emitMessage({ type: 'SEATS_UPDATED', showtimeId: 'showtime-1' })
    await waitFor(() => expect(mockedSeatMap).toHaveBeenCalledTimes(2))
    socket.emitClose()
    expect(await screen.findByRole('status')).toHaveTextContent(/realtime/i)
  })

  it('reconciles a conflict and shows the expired-hold dialog for a 410 response', async () => {
    const user = userEvent.setup()
    mockedCreateHold.mockRejectedValueOnce(axiosFailure(409))
    render(<MemoryRouter initialEntries={['/showtimes/showtime-1/seats']}><Routes>
      <Route path="/showtimes/:showtimeId/seats" element={<SeatSelectionPage />} />
    </Routes></MemoryRouter>)

    await user.click(await screen.findByRole('button', { name: /A1/i }))
    await user.click(screen.getByRole('button', { name: /Giữ ghế/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/vừa được/i)
    await waitFor(() => expect(mockedSeatMap).toHaveBeenCalledTimes(2))

    cleanup()
    sessionStorage.setItem('lak:seat-hold:showtime-1', 'hold-1')
    mockedGetHold.mockRejectedValueOnce(axiosFailure(410))
    render(<MemoryRouter initialEntries={['/showtimes/showtime-1/seats']}><Routes>
      <Route path="/showtimes/:showtimeId/seats" element={<SeatSelectionPage />} />
    </Routes></MemoryRouter>)
    expect(await screen.findByRole('dialog')).toHaveTextContent(/Thời gian/i)
  })

  it('reuses the persisted checkout idempotency key after a network failure and refresh', async () => {
    const user = userEvent.setup()
    sessionStorage.setItem('lak:seat-hold:showtime-1', 'hold-1')
    mockedGetHold.mockResolvedValue(activeHold())
    mockedCheckout.mockRejectedValueOnce(new Error('offline'))
    const first = render(<MemoryRouter initialEntries={['/showtimes/showtime-1/seats']}><Routes>
      <Route path="/showtimes/:showtimeId/seats" element={<SeatSelectionPage />} />
    </Routes></MemoryRouter>)

    await user.click(await screen.findByRole('button', { name: /Tiếp tục xác nhận/i }))
    const firstKey = mockedCheckout.mock.calls[0][1]
    expect(await screen.findByRole('status')).toHaveTextContent(/chờ xác nhận/i)

    first.unmount()
    mockedGetHold.mockResolvedValue(activeHold())
    mockedCheckout.mockRejectedValueOnce(new Error('offline'))
    render(<MemoryRouter initialEntries={['/showtimes/showtime-1/seats']}><Routes>
      <Route path="/showtimes/:showtimeId/seats" element={<SeatSelectionPage />} />
    </Routes></MemoryRouter>)
    await user.click(await screen.findByRole('button', { name: /Tiếp tục xác nhận/i }))
    expect(mockedCheckout.mock.calls[1][1]).toBe(firstKey)
  })
})
