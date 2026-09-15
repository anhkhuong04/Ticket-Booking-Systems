import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { PaymentResultPage } from './PaymentResultPage'
import { getPaymentStatus } from './paymentApi'

vi.mock('./paymentApi', () => ({ getPaymentStatus: vi.fn() }))

const mockedGetPaymentStatus = vi.mocked(getPaymentStatus)

function payment(overrides: Partial<Awaited<ReturnType<typeof getPaymentStatus>>> = {}) {
  return {
    id: 'payment-1', bookingCode: 'LAK-ABC123', bookingStatus: 'PENDING_PAYMENT' as const,
    provider: 'sandbox', amount: 90_000, currency: 'VND' as const, status: 'INITIATED' as const,
    paymentUrl: null, expiresAt: '2026-01-01T10:05:00Z', paidAt: null, serverNow: '2026-01-01T10:00:00Z', ...overrides,
  }
}

function renderPage(initialEntry = '/payments/payment-1/result') {
  render(<MemoryRouter initialEntries={[initialEntry]}><Routes>
    <Route path="/payments/:paymentId/result" element={<PaymentResultPage />} />
  </Routes></MemoryRouter>)
}

describe('PaymentResultPage', () => {
  afterEach(() => {
    cleanup()
    mockedGetPaymentStatus.mockReset()
  })

  it('uses only the payment status API and ignores success-looking redirect query parameters', async () => {
    mockedGetPaymentStatus.mockResolvedValue(payment({ status: 'SUCCESS', bookingStatus: 'PAID', paidAt: '2026-01-01T10:01:00Z' }))
    renderPage('/payments/payment-1/result?gateway=success&amount=1')

    expect(await screen.findByRole('heading', { name: 'Thanh toán thành công' })).toBeInTheDocument()
    expect(mockedGetPaymentStatus).toHaveBeenCalledWith('payment-1')
    expect(mockedGetPaymentStatus).toHaveBeenCalledTimes(1)
  })

  it('does not claim a late payment is successful while the backend reports payment review', async () => {
    mockedGetPaymentStatus.mockResolvedValue(payment({ status: 'SUCCESS', bookingStatus: 'PAYMENT_REVIEW', paidAt: '2026-01-01T10:01:00Z' }))
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Đang xác nhận giao dịch' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Thanh toán thành công' })).not.toBeInTheDocument()
    expect(screen.getByText(/không cần thanh toán lại/i)).toBeInTheDocument()
  })

  it('retries a failed status read with GET only and keeps the customer out of a duplicate payment flow', async () => {
    const user = userEvent.setup()
    mockedGetPaymentStatus.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(payment({ status: 'EXPIRED', bookingStatus: 'EXPIRED' }))
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Kiểm tra lại' }))
    await waitFor(() => expect(mockedGetPaymentStatus).toHaveBeenCalledTimes(2))
    expect(await screen.findByRole('heading', { name: 'Phiên đặt vé đã hết hạn' })).toBeInTheDocument()
  })
})
