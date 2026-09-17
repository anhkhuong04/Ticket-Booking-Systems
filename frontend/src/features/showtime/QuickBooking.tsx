import { CalendarDays, ChevronDown, Clock3, Film, MapPin, Ticket } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMovies, type Movie } from '../catalog/catalogApi'
import { getOpenShowtimes, getShowtimeAvailability, type Showtime, type ShowtimeAvailability } from './showtimeApi'

type LoadState<T> = { kind: 'idle' } | { kind: 'loading' } | { kind: 'loaded'; data: T } | { kind: 'error' }

const vietnamZone = 'Asia/Ho_Chi_Minh'
const dateFormatter = new Intl.DateTimeFormat('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit', timeZone: vietnamZone })
const timeFormatter = new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: vietnamZone })

function formatDate(value: string) {
  return dateFormatter.format(new Date(`${value}T12:00:00+07:00`))
}

function formatShowtime(showtime: Showtime) {
  return `${timeFormatter.format(new Date(showtime.startAt))} · ${showtime.screenFormat}`
}

export function QuickBooking({ overlapHero = false }: { overlapHero?: boolean }) {
  const navigate = useNavigate()
  const [movies, setMovies] = useState<LoadState<Movie[]>>({ kind: 'loading' })
  const [movieRetry, setMovieRetry] = useState(0)
  const [availabilityRetry, setAvailabilityRetry] = useState(0)
  const [showtimeRetry, setShowtimeRetry] = useState(0)
  const [movieId, setMovieId] = useState('')
  const [cinemaId, setCinemaId] = useState('')
  const [date, setDate] = useState('')
  const [showtimeId, setShowtimeId] = useState('')
  const [availability, setAvailability] = useState<LoadState<ShowtimeAvailability>>({ kind: 'idle' })
  const [showtimes, setShowtimes] = useState<LoadState<Showtime[]>>({ kind: 'idle' })

  useEffect(() => {
    let active = true
    void getMovies({ status: 'NOW_SHOWING', size: 100 }).then(
      (page) => { if (active) setMovies({ kind: 'loaded', data: page.content }) },
      () => { if (active) setMovies({ kind: 'error' }) },
    )
    return () => { active = false }
  }, [movieRetry])

  useEffect(() => {
    if (!movieId) return
    let active = true
    void getShowtimeAvailability(movieId).then(
      (data) => { if (active) setAvailability({ kind: 'loaded', data }) },
      () => { if (active) setAvailability({ kind: 'error' }) },
    )
    return () => { active = false }
  }, [availabilityRetry, movieId])

  useEffect(() => {
    if (!movieId || !cinemaId || !date) return
    let active = true
    void getOpenShowtimes(movieId, date, cinemaId).then(
      (data) => { if (active) setShowtimes({ kind: 'loaded', data }) },
      () => { if (active) setShowtimes({ kind: 'error' }) },
    )
    return () => { active = false }
  }, [cinemaId, date, movieId, showtimeRetry])

  const cinemas = availability.kind === 'loaded' ? availability.data.cinemas : []
  const selectedCinema = cinemas.find((cinema) => cinema.cinemaId === cinemaId)
  const dates = selectedCinema?.dates ?? []
  const availableShowtimes = showtimes.kind === 'loaded' ? showtimes.data : []
  const statusMessage = useMemo(() => {
    if (movies.kind === 'error') return 'Không thể tải danh sách phim.'
    if (availability.kind === 'error') return 'Không thể tải rạp và ngày chiếu.'
    if (availability.kind === 'loaded' && movieId && cinemas.length === 0) return 'Phim này chưa có suất chiếu đang mở bán.'
    if (showtimes.kind === 'error') return 'Không thể tải suất chiếu.'
    if (showtimes.kind === 'loaded' && date && availableShowtimes.length === 0) return 'Không còn suất chiếu mở bán trong ngày đã chọn.'
    return ''
  }, [availability.kind, availableShowtimes.length, cinemas.length, date, movieId, movies.kind, showtimes.kind])

  const changeMovie = (value: string) => {
    setMovieId(value)
    setAvailability(value ? { kind: 'loading' } : { kind: 'idle' })
    setCinemaId('')
    setDate('')
    setShowtimeId('')
    setShowtimes({ kind: 'idle' })
  }

  const changeCinema = (value: string) => {
    setCinemaId(value)
    setDate('')
    setShowtimeId('')
    setShowtimes({ kind: 'idle' })
  }

  const changeDate = (value: string) => {
    setDate(value)
    setShowtimeId('')
    setShowtimes(value ? { kind: 'loading' } : { kind: 'idle' })
  }

  return <section className={`relative z-10 mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 ${overlapHero ? '-mt-20 sm:-mt-24' : 'mt-8'}`} aria-labelledby="quick-booking-title">
    <div className="rounded-xl border border-border bg-surface p-5 shadow-lg shadow-slate-900/10 sm:p-6">
      <div className="flex items-start gap-3">
        <span className="grid size-11 shrink-0 place-items-center rounded-lg bg-primary-soft text-primary" aria-hidden="true"><Ticket size={22} /></span>
        <div><h2 id="quick-booking-title" className="text-lg font-semibold text-text-primary">Đặt vé nhanh</h2><p className="mt-1 text-sm text-text-secondary">Chỉ hiển thị rạp, ngày và suất chiếu đang mở bán.</p></div>
      </div>

      <form className="mt-5 grid gap-3 lg:grid-cols-[1.15fr_1.15fr_0.9fr_0.9fr_auto] lg:items-center" onSubmit={(event) => { event.preventDefault(); if (showtimeId) navigate(`/showtimes/${showtimeId}/seats`) }}>
        <label className="relative block"><span className="sr-only">Chọn phim</span><Film className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-primary" size={18} aria-hidden="true" /><select aria-label="Chọn phim" className="control quick-booking-select min-h-12 w-full appearance-none" value={movieId} disabled={movies.kind === 'loading' || movies.kind === 'error'} onChange={(event) => changeMovie(event.target.value)}><option value="">Chọn phim</option>{movies.kind === 'loaded' && movies.data.map((movie) => <option key={movie.id} value={movie.id}>{movie.title}</option>)}</select><ChevronDown className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-text-muted" size={18} aria-hidden="true" /></label>
        <label className="relative block"><span className="sr-only">Chọn rạp</span><MapPin className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-primary" size={18} aria-hidden="true" /><select aria-label="Chọn rạp" className="control quick-booking-select min-h-12 w-full appearance-none" value={cinemaId} disabled={!movieId || availability.kind !== 'loaded' || cinemas.length === 0} onChange={(event) => changeCinema(event.target.value)}><option value="">{availability.kind === 'loading' ? 'Đang tải rạp…' : 'Chọn rạp'}</option>{cinemas.map((cinema) => <option key={cinema.cinemaId} value={cinema.cinemaId}>{cinema.cinemaName}</option>)}</select><ChevronDown className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-text-muted" size={18} aria-hidden="true" /></label>
        <label className="relative block"><span className="sr-only">Chọn ngày</span><CalendarDays className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-primary" size={18} aria-hidden="true" /><select aria-label="Chọn ngày" className="control quick-booking-select min-h-12 w-full appearance-none" value={date} disabled={!cinemaId || dates.length === 0} onChange={(event) => changeDate(event.target.value)}><option value="">Chọn ngày</option>{dates.map((value) => <option key={value} value={value}>{formatDate(value)}</option>)}</select><ChevronDown className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-text-muted" size={18} aria-hidden="true" /></label>
        <label className="relative block"><span className="sr-only">Chọn suất chiếu</span><Clock3 className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-primary" size={18} aria-hidden="true" /><select aria-label="Chọn suất chiếu" className="control quick-booking-select min-h-12 w-full appearance-none" value={showtimeId} disabled={!date || showtimes.kind !== 'loaded' || availableShowtimes.length === 0} onChange={(event) => setShowtimeId(event.target.value)}><option value="">{showtimes.kind === 'loading' ? 'Đang tải suất…' : 'Chọn suất'}</option>{availableShowtimes.map((showtime) => <option key={showtime.id} value={showtime.id}>{formatShowtime(showtime)}</option>)}</select><ChevronDown className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-text-muted" size={18} aria-hidden="true" /></label>
        <button type="submit" disabled={!showtimeId} className="primary-button min-h-12 whitespace-nowrap px-5 disabled:cursor-not-allowed disabled:opacity-50">Mua vé nhanh</button>
      </form>

      <div className="mt-3 min-h-5 text-sm" aria-live="polite">
        {statusMessage && <p className="text-text-secondary">{statusMessage}{movies.kind === 'error' && <button type="button" className="ml-2 font-semibold text-primary underline" onClick={() => { setMovies({ kind: 'loading' }); setMovieRetry((value) => value + 1) }}>Thử lại</button>}{availability.kind === 'error' && <button type="button" className="ml-2 font-semibold text-primary underline" onClick={() => { setAvailability({ kind: 'loading' }); setAvailabilityRetry((value) => value + 1) }}>Thử lại</button>}{showtimes.kind === 'error' && <button type="button" className="ml-2 font-semibold text-primary underline" onClick={() => { setShowtimes({ kind: 'loading' }); setShowtimeRetry((value) => value + 1) }}>Thử lại</button>}</p>}
      </div>
    </div>
  </section>
}
