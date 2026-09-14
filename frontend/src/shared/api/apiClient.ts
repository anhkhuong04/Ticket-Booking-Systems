import axios from 'axios'

const apiBaseUrl = import.meta.env.VITE_API_URL?.replace(/\/$/, '')

export const apiClient = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    Accept: 'application/json',
  },
  timeout: 5_000,
})

export function assertApiConfigured(): void {
  if (!apiBaseUrl) {
    throw new Error('VITE_API_URL chưa được cấu hình.')
  }
}
