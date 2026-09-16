import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getOpenShowtimes, getShowtimeAvailability, type Showtime, type ShowtimeAvailability } from './showtimeApi'

const zone = 'Asia/Ho_Chi_Minh'
const dateLabel = new Intl.DateTimeFormat('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit', timeZone: zone })
const timeLabel = new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: zone })
const priceLabel = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })

export function MovieShowtimes({ movieId }: { movieId: string }) {
  const [params, setParams] = useSearchParams()
  const cinemaId = params.get('cinemaId') ?? ''
  const requestedDate = params.get('date') ?? ''
  const [availability, setAvailability] = useState<ShowtimeAvailability | null>(null)
  const [availabilityError, setAvailabilityError] = useState(false)
  const [availabilityRetry, setAvailabilityRetry] = useState(0)
  const [showtimes, setShowtimes] = useState<Showtime[] | null>(null)
  const [showtimesError, setShowtimesError] = useState(false)
  const [showtimesRetry, setShowtimesRetry] = useState(0)

  useEffect(() => {
    let current = true
    getShowtimeAvailability(movieId).then((data) => {
      if (current) { setAvailability(data); setAvailabilityError(false) }
    }, () => { if (current) setAvailabilityError(true) })
    return () => { current = false }
  }, [movieId, availabilityRetry])

  const cinemas = availability?.cinemas ?? []
  const dates = [...new Set(cinemas.filter((cinema) => !cinemaId || cinema.cinemaId === cinemaId).flatMap((cinema) => cinema.dates))].sort()
  const date = dates.includes(requestedDate) ? requestedDate : dates[0] ?? ''

  useEffect(() => {
    if (!availability || !date) return
    let current = true
    getOpenShowtimes(movieId, date, cinemaId || undefined).then((data) => {
      if (current) { setShowtimes(data); setShowtimesError(false) }
    }, () => { if (current) setShowtimesError(true) })
    return () => { current = false }
  }, [availability, movieId, date, cinemaId, showtimesRetry])

  const update = (key: 'date' | 'cinemaId', value: string) => {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    if (key === 'cinemaId') next.delete('date')
    setShowtimes(null)
    setParams(next, { replace: true })
  }

  const groups = useMemo(() => {
    const result = new Map<string, { name: string; address: string; formats: Map<string, Showtime[]> }>()
    for (const item of showtimes ?? []) {
      const group = result.get(item.cinemaId) ?? { name: item.cinemaName, address: item.cinemaAddress, formats: new Map<string, Showtime[]>() }
      group.formats.set(item.screenFormat, [...(group.formats.get(item.screenFormat) ?? []), item])
      result.set(item.cinemaId, group)
    }
    return [...result.values()]
  }, [showtimes])

  return <section id="lich-chieu" aria-labelledby="showtime-heading" className="mx-auto mt-12 max-w-5xl scroll-mt-24 px-4 pb-12 sm:px-6 lg:px-8">
    <h2 id="showtime-heading" className="text-2xl font-bold">Lịch chiếu</h2>
    <p className="mt-1 text-sm text-text-secondary">Các suất đang mở bán · Giờ Việt Nam (GMT+7). Giá hiển thị là giá thấp nhất, tùy loại ghế.</p>
    {availabilityError ? <p role="alert" className="mt-5 rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">Không thể tải lịch chiếu. <button className="font-semibold underline" onClick={() => setAvailabilityRetry((value) => value + 1)}>Thử lại</button></p>
      : !availability ? <p className="mt-5 text-text-secondary" role="status">Đang tải lịch chiếu…</p>
        : <>
          <div className="mt-6 grid gap-4 sm:grid-cols-[minmax(0,1fr)_220px]">
            <label className="text-sm font-semibold">Rạp<select value={cinemaId} onChange={(event) => update('cinemaId', event.target.value)} className="control mt-2"><option value="">Tất cả rạp</option>{cinemas.map((cinema) => <option key={cinema.cinemaId} value={cinema.cinemaId}>{cinema.cinemaName}</option>)}</select></label>
            <label className="text-sm font-semibold">Ngày xem<select value={date} onChange={(event) => update('date', event.target.value)} disabled={!dates.length} className="control mt-2"><option value="">{dates.length ? 'Chọn ngày' : 'Chưa có ngày'}</option>{dates.map((value) => <option key={value} value={value}>{dateLabel.format(new Date(`${value}T12:00:00+07:00`))}</option>)}</select></label>
          </div>
          <div className="mt-6" aria-live="polite">
            {!date ? <p className="rounded-xl border border-border bg-surface p-6 text-text-secondary">Chưa có suất chiếu đang mở bán.</p>
              : showtimesError ? <p role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">Không thể tải suất chiếu. <button className="font-semibold underline" onClick={() => setShowtimesRetry((value) => value + 1)}>Thử lại</button></p>
                : !showtimes ? <p role="status" className="text-text-secondary">Đang tải suất chiếu…</p>
                  : !groups.length ? <p className="rounded-xl border border-border bg-surface p-6 text-text-secondary">Không còn suất mở bán cho ngày và rạp đã chọn.</p>
                    : groups.map((group) => <article key={group.name} className="mb-4 rounded-xl border border-border bg-surface p-5"><h3 className="text-lg font-semibold">{group.name}</h3><p className="text-sm text-text-secondary">{group.address}</p>{[...group.formats].map(([format, items]) => <div key={format} className="mt-5"><h4 className="text-sm font-semibold text-text-secondary">{format}</h4><div className="mt-2 flex flex-wrap gap-3">{items.map((showtime) => <Link key={showtime.id} to={`/showtimes/${showtime.id}/seats`} className="min-w-28 rounded-lg border border-primary bg-primary-soft px-4 py-3 text-center hover:bg-rose-100"><span className="block font-bold text-primary">{timeLabel.format(new Date(showtime.startAt))}</span><span className="mt-1 block text-xs text-text-secondary">{Object.values(showtime.prices).length ? `Từ ${priceLabel.format(Math.min(...Object.values(showtime.prices)))}` : 'Đang cập nhật giá'}</span></Link>)}</div></div>)}</article>)}
          </div>
        </>}
  </section>
}
