import { apiClient } from '../../shared/api/apiClient'

export type AuthUser = { id: string; email: string; fullName: string; roles: string[] }
export type AuthResponse = { accessToken: string; user: AuthUser }

export async function initializeCsrf(): Promise<void> { await apiClient.get('/api/auth/csrf') }
export async function login(email: string, password: string): Promise<AuthResponse> {
  return (await apiClient.post<AuthResponse>('/api/auth/login', { email, password })).data
}
export async function register(fullName: string, email: string, password: string): Promise<AuthResponse> {
  return (await apiClient.post<AuthResponse>('/api/auth/register', { fullName, email, password })).data
}
export async function refresh(): Promise<AuthResponse> { return (await apiClient.post<AuthResponse>('/api/auth/refresh')).data }
export async function logout(): Promise<void> { await apiClient.post('/api/auth/logout') }
export async function forgotPassword(email: string): Promise<void> { await apiClient.post('/api/auth/forgot-password', { email }) }
export async function resetPassword(token: string, password: string): Promise<void> {
  await apiClient.post('/api/auth/reset-password', { token, password })
}
