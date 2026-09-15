import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { StaffScannerPage } from './StaffScannerPage'
import { validateTicket } from './scannerApi'

vi.mock('./scannerApi', () => ({ validateTicket: vi.fn() }))

const mockedValidateTicket = vi.mocked(validateTicket)

describe('StaffScannerPage', () => {
  afterEach(() => {
    cleanup()
    mockedValidateTicket.mockReset()
  })

  it('validates a manually entered ticket and shows the backend result', async () => {
    mockedValidateTicket.mockResolvedValue({
      result: 'VALID', ticketCode: 'TKT-123', movieTitle: 'Phim thử nghiệm',
      cinemaName: 'LAK Quận 1', auditoriumName: 'Phòng 1', startAt: '2030-01-01T10:00:00Z',
      seatLabels: ['A1'], firstScannedAt: '2030-01-01T10:01:00Z',
    })
    render(<StaffScannerPage />)
    fireEvent.change(screen.getByLabelText('Nhập mã vé thủ công'), { target: { value: 'TKT-123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác thực vé' }))
    expect(await screen.findByText('VÉ HỢP LỆ')).toBeInTheDocument()
    expect(mockedValidateTicket).toHaveBeenCalledWith('TKT-123')
  })
})
