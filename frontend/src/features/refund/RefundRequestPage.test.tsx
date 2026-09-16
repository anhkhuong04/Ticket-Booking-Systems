import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { getBooking } from '../booking/bookingApi'
import { getMyBookings } from '../ticketing/ticketApi'
import { RefundRequestPage } from './RefundRequestPage'
import { getCustomerRefund, requestCustomerRefund } from './refundApi'

vi.mock('../booking/bookingApi', () => ({ getBooking: vi.fn() }))
vi.mock('../ticketing/ticketApi', () => ({ getMyBookings: vi.fn() }))
vi.mock('./refundApi', () => ({ getCustomerRefund: vi.fn(), requestCustomerRefund: vi.fn() }))

const mockedGetBooking = vi.mocked(getBooking)
const mockedGetMyBookings = vi.mocked(getMyBookings)
const mockedGetCustomerRefund = vi.mocked(getCustomerRefund)
const mockedRequestCustomerRefund = vi.mocked(requestCustomerRefund)

const history = {
  bookingId: 'booking-1',
  bookingCode: 'LAK-ABC123',
  bookingStatus: 'PAID',
  movieTitle: 'Phim thử nghiệm',
  posterUrl: null,
  cinemaName: 'LAK Quận 1',
  auditoriumName: 'Phòng 1',
  startAt: '2030-01-01T10:00:00Z',
  seatLabels: ['A1'],
  ticketCode: 'TKT-123',
  ticketStatus: 'VALID' as const,
  createdAt: '2029-12-01T10:00:00Z',
  paymentStatus: 'SUCCESS',
  refundStatus: null,
}

const booking = {
  id: 'booking-1',
  bookingCode: 'LAK-ABC123',
  holdId: 'hold-1',
  showtimeId: 'showtime-1',
  movieTitle: 'Phim thử nghiệm',
  ageRating: 'T16',
  cinemaName: 'LAK Quận 1',
  auditoriumName: 'Phòng 1',
  startAt: '2030-01-01T10:00:00Z',
  status: 'PAID' as const,
  subtotal: 180_000,
  voucherCode: 'SAVE10',
  discountAmount: 20_000,
  serviceFee: 0,
  totalAmount: 160_000,
  paymentDeadline: '2030-01-01T09:00:00Z',
  hardDeadline: '2030-01-01T09:02:00Z',
  serverNow: '2030-01-01T08:00:00Z',
  items: [{ showtimeSeatId: 'seat-1', seatLabel: 'A1', seatType: 'STANDARD', unitPrice: 180_000 }],
}

function renderPage(entry = '/me/bookings/booking-1/refund') {
  render(<MemoryRouter initialEntries={[entry]}><Routes>
    <Route path="/me/bookings/:bookingId/refund" element={<RefundRequestPage />} />
  </Routes></MemoryRouter>)
}

function setUpBooking() {
  mockedGetMyBookings.mockResolvedValue([history])
  mockedGetBooking.mockResolvedValue(booking)
}

describe('RefundRequestPage', () => {
  afterEach(() => {
    cleanup()
    mockedGetBooking.mockReset()
    mockedGetMyBookings.mockReset()
    mockedGetCustomerRefund.mockReset()
    mockedRequestCustomerRefund.mockReset()
  })

  it('shows the server-backed conditions, requires confirmation, and tracks the accepted request', async () => {
    setUpBooking()
    mockedRequestCustomerRefund.mockResolvedValue({
      id: 'refund-1', bookingId: 'booking-1', paymentId: 'payment-1', amount: 160_000, status: 'REQUESTED', requestedAt: '2030-01-01T08:01:00Z', refundedAt: null,
    })
    mockedGetCustomerRefund.mockResolvedValue({
      id: 'refund-1', bookingId: 'booking-1', paymentId: 'payment-1', amount: 160_000, status: 'REQUESTED', requestedAt: '2030-01-01T08:01:00Z', refundedAt: null,
    })
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Kiểm tra điều kiện hoàn tiền' })).toBeInTheDocument()
    expect(screen.getByText('Voucher SAVE10 sẽ được khôi phục nếu vẫn còn hiệu lực.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Tiếp tục' }))
    expect(screen.getByRole('heading', { name: 'Xác nhận yêu cầu hoàn' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận hoàn' }))

    await waitFor(() => expect(mockedRequestCustomerRefund).toHaveBeenCalledWith('booking-1', expect.any(String)))
    expect(await screen.findByRole('heading', { name: 'Đang xử lý hoàn tiền' })).toBeInTheDocument()
    expect(screen.getByText('Đã tiếp nhận')).toBeInTheDocument()
  })

  it('uses a safe rejection message and does not expose a provider response', async () => {
    setUpBooking()
    mockedRequestCustomerRefund.mockRejectedValue({
      isAxiosError: true,
      response: { data: { code: 'REFUND_WINDOW_CLOSED', message: 'provider detail' } },
    })
    renderPage()

    await screen.findByRole('heading', { name: 'Kiểm tra điều kiện hoàn tiền' })
    fireEvent.click(screen.getByRole('button', { name: 'Tiếp tục' }))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận hoàn' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Yêu cầu hoàn cần được gửi ít nhất 45 phút trước giờ chiếu.')
    expect(screen.queryByText('provider detail')).not.toBeInTheDocument()
  })

  it('restores refund tracking from the refundId in the URL and displays manual review safely', async () => {
    setUpBooking()
    mockedGetCustomerRefund.mockResolvedValue({
      id: 'refund-1', bookingId: 'booking-1', paymentId: 'payment-1', amount: 160_000, status: 'REFUND_FAILED', requestedAt: '2030-01-01T08:01:00Z', refundedAt: null,
    })
    renderPage('/me/bookings/booking-1/refund?refundId=refund-1')

    expect(await screen.findByRole('heading', { name: 'Hoàn tiền cần xử lý thủ công' })).toBeInTheDocument()
    expect(mockedGetCustomerRefund).toHaveBeenCalledWith('refund-1')
    expect(screen.getByText(/không tạo thêm yêu cầu hoàn mới/i)).toBeInTheDocument()
  })
})
