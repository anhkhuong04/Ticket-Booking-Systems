import { apiClient } from '../../shared/api/apiClient'

export type BookingHistoryItem = {
  bookingId: string
  bookingCode: string
  bookingStatus: string
  movieTitle: string
  posterUrl: string | null
  cinemaName: string
  auditoriumName: string
  startAt: string
  seatLabels: string[]
  ticketCode: string | null
  ticketStatus: 'VALID' | 'USED' | 'CANCELLED' | null
}

export type Ticket = {
  id: string
  ticketCode: string
  status: 'VALID' | 'USED' | 'CANCELLED'
  issuedAt: string
  usedAt: string | null
  bookingCode: string
  bookingStatus: string
  movieTitle: string
  posterUrl: string | null
  cinemaName: string
  auditoriumName: string
  startAt: string
  seatLabels: string[]
  qrPayload: string | null
}

export async function getMyBookings(): Promise<BookingHistoryItem[]> {
  const response = await apiClient.get<BookingHistoryItem[]>('/api/me/bookings')
  return response.data
}

export async function getTicket(ticketCode: string): Promise<Ticket> {
  const response = await apiClient.get<Ticket>(`/api/tickets/${encodeURIComponent(ticketCode)}`)
  return response.data
}
