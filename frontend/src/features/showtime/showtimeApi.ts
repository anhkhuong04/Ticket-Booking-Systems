import { apiClient } from '../../shared/api/apiClient'

export type Showtime = {
  id: string
  movieId: string
  cinemaId: string
  cinemaName: string
  cinemaAddress: string
  screenFormat: string
  startAt: string
  endAt: string
  salesCloseAt: string
  prices: Record<string, number>
}

export type ShowtimeSeat = {
  id: string
  rowLabel: string
  seatNumber: number
  seatType: 'STANDARD' | 'VIP' | 'COUPLE'
  pairKey: string | null
  status: 'AVAILABLE' | 'HELD' | 'SOLD' | 'BLOCKED' | 'PAYMENT_PENDING'
  price: number
}

export type ShowtimeSeatMap = {
  showtimeId: string
  movieId: string
  movieTitle: string
  cinemaId: string
  cinemaName: string
  auditoriumName: string
  startAt: string
  seats: ShowtimeSeat[]
  prices: Record<string, number>
}

export async function getOpenShowtimes(movieId: string, date: string, cinemaId?: string): Promise<Showtime[]> {
  const response = await apiClient.get<Showtime[]>('/api/showtimes', {
    params: { movieId, date, cinemaId: cinemaId || undefined },
  })
  return response.data
}

export async function getShowtimeSeatMap(showtimeId: string): Promise<ShowtimeSeatMap> {
  const response = await apiClient.get<ShowtimeSeatMap>(`/api/showtimes/${showtimeId}/seats`)
  return response.data
}
