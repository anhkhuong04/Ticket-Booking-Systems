import { apiClient } from '../../shared/api/apiClient'

export type SeatHold = {
  id: string
  showtimeId: string
  status: 'ACTIVE' | 'CONSUMED' | 'RELEASED' | 'EXPIRED'
  serverNow: string
  expiresAt: string
  hardExpiresAt: string
  showtimeSeatIds: string[]
}

export async function createSeatHold(showtimeId: string, showtimeSeatIds: string[], idempotencyKey: string): Promise<SeatHold> {
  const response = await apiClient.post<SeatHold>('/api/seat-holds', { showtimeId, showtimeSeatIds }, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
  return response.data
}

export async function getSeatHold(holdId: string): Promise<SeatHold> {
  const response = await apiClient.get<SeatHold>(`/api/seat-holds/${holdId}`)
  return response.data
}

export async function releaseSeatHold(holdId: string): Promise<void> {
  await apiClient.delete(`/api/seat-holds/${holdId}`)
}
