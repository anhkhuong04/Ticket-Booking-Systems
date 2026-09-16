import { useState, type ChangeEvent, type FormEvent } from 'react'
import type { Genre, MovieDetail } from '../catalog/catalogApi'
import { createMovie, updateMovie, uploadPoster, type AdminMovie, type MoviePayload } from './adminApi'

type Props = {
  movie: AdminMovie | null
  genres: Genre[]
  onSaved: () => void
  onCancel: () => void
  onError: (error: unknown) => void
}

export function MovieEditorForm({ movie, genres, onSaved, onCancel, onError }: Props) {
  const [busy, setBusy] = useState(false)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    const payload: MoviePayload = {
      title: String(data.get('title')).trim(),
      description: String(data.get('description')).trim() || null,
      durationMinutes: Number(data.get('durationMinutes')),
      ageRating: String(data.get('ageRating')).trim(),
      releaseDate: String(data.get('releaseDate')),
      posterUrl: String(data.get('posterUrl')).trim() || null,
      trailerUrl: String(data.get('trailerUrl')).trim() || null,
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

  const upload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.currentTarget.files?.[0]
    const target = event.currentTarget.form?.elements.namedItem('posterUrl')
    if (!file || !(target instanceof HTMLInputElement)) return
    setBusy(true)
    try {
      target.value = await uploadPoster(file)
    } catch (error) {
      onError(error)
    } finally {
      setBusy(false)
    }
  }

  return <form key={movie?.id ?? 'new'} onSubmit={submit} className="rounded-xl border border-border bg-surface p-5">
    <h2 className="font-semibold">{movie ? `Chỉnh sửa ${movie.title}` : 'Thêm phim'}</h2>
    <div className="mt-4 space-y-3">
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
      <label className="block text-sm">URL poster<input name="posterUrl" type="url" defaultValue={movie?.posterUrl ?? ''} className="control mt-1" /></label>
      <label className="block text-sm">Hoặc tải poster<input type="file" accept="image/jpeg,image/png,image/webp" className="mt-1 block w-full text-sm" onChange={upload} /></label>
      <label className="block text-sm">URL trailer<input name="trailerUrl" type="url" defaultValue={movie?.trailerUrl ?? ''} className="control mt-1" /></label>
      <fieldset><legend className="text-sm font-medium">Thể loại</legend><div className="mt-2 grid grid-cols-2 gap-2">{genres.map((genre) => <label key={genre.id} className="flex items-center gap-2 text-sm"><input type="checkbox" name="genreIds" value={genre.id} defaultChecked={movie?.genres.includes(genre.name)} />{genre.name}</label>)}</div></fieldset>
      <div className="flex gap-3"><button disabled={busy} className="primary-button flex-1">{busy ? 'Đang lưu…' : movie ? 'Lưu thay đổi' : 'Thêm phim'}</button>{movie && <button type="button" onClick={onCancel} className="secondary-button">Hủy</button>}</div>
    </div>
  </form>
}
