import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { updateMovie, uploadMovieMedia, type AdminMovie } from './adminApi'
import { MovieEditorForm } from './MovieEditorForm'

vi.mock('./adminApi', () => ({ createMovie: vi.fn(), updateMovie: vi.fn(), uploadMovieMedia: vi.fn(), MAX_MOVIE_MEDIA_BYTES: 3 * 1024 * 1024 }))

const movie: AdminMovie = {
  id: 'movie-1', title: 'Film', description: 'Description', durationMinutes: 120, ageRating: 'T13',
  releaseDate: '2030-01-01', posterUrl: null, trailerUrl: null, status: 'NOW_SHOWING', genres: ['Drama'],
  country: 'Việt Nam', director: 'Director', castMembers: ['Actor One', 'Actor Two'],
}

describe('MovieEditorForm', () => {
  afterEach(() => { cleanup(); vi.mocked(updateMovie).mockReset(); vi.mocked(uploadMovieMedia).mockReset() })

  it('sends ordered credits when editing an existing movie', async () => {
    vi.mocked(updateMovie).mockResolvedValue({ ...movie, status: 'NOW_SHOWING' })
    const saved = vi.fn()
    render(<MovieEditorForm movie={movie} genres={[{ id: 'genre-1', name: 'Drama', slug: 'drama' }]} onSaved={saved} onCancel={vi.fn()} onError={vi.fn()} />)
    expect(screen.getByLabelText('Diễn viên (mỗi người một dòng)')).toHaveValue('Actor One\nActor Two')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))
    await waitFor(() => expect(updateMovie).toHaveBeenCalledWith('movie-1', expect.objectContaining({
      country: 'Việt Nam', director: 'Director', castMembers: ['Actor One', 'Actor Two'], genreIds: ['genre-1'],
    })))
    expect(saved).toHaveBeenCalled()
  })

  it('rejects a selected file above 3 MB before uploading', async () => {
    const onError = vi.fn()
    render(<MovieEditorForm movie={movie} genres={[]} onSaved={vi.fn()} onCancel={vi.fn()} onError={onError} />)
    const file = new File([new Uint8Array(3 * 1024 * 1024 + 1)], 'poster.png', { type: 'image/png' })
    fireEvent.change(screen.getByLabelText('Poster phim – chọn tệp'), { target: { files: [file] } })
    await waitFor(() => expect(onError).toHaveBeenCalledWith(expect.objectContaining({ message: 'Tệp vượt quá giới hạn 3 MB.' })))
    expect(uploadMovieMedia).not.toHaveBeenCalled()
  })
})
