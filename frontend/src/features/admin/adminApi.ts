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
export async function updateMovie(id: string, payload: MoviePayload) { return (await apiClient.put<MovieDetail>(`/api/admin/movies/${id}`, payload)).data }
export async function archiveMovie(id: string) { await apiClient.delete(`/api/admin/movies/${id}`) }
export async function createGenre(payload: { name: string; slug: string }) { return (await apiClient.post<Genre>('/api/admin/genres', payload)).data }
export const MAX_MOVIE_MEDIA_BYTES = 3 * 1024 * 1024
const movieMediaTypes = new Set(['image/jpeg', 'image/png', 'image/webp', 'video/mp4', 'video/webm'])

export async function signMedia(file: File) {
  return (await apiClient.post<{ cloudName: string; apiKey: string; folder: string; resourceType: 'image' | 'video'; timestamp: number; signature: string }>(
    '/api/admin/media/signatures', { filename: file.name, contentType: file.type, sizeBytes: file.size },
  )).data
}

export async function uploadMovieMedia(file: File) {
  if (!movieMediaTypes.has(file.type) || file.size <= 0 || file.size > MAX_MOVIE_MEDIA_BYTES) {
    throw new Error('Chỉ nhận ảnh JPEG, PNG, WebP hoặc video MP4, WebM không quá 3 MB.')
  }
  const signed = await signMedia(file)
  const data = new FormData()
  data.set('file', file)
  data.set('api_key', signed.apiKey)
  data.set('timestamp', String(signed.timestamp))
  data.set('signature', signed.signature)
  data.set('folder', signed.folder)
  const response = await fetch(`https://api.cloudinary.com/v1_1/${signed.cloudName}/${signed.resourceType}/upload`, { method: 'POST', body: data })
  if (!response.ok) throw new Error('Không thể tải tệp lên.')
  const body: unknown = await response.json()
  if (!body || typeof body !== 'object' || !('secure_url' in body) || typeof body.secure_url !== 'string') {
    throw new Error('Phản hồi tải tệp không hợp lệ.')
  }
  return body.secure_url
}
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

export type AdminBookingRow = { id: string; bookingCode: string; customerName: string; customerEmail: string; cinemaId: string; cinemaName: string; movieTitle: string; startAt: string; totalAmount: number; status: string; createdAt: string }
export type AdminPaymentRow = { id: string; bookingCode: string; cinemaId: string; cinemaName: string; provider: string; transactionReference: string; amount: number; status: string; createdAt: string; paidAt: string | null }
export type AdminRefundRow = { id: string; bookingCode: string; cinemaId: string; cinemaName: string; amount: number; reason: string; status: string; attemptCount: number; requestedAt: string; refundedAt: string | null }
export type AdminUserRow = { id: string; fullName: string; email: string; phone: string | null; status: 'ACTIVE' | 'LOCKED'; roles: string[]; cinemaNames: string[]; createdAt: string }
export type ReportSummary = {
  netRevenue: number
  bookings: number
  seatsSold: number
  occupancyPercent: number
  alerts: { totalBookings: number; paymentReview: number; overduePayments: number; overdueRefunds: number; failedRefunds: number; cancelledShowtimeBookings: number; paidWithoutTicket: number }
  previousPeriod: { netRevenue: number; bookings: number; seatsSold: number }
  daily: { date: string; netRevenue: number; bookings: number; seatsSold: number }[]
  topMovies: { movieId: string; title: string; netRevenue: number; seatsSold: number }[]
  asOf: string
}
export async function getAdminBookings(params: Record<string, string | undefined>) { return (await apiClient.get<AdminBookingRow[]>('/api/admin/bookings', { params })).data }
export async function getAdminPayments(params: Record<string, string | undefined>) { return (await apiClient.get<AdminPaymentRow[]>('/api/admin/payments', { params })).data }
export async function getAdminRefunds(params: Record<string, string | undefined>) { return (await apiClient.get<AdminRefundRow[]>('/api/admin/refunds', { params })).data }
export async function retryRefund(id: string) { await apiClient.post(`/api/admin/refunds/${encodeURIComponent(id)}/retry`) }
export async function getAdminUsers(q?: string) { return (await apiClient.get<AdminUserRow[]>('/api/admin/users', { params: { q } })).data }
export async function setUserLocked(id: string, locked: boolean) { await apiClient.post(`/api/admin/users/${encodeURIComponent(id)}/${locked ? 'lock' : 'unlock'}`) }
export async function getReportSummary(params: { from: string; to: string; cinemaId?: string }) { return (await apiClient.get<ReportSummary>('/api/admin/reports/summary', { params })).data }
