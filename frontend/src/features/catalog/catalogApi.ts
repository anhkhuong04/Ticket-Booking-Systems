import { apiClient } from '../../shared/api/apiClient'

export type Movie = {
  id: string
  title: string
  durationMinutes: number
  ageRating: string
  releaseDate: string
  posterUrl: string | null
  status: 'NOW_SHOWING' | 'COMING_SOON'
  genres: string[]
}

export type MovieDetail = Movie & {
  description: string | null
  trailerUrl: string | null
  country: string | null
  director: string | null
  castMembers: string[]
}

export type MoviePage = {
  content: Movie[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type Genre = { id: string; name: string; slug: string }
export type Cinema = { id: string; name: string; address: string; city: string; timezone: string }

export async function getMovies(params: Record<string, string | number | undefined> = {}): Promise<MoviePage> {
  const response = await apiClient.get<MoviePage>('/api/movies', { params })
  return response.data
}

export async function getMovie(movieId: string): Promise<MovieDetail> {
  const response = await apiClient.get<MovieDetail>(`/api/movies/${movieId}`)
  return response.data
}

export async function getGenres(): Promise<Genre[]> {
  const response = await apiClient.get<Genre[]>('/api/genres')
  return response.data
}

export async function getCinemas(): Promise<Cinema[]> {
  const response = await apiClient.get<Cinema[]>('/api/cinemas')
  return response.data
}
