import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { ArrowRight, Eye, EyeOff, LockKeyhole, Mail } from 'lucide-react'
import { forgotPassword, resetPassword } from './authApi'
import { useAuth } from './AuthProvider'
import logo from '../../assets/logo-banners/logo.png'

function AuthCard({ title, children }: { title: string; children: React.ReactNode }) {
  const loginLayout = title === 'Đăng nhập'
  if (loginLayout) {
    return <main className="relative flex min-h-screen items-center justify-end overflow-hidden bg-slate-950 bg-cover bg-center bg-no-repeat p-4 sm:p-8 lg:p-12" style={{ backgroundImage: "url('/posters/bg-login.png')" }}>
      <div className="absolute inset-0 bg-slate-950/25" aria-hidden="true" />
      <section className="relative w-full max-w-[472px] rounded-2xl border border-white/60 bg-white/95 px-6 py-6 shadow-2xl backdrop-blur-sm sm:px-10 sm:py-8 lg:mr-[5vw]">
        <div className="mx-auto w-full max-w-md">
          <Link to="/" className="inline-flex rounded-lg focus-visible:outline-2 focus-visible:outline-info" aria-label="LAK Cinema - Trang chủ"><img src={logo} alt="LAK Cinema" className="h-20 w-60 object-contain object-left" /></Link>
          <h1 className="mt-5 text-3xl font-bold tracking-tight text-text-primary">{title}</h1>
          <p className="mt-2 max-w-sm text-sm leading-6 text-text-secondary">Chào mừng bạn trở lại! Đăng nhập để tiếp tục đặt vé và khám phá những bộ phim hấp dẫn tại LAK.</p>
          {children}
        </div>
      </section>
    </main>
  }
  return <main className="grid min-h-screen place-items-center bg-background p-4"><section className="w-full max-w-md rounded-2xl border border-border bg-surface p-6 shadow-sm sm:p-8"><Link to="/" className="text-lg font-bold text-primary">LAK</Link><h1 className="mt-6 text-2xl font-bold text-text-primary">{title}</h1>{children}</section></main>
}

function Field({ label, ...props }: React.InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  const id = props.name
  const autoComplete = props.name === 'email' && props.autoComplete === 'email' ? 'username' : props.autoComplete
  return <label className="block text-sm font-medium text-text-primary" htmlFor={id}>{label}<input id={id} {...props} autoComplete={autoComplete} className="mt-1 min-h-11 w-full rounded-lg border border-border px-3 text-text-primary outline-none focus:border-info focus:ring-2 focus:ring-info/20" /></label>
}

function LoginField({ label, name, type, placeholder, icon: Icon, visible, onToggle }: { label: string; name: string; type: 'email' | 'password' | 'text'; placeholder: string; icon: typeof Mail; visible?: boolean; onToggle?: () => void }) {
  return <label className="block text-sm font-semibold text-text-primary" htmlFor={name}>
    {label}
    <span className="relative mt-2 block">
      <Icon size={19} aria-hidden="true" className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" />
      <input id={name} name={name} type={type} placeholder={placeholder} autoComplete={name === 'email' ? 'username' : 'current-password'} required className="min-h-11 w-full rounded-lg border border-slate-300 bg-white pl-11 pr-12 text-sm font-normal text-text-primary outline-none placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/15" />
      {onToggle && <button type="button" onClick={onToggle} className="absolute right-2 top-1/2 grid size-9 -translate-y-1/2 place-items-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-text-primary focus-visible:outline-2 focus-visible:outline-info" aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>{visible ? <EyeOff size={19} aria-hidden="true" /> : <Eye size={19} aria-hidden="true" />}</button>}
    </span>
  </label>
}

function ErrorMessage({ value }: { value: string | null }) { return value ? <p role="alert" className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-error">{value}</p> : null }
function Submit({ children, pending }: { children: string; pending: boolean }) { return <button type="submit" disabled={pending} className="min-h-11 w-full rounded-lg bg-primary px-4 py-2 font-semibold text-white transition-colors hover:bg-primary-hover disabled:opacity-60">{pending ? 'Đang xử lý…' : children}</button> }

export function LoginPage() {
  const { login } = useAuth(); const navigate = useNavigate(); const location = useLocation(); const [error, setError] = useState<string | null>(null); const [pending, setPending] = useState(false); const [showPassword, setShowPassword] = useState(false)
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const form = new FormData(event.currentTarget); setPending(true); setError(null); try { await login(String(form.get('email')), String(form.get('password'))); navigate((location.state as { from?: string } | null)?.from ?? '/', { replace: true }) } catch (reason) { setError(reason instanceof Error ? reason.message : 'Không thể đăng nhập.') } finally { setPending(false) } }
  return <AuthCard title="Đăng nhập"><form autoComplete="on" className="mt-5 space-y-4" onSubmit={submit}><ErrorMessage value={error}/><LoginField label="Email" name="email" type="email" placeholder="Nhập email của bạn" icon={Mail}/><LoginField label="Mật khẩu" name="password" type={showPassword ? 'text' : 'password'} placeholder="Nhập mật khẩu" icon={LockKeyhole} visible={showPassword} onToggle={() => setShowPassword((value) => !value)}/><button type="submit" disabled={pending} className="flex min-h-12 w-full items-center justify-center gap-3 rounded-lg bg-primary px-4 py-2 font-semibold text-white transition-colors hover:bg-primary-hover disabled:opacity-60"><span>{pending ? 'Đang xử lý…' : 'Đăng nhập'}</span><ArrowRight size={21} aria-hidden="true" /></button><div className="flex items-center gap-4 py-1 text-sm text-text-muted"><span className="h-px flex-1 bg-border" /> <span>Hoặc</span> <span className="h-px flex-1 bg-border" /></div><p className="text-center text-sm font-medium text-text-secondary">Chưa có tài khoản? <Link className="font-bold text-primary underline underline-offset-2" to="/register">Đăng ký</Link></p><p className="pt-1 text-center text-[10px] leading-4 text-text-muted">Bằng việc đăng nhập, bạn đồng ý với <Link className="underline" to="/terms">Điều khoản sử dụng</Link><br/>và <Link className="underline" to="/privacy">Chính sách bảo mật của LAK Cinema</Link>.</p></form></AuthCard>
}

export function RegisterPage() {
  const { register } = useAuth(); const navigate = useNavigate(); const [error, setError] = useState<string | null>(null); const [pending, setPending] = useState(false)
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const form = new FormData(event.currentTarget); const password = String(form.get('password')); if (password !== String(form.get('confirmPassword'))) { setError('Xác nhận mật khẩu không khớp.'); return } setPending(true); setError(null); try { await register(String(form.get('fullName')), String(form.get('email')), password); navigate('/', { replace: true }) } catch (reason) { setError(reason instanceof Error ? reason.message : 'Không thể đăng ký.') } finally { setPending(false) } }
  return <AuthCard title="Đăng ký tài khoản"><form className="mt-6 space-y-4" onSubmit={submit}><ErrorMessage value={error}/><Field label="Họ tên" name="fullName" autoComplete="name" required/><Field label="Email" name="email" type="email" autoComplete="email" required/><Field label="Mật khẩu (ít nhất 12 ký tự)" name="password" type="password" autoComplete="new-password" minLength={12} required/><Field label="Xác nhận mật khẩu" name="confirmPassword" type="password" autoComplete="new-password" minLength={12} required/><Submit pending={pending}>Đăng ký</Submit></form><p className="mt-5 text-sm text-text-secondary">Đã có tài khoản? <Link className="text-primary underline" to="/login">Đăng nhập</Link></p></AuthCard>
}

export function ForgotPasswordPage() { const [email, setEmail] = useState(''); const [done, setDone] = useState(false); const [error, setError] = useState<string | null>(null); async function submit(e: FormEvent) { e.preventDefault(); setError(null); try { await forgotPassword(email); setDone(true) } catch { setError('Không thể gửi yêu cầu. Vui lòng thử lại.') } } return <AuthCard title="Quên mật khẩu"><p className="mt-2 text-sm text-text-secondary">Nếu tài khoản tồn tại, hệ thống sẽ gửi hướng dẫn tới email của bạn.</p>{done ? <p className="mt-6 rounded-lg bg-green-50 p-3 text-sm text-success">Yêu cầu đã được tiếp nhận.</p> : <form className="mt-6 space-y-4" onSubmit={submit}><ErrorMessage value={error}/><Field label="Email" name="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required/><Submit pending={false}>Gửi hướng dẫn</Submit></form>}</AuthCard> }

export function ResetPasswordPage() { const [params] = useSearchParams(); const navigate = useNavigate(); const [error, setError] = useState<string | null>(null); const [done, setDone] = useState(false); async function submit(e: FormEvent<HTMLFormElement>) { e.preventDefault(); const password = String(new FormData(e.currentTarget).get('password')); const token = params.get('token'); if (!token) { setError('Liên kết đặt lại mật khẩu không hợp lệ.'); return } try { await resetPassword(token, password); setDone(true); window.setTimeout(() => navigate('/login'), 1200) } catch { setError('Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.') } } return <AuthCard title="Đặt lại mật khẩu">{done ? <p className="mt-6 text-sm text-success">Đã cập nhật mật khẩu. Đang chuyển tới trang đăng nhập…</p> : <form className="mt-6 space-y-4" onSubmit={submit}><ErrorMessage value={error}/><Field label="Mật khẩu mới (ít nhất 12 ký tự)" name="password" type="password" minLength={12} required/><Submit pending={false}>Cập nhật mật khẩu</Submit></form>}</AuthCard> }
