import { apiClient } from '../../shared/api/apiClient'

export type BookingItem = {
  showtimeSeatId: string
  seatLabel: string
  seatType: string
  unitPrice: number
}

export type Booking = {
  id: string
  bookingCode: string
  holdId: string
  showtimeId: string
  movieTitle: string
  cinemaName: string
  auditoriumName: string
  startAt: string
  status: 'PENDING_PAYMENT' | 'PAID' | 'EXPIRED' | 'PAYMENT_REVIEW' | 'REFUND_PENDING' | 'CANCELLED'
  subtotal: number
  voucherCode: string | null
  discountAmount: number
  serviceFee: number
  totalAmount: number
  paymentDeadline: string
  hardDeadline: string
  serverNow: string
  items: BookingItem[]
}

export async function checkout(holdId: string, idempotencyKey: string, voucherCode?: string): Promise<Booking> {
  const response = await apiClient.post<Booking>('/api/bookings/checkout', { holdId, voucherCode }, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
  return response.data
}

export async function getBooking(bookingCode: string): Promise<Booking> {
  const response = await apiClient.get<Booking>(`/api/bookings/${encodeURIComponent(bookingCode)}`)
  return response.data
}
