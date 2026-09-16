import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AgeAdvisory } from './ageAdvisory'

describe('AgeAdvisory', () => {
  it('warns when the viewer turns sixteen after the showtime and does not block booking', () => {
    render(<MemoryRouter><AgeAdvisory rating="T16" birthDate="2010-09-17" at="2026-09-16T10:00:00Z" compact /></MemoryRouter>)
    expect(screen.getByText(/chưa đủ 16 tuổi/)).toBeInTheDocument()
    expect(screen.getByText(/kiểm tra độ tuổi của từng người/)).toBeInTheDocument()
  })

  it('prompts for missing birth date without assuming eligibility', () => {
    render(<MemoryRouter><AgeAdvisory rating="T18" birthDate={null} at="2026-09-16T18:00:00Z" /></MemoryRouter>)
    expect(screen.getByRole('link', { name: 'Bổ sung ngày sinh' })).toHaveAttribute('href', '/me/profile')
  })
})
