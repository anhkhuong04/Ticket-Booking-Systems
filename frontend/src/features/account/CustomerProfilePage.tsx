import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { getProfile, updateProfile, type CustomerProfile } from './profileApi'
import { useAuth } from '../auth/AuthProvider'

type State = { kind: 'loading' } | { kind: 'error' } | { kind: 'loaded'; data: CustomerProfile }

const todayInVietnam = () => {
  const parts = new Intl.DateTimeFormat('en-US', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date())
  return ['year', 'month', 'day'].map(key => parts.find(part => part.type === key)?.value).join('-')
}

export function CustomerProfilePage() {
  const { updateDisplayName } = useAuth()
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [retry, setRetry] = useState(0)
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => { let active = true; void getProfile().then(data => { if (active) setState({ kind: 'loaded', data }) }, () => { if (active) setState({ kind: 'error' }) }); return () => { active = false } }, [retry])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving) return
    const fields = new FormData(event.currentTarget)
    setSaving(true); setNotice(null)
    try {
      const data = await updateProfile({ fullName: String(fields.get('fullName')).trim(), phone: String(fields.get('phone')).trim() || null, birthDate: String(fields.get('birthDate')) || null })
      setState({ kind: 'loaded', data }); updateDisplayName(data.fullName); setNotice('Đã lưu thông tin cá nhân.')
    } catch { setNotice('Không thể lưu hồ sơ. Kiểm tra thông tin và thử lại.') } finally { setSaving(false) }
  }

  return <main className="min-h-screen bg-background py-8 sm:py-12"><div className="mx-auto max-w-3xl px-4 sm:px-6">
    <Link to="/me" className="text-sm font-semibold text-primary underline">← Tài khoản</Link>
    <h1 className="mt-4 text-3xl font-bold">Thông tin cá nhân</h1>
    <p className="mt-2 text-text-secondary">Ngày sinh giúp LAK cảnh báo phân loại phim. Thông tin người xem vẫn cần được kiểm tra khi mua hộ.</p>
    {state.kind === 'loading' && <div className="mt-8 h-80 animate-pulse rounded-xl bg-slate-200" aria-busy="true" />}
    {state.kind === 'error' && <div role="alert" className="mt-8 rounded-xl border border-red-200 bg-red-50 p-5">Không thể tải hồ sơ. <button className="min-h-11 font-semibold text-primary underline" onClick={() => { setState({ kind: 'loading' }); setRetry(v => v + 1) }}>Thử lại</button></div>}
    {state.kind === 'loaded' && <form key={`${state.data.fullName}:${state.data.phone}:${state.data.birthDate}`} onSubmit={submit} className="mt-8 space-y-5 rounded-xl border border-border bg-surface p-5 sm:p-7">
      <label className="block text-sm font-semibold">Họ và tên<input className="control mt-2" name="fullName" autoComplete="name" maxLength={150} defaultValue={state.data.fullName} required /></label>
      <div><label className="block text-sm font-semibold">Email<input className="control mt-2 bg-slate-50" value={state.data.email} readOnly /></label><p className="mt-1 text-xs text-text-muted">Email đăng nhập hiện không thể đổi tại đây.</p></div>
      <label className="block text-sm font-semibold">Số điện thoại<input className="control mt-2" name="phone" type="tel" inputMode="tel" autoComplete="tel" maxLength={32} defaultValue={state.data.phone ?? ''} /></label>
      <div><label className="block text-sm font-semibold">Ngày sinh<input className="control mt-2" name="birthDate" type="date" max={todayInVietnam()} defaultValue={state.data.birthDate ?? ''} /></label><p className="mt-1 text-xs text-text-muted">Chỉ dùng để hỗ trợ cảnh báo độ tuổi; không hiển thị trên vé.</p></div>
      {notice && <p role="status" className="text-sm text-text-secondary">{notice}</p>}
      <button className="primary-button" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu thay đổi'}</button>
    </form>}
    <div className="mt-6 flex flex-wrap gap-4 text-sm"><Link to="/me/billing" className="inline-flex min-h-11 items-center font-semibold text-primary underline">Thông tin hóa đơn</Link><Link to="/forgot-password" className="inline-flex min-h-11 items-center font-semibold text-primary underline">Đặt lại mật khẩu</Link></div>
  </div></main>
}
