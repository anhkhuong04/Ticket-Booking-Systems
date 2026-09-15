import axios from 'axios'

const apiBaseUrl = import.meta.env.VITE_API_URL?.replace(/\/$/, '')

export const apiClient = axios.create({
  baseURL: apiBaseUrl,
  withCredentials: true,
  headers: {
    Accept: 'application/json',
  },
  timeout: 5_000,
})

function csrfToken(): string | undefined {
  return document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith('lak_csrf_token='))
    ?.split('=')[1]
}

apiClient.interceptors.request.use((config) => {
  if (['post', 'put', 'patch', 'delete'].includes(config.method?.toLowerCase() ?? '')) {
    const token = csrfToken()
    if (token) {
      config.headers.set('X-CSRF-Token', decodeURIComponent(token))
    }
  }
  return config
})

export function setAccessToken(token?: string): void {
  if (token) {
    apiClient.defaults.headers.common.Authorization = `Bearer ${token}`
  } else {
    delete apiClient.defaults.headers.common.Authorization
  }
}

export function assertApiConfigured(): void {
  if (!apiBaseUrl) {
    throw new Error('VITE_API_URL chưa được cấu hình.')
  }
}
