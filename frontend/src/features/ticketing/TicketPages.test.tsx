import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MyTicketsPage, TicketDetailPage } from './TicketPages'
import { getMyBookings, getTicket } from './ticketApi'

vi.mock('./ticketApi', () => ({ getMyBookings: vi.fn(), getTicket: vi.fn() }))
vi.mock('qrcode', () => ({ toDataURL: vi.fn().mockResolvedValue('data:image/png;base64,qr') }))

const mockedGetMyBookings = vi.mocked(getMyBookings)
const mockedGetTicket = vi.mocked(getTicket)

const historyItem = {
  bookingId: 'booking-1',
  bookingCode: 'LAK-ABC123',
  bookingStatus: 'PAID',
  movieTitle: 'Phim thử nghiệm',
  posterUrl: null,
  cinemaName: 'LAK Quận 1',
  auditoriumName: 'Phòng 1',
  startAt: '2030-01-01T10:00:00Z',
  seatLabels: ['A1', 'A2'],
  ticketCode: 'TKT-123',
  ticketStatus: 'VALID' as const,
}

function renderHistory() {
  render(<MemoryRouter initialEntries={['/me/tickets']}><Routes>
    <Route path="/me/tickets" element={<MyTicketsPage />} />
  </Routes></MemoryRouter>)
}

function renderTicket() {
  render(<MemoryRouter initialEntries={['/tickets/TKT-123']}><Routes>
    <Route path="/tickets/:ticketCode" element={<TicketDetailPage />} />
  </Routes></MemoryRouter>)
}

describe('ticket pages', () => {
  afterEach(() => {
    cleanup()
    window.localStorage.clear()
    window.sessionStorage.clear()
    mockedGetMyBookings.mockReset()
    mockedGetTicket.mockReset()
  })

  it('shows booking history from the authenticated customer endpoint without a QR image', async () => {
    mockedGetMyBookings.mockResolvedValue([historyItem])
    renderHistory()

    expect(await screen.findByRole('heading', { name: 'Phim thử nghiệm' })).toBeInTheDocument()
    expect(mockedGetMyBookings).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('img', { name: /mã qr/i })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Xem vé' })).toHaveAttribute('href', '/tickets/TKT-123')
  })

  it('requests the ticket from the backend and renders a QR only when its owner payload is returned', async () => {
    mockedGetTicket.mockResolvedValue({
      ...historyItem,
      id: 'ticket-1',
      status: 'VALID',
      issuedAt: '2030-01-01T09:00:00Z',
      usedAt: null,
      qrPayload: 'owner-only-qr-value',
    })
    renderTicket()

    expect(await screen.findByRole('img', { name: 'Mã QR vé TKT-123' })).toBeInTheDocument()
    await waitFor(() => expect(mockedGetTicket).toHaveBeenCalledWith('TKT-123'))
    expect(window.sessionStorage.length).toBe(0)
    expect(window.localStorage.length).toBe(0)
  })
})
