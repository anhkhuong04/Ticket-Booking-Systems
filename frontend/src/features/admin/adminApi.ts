import { apiClient } from '../../shared/api/apiClient'
import type { Genre, MovieDetail } from '../catalog/catalogApi'

export type CinemaAdmin = { id: string; name: string; address: string; city: string; timezone: string; status: 'ACTIVE' | 'INACTIVE'; auditoriumCount: number }
export type Auditorium = { id: string; cinemaId: string; name: string; screenFormat: string; cleanupMinutes: number; status: 'ACTIVE' | 'INACTIVE' }
export type Seat = { id?: string; rowLabel: string; seatNumber: number; seatType: 'STANDARD' | 'VIP' | 'COUPLE'; pairKey: string | null; status: 'ACTIVE' | 'LOCKED' }
export type MoviePayload = Omit<MovieDetail, 'id' | 'genres'> & { genreIds: string[] }
export type AdminMovie = Omit<MovieDetail, 'status'> & { status: 'NOW_SHOWING' | 'COMING_SOON' | 'ARCHIVED' }

export async function getAdminMovies() { return (await apiClient.get<AdminMovie[]>('/api/admin/movies')).data }
export async function createMovie(payload: MoviePayload) { return (await apiClient.post<MovieDetail>('/api/admin/movies', payload)).data }
export async function archiveMovie(id: string) { await apiClient.delete(`/api/admin/movies/${id}`) }
export async function createGenre(payload: { name: string; slug: string }) { return (await apiClient.post<Genre>('/api/admin/genres', payload)).data }
export async function signMedia(file: File) { return (await apiClient.post<{ cloudName: string; apiKey: string; folder: string; timestamp: number; signature: string }>('/api/admin/media/signatures', { filename: file.name, contentType: file.type, sizeBytes: file.size })).data }
export async function uploadPoster(file: File) { const signed = await signMedia(file); const data = new FormData(); data.set('file', file); data.set('api_key', signed.apiKey); data.set('timestamp', String(signed.timestamp)); data.set('signature', signed.signature); data.set('folder', signed.folder); const response = await fetch(`https://api.cloudinary.com/v1_1/${signed.cloudName}/image/upload`, { method: 'POST', body: data }); if (!response.ok) throw new Error('Không thể tải ảnh lên.'); const body: unknown = await response.json(); if (!body || typeof body !== 'object' || !('secure_url' in body) || typeof body.secure_url !== 'string') throw new Error('Phản hồi tải ảnh không hợp lệ.'); return body.secure_url }

export async function getAdminCinemas() { return (await apiClient.get<CinemaAdmin[]>('/api/admin/cinemas')).data }
export async function createCinema(payload: Omit<CinemaAdmin, 'id' | 'auditoriumCount'>) { return (await apiClient.post('/api/admin/cinemas', payload)).data }
export async function deactivateCinema(id: string) { await apiClient.delete(`/api/admin/cinemas/${id}`) }
export async function getAuditoriums(cinemaId: string) { return (await apiClient.get<Auditorium[]>(`/api/admin/cinemas/${cinemaId}/auditoriums`)).data }
export async function createAuditorium(cinemaId: string, payload: Omit<Auditorium, 'id' | 'cinemaId'>) { return (await apiClient.post<Auditorium>(`/api/admin/cinemas/${cinemaId}/auditoriums`, payload)).data }
export async function deactivateAuditorium(id: string) { await apiClient.delete(`/api/admin/auditoriums/${id}`) }
export async function getSeats(auditoriumId: string) { return (await apiClient.get<Seat[]>(`/api/admin/auditoriums/${auditoriumId}/seats`)).data }
export async function saveSeats(auditoriumId: string, seats: Seat[]) { return (await apiClient.put<Seat[]>(`/api/admin/auditoriums/${auditoriumId}/seats`, seats)).data }
