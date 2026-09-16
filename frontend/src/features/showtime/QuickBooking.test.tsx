import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { getMovies } from '../catalog/catalogApi'
import { getOpenShowtimes, getShowtimeAvailability } from './showtimeApi'
import { QuickBooking } from './QuickBooking'

vi.mock('../catalog/catalogApi', () => ({ getMovies: vi.fn() }))
vi.mock('./showtimeApi', () => ({ getOpenShowtimes: vi.fn(), getShowtimeAvailability: vi.fn() }))

const mockedGetMovies = vi.mocked(getMovies)
const mockedGetAvailability = vi.mocked(getShowtimeAvailability)
const mockedGetShowtimes = vi.mocked(getOpenShowtimes)

describe('QuickBooking', () => {
  afterEach(() => {
    cleanup()
    mockedGetMovies.mockReset()
    mockedGetAvailability.mockReset()
    mockedGetShowtimes.mockReset()
  })

  it('loads each real availability step and navigates only after a showtime is selected', async () => {
    mockedGetMovies.mockResolvedValue({
      content: [{ id: 'movie-1', title: 'Phim LAK', durationMinutes: 120, ageRating: 'T13', releaseDate: '2030-01-01', posterUrl: null, status: 'NOW_SHOWING', genres: ['Hành động'] }],
      page: 0, size: 100, totalElements: 1, totalPages: 1,
    })
    mockedGetAvailability.mockResolvedValue({
      movieId: 'movie-1',
      cinemas: [{ cinemaId: 'cinema-1', cinemaName: 'LAK Trung tâm', cinemaAddress: '1 Đường LAK', dates: ['2030-01-02'] }],
    })
    mockedGetShowtimes.mockResolvedValue([{
      id: 'showtime-1', movieId: 'movie-1', cinemaId: 'cinema-1', cinemaName: 'LAK Trung tâm', cinemaAddress: '1 Đường LAK', screenFormat: '2D', startAt: '2030-01-02T13:00:00Z', endAt: '2030-01-02T15:00:00Z', salesCloseAt: '2030-01-02T12:55:00Z', prices: { STANDARD: 90_000 },
    }])

    render(<MemoryRouter><Routes>
      <Route path="/" element={<QuickBooking />} />
      <Route path="/showtimes/:showtimeId/seats" element={<h1>Chọn ghế cho suất chiếu</h1>} />
    </Routes></MemoryRouter>)

    expect(screen.getByRole('button', { name: 'Chọn ghế' })).toBeDisabled()
    fireEvent.change(await screen.findByLabelText('Chọn phim'), { target: { value: 'movie-1' } })
    await waitFor(() => expect(mockedGetAvailability).toHaveBeenCalledWith('movie-1'))
    fireEvent.change(await screen.findByLabelText('Chọn rạp'), { target: { value: 'cinema-1' } })
    fireEvent.change(screen.getByLabelText('Chọn ngày'), { target: { value: '2030-01-02' } })
    await waitFor(() => expect(mockedGetShowtimes).toHaveBeenCalledWith('movie-1', '2030-01-02', 'cinema-1'))
    fireEvent.change(screen.getByLabelText('Chọn suất chiếu'), { target: { value: 'showtime-1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Chọn ghế' }))

    expect(await screen.findByRole('heading', { name: 'Chọn ghế cho suất chiếu' })).toBeInTheDocument()
  })
})
