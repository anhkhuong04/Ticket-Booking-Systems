import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { getBillingPreferences, getProfile, updateBillingPreferences, type BillingPreferences } from './profileApi'

export function BillingPreferencesPage() {
  const [value, setValue] = useState<BillingPreferences | null>(null)
  const [type, setType] = useState<'PERSONAL' | 'BUSINESS'>('PERSONAL')
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [retry, setRetry] = useState(0)
  useEffect(() => { let active = true; void Promise.all([getBillingPreferences(), getProfile()]).then(([billing, profile]) => { if (active) { setValue(billing ?? { recipientType: 'PERSONAL', recipientName: profile.fullName, email: profile.email, taxCode: null, address: null }); setType(billing?.recipientType ?? 'PERSONAL'); setLoading(false) } }, () => { if (active) { setFailed(true); setLoading(false) } }); return () => { active = false } }, [retry])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (saving) return
    const fields = new FormData(event.currentTarget)
    setSaving(true); setNotice(null)
    try { const saved = await updateBillingPreferences({ recipientType: type, recipientName: String(fields.get('recipientName')).trim(), taxCode: type === 'BUSINESS' ? String(fields.get('taxCode')).trim() : null, address: String(fields.get('address')).trim() || null, email: String(fields.get('email')).trim() }); setValue(saved); setNotice('Đã lưu thông tin hóa đơn mặc định.') }
    catch { setNotice('Không thể lưu thông tin hóa đơn. Vui lòng kiểm tra và thử lại.') }
    finally { setSaving(false) }
  }

  return <main className="min-h-screen bg-background py-8 sm:py-12"><div className="mx-auto max-w-3xl px-4 sm:px-6"><Link to="/me/profile" className="text-sm font-semibold text-primary underline">← Thông tin cá nhân</Link><h1 className="mt-4 text-3xl font-bold">Thông tin hóa đơn</h1><p className="mt-2 text-text-secondary">Lưu thông tin người nhận để tiện sử dụng. Đây là thông tin mặc định, chưa phải hóa đơn đã phát hành cho một booking.</p>
    {loading && <div className="mt-8 h-72 animate-pulse rounded-xl bg-slate-200" aria-busy="true" />}
    {failed && <p role="alert" className="mt-8 rounded-xl bg-red-50 p-5">Không thể tải thông tin. <button className="min-h-11 text-primary underline" onClick={() => { setFailed(false); setLoading(true); setRetry(v => v + 1) }}>Thử lại</button></p>}
    {value && !loading && <form key={JSON.stringify(value)} onSubmit={submit} className="mt-8 space-y-5 rounded-xl border border-border bg-surface p-5 sm:p-7">
      <fieldset><legend className="text-sm font-semibold">Người nhận</legend><div className="mt-2 flex gap-5"><label className="flex min-h-11 items-center gap-2"><input type="radio" name="recipientType" checked={type === 'PERSONAL'} onChange={() => setType('PERSONAL')} /> Cá nhân</label><label className="flex min-h-11 items-center gap-2"><input type="radio" name="recipientType" checked={type === 'BUSINESS'} onChange={() => setType('BUSINESS')} /> Doanh nghiệp</label></div></fieldset>
      <label className="block text-sm font-semibold">Tên người nhận / doanh nghiệp<input className="control mt-2" name="recipientName" maxLength={150} defaultValue={value.recipientName} required /></label>
      {type === 'BUSINESS' && <label className="block text-sm font-semibold">Mã số thuế<input className="control mt-2" name="taxCode" maxLength={32} defaultValue={value.taxCode ?? ''} required /></label>}
      <label className="block text-sm font-semibold">Địa chỉ{type === 'BUSINESS' ? '' : ' (tùy chọn)'}<input className="control mt-2" name="address" maxLength={500} defaultValue={value.address ?? ''} required={type === 'BUSINESS'} /></label>
      <label className="block text-sm font-semibold">Email nhận hóa đơn<input className="control mt-2" name="email" type="email" maxLength={320} defaultValue={value.email} required /></label>
      {notice && <p role="status" className="text-sm text-text-secondary">{notice}</p>}
      <button className="primary-button" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu thông tin'}</button>
    </form>}
  </div></main>
}
