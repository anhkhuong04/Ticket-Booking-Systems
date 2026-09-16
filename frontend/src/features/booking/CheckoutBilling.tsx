import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { getBillingPreferences, getProfile, type BillingPreferences } from '../account/profileApi'
import { getBookingBilling, requestBookingBilling } from './bookingApi'

export function CheckoutBilling({ bookingCode, payable }: { bookingCode: string; payable: boolean }) {
  const [value, setValue] = useState<BillingPreferences | null>(null)
  const [requested, setRequested] = useState<BillingPreferences | null>(null)
  const [enabled, setEnabled] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [retry, setRetry] = useState(0)

  useEffect(() => { let active = true; void Promise.all([getBillingPreferences(), getBookingBilling(bookingCode), getProfile()]).then(([saved, current, profile]) => {
    if (active) { setValue(current ?? saved ?? { recipientType: 'PERSONAL', recipientName: profile.fullName, email: profile.email, taxCode: null, address: null }); setRequested(current); setEnabled(current !== null); setLoading(false) }
  }, () => { if (active) { setNotice('Không thể tải thông tin hóa đơn.'); setLoading(false) } }); return () => { active = false } }, [bookingCode, retry])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!value || saving || !payable) return
    const fields = new FormData(event.currentTarget)
    setSaving(true); setNotice(null)
    try { const saved = await requestBookingBilling(bookingCode, { ...value, recipientName: String(fields.get('recipientName')).trim(), taxCode: value.recipientType === 'BUSINESS' ? String(fields.get('taxCode')).trim() : null, address: String(fields.get('address')).trim() || null, email: String(fields.get('email')).trim() }); setRequested(saved); setValue(saved); setNotice('Đã ghi nhận yêu cầu hóa đơn cho booking này.') }
    catch { setNotice('Không thể lưu yêu cầu hóa đơn. Vui lòng thử lại trước khi thanh toán.') }
    finally { setSaving(false) }
  }

  return <section className="mt-6 rounded-xl border border-border bg-surface p-5 sm:p-7" aria-labelledby="billing-title"><h2 id="billing-title" className="text-lg font-bold">Thông tin hóa đơn</h2><p className="mt-2 text-sm text-text-secondary">Tùy chọn. Thông tin được ghi nhận cho booking này khi bạn bấm lưu. LAK chưa phát hành hóa đơn điện tử tự động trên trang này.</p>
    {loading && <p className="mt-3 text-sm text-text-secondary" aria-busy="true">Đang tải thông tin…</p>}
    {!loading && notice && <p className="mt-3 text-sm text-text-secondary" role="status">{notice} {notice.startsWith('Không thể tải') && <button className="min-h-11 font-semibold text-primary underline" onClick={() => { setLoading(true); setRetry(v => v + 1) }}>Thử lại</button>}</p>}
    {!loading && requested && <p className="mt-3 rounded-lg bg-green-50 p-3 text-sm text-green-800">Đã ghi nhận: {requested.recipientName} · {requested.email}</p>}
    {!loading && !enabled && payable && <button className="mt-3 min-h-11 font-semibold text-primary underline" onClick={() => setEnabled(true)}>Yêu cầu hóa đơn</button>}
    {!loading && enabled && value && payable && <form onSubmit={submit} className="mt-4 grid gap-4 sm:grid-cols-2">
      <label className="block text-sm font-semibold">Loại người nhận<select className="control mt-2" value={value.recipientType} onChange={event => setValue({ ...value, recipientType: event.target.value as BillingPreferences['recipientType'] })}><option value="PERSONAL">Cá nhân</option><option value="BUSINESS">Doanh nghiệp</option></select></label>
      <label className="block text-sm font-semibold">Tên người nhận<input className="control mt-2" name="recipientName" maxLength={150} defaultValue={value.recipientName} required /></label>
      {value.recipientType === 'BUSINESS' && <label className="block text-sm font-semibold">Mã số thuế<input className="control mt-2" name="taxCode" maxLength={32} defaultValue={value.taxCode ?? ''} required /></label>}
      <label className="block text-sm font-semibold">Địa chỉ<input className="control mt-2" name="address" maxLength={500} defaultValue={value.address ?? ''} required={value.recipientType === 'BUSINESS'} /></label>
      <label className="block text-sm font-semibold">Email nhận hóa đơn<input className="control mt-2" name="email" type="email" maxLength={320} defaultValue={value.email} required /></label>
      <div className="flex items-end"><button className="secondary-button" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu cho booking'}</button></div>
    </form>}
    {!loading && <Link to="/me/billing" className="mt-3 inline-flex min-h-11 items-center text-sm font-semibold text-primary underline">Quản lý thông tin mặc định</Link>}
  </section>
}
