import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { CustomerShell } from './CustomerShell'
import { useAuth } from '../auth/AuthProvider'
import i18n, { LANGUAGE_KEY } from '../../i18n'

vi.mock('../auth/AuthProvider', () => ({ useAuth: vi.fn() }))

const mockedUseAuth = vi.mocked(useAuth)
const logout = vi.fn().mockResolvedValue(undefined)

function renderShell() {
  return render(<MemoryRouter initialEntries={['/']}><Routes>
    <Route element={<CustomerShell />}>
      <Route path="/" element={<h1>Trang chủ</h1>} />
      <Route path="/me" element={<h1>Trang tài khoản</h1>} />
      <Route path="/admin" element={<h1>Trang quản trị</h1>} />
    </Route>
  </Routes></MemoryRouter>)
}

describe('CustomerShell', () => {
  afterEach(cleanup)

  beforeEach(async () => {
    await i18n.changeLanguage('vi')
    logout.mockClear()
    mockedUseAuth.mockReturnValue({ user: { id: 'user-1', email: 'customer@lak.vn', fullName: 'Khách hàng', roles: ['CUSTOMER'] }, ready: true, login: vi.fn(), register: vi.fn(), logout, updateDisplayName: vi.fn() })
  })

  it('replaces the login call to action with an account menu for a signed-in customer', () => {
    renderShell()

    expect(screen.queryByRole('link', { name: 'Đăng nhập' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByText('Tài khoản'))
    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('href', '/me')
    fireEvent.click(screen.getByRole('button', { name: 'Đăng xuất' }))
    expect(logout).toHaveBeenCalledOnce()
  })

  it('links an administrator dashboard menu item to the admin dashboard', () => {
    mockedUseAuth.mockReturnValue({ user: { id: 'admin-1', email: 'admin@lak.vn', fullName: 'Quản trị viên', roles: ['SUPER_ADMIN'] }, ready: true, login: vi.fn(), register: vi.fn(), logout, updateDisplayName: vi.fn() })
    renderShell()

    fireEvent.click(screen.getByText('Tài khoản'))
    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('href', '/admin')
  })

  it('changes the customer navigation to English and remembers the choice', async () => {
    renderShell()
    fireEvent.click(screen.getByText('Ngôn ngữ'))
    fireEvent.click(screen.getByRole('button', { name: 'English' }))

    expect(await screen.findByRole('link', { name: 'Movies' })).toBeInTheDocument()
    expect(document.documentElement.lang).toBe('en')
    expect(window.localStorage.getItem(LANGUAGE_KEY)).toBe('en')
  })
})
