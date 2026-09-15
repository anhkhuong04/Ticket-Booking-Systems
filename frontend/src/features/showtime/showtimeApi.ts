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

export async function getOpenShowtimes(movieId: string, date: string, cinemaId?: string): Promise<Showtime[]> {
  const response = await apiClient.get<Showtime[]>('/api/showtimes', {
    params: { movieId, date, cinemaId: cinemaId || undefined },
  })
  return response.data
}
