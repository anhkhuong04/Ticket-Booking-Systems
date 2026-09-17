import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { displayLocale } from '../../i18n'
import { getCinemas, getGenres, getMovie, getMovies, type Cinema, type Genre, type Movie, type MovieDetail } from './catalogApi'
import { QuickBooking } from '../showtime/QuickBooking'
import { ProfileAgeAdvisory } from '../account/ageAdvisory'
import { MovieShowtimes } from '../showtime/MovieShowtimes'

type LoadState<T> = { kind: 'loading' } | { kind: 'loaded'; data: T } | { kind: 'error' }

function MoviePoster({ movie, priority = false }: { movie: Movie; priority?: boolean }) {
  const { t } = useTranslation()
  if (!movie.posterUrl) {
    return <div className="grid aspect-[2/3] place-items-center bg-slate-200 p-4 text-center text-sm font-semibold text-text-secondary">{movie.title}</div>
  }
  return <img className="aspect-[2/3] w-full object-cover" src={movie.posterUrl} alt={`${t('movies')} ${movie.title}`} loading={priority ? 'eager' : 'lazy'} />
}

function FeaturedHero({ movie }: { movie: Movie }) {
  const { t } = useTranslation()
  return <section className="relative isolate min-h-[360px] overflow-hidden bg-slate-900 sm:min-h-[430px]">
    {movie.posterUrl && <img className="absolute inset-0 size-full object-cover object-center" src={movie.posterUrl} alt="" aria-hidden="true" loading="eager" />}
    <div className="absolute inset-0 bg-gradient-to-r from-white via-white/90 to-slate-900/25 sm:via-white/80" />
    <div className="absolute inset-0 bg-gradient-to-t from-slate-950/30 via-transparent to-transparent" />
    <div className="relative mx-auto flex min-h-[360px] max-w-7xl items-center px-4 pb-24 pt-12 sm:min-h-[430px] sm:px-6 sm:pb-32 lg:px-8">
      <div className="max-w-xl"><p className="text-sm font-semibold uppercase tracking-[0.14em] text-primary">{t('featured')}</p><h1 className="mt-3 text-4xl font-bold leading-[1.08] text-text-primary sm:text-5xl">{movie.title}</h1><p className="mt-4 text-sm font-medium text-text-secondary sm:text-base">{movie.ageRating} · {movie.genres.join(' · ')} · {t('minutes', { count: movie.durationMinutes })}</p><Link className="mt-7 inline-flex min-h-11 items-center rounded-lg bg-primary px-5 font-semibold text-white shadow-sm hover:bg-primary-hover" to={`/movies/${movie.id}`}>{t('bookNow')}</Link></div>
    </div>
  </section>
}

function MovieCard({ movie }: { movie: Movie }) {
  const { t } = useTranslation()
  return <article className="overflow-hidden rounded-xl border border-border bg-surface transition-shadow hover:shadow-md">
    <Link to={`/movies/${movie.id}`} className="block focus-visible:outline-none">
      <MoviePoster movie={movie} />
      <div className="p-4">
        <div className="flex items-center justify-between gap-2"><span className="rounded-full bg-primary-soft px-2 py-1 text-xs font-semibold text-primary">{movie.ageRating}</span><span className="text-xs text-text-muted">{t('minutes', { count: movie.durationMinutes })}</span></div>
        <h3 className="mt-3 line-clamp-2 text-base font-semibold text-text-primary">{movie.title}</h3>
        <p className="mt-2 line-clamp-1 text-sm text-text-secondary">{movie.genres.join(' · ') || t('genresPending')}</p>
      </div>
    </Link>
  </article>
}

function MovieSkeletons({ count = 4 }: { count?: number }) {
  return <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">{Array.from({ length: count }, (_, index) => <div key={index} className="animate-pulse overflow-hidden rounded-xl border border-border bg-surface"><div className="aspect-[2/3] bg-slate-200" /><div className="space-y-3 p-4"><div className="h-4 w-2/5 rounded bg-slate-200" /><div className="h-5 rounded bg-slate-200" /><div className="h-4 w-3/4 rounded bg-slate-200" /></div></div>)}</div>
}

function ErrorBlock({ retry }: { retry: () => void }) {
  const { t } = useTranslation()
  return <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-5 text-center text-sm text-red-700">{t('loadError')} <button className="font-semibold underline" onClick={retry}>{t('retry')}</button></div>
}

function Section({ title, to, children }: { title: string; to: string; children: ReactNode }) {
  const { t } = useTranslation()
  return <section className="mt-12"><div className="mb-5 flex items-center justify-between gap-4"><h2 className="text-2xl font-semibold text-text-primary">{title}</h2><Link className="text-sm font-semibold text-primary hover:underline" to={to}>{t('all')}</Link></div>{children}</section>
}

export function HomePage() {
  const { t } = useTranslation()
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
    <Section title={t('nowShowing')} to="/movies?status=NOW_SHOWING">{nowShowing.kind === 'loading' ? <MovieSkeletons /> : nowShowing.kind === 'error' ? <ErrorBlock retry={load} /> : nowShowing.data.length ? <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">{nowShowing.data.map((movie) => <MovieCard key={movie.id} movie={movie} />)}</div> : <p className="rounded-xl border border-border bg-surface p-5 text-text-secondary">{t('noNowShowing')}</p>}</Section>
    <Section title={t('comingSoon')} to="/movies?status=COMING_SOON">{comingSoon.kind === 'loading' ? <MovieSkeletons /> : comingSoon.kind === 'error' ? <ErrorBlock retry={load} /> : comingSoon.data.length ? <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">{comingSoon.data.map((movie) => <MovieCard key={movie.id} movie={movie} />)}</div> : <p className="rounded-xl border border-border bg-surface p-5 text-text-secondary">{t('noComingSoon')}</p>}</Section>
    <Section title={t('lakCinemas')} to="/cinemas">{cinemas.kind === 'loading' ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{[1, 2, 3].map((item) => <div className="h-36 animate-pulse rounded-xl bg-slate-200" key={item} />)}</div> : cinemas.kind === 'error' ? <ErrorBlock retry={load} /> : cinemas.data.length ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{cinemas.data.slice(0, 3).map((cinema) => <CinemaCard key={cinema.id} cinema={cinema} />)}</div> : <p className="rounded-xl border border-border bg-surface p-5 text-text-secondary">{t('noCinemas')}</p>}</Section>
  </div></main>
}

export function MoviesPage() {
  const { t } = useTranslation()
  const [searchParams, setSearchParams] = useSearchParams()
  const [movies, setMovies] = useState<LoadState<Movie[]>>({ kind: 'loading' })
  const [genres, setGenres] = useState<Genre[]>([])
  const status = searchParams.get('status') === 'COMING_SOON' ? 'COMING_SOON' : 'NOW_SHOWING'
  const query = searchParams.get('q') ?? ''
  const genre = searchParams.get('genre') ?? ''
  const load = useCallback(() => { setMovies({ kind: 'loading' }); void getMovies({ status, q: query || undefined, genre: genre || undefined }).then((page) => setMovies({ kind: 'loaded', data: page.content }), () => setMovies({ kind: 'error' })) }, [status, query, genre])
  useEffect(load, [load]); useEffect(() => { void getGenres().then(setGenres, () => setGenres([])) }, [])
  const update = (values: Record<string, string>) => { const next = new URLSearchParams(searchParams); Object.entries(values).forEach(([key, value]) => value ? next.set(key, value) : next.delete(key)); setSearchParams(next) }
  return <main className="min-h-screen bg-background py-8 sm:py-12"><div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8"><h1 className="text-3xl font-bold text-text-primary">{t('movies')}</h1><div className="mt-6 flex gap-2 border-b border-border"><button onClick={() => update({ status: 'NOW_SHOWING' })} className={`min-h-11 border-b-2 px-4 text-sm font-semibold ${status === 'NOW_SHOWING' ? 'border-primary text-primary' : 'border-transparent text-text-secondary'}`}>{t('nowShowing')}</button><button onClick={() => update({ status: 'COMING_SOON' })} className={`min-h-11 border-b-2 px-4 text-sm font-semibold ${status === 'COMING_SOON' ? 'border-primary text-primary' : 'border-transparent text-text-secondary'}`}>{t('comingSoon')}</button></div><form className="mt-6 grid gap-3 sm:grid-cols-[1fr_220px_auto]" onSubmit={(event) => { event.preventDefault(); const data = new FormData(event.currentTarget); update({ q: String(data.get('q') ?? ''), genre: String(data.get('genre') ?? '') }) }}><input name="q" defaultValue={query} className="min-h-11 rounded-lg border border-border bg-surface px-3" placeholder={t('searchMovie')} aria-label={t('searchMovie')} /><select name="genre" defaultValue={genre} className="min-h-11 rounded-lg border border-border bg-surface px-3" aria-label={t('genre')}><option value="">{t('allGenres')}</option>{genres.map((item) => <option key={item.id} value={item.slug}>{item.name}</option>)}</select><button className="min-h-11 rounded-lg bg-primary px-5 font-semibold text-white hover:bg-primary-hover">{t('apply')}</button></form><section className="mt-8" aria-live="polite" aria-busy={movies.kind === 'loading'}>{movies.kind === 'loading' ? <MovieSkeletons count={8} /> : movies.kind === 'error' ? <ErrorBlock retry={load} /> : movies.data.length ? <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">{movies.data.map((movie) => <MovieCard key={movie.id} movie={movie} />)}</div> : <div className="rounded-xl border border-border bg-surface p-8 text-center"><h2 className="font-semibold">{t('noMovies')}</h2><button className="mt-3 text-sm font-semibold text-primary underline" onClick={() => setSearchParams({ status })}>{t('clearFilters')}</button></div>}</section></div></main>
}

export function MovieDetailPage() {
  const { t, i18n } = useTranslation()
  const { movieId = '' } = useParams(); const [state, setState] = useState<LoadState<MovieDetail>>({ kind: 'loading' })
  const load = useCallback(() => { setState({ kind: 'loading' }); void getMovie(movieId).then((data) => setState({ kind: 'loaded', data }), () => setState({ kind: 'error' })) }, [movieId]); useEffect(load, [load])
  if (state.kind === 'loading') return <main className="min-h-screen bg-background p-6"><div className="mx-auto h-96 max-w-5xl animate-pulse rounded-2xl bg-slate-200" /></main>
  if (state.kind === 'error') return <main className="min-h-screen bg-background p-6"><div className="mx-auto max-w-md"><ErrorBlock retry={load} /></div></main>
  const movie = state.data
  return <main className="min-h-screen bg-background py-8 sm:py-12">
    <article className="mx-auto grid max-w-5xl gap-8 px-4 sm:grid-cols-[280px_1fr] sm:px-6 lg:px-8">
      <div className="overflow-hidden rounded-xl border border-border bg-surface"><MoviePoster movie={movie} priority /></div>
      <div>
        <Link to="/movies" className="text-sm font-semibold text-primary hover:underline">← {t('allMovies')}</Link>
        <div className="mt-4 flex flex-wrap gap-2"><span className="rounded-full bg-primary-soft px-3 py-1 text-sm font-semibold text-primary">{movie.ageRating}</span><span className="rounded-full bg-slate-100 px-3 py-1 text-sm text-text-secondary">{t(movie.status === 'NOW_SHOWING' ? 'nowShowing' : 'comingSoon')}</span></div>
        <h1 className="mt-4 text-3xl font-bold text-text-primary sm:text-4xl">{movie.title}</h1>
        <p className="mt-3 text-text-secondary">{movie.genres.join(' · ') || t('genresPending')} · {t('minutes', { count: movie.durationMinutes })} · {t('releaseDate', { date: new Intl.DateTimeFormat(displayLocale(i18n.language), { dateStyle: 'medium' }).format(new Date(`${movie.releaseDate}T00:00:00`)) })}</p>
        <div className="mt-5"><ProfileAgeAdvisory rating={movie.ageRating} /></div>
        {(movie.country || movie.director || movie.castMembers.length > 0) && <dl className="mt-6 grid gap-2 text-sm sm:grid-cols-[110px_1fr]">
          {movie.country && <><dt className="font-semibold">{t('country')}</dt><dd>{movie.country}</dd></>}
          {movie.director && <><dt className="font-semibold">{t('director')}</dt><dd>{movie.director}</dd></>}
          {movie.castMembers.length > 0 && <><dt className="font-semibold">{t('cast')}</dt><dd>{movie.castMembers.join(', ')}</dd></>}
        </dl>}
        <h2 className="mt-7 text-xl font-semibold">{t('synopsis')}</h2>
        <p className="mt-2 whitespace-pre-line leading-7 text-text-secondary">{movie.description || t('synopsisPending')}</p>
        <div className="mt-8 flex flex-wrap gap-3"><a className="inline-flex min-h-11 items-center rounded-lg bg-primary px-5 font-semibold text-white hover:bg-primary-hover" href="#lich-chieu">{t('chooseShowtime')}</a>{movie.trailerUrl && <a className="inline-flex min-h-11 items-center rounded-lg border border-primary px-5 font-semibold text-primary hover:bg-primary-soft" href={movie.trailerUrl} target="_blank" rel="noreferrer">{t('trailer')}</a>}</div>
      </div>
    </article>
    <MovieShowtimes movieId={movie.id} />
  </main>
}

function CinemaCard({ cinema }: { cinema: Cinema }) { return <article className="rounded-xl border border-border bg-surface p-5"><p className="text-xs font-semibold uppercase tracking-wide text-primary">{cinema.city}</p><h3 className="mt-2 text-lg font-semibold text-text-primary">{cinema.name}</h3><p className="mt-2 text-sm leading-6 text-text-secondary">{cinema.address}</p><p className="mt-3 text-xs text-text-muted">{cinema.timezone}</p></article> }

export function CinemasPage() { const { t } = useTranslation(); const [state, setState] = useState<LoadState<Cinema[]>>({ kind: 'loading' }); const load = useCallback(() => { setState({ kind: 'loading' }); void getCinemas().then((data) => setState({ kind: 'loaded', data }), () => setState({ kind: 'error' })) }, []); useEffect(load, [load]); return <main className="min-h-screen bg-background py-8 sm:py-12"><div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8"><h1 className="text-3xl font-bold text-text-primary">{t('lakCinemas')}</h1><p className="mt-2 text-text-secondary">{t('chooseCinemaHint')}</p><section className="mt-8">{state.kind === 'loading' ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{[1, 2, 3].map((item) => <div className="h-36 animate-pulse rounded-xl bg-slate-200" key={item} />)}</div> : state.kind === 'error' ? <ErrorBlock retry={load} /> : state.data.length ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{state.data.map((cinema) => <CinemaCard cinema={cinema} key={cinema.id} />)}</div> : <p className="rounded-xl border border-border bg-surface p-6 text-text-secondary">{t('noCinemas')}</p>}</section></div></main> }
