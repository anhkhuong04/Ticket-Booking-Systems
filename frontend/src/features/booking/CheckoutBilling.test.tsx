import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CheckoutBilling } from './CheckoutBilling'
import { getBillingPreferences, getProfile } from '../account/profileApi'
import { getBookingBilling, requestBookingBilling } from './bookingApi'

vi.mock('../account/profileApi', () => ({ getBillingPreferences: vi.fn(), getProfile: vi.fn() }))
vi.mock('./bookingApi', () => ({ getBookingBilling: vi.fn(), requestBookingBilling: vi.fn() }))

describe('CheckoutBilling', () => {
  beforeEach(() => {
    vi.mocked(getBillingPreferences).mockResolvedValue(null)
    vi.mocked(getBookingBilling).mockResolvedValue(null)
    vi.mocked(getProfile).mockResolvedValue({ fullName: 'Khách LAK', email: 'customer@example.com', phone: null, birthDate: null })
    vi.mocked(requestBookingBilling).mockImplementation(async (_, value) => value)
  })

  it('saves an optional invoice request against the current booking', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><CheckoutBilling bookingCode="LAK-123" payable /></MemoryRouter>)
    await user.click(await screen.findByRole('button', { name: 'Yêu cầu hóa đơn' }))
    expect(screen.getByRole('textbox', { name: 'Tên người nhận' })).toHaveValue('Khách LAK')
    await user.click(screen.getByRole('button', { name: 'Lưu cho booking' }))
    await waitFor(() => expect(requestBookingBilling).toHaveBeenCalledWith('LAK-123', {
      recipientType: 'PERSONAL', recipientName: 'Khách LAK', taxCode: null, address: null, email: 'customer@example.com',
    }))
    expect(await screen.findByText(/Đã ghi nhận yêu cầu hóa đơn cho booking này/)).toBeInTheDocument()
  })
})
