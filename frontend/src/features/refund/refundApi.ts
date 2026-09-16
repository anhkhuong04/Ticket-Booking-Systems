import { apiClient } from '../../shared/api/apiClient'

export type RefundStatus = 'REQUESTED' | 'REFUNDED' | 'REFUND_FAILED'

export type Refund = {
  id: string
  bookingId: string
  paymentId: string
  amount: number
  status: RefundStatus
  requestedAt: string
  refundedAt: string | null
}

export async function requestCustomerRefund(bookingId: string, idempotencyKey: string): Promise<Refund> {
  const response = await apiClient.post<Refund>(`/api/bookings/${encodeURIComponent(bookingId)}/refunds`, undefined, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
  return response.data
}

export async function getCustomerRefund(refundId: string): Promise<Refund> {
  const response = await apiClient.get<Refund>(`/api/refunds/${encodeURIComponent(refundId)}`)
  return response.data
}
