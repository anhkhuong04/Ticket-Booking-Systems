import { apiClient } from '../../shared/api/apiClient'
import type { Genre, MovieDetail } from '../catalog/catalogApi'

export type CinemaAdmin = { id: string; name: string; address: string; city: string; timezone: string; status: 'ACTIVE' | 'INACTIVE'; auditoriumCount: number }
export type Auditorium = { id: string; cinemaId: string; name: string; screenFormat: string; cleanupMinutes: number; status: 'ACTIVE' | 'INACTIVE' }
export type Seat = { id?: string; rowLabel: string; seatNumber: number; seatType: 'STANDARD' | 'VIP' | 'COUPLE'; pairKey: string | null; status: 'ACTIVE' | 'LOCKED' }
export type MoviePayload = Omit<MovieDetail, 'id' | 'genres'> & { genreIds: string[] }
export type AdminMovie = Omit<MovieDetail, 'status'> & { status: 'NOW_SHOWING' | 'COMING_SOON' | 'ARCHIVED' }
export type AdminShowtime = { id: string; cinemaId: string; cinemaName: string; auditoriumId: string; auditoriumName: string; movieId: string; movieTitle: string; startAt: string; endAt: string; salesCloseAt: string; status: 'SCHEDULED' | 'CANCELLED'; cancellationBlocked: boolean }
export type PriceRule = { id: string; profileId: string; dayType: 'ANY' | 'WEEKDAY' | 'WEEKEND'; timeFrom: string | null; timeTo: string | null; screenFormat: string | null; seatType: string | null; amount: number; priority: number }
export type PriceProfile = { id: string; cinemaId: string | null; name: string; effectiveFrom: string; effectiveTo: string | null; status: 'ACTIVE' | 'INACTIVE' }
export type PriceProfileDetail = { profile: PriceProfile; rules: PriceRule[] }

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
export async function getAdminShowtimes(cinemaId: string, date: string) { return (await apiClient.get<AdminShowtime[]>('/api/admin/showtimes', { params: { cinemaId, date } })).data }
export async function createShowtime(payload: { movieId: string; auditoriumId: string; startAt: string; priceOverrides: Record<string, number> }) { return (await apiClient.post<AdminShowtime>('/api/admin/showtimes', payload)).data }
export async function cancelShowtime(showtimeId: string) { return (await apiClient.delete<AdminShowtime>(`/api/admin/showtimes/${showtimeId}`)).data }
export async function getPriceProfiles(cinemaId?: string) { return (await apiClient.get<PriceProfileDetail[]>('/api/admin/price-profiles', { params: { cinemaId } })).data }
export async function createPriceProfile(payload: Omit<PriceProfile, 'id'>) { return (await apiClient.post<PriceProfile>('/api/admin/price-profiles', payload)).data }
export async function updatePriceProfile(id: string, payload: Omit<PriceProfile, 'id'>) { return (await apiClient.put<PriceProfile>(`/api/admin/price-profiles/${id}`, payload)).data }
export async function deactivatePriceProfile(id: string) { await apiClient.delete(`/api/admin/price-profiles/${id}`) }
export async function createPriceRule(profileId: string, payload: Omit<PriceRule, 'id' | 'profileId'>) { return (await apiClient.post<PriceRule>(`/api/admin/price-profiles/${profileId}/rules`, payload)).data }
export async function updatePriceRule(id: string, payload: Omit<PriceRule, 'id' | 'profileId'>) { return (await apiClient.put<PriceRule>(`/api/admin/price-rules/${id}`, payload)).data }
export async function deletePriceRule(id: string) { await apiClient.delete(`/api/admin/price-rules/${id}`) }
