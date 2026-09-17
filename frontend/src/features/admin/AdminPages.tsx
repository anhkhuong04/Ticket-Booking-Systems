import { useCallback, useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { isAxiosError } from 'axios'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { getGenres, type Genre } from '../catalog/catalogApi'
import {
  archiveMovie,
  createAuditorium,
  createCinema,
  createGenre,
  deactivateCinema,
  getAdminCinemas,
  getAdminMovies,
  getAuditoriums,
  getSeats,
  saveSeats,
  type AdminMovie,
  type Auditorium,
  type CinemaAdmin,
  type Seat,
} from './adminApi'
import { AdminDialog } from './AdminDialog'
import { MovieEditorForm } from './MovieEditorForm'

function message(error: unknown) {
  return isAxiosError<{ message?: string }>(error)
    ? error.response?.data?.message ?? 'Không thể hoàn tất thao tác.'
    : error instanceof Error ? error.message : 'Không thể hoàn tất thao tác.'
}

function Notice({ error }: { error: string | null }) {
  return error ? <p role="alert" className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</p> : null
}

function Page({ title, action, children }: { title: string; action?: ReactNode; children: ReactNode }) {
  return <main className="mx-auto max-w-7xl p-4 sm:p-6">
    <header className="flex flex-wrap items-start justify-between gap-4">
      <h1 className="text-2xl font-bold text-text-primary">{title}</h1>
      {action}
    </header>
    {children}
  </main>
}

function AddButton({ children, onClick, disabled = false }: { children?: string; onClick: () => void; disabled?: boolean }) {
  return <button type="button" onClick={onClick} disabled={disabled} className="primary-button gap-2 disabled:cursor-not-allowed">
    <Plus size={18} aria-hidden="true" />
    {children ?? 'Thêm mới'}
  </button>
}

export function AdminMoviesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [movies, setMovies] = useState<AdminMovie[]>([])
  const [genres, setGenres] = useState<Genre[]>([])
  const [selected, setSelected] = useState<AdminMovie | null>(null)
  const [editorOpen, setEditorOpen] = useState(false)
  const [genreOpen, setGenreOpen] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const load = useCallback(() => {
    void Promise.all([getAdminMovies(), getGenres()]).then(
      ([allMovies, allGenres]) => { setMovies(allMovies); setGenres(allGenres) },
      (reason) => setError(message(reason)),
    )
  }, [])
  useEffect(load, [load])

  const movieQuery = searchParams.get('q') ?? ''
  const movieStatus = searchParams.get('status') ?? ''
  const movieGenre = searchParams.get('genre') ?? ''
  const filteredMovies = useMemo(() => movies.filter((movie) => {
    const query = movieQuery.trim().toLocaleLowerCase('vi-VN')
    return (!query || movie.title.toLocaleLowerCase('vi-VN').includes(query))
      && (!movieStatus || movie.status === movieStatus)
      && (!movieGenre || movie.genres.includes(movieGenre))
  }), [movieGenre, movieQuery, movieStatus, movies])
  const setMovieFilter = (key: 'q' | 'status' | 'genre', value: string) => setSearchParams((current) => {
    const next = new URLSearchParams(current)
    if (value) next.set(key, value)
    else next.delete(key)
    return next
  })

  const closeEditor = () => { if (!busy) { setEditorOpen(false); setSelected(null) } }
  const submitGenre = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    setBusy(true); setError(null)
    try {
      await createGenre({ name: String(data.get('name')), slug: String(data.get('slug')) })
      setGenreOpen(false); load()
    } catch (reason) { setError(message(reason)) } finally { setBusy(false) }
  }
  const archive = async (movie: AdminMovie) => {
    if (!window.confirm(`Lưu trữ phim “${movie.title}”? Phim sẽ không còn hiển thị trên website.`)) return
    setBusy(true); setError(null)
    try { await archiveMovie(movie.id); load() } catch (reason) { setError(message(reason)) } finally { setBusy(false) }
  }

  return <Page title="Phim" action={<div className="flex flex-wrap gap-2"><button type="button" onClick={() => setGenreOpen(true)} className="secondary-button">Thêm thể loại</button><AddButton onClick={() => { setSelected(null); setEditorOpen(true) }} /></div>}>
    <p className="mt-2 text-sm text-text-secondary">Quản lý thông tin và hiển thị phim. Chỉ Super Admin có quyền thay đổi catalog.</p>
    <Notice error={error} />
    <div className="mt-5 grid gap-3 rounded-xl border border-border bg-surface p-4 sm:grid-cols-3">
      <label className="text-sm font-medium text-text-primary">Tìm phim<input value={movieQuery} onChange={(event) => setMovieFilter('q', event.target.value)} placeholder="Tên phim" className="control mt-1" /></label>
      <label className="text-sm font-medium text-text-primary">Trạng thái<select value={movieStatus} onChange={(event) => setMovieFilter('status', event.target.value)} className="control mt-1"><option value="">Tất cả trạng thái</option><option value="NOW_SHOWING">Đang chiếu</option><option value="COMING_SOON">Sắp chiếu</option><option value="ARCHIVED">Đã lưu trữ</option></select></label>
      <label className="text-sm font-medium text-text-primary">Thể loại<select value={movieGenre} onChange={(event) => setMovieFilter('genre', event.target.value)} className="control mt-1"><option value="">Tất cả thể loại</option>{genres.map((genre) => <option key={genre.id} value={genre.name}>{genre.name}</option>)}</select></label>
    </div>
    <section className="mt-6 overflow-x-auto rounded-xl border border-border bg-surface">
      <table className="min-w-full text-left text-sm">
        <thead className="border-b border-border bg-slate-50 text-text-secondary"><tr><th className="p-3">Tên phim</th><th className="p-3">Ngày chiếu</th><th className="p-3">Phân loại</th><th className="p-3">Trạng thái</th><th className="p-3">Thao tác</th></tr></thead>
        <tbody>{filteredMovies.map((movie) => <tr key={movie.id} className="border-b border-border">
          <td className="p-3 font-medium">{movie.title}<span className="block text-xs font-normal text-text-muted">{movie.durationMinutes} phút · {movie.genres.join(', ')}</span></td>
          <td className="p-3">{movie.releaseDate}</td><td className="p-3">{movie.ageRating}</td><td className="p-3">{movie.status}</td>
          <td className="p-3"><div className="flex gap-3"><button type="button" disabled={movie.status === 'ARCHIVED'} onClick={() => { setSelected(movie); setError(null); setEditorOpen(true) }} className="min-h-11 font-semibold text-primary disabled:opacity-50">Sửa</button><button type="button" disabled={busy || movie.status === 'ARCHIVED'} onClick={() => void archive(movie)} className="min-h-11 font-semibold text-primary disabled:opacity-50">Lưu trữ</button></div></td>
        </tr>)}</tbody>
      </table>
      {movies.length === 0 && <p className="p-6 text-text-secondary">Chưa có phim.</p>}
      {movies.length > 0 && filteredMovies.length === 0 && <p className="p-6 text-text-secondary">Không có dữ liệu khớp với điều kiện tìm kiếm.</p>}
    </section>
    {editorOpen && <AdminDialog title={selected ? `Chỉnh sửa ${selected.title}` : 'Thêm phim'} description="Các trường có dấu bắt buộc sẽ được backend kiểm tra lại khi lưu." onClose={closeEditor} size="large"><Notice error={error} /><MovieEditorForm key={selected?.id ?? 'new'} movie={selected} genres={genres} onSaved={() => { setEditorOpen(false); setSelected(null); setError(null); load() }} onCancel={closeEditor} onError={(reason) => setError(message(reason))} /></AdminDialog>}
    {genreOpen && <AdminDialog title="Thêm thể loại" onClose={() => !busy && setGenreOpen(false)}><Notice error={error} /><form onSubmit={submitGenre} className="grid gap-3"><label className="text-sm font-medium">Tên thể loại<input required name="name" className="control mt-1" /></label><label className="text-sm font-medium">Slug<input required name="slug" pattern="[a-z0-9]+(-[a-z0-9]+)*" className="control mt-1" /></label><button disabled={busy} className="primary-button justify-center">{busy ? 'Đang lưu…' : 'Thêm thể loại'}</button></form></AdminDialog>}
  </Page>
}

export function AdminCinemasPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [cinemas, setCinemas] = useState<CinemaAdmin[]>([])
  const [createOpen, setCreateOpen] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const load = useCallback(() => { void getAdminCinemas().then(setCinemas, (reason) => setError(message(reason))) }, [])
  useEffect(load, [load])
  const cinemaQuery = searchParams.get('q') ?? ''
  const cinemaCity = searchParams.get('city') ?? ''
  const cinemaStatus = searchParams.get('status') ?? ''
  const cinemaCities = useMemo(() => [...new Set(cinemas.map((cinema) => cinema.city))].toSorted((left, right) => left.localeCompare(right, 'vi-VN')), [cinemas])
  const filteredCinemas = useMemo(() => cinemas.filter((cinema) => {
    const query = cinemaQuery.trim().toLocaleLowerCase('vi-VN')
    return (!query || `${cinema.name} ${cinema.address} ${cinema.city}`.toLocaleLowerCase('vi-VN').includes(query))
      && (!cinemaCity || cinema.city === cinemaCity)
      && (!cinemaStatus || cinema.status === cinemaStatus)
  }), [cinemaCity, cinemaQuery, cinemaStatus, cinemas])
  const setCinemaFilter = (key: 'q' | 'city' | 'status', value: string) => setSearchParams((current) => {
    const next = new URLSearchParams(current)
    if (value) next.set(key, value)
    else next.delete(key)
    return next
  })
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault(); const data = new FormData(event.currentTarget); setBusy(true); setError(null)
    try { await createCinema({ name: String(data.get('name')), address: String(data.get('address')), city: String(data.get('city')), timezone: String(data.get('timezone')), status: 'ACTIVE' }); setCreateOpen(false); load() } catch (reason) { setError(message(reason)) } finally { setBusy(false) }
  }
  return <Page title="Rạp" action={<AddButton onClick={() => setCreateOpen(true)} />}>
    <p className="mt-2 text-sm text-text-secondary">Manager chỉ nhìn và thao tác trên các chi nhánh được phân công.</p><Notice error={error} />
    <div className="mt-5 grid gap-3 rounded-xl border border-border bg-surface p-4 sm:grid-cols-3">
      <label className="text-sm font-medium text-text-primary">Tìm rạp<input value={cinemaQuery} onChange={(event) => setCinemaFilter('q', event.target.value)} placeholder="Tên, địa chỉ hoặc thành phố" className="control mt-1" /></label>
      <label className="text-sm font-medium text-text-primary">Thành phố<select value={cinemaCity} onChange={(event) => setCinemaFilter('city', event.target.value)} className="control mt-1"><option value="">Tất cả thành phố</option>{cinemaCities.map((city) => <option key={city} value={city}>{city}</option>)}</select></label>
      <label className="text-sm font-medium text-text-primary">Trạng thái<select value={cinemaStatus} onChange={(event) => setCinemaFilter('status', event.target.value)} className="control mt-1"><option value="">Tất cả trạng thái</option><option value="ACTIVE">Hoạt động</option><option value="INACTIVE">Ngừng hoạt động</option></select></label>
    </div>
    <section className="mt-6 grid gap-4 sm:grid-cols-2">{filteredCinemas.map((cinema) => <article key={cinema.id} className="rounded-xl border border-border bg-surface p-5"><p className="text-xs font-semibold text-primary">{cinema.city}</p><h2 className="mt-2 text-lg font-semibold">{cinema.name}</h2><p className="mt-2 text-sm text-text-secondary">{cinema.address}</p><p className="mt-3 text-sm">{cinema.auditoriumCount} phòng · {cinema.status}</p><div className="mt-4 flex gap-3"><Link className="secondary-button" to={`/admin/cinemas/${cinema.id}/auditoriums`}>Xem phòng</Link><button onClick={() => void deactivateCinema(cinema.id).then(load, (reason) => setError(message(reason)))} disabled={busy || cinema.status === 'INACTIVE'} className="min-h-11 text-sm font-semibold text-primary disabled:opacity-50">Ngừng hoạt động</button></div></article>)}{cinemas.length === 0 && <p className="text-text-secondary">Chưa có chi nhánh trong phạm vi của bạn.</p>}{cinemas.length > 0 && filteredCinemas.length === 0 && <p className="text-text-secondary">Không có dữ liệu khớp với điều kiện tìm kiếm.</p>}</section>
    {createOpen && <AdminDialog title="Thêm rạp" description="Chỉ Super Admin có thể tạo chi nhánh mới." onClose={() => !busy && setCreateOpen(false)}><Notice error={error} /><form onSubmit={submit} className="space-y-3"><label className="block text-sm font-medium">Tên rạp<input required name="name" className="control mt-1" /></label><label className="block text-sm font-medium">Địa chỉ<input required name="address" className="control mt-1" /></label><label className="block text-sm font-medium">Thành phố<input required name="city" className="control mt-1" /></label><label className="block text-sm font-medium">Múi giờ<input required name="timezone" defaultValue="Asia/Ho_Chi_Minh" className="control mt-1" /></label><button disabled={busy} className="primary-button w-full">{busy ? 'Đang lưu…' : 'Thêm rạp'}</button></form></AdminDialog>}
  </Page>
}

export function AdminAuditoriumsPage() {
  const { cinemaId = '' } = useParams()
  const [rooms, setRooms] = useState<Auditorium[]>([])
  const [selected, setSelected] = useState<Auditorium | null>(null)
  const [seats, setSeats] = useState<Seat[]>([])
  const [createOpen, setCreateOpen] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const load = useCallback(() => { if (cinemaId) void getAuditoriums(cinemaId).then(setRooms, (reason) => setError(message(reason))) }, [cinemaId])
  useEffect(load, [load])
  const select = async (room: Auditorium) => { setSelected(room); setError(null); try { setSeats(await getSeats(room.id)) } catch (reason) { setError(message(reason)) } }
  const addSeat = (type: Seat['seatType']) => { const row = window.prompt('Hàng ghế (ví dụ A)'); const number = Number(window.prompt('Số ghế đầu tiên')); if (!row || !Number.isInteger(number) || number < 1) return; if (type === 'COUPLE') { const pairKey = `${row}-${number}-${number + 1}`; setSeats((current) => [...current, { rowLabel: row, seatNumber: number, seatType: type, pairKey, status: 'ACTIVE' }, { rowLabel: row, seatNumber: number + 1, seatType: type, pairKey, status: 'ACTIVE' }]) } else setSeats((current) => [...current, { rowLabel: row, seatNumber: number, seatType: type, pairKey: null, status: 'ACTIVE' }]) }
  const create = async (event: FormEvent<HTMLFormElement>) => { event.preventDefault(); const data = new FormData(event.currentTarget); setBusy(true); setError(null); try { const room = await createAuditorium(cinemaId, { name: String(data.get('name')), screenFormat: String(data.get('screenFormat')), cleanupMinutes: Number(data.get('cleanupMinutes')), status: 'ACTIVE' }); setCreateOpen(false); load(); await select(room) } catch (reason) { setError(message(reason)) } finally { setBusy(false) } }
  const save = async () => { if (!selected) return; setBusy(true); try { setSeats(await saveSeats(selected.id, seats)) } catch (reason) { setError(message(reason)) } finally { setBusy(false) } }
  return <Page title="Phòng chiếu & sơ đồ ghế" action={<AddButton onClick={() => setCreateOpen(true)} />}>
    <Link to="/admin/cinemas" className="mt-2 inline-block text-sm font-semibold text-primary">← Rạp</Link><Notice error={error} />
    <div className="mt-6 grid gap-6 xl:grid-cols-[300px_1fr]"><aside className="rounded-xl border border-border bg-surface p-3">{rooms.map((room) => <button key={room.id} onClick={() => void select(room)} className={`block min-h-11 w-full rounded-lg px-3 text-left text-sm ${selected?.id === room.id ? 'bg-primary-soft text-primary' : 'hover:bg-slate-50'}`}>{room.name}<span className="block text-xs text-text-muted">{room.screenFormat} · {room.status}</span></button>)}{!rooms.length && <p className="p-3 text-sm text-text-secondary">Chưa có phòng chiếu.</p>}</aside><section className="rounded-xl border border-border bg-surface p-5">{selected ? <><div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-xl font-semibold">{selected.name}</h2><p className="text-sm text-text-secondary">{selected.screenFormat} · dọn phòng {selected.cleanupMinutes} phút</p></div><button onClick={() => void save()} disabled={busy} className="primary-button">{busy ? 'Đang lưu…' : 'Lưu sơ đồ'}</button></div><div className="mt-8 rounded-lg border border-dashed border-border p-3 text-center text-xs font-semibold tracking-widest text-text-muted">MÀN HÌNH</div><div className="mt-6 grid gap-2">{seats.length ? seats.toSorted((a, b) => a.rowLabel.localeCompare(b.rowLabel) || a.seatNumber - b.seatNumber).map((seat) => <div key={`${seat.rowLabel}-${seat.seatNumber}`} className="flex items-center justify-between rounded-lg border border-border p-3 text-sm"><span>{seat.rowLabel}{seat.seatNumber} · {seat.seatType}{seat.pairKey ? ` · cặp ${seat.pairKey}` : ''}</span><button onClick={() => setSeats((current) => current.filter((item) => seat.pairKey ? item.pairKey !== seat.pairKey : !(item.rowLabel === seat.rowLabel && item.seatNumber === seat.seatNumber)))} className="min-h-11 text-primary">Xóa</button></div>) : <p className="text-sm text-text-secondary">Chưa có ghế.</p>}</div><div className="mt-5 flex flex-wrap gap-3"><button onClick={() => addSeat('STANDARD')} className="secondary-button">Thêm ghế thường</button><button onClick={() => addSeat('VIP')} className="secondary-button">Thêm ghế VIP</button><button onClick={() => addSeat('COUPLE')} className="secondary-button">Thêm ghế đôi</button></div><p className="mt-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-800">Thay đổi layout có thể bị chặn khi phòng đã có suất chiếu. Ghế đôi luôn được thêm theo đúng hai ghế.</p></> : <p className="text-text-secondary">Chọn một phòng để chỉnh sửa sơ đồ ghế.</p>}</section></div>
    {createOpen && <AdminDialog title="Thêm phòng" onClose={() => !busy && setCreateOpen(false)}><Notice error={error} /><form onSubmit={create} className="space-y-3"><label className="block text-sm font-medium">Tên phòng<input required name="name" placeholder="Phòng 1" className="control mt-1" /></label><label className="block text-sm font-medium">Định dạng<input required name="screenFormat" defaultValue="2D" className="control mt-1" /></label><label className="block text-sm font-medium">Thời gian dọn phòng (phút)<input required name="cleanupMinutes" type="number" min="0" defaultValue="15" className="control mt-1" /></label><button disabled={busy} className="primary-button w-full">{busy ? 'Đang lưu…' : 'Thêm phòng'}</button></form></AdminDialog>}
  </Page>
}
