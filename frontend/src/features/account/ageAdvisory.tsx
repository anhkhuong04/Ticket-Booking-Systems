import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
import { getProfile } from './profileApi'

function ageOn(dateOfBirth: string, at: string): number | null {
  const birth = dateOfBirth.split('-').map(Number)
  const parts = new Intl.DateTimeFormat('en-US', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date(at))
  const date = ['year', 'month', 'day'].map(key => Number(parts.find(part => part.type === key)?.value))
  if (birth.length !== 3 || date.length !== 3 || birth.some(Number.isNaN) || date.some(Number.isNaN)) return null
  return date[0] - birth[0] - (date[1] < birth[1] || date[1] === birth[1] && date[2] < birth[2] ? 1 : 0)
}

export function ProfileAgeAdvisory({ rating, at, compact = false }: { rating: string; at?: string; compact?: boolean }) {
  const { user } = useAuth()
  const [birthDate, setBirthDate] = useState<string | null>(null)
  const [today] = useState(() => new Date().toISOString())
  useEffect(() => { if (!user) return; let active = true; void getProfile().then(profile => { if (active) setBirthDate(profile.birthDate) }, () => {}); return () => { active = false } }, [user])
  return <AgeAdvisory rating={rating} birthDate={birthDate} at={at ?? today} compact={compact} />
}

export function AgeAdvisory({ rating, birthDate, at, compact = false }: { rating: string; birthDate: string | null; at: string; compact?: boolean }) {
  const requiredAge = /^T(\d+)$/.exec(rating)?.[1]
  if (!requiredAge) return null
  const age = birthDate ? ageOn(birthDate, at) : null
  const below = age !== null && age < Number(requiredAge)
  return <aside className={`rounded-xl border p-4 text-sm ${below ? 'border-amber-300 bg-amber-50 text-amber-950' : 'border-border bg-slate-50 text-text-secondary'}`} aria-label="Lưu ý độ tuổi" aria-live="polite">
    <p className="font-semibold">Phim {rating}: dành cho người xem từ {requiredAge} tuổi</p>
    {below ? <p className="mt-1">Theo ngày sinh trong hồ sơ, bạn chưa đủ {requiredAge} tuổi {compact ? 'vào ngày chiếu.' : 'vào thời điểm này.'} Vui lòng kiểm tra độ tuổi của từng người dùng vé trước khi mua.</p>
      : age === null ? <p className="mt-1">Chưa có ngày sinh trong hồ sơ. Vui lòng kiểm tra độ tuổi của từng người xem.</p>
        : <p className="mt-1">Nếu mua hộ, hãy kiểm tra độ tuổi của từng người dùng vé.</p>}
    <p className="mt-1">Thông tin tự khai chỉ hỗ trợ cảnh báo; rạp có thể kiểm tra giấy tờ khi vào xem.</p>
    {!birthDate && <Link className="mt-2 inline-flex min-h-11 items-center font-semibold text-primary underline" to="/me/profile">Bổ sung ngày sinh</Link>}
  </aside>
}
