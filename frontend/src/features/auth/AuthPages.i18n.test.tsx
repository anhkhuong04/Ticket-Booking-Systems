import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import i18n from '../../i18n'
import { LoginPage } from './AuthPages'

vi.mock('./AuthProvider', () => ({ useAuth: () => ({ login: vi.fn() }) }))

beforeEach(async () => { await i18n.changeLanguage('vi') })
afterEach(cleanup)

it('switches the login page to English without reloading', async () => {
  render(<MemoryRouter><LoginPage /></MemoryRouter>)
  fireEvent.click(screen.getByText('Ngôn ngữ'))
  fireEvent.click(screen.getByRole('button', { name: 'English' }))

  expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  expect(screen.getByPlaceholderText('Enter your password')).toBeInTheDocument()
  expect(document.documentElement.lang).toBe('en')
})
