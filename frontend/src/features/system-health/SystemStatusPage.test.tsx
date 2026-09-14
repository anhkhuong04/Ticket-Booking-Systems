import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getSystemHealth } from './api/healthApi'
import { SystemStatusPage } from './SystemStatusPage'

vi.mock('./api/healthApi', () => ({
  getSystemHealth: vi.fn(),
}))

const mockedGetSystemHealth = vi.mocked(getSystemHealth)

describe('SystemStatusPage', () => {
  beforeEach(() => {
    mockedGetSystemHealth.mockReset()
  })

  it('shows the status returned by the backend', async () => {
    mockedGetSystemHealth.mockResolvedValue({
      status: 'UP',
      timestamp: '2026-01-01T00:00:00Z',
      services: {
        backend: { status: 'UP' },
        database: { status: 'UP' },
        redis: { status: 'UP' },
      },
    })

    render(<SystemStatusPage />)

    expect(await screen.findByText('PostgreSQL')).toBeInTheDocument()
    expect(screen.getAllByText('Hoạt động')).toHaveLength(3)
  })

  it('shows a safe error when the backend cannot be reached', async () => {
    mockedGetSystemHealth.mockRejectedValue(new Error('Kết nối thất bại'))

    render(<SystemStatusPage />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Kết nối thất bại')
    expect(screen.getAllByText('Chưa xác định')).toHaveLength(3)
  })
})
