import { useState, type ChangeEvent, type FormEvent } from 'react'
import type { Genre, MovieDetail } from '../catalog/catalogApi'
import { createMovie, MAX_MOVIE_MEDIA_BYTES, updateMovie, uploadMovieMedia, type AdminMovie, type MoviePayload } from './adminApi'

type Props = {
  movie: AdminMovie | null
  genres: Genre[]
  onSaved: () => void
  onCancel: () => void
  onError: (error: unknown) => void
}

type MediaFieldProps = {
  label: string
  accept: string
  value: string | null
  busy: boolean
  kind: 'poster' | 'trailer'
  onChange: (kind: 'poster' | 'trailer', event: ChangeEvent<HTMLInputElement>) => void
  onRemove: () => void
}

function MovieMediaField({ label, accept, value, busy, kind, onChange, onRemove }: MediaFieldProps) {
  return <fieldset className="rounded-xl border border-border p-3">
    <legend className="px-1 text-sm font-medium text-text-primary">{label}</legend>
    {value && (kind === 'poster'
      ? <img src={value} alt="Poster đã chọn" className="mt-2 h-36 w-24 rounded-lg object-cover" />
      : <video src={value} controls preload="metadata" className="mt-2 max-h-48 w-full rounded-lg bg-slate-900" />)}
    <div className="mt-3 flex flex-wrap items-center gap-3">
      <label className="secondary-button cursor-pointer">
        <span>{value ? 'Thay tệp' : 'Chọn tệp'}</span>
        <input type="file" accept={accept} className="sr-only" aria-label={`${label} – chọn tệp`} disabled={busy} onChange={(event) => onChange(kind, event)} />
      </label>
      {value && <button type="button" onClick={onRemove} disabled={busy} className="min-h-11 text-sm font-semibold text-primary disabled:opacity-50">Gỡ tệp</button>}
      <span className="text-xs text-text-secondary">JPEG, PNG, WebP hoặc MP4, WebM; tối đa 3 MB.</span>
    </div>
  </fieldset>
}

export function MovieEditorForm({ movie, genres, onSaved, onCancel, onError }: Props) {
  const [busy, setBusy] = useState(false)
  const [posterUrl, setPosterUrl] = useState<string | null>(movie?.posterUrl ?? null)
  const [trailerUrl, setTrailerUrl] = useState<string | null>(movie?.trailerUrl ?? null)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    const payload: MoviePayload = {
      title: String(data.get('title')).trim(),
      description: String(data.get('description')).trim() || null,
      durationMinutes: Number(data.get('durationMinutes')),
      ageRating: String(data.get('ageRating')).trim(),
      releaseDate: String(data.get('releaseDate')),
      posterUrl,
      trailerUrl,
      status: String(data.get('status')) as MovieDetail['status'],
      genreIds: data.getAll('genreIds').map(String),
      country: String(data.get('country')).trim() || null,
      director: String(data.get('director')).trim() || null,
      castMembers: String(data.get('castMembers')).split('\n').map((name) => name.trim()).filter(Boolean),
    }
    setBusy(true)
    try {
      if (movie) await updateMovie(movie.id, payload)
      else await createMovie(payload)
      onSaved()
    } catch (error) {
      onError(error)
    } finally {
      setBusy(false)
    }
  }

  const upload = async (kind: 'poster' | 'trailer', event: ChangeEvent<HTMLInputElement>) => {
    const file = event.currentTarget.files?.[0]
    event.currentTarget.value = ''
    if (!file) return
    if (file.size > MAX_MOVIE_MEDIA_BYTES) {
      onError(new Error('Tệp vượt quá giới hạn 3 MB.'))
      return
    }
    setBusy(true)
    try {
      const url = await uploadMovieMedia(file)
      if (kind === 'poster') setPosterUrl(url)
      else setTrailerUrl(url)
    } catch (error) {
      onError(error)
    } finally {
      setBusy(false)
    }
  }

  return <form key={movie?.id ?? 'new'} onSubmit={submit}>
    <div className="space-y-3">
      <label className="block text-sm">Tên phim<input required name="title" maxLength={255} defaultValue={movie?.title} className="control mt-1" /></label>
      <label className="block text-sm">Mô tả<textarea name="description" defaultValue={movie?.description ?? ''} className="control mt-1 min-h-24" /></label>
      <div className="grid grid-cols-2 gap-3">
        <label className="block text-sm">Thời lượng (phút)<input required name="durationMinutes" type="number" min="1" defaultValue={movie?.durationMinutes} className="control mt-1" /></label>
        <label className="block text-sm">Phân loại tuổi<input required name="ageRating" defaultValue={movie?.ageRating} className="control mt-1" /></label>
      </div>
      <label className="block text-sm">Ngày khởi chiếu<input required name="releaseDate" type="date" defaultValue={movie?.releaseDate} className="control mt-1" /></label>
      <label className="block text-sm">Trạng thái<select name="status" defaultValue={movie?.status ?? 'COMING_SOON'} className="control mt-1"><option value="COMING_SOON">Sắp chiếu</option><option value="NOW_SHOWING">Đang chiếu</option></select></label>
      <label className="block text-sm">Quốc gia<input name="country" maxLength={100} defaultValue={movie?.country ?? ''} className="control mt-1" /></label>
      <label className="block text-sm">Đạo diễn<input name="director" maxLength={255} defaultValue={movie?.director ?? ''} className="control mt-1" /></label>
      <label className="block text-sm">Diễn viên (mỗi người một dòng)<textarea name="castMembers" maxLength={4530} defaultValue={movie?.castMembers.join('\n') ?? ''} className="control mt-1 min-h-24" /></label>
      <MovieMediaField label="Poster phim" kind="poster" accept="image/jpeg,image/png,image/webp" value={posterUrl} busy={busy} onChange={upload} onRemove={() => setPosterUrl(null)} />
      <MovieMediaField label="Trailer" kind="trailer" accept="video/mp4,video/webm" value={trailerUrl} busy={busy} onChange={upload} onRemove={() => setTrailerUrl(null)} />
      <fieldset><legend className="text-sm font-medium">Thể loại</legend><div className="mt-2 grid grid-cols-2 gap-2">{genres.map((genre) => <label key={genre.id} className="flex items-center gap-2 text-sm"><input type="checkbox" name="genreIds" value={genre.id} defaultChecked={movie?.genres.includes(genre.name)} />{genre.name}</label>)}</div></fieldset>
      <div className="flex gap-3"><button disabled={busy} className="primary-button flex-1">{busy ? 'Đang lưu…' : movie ? 'Lưu thay đổi' : 'Thêm phim'}</button>{movie && <button type="button" onClick={onCancel} className="secondary-button">Hủy</button>}</div>
    </div>
  </form>
}
