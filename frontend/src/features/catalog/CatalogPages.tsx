import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { getCinemas, getGenres, getMovie, getMovies, type Cinema, type Genre, type Movie, type MovieDetail } from './catalogApi'
import { QuickBooking } from '../showtime/QuickBooking'
import { ProfileAgeAdvisory } from '../account/ageAdvisory'

type LoadState<T> = { kind: 'loading' } | { kind: 'loaded'; data: T } | { kind: 'error' }

const statusLabel = { NOW_SHOWING: 'Đang chiếu', COMING_SOON: 'Sắp chiếu' } as const

function MoviePoster({ movie, priority = false }: { movie: Movie; priority?: boolean }) {
  if (!movie.posterUrl) {
    return <div className="grid aspect-[2/3] place-items-center bg-slate-200 p-4 text-center text-sm font-semibold text-text-secondary">{movie.title}</div>
  }
  return <img className="aspect-[2/3] w-full object-cover" src={movie.posterUrl} alt={`Poster phim ${movie.title}`} loading={priority ? 'eager' : 'lazy'} />
}

function FeaturedHero({ movie }: { movie: Movie }) {
  return <section className="relative isolate min-h-[360px] overflow-hidden bg-slate-900 sm:min-h-[430px]">
    {movie.posterUrl && <img className="absolute inset-0 size-full object-cover object-center" src={movie.posterUrl} alt="" aria-hidden="true" loading="eager" />}
    <div className="absolute inset-0 bg-gradient-to-r from-white via-white/90 to-slate-900/25 sm:via-white/80" />
    <div className="absolute inset-0 bg-gradient-to-t from-slate-950/30 via-transparent to-transparent" />
    <div className="relative mx-auto flex min-h-[360px] max-w-7xl items-center px-4 pb-24 pt-12 sm:min-h-[430px] sm:px-6 sm:pb-32 lg:px-8">
      <div className="max-w-xl"><p className="text-sm font-semibold uppercase tracking-[0.14em] text-primary">Phim nổi bật tuần này</p><h1 className="mt-3 text-4xl font-bold leading-[1.08] text-text-primary sm:text-5xl">{movie.title}</h1><p className="mt-4 text-sm font-medium text-text-secondary sm:text-base">{movie.ageRating} · {movie.genres.join(' · ')} · {movie.durationMinutes} phút</p><Link className="mt-7 inline-flex min-h-11 items-center rounded-lg bg-primary px-5 font-semibold text-white shadow-sm hover:bg-primary-hover" to={`/movies/${movie.id}`}>Đặt vé ngay</Link></div>
    </div>
  </section>
}

function MovieCard({ movie }: { movie: Movie }) {
  return <article className="overflow-hidden rounded-xl border border-border bg-surface transition-shadow hover:shadow-md">
    <Link to={`/movies/${movie.id}`} className="block focus-visible:outline-none">
      <MoviePoster movie={movie} />
      <div className="p-4">
        <div className="flex items-center justify-between gap-2"><span className="rounded-full bg-primary-soft px-2 py-1 text-xs font-semibold text-primary">{movie.ageRating}</span><span className="text-xs text-text-muted">{movie.durationMinutes} phút</span></div>
        <h3 className="mt-3 line-clamp-2 text-base font-semibold text-text-primary">{movie.title}</h3>
        <p className="mt-2 line-clamp-1 text-sm text-text-secondary">{movie.genres.join(' · ') || 'Đang cập nhật thể loại'}</p>
      </div>
    </Link>
  </article>
}

function MovieSkeletons({ count = 4 }: { count?: number }) {
  return <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">{Array.from({ length: count }, (_, index) => <div key={index} className="animate-pulse overflow-hidden rounded-xl border border-border bg-surface"><div className="aspect-[2/3] bg-slate-200" /><div className="space-y-3 p-4"><div className="h-4 w-2/5 rounded bg-slate-200" /><div className="h-5 rounded bg-slate-200" /><div className="h-4 w-3/4 rounded bg-slate-200" /></div></div>)}</div>
}

function ErrorBlock({ retry }: { retry: () => void }) {
  return <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-5 text-center text-sm text-red-700">Không thể tải dữ liệu. <button className="font-semibold underline" onClick={retry}>Thử lại</button></div>
}

function Section({ title, to, children }: { title: string; to: string; children: ReactNode }) {
  return <section className="mt-12"><div className="mb-5 flex items-center justify-between gap-4"><h2 className="text-2xl font-semibold text-text-primary">{title}</h2><Link className="text-sm font-semibold text-primary hover:underline" to={to}>Xem tất cả</Link></div>{children}</section>
}

export function HomePage() {
  const [nowShowing, setNowShowing] = useState<LoadState<Movie[]>>({ kind: 'loading' })
  const [comingSoon, setComingSoon] = useState<LoadState<Movie[]>>({ kind: 'loading' })
  const [cinemas, setCinemas] = useState<LoadState<Cinema[]>>({ kind: 'loading' })
  const load = useCallback(() => {
    setNowShowing({ kind: 'loading' }); setComingSoon({ kind: 'loading' }); setCinemas({ kind: 'loading' })
    void getMovies({ status: 'NOW_SHOWING', size: 4 }).then((page) => setNowShowing({ kind: 'loaded', data: page.content }), () => setNowShowing({ kind: 'error' }))
    void getMovies({ status: 'COMING_SOON', size: 4 }).then((page) => setComingSoon({ kind: 'loaded', data: page.content }), () => setComingSoon({ kind: 'error' }))
    void getCinemas().then((value) => setCinemas({ kind: 'loaded', data: value }), () => setCinemas({ kind: 'error' }))
  }, [])
  useEffect(load, [load])
  const featured = nowShowing.kind === 'loaded' ? nowShowing.data[0] : null
  return <main className="bg-background pb-16">
    {featured && <FeaturedHero movie={featured} />}
    {nowShowing.kind === 'loading' && <div className="h-[360px] animate-pulse bg-slate-200 sm:h-[430px]" />}
    <QuickBooking overlapHero={Boolean(featured)} /><div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
    <Section title="Phim đang chiếu" to="/movies?status=NOW_SHOWING">{nowShowing.kind === 'loading' ? <MovieSkeletons /> : nowShowing.kind === 'error' ? <ErrorBlock retry={load} /> : nowShowing.data.length ? <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">{nowShowing.data.map((movie) => <MovieCard key={movie.id} movie={movie} />)}</div> : <p className="rounded-xl border border-border bg-surface p-5 text-text-secondary">Chưa có phim đang chiếu.</p>}</Section>
    <Section title="Phim sắp chiếu" to="/movies?status=COMING_SOON">{comingSoon.kind === 'loading' ? <MovieSkeletons /> : comingSoon.kind === 'error' ? <ErrorBlock retry={load} /> : comingSoon.data.length ? <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">{comingSoon.data.map((movie) => <MovieCard key={movie.id} movie={movie} />)}</div> : <p className="rounded-xl border border-border bg-surface p-5 text-text-secondary">Chưa có phim sắp chiếu.</p>}</Section>
    <Section title="Rạp LAK" to="/cinemas">{cinemas.kind === 'loading' ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{[1, 2, 3].map((item) => <div className="h-36 animate-pulse rounded-xl bg-slate-200" key={item} />)}</div> : cinemas.kind === 'error' ? <ErrorBlock retry={load} /> : cinemas.data.length ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{cinemas.data.slice(0, 3).map((cinema) => <CinemaCard key={cinema.id} cinema={cinema} />)}</div> : <p className="rounded-xl border border-border bg-surface p-5 text-text-secondary">Chưa có chi nhánh đang hoạt động.</p>}</Section>
  </div></main>
}

export function MoviesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [movies, setMovies] = useState<LoadState<Movie[]>>({ kind: 'loading' })
  const [genres, setGenres] = useState<Genre[]>([])
  const status = searchParams.get('status') === 'COMING_SOON' ? 'COMING_SOON' : 'NOW_SHOWING'
  const query = searchParams.get('q') ?? ''
  const genre = searchParams.get('genre') ?? ''
  const load = useCallback(() => { setMovies({ kind: 'loading' }); void getMovies({ status, q: query || undefined, genre: genre || undefined }).then((page) => setMovies({ kind: 'loaded', data: page.content }), () => setMovies({ kind: 'error' })) }, [status, query, genre])
  useEffect(load, [load]); useEffect(() => { void getGenres().then(setGenres, () => setGenres([])) }, [])
  const update = (values: Record<string, string>) => { const next = new URLSearchParams(searchParams); Object.entries(values).forEach(([key, value]) => value ? next.set(key, value) : next.delete(key)); setSearchParams(next) }
  return <main className="min-h-screen bg-background py-8 sm:py-12"><div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8"><h1 className="text-3xl font-bold text-text-primary">Phim</h1><div className="mt-6 flex gap-2 border-b border-border"><button onClick={() => update({ status: 'NOW_SHOWING' })} className={`min-h-11 border-b-2 px-4 text-sm font-semibold ${status === 'NOW_SHOWING' ? 'border-primary text-primary' : 'border-transparent text-text-secondary'}`}>Đang chiếu</button><button onClick={() => update({ status: 'COMING_SOON' })} className={`min-h-11 border-b-2 px-4 text-sm font-semibold ${status === 'COMING_SOON' ? 'border-primary text-primary' : 'border-transparent text-text-secondary'}`}>Sắp chiếu</button></div><form className="mt-6 grid gap-3 sm:grid-cols-[1fr_220px_auto]" onSubmit={(event) => { event.preventDefault(); const data = new FormData(event.currentTarget); update({ q: String(data.get('q') ?? ''), genre: String(data.get('genre') ?? '') }) }}><input name="q" defaultValue={query} className="min-h-11 rounded-lg border border-border bg-surface px-3" placeholder="Tìm theo tên phim" aria-label="Tìm theo tên phim" /><select name="genre" defaultValue={genre} className="min-h-11 rounded-lg border border-border bg-surface px-3" aria-label="Thể loại"><option value="">Tất cả thể loại</option>{genres.map((item) => <option key={item.id} value={item.slug}>{item.name}</option>)}</select><button className="min-h-11 rounded-lg bg-primary px-5 font-semibold text-white hover:bg-primary-hover">Áp dụng</button></form><section className="mt-8" aria-live="polite" aria-busy={movies.kind === 'loading'}>{movies.kind === 'loading' ? <MovieSkeletons count={8} /> : movies.kind === 'error' ? <ErrorBlock retry={load} /> : movies.data.length ? <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">{movies.data.map((movie) => <MovieCard key={movie.id} movie={movie} />)}</div> : <div className="rounded-xl border border-border bg-surface p-8 text-center"><h2 className="font-semibold">Không tìm thấy phim phù hợp</h2><button className="mt-3 text-sm font-semibold text-primary underline" onClick={() => setSearchParams({ status })}>Xóa bộ lọc</button></div>}</section></div></main>
}

export function MovieDetailPage() {
  const { movieId = '' } = useParams(); const [state, setState] = useState<LoadState<MovieDetail>>({ kind: 'loading' })
  const load = useCallback(() => { setState({ kind: 'loading' }); void getMovie(movieId).then((data) => setState({ kind: 'loaded', data }), () => setState({ kind: 'error' })) }, [movieId]); useEffect(load, [load])
  if (state.kind === 'loading') return <main className="min-h-screen bg-background p-6"><div className="mx-auto h-96 max-w-5xl animate-pulse rounded-2xl bg-slate-200" /></main>
  if (state.kind === 'error') return <main className="min-h-screen bg-background p-6"><div className="mx-auto max-w-md"><ErrorBlock retry={load} /></div></main>
  const movie = state.data
  return <main className="min-h-screen bg-background py-8 sm:py-12"><article className="mx-auto grid max-w-5xl gap-8 px-4 sm:grid-cols-[280px_1fr] sm:px-6 lg:px-8"><div className="overflow-hidden rounded-xl border border-border bg-surface"><MoviePoster movie={movie} priority /></div><div><Link to="/movies" className="text-sm font-semibold text-primary hover:underline">← Tất cả phim</Link><div className="mt-4 flex flex-wrap gap-2"><span className="rounded-full bg-primary-soft px-3 py-1 text-sm font-semibold text-primary">{movie.ageRating}</span><span className="rounded-full bg-slate-100 px-3 py-1 text-sm text-text-secondary">{statusLabel[movie.status]}</span></div><h1 className="mt-4 text-3xl font-bold text-text-primary sm:text-4xl">{movie.title}</h1><p className="mt-3 text-text-secondary">{movie.genres.join(' · ') || 'Đang cập nhật thể loại'} · {movie.durationMinutes} phút · Khởi chiếu {new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(new Date(`${movie.releaseDate}T00:00:00`))}</p><div className="mt-5"><ProfileAgeAdvisory rating={movie.ageRating} /></div><p className="mt-6 whitespace-pre-line leading-7 text-text-secondary">{movie.description || 'Thông tin phim đang được cập nhật.'}</p><div className="mt-8 flex flex-wrap gap-3"><Link className="inline-flex min-h-11 items-center rounded-lg bg-primary px-5 font-semibold text-white hover:bg-primary-hover" to={`/movies/${movie.id}/showtimes`}>Chọn suất chiếu</Link>{movie.trailerUrl && <a className="inline-flex min-h-11 items-center rounded-lg border border-primary px-5 font-semibold text-primary hover:bg-primary-soft" href={movie.trailerUrl} target="_blank" rel="noreferrer">Xem trailer</a>}</div></div></article></main>
}

function CinemaCard({ cinema }: { cinema: Cinema }) { return <article className="rounded-xl border border-border bg-surface p-5"><p className="text-xs font-semibold uppercase tracking-wide text-primary">{cinema.city}</p><h3 className="mt-2 text-lg font-semibold text-text-primary">{cinema.name}</h3><p className="mt-2 text-sm leading-6 text-text-secondary">{cinema.address}</p><p className="mt-3 text-xs text-text-muted">{cinema.timezone}</p></article> }

export function CinemasPage() { const [state, setState] = useState<LoadState<Cinema[]>>({ kind: 'loading' }); const load = useCallback(() => { setState({ kind: 'loading' }); void getCinemas().then((data) => setState({ kind: 'loaded', data }), () => setState({ kind: 'error' })) }, []); useEffect(load, [load]); return <main className="min-h-screen bg-background py-8 sm:py-12"><div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8"><h1 className="text-3xl font-bold text-text-primary">Rạp LAK</h1><p className="mt-2 text-text-secondary">Chọn chi nhánh thuận tiện để xem lịch chiếu.</p><section className="mt-8">{state.kind === 'loading' ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{[1, 2, 3].map((item) => <div className="h-36 animate-pulse rounded-xl bg-slate-200" key={item} />)}</div> : state.kind === 'error' ? <ErrorBlock retry={load} /> : state.data.length ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{state.data.map((cinema) => <CinemaCard cinema={cinema} key={cinema.id} />)}</div> : <p className="rounded-xl border border-border bg-surface p-6 text-text-secondary">Chưa có chi nhánh đang hoạt động.</p>}</section></div></main> }
