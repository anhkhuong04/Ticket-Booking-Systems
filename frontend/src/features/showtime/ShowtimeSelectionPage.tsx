import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { getCinemas, getMovie, type Cinema, type MovieDetail } from '../catalog/catalogApi'
import { getOpenShowtimes, type Showtime } from './showtimeApi'

type LoadState<T> = { kind: 'loading' } | { kind: 'loaded'; data: T } | { kind: 'error' }

const vietnamZone = 'Asia/Ho_Chi_Minh'
const isoDateFormatter = new Intl.DateTimeFormat('sv-SE', { timeZone: vietnamZone })
const dateFormatter = new Intl.DateTimeFormat('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit', timeZone: vietnamZone })
const timeFormatter = new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: vietnamZone })
const priceFormatter = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })

function vietnamDate(offset = 0) {
  const date = new Date()
  date.setUTCDate(date.getUTCDate() + offset)
  return isoDateFormatter.format(date)
}

function dateChoices() {
  return Array.from({ length: 7 }, (_, index) => vietnamDate(index))
}

function formatFromPrice(showtime: Showtime) {
  const values = Object.values(showtime.prices)
  return values.length ? `Từ ${priceFormatter.format(Math.min(...values))}` : 'Đang cập nhật giá'
}

export function ShowtimeSelectionPage() {
  const { movieId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const date = searchParams.get('date') || vietnamDate()
  const cinemaId = searchParams.get('cinemaId') || ''
  const [movie, setMovie] = useState<LoadState<MovieDetail>>({ kind: 'loading' })
  const [cinemas, setCinemas] = useState<LoadState<Cinema[]>>({ kind: 'loading' })
  const [showtimes, setShowtimes] = useState<LoadState<Showtime[]>>({ kind: 'loading' })

  const load = useCallback(() => {
    void getOpenShowtimes(movieId, date, cinemaId || undefined)
      .then((data) => setShowtimes({ kind: 'loaded', data }), () => setShowtimes({ kind: 'error' }))
  }, [cinemaId, date, movieId])

  useEffect(load, [load])
  useEffect(() => {
    void getMovie(movieId).then((data) => setMovie({ kind: 'loaded', data }), () => setMovie({ kind: 'error' }))
    void getCinemas().then((data) => setCinemas({ kind: 'loaded', data }), () => setCinemas({ kind: 'error' }))
  }, [movieId])

  const groups = useMemo(() => {
    if (showtimes.kind !== 'loaded') return []
    return Object.values(showtimes.data.reduce<Record<string, { cinemaName: string; cinemaAddress: string; formats: Record<string, Showtime[]> }>>((result, showtime) => {
      const group = result[showtime.cinemaId] ?? { cinemaName: showtime.cinemaName, cinemaAddress: showtime.cinemaAddress, formats: {} }
      group.formats[showtime.screenFormat] = [...(group.formats[showtime.screenFormat] ?? []), showtime]
      result[showtime.cinemaId] = group
      return result
    }, {}))
  }, [showtimes])

  const updateQuery = (key: 'date' | 'cinemaId', value: string) => {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    setSearchParams(next)
  }

  const choices = dateChoices()
  return <main className="min-h-screen bg-background py-8 sm:py-12"><div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
    <Link to={`/movies/${movieId}`} className="text-sm font-semibold text-primary hover:underline">← Chi tiết phim</Link>
    <h1 className="mt-4 text-3xl font-bold text-text-primary">Chọn suất chiếu</h1>
    <p className="mt-2 text-text-secondary">{movie.kind === 'loaded' ? movie.data.title : 'Đang tải thông tin phim…'} · Giờ Việt Nam (GMT+7)</p>

    <section className="mt-8" aria-label="Chọn ngày"><h2 className="text-lg font-semibold">Ngày xem</h2><div className="mt-3 flex gap-2 overflow-x-auto pb-2">{choices.map((value) => <button key={value} onClick={() => updateQuery('date', value)} className={`min-h-11 shrink-0 rounded-lg border px-4 text-sm font-semibold ${date === value ? 'border-primary bg-primary text-white' : 'border-border bg-surface text-text-secondary hover:bg-primary-soft'}`}>{dateFormatter.format(new Date(`${value}T12:00:00+07:00`))}</button>)}</div></section>
    <section className="mt-7" aria-label="Chọn rạp"><label className="text-lg font-semibold" htmlFor="cinema">Rạp</label><select id="cinema" value={cinemaId} onChange={(event) => updateQuery('cinemaId', event.target.value)} className="mt-3 min-h-11 w-full rounded-lg border border-border bg-surface px-3 sm:max-w-md"><option value="">Tất cả rạp</option>{cinemas.kind === 'loaded' && cinemas.data.map((cinema) => <option key={cinema.id} value={cinema.id}>{cinema.name} · {cinema.address}</option>)}</select></section>

    <section className="mt-9" aria-live="polite" aria-busy={showtimes.kind === 'loading'}>
      {showtimes.kind === 'loading' && <div className="space-y-4">{[1, 2].map((item) => <div key={item} className="h-36 animate-pulse rounded-xl bg-slate-200" />)}</div>}
      {showtimes.kind === 'error' && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-5 text-red-700">Không thể tải lịch chiếu. <button className="font-semibold underline" onClick={load}>Thử lại</button></div>}
      {showtimes.kind === 'loaded' && !groups.length && <div className="rounded-xl border border-border bg-surface p-7 text-center"><h2 className="font-semibold">Chưa có suất chiếu đang mở bán</h2><p className="mt-2 text-sm text-text-secondary">Hãy chọn ngày hoặc rạp khác.</p></div>}
      {groups.map((group) => <article key={group.cinemaName} className="mb-5 rounded-xl border border-border bg-surface p-5"><h2 className="text-xl font-semibold text-text-primary">{group.cinemaName}</h2><p className="mt-1 text-sm text-text-secondary">{group.cinemaAddress}</p>{Object.entries(group.formats).map(([format, items]) => <div key={format} className="mt-5"><h3 className="text-sm font-semibold text-text-secondary">{format}</h3><div className="mt-3 flex flex-wrap gap-3">{items.map((showtime) => <Link key={showtime.id} to={`/showtimes/${showtime.id}/seats`} className="min-w-28 rounded-lg border border-primary bg-primary-soft px-4 py-3 text-center hover:bg-rose-100 focus-visible:outline-none"><span className="block font-bold text-primary">{timeFormatter.format(new Date(showtime.startAt))}</span><span className="mt-1 block text-xs text-text-secondary">{formatFromPrice(showtime)}</span></Link>)}</div></div>)}</article>)}
    </section>
  </div></main>
}
