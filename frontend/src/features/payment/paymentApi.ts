import { apiClient } from '../../shared/api/apiClient'

export type Payment = {
  id: string
  bookingCode: string
  bookingStatus: 'PENDING_PAYMENT' | 'PAID' | 'EXPIRED' | 'PAYMENT_REVIEW' | 'REFUND_PENDING' | 'CANCELLED'
  provider: string
  amount: number
  currency: 'VND'
  status: 'INITIATED' | 'SUCCESS' | 'FAILED' | 'EXPIRED'
  paymentUrl: string | null
  expiresAt: string
  paidAt: string | null
  serverNow: string
}

export async function getPaymentStatus(paymentId: string): Promise<Payment> {
  const response = await apiClient.get<Payment>(`/api/payments/${encodeURIComponent(paymentId)}/status`)
  return response.data
}

export async function createPayment(bookingCode: string, provider = 'sandbox'): Promise<Payment> {
  const response = await apiClient.post<Payment>('/api/payments', { bookingCode, provider })
  return response.data
}
