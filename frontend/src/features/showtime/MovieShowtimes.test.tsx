import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { getOpenShowtimes, getShowtimeAvailability } from './showtimeApi'
import { MovieShowtimes } from './MovieShowtimes'

vi.mock('./showtimeApi', () => ({ getOpenShowtimes: vi.fn(), getShowtimeAvailability: vi.fn() }))
const availability = vi.mocked(getShowtimeAvailability)
const showtimes = vi.mocked(getOpenShowtimes)

describe('MovieShowtimes', () => {
  afterEach(() => { cleanup(); availability.mockReset(); showtimes.mockReset() })

  it('uses live availability and links the selected showtime to seats', async () => {
    availability.mockResolvedValue({ movieId: 'movie-1', cinemas: [{ cinemaId: 'cinema-1', cinemaName: 'LAK Quận 1', cinemaAddress: '1 Test', dates: ['2030-01-02'] }] })
    showtimes.mockResolvedValue([{ id: 'show-1', movieId: 'movie-1', cinemaId: 'cinema-1', cinemaName: 'LAK Quận 1', cinemaAddress: '1 Test', screenFormat: 'IMAX', startAt: '2030-01-02T13:00:00Z', endAt: '2030-01-02T15:00:00Z', salesCloseAt: '2030-01-02T12:55:00Z', prices: { STANDARD: 90000 } }])
    render(<MemoryRouter><Routes><Route path="/" element={<MovieShowtimes movieId="movie-1" />} /></Routes></MemoryRouter>)

    await waitFor(() => expect(showtimes).toHaveBeenCalledWith('movie-1', '2030-01-02', undefined))
    expect(await screen.findByRole('heading', { name: 'LAK Quận 1' })).toBeInTheDocument()
    expect(screen.getByText('IMAX')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /90\.000/ })).toHaveAttribute('href', '/showtimes/show-1/seats')
  })

  it('shows a safe empty state when no open dates exist', async () => {
    availability.mockResolvedValue({ movieId: 'movie-1', cinemas: [] })
    render(<MemoryRouter><MovieShowtimes movieId="movie-1" /></MemoryRouter>)
    expect(await screen.findByText('Chưa có suất chiếu đang mở bán.')).toBeInTheDocument()
    expect(showtimes).not.toHaveBeenCalled()
  })
})
