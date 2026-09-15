import { apiClient } from '../../shared/api/apiClient'

export type TicketScanResult = {
  result: 'VALID' | 'USED'
  ticketCode: string
  movieTitle: string
  cinemaName: string
  auditoriumName: string
  startAt: string
  seatLabels: string[]
  firstScannedAt: string
}

export async function validateTicket(code: string): Promise<TicketScanResult> {
  const response = await apiClient.post<TicketScanResult>('/api/tickets/validate', { code })
  return response.data
}
