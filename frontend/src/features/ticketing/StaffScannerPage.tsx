import { isAxiosError } from 'axios'
import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { validateTicket, type TicketScanResult } from './scannerApi'

type ScanState =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'result'; data: TicketScanResult }
  | { kind: 'error'; message: string }

type BarcodeDetectorLike = { detect: (source: HTMLVideoElement) => Promise<Array<{ rawValue?: string }>> }
type BarcodeDetectorConstructor = new (options: { formats: string[] }) => BarcodeDetectorLike

const dateTime = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh',
})

function scanError(error: unknown) {
  if (isAxiosError<{ message?: string }>(error)) return error.response?.data?.message ?? 'Không thể xác thực vé.'
  return 'Không thể kết nối tới hệ thống. Vui lòng thử lại.'
}

export function StaffScannerPage() {
  const [manualCode, setManualCode] = useState('')
  const [state, setState] = useState<ScanState>({ kind: 'idle' })
  const [cameraMessage, setCameraMessage] = useState<string>()
  const videoRef = useRef<HTMLVideoElement>(null)
  const streamRef = useRef<MediaStream | undefined>(undefined)
  const scanTimer = useRef<number | undefined>(undefined)
  const submitting = useRef(false)

  const stopCamera = () => {
    if (scanTimer.current) window.clearInterval(scanTimer.current)
    scanTimer.current = undefined
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = undefined
  }

  const submit = (value: string) => {
    const code = value.trim()
    if (!code || submitting.current) return
    submitting.current = true
    setState({ kind: 'loading' })
    void validateTicket(code).then(
      (data) => { setState({ kind: 'result', data }); stopCamera() },
      (error: unknown) => setState({ kind: 'error', message: scanError(error) }),
    ).finally(() => { submitting.current = false })
  }

  const startCamera = async () => {
    stopCamera()
    const Detector = (window as unknown as { BarcodeDetector?: BarcodeDetectorConstructor }).BarcodeDetector
    if (!Detector || !navigator.mediaDevices?.getUserMedia) {
      setCameraMessage('Thiết bị chưa hỗ trợ quét camera. Hãy nhập mã vé thủ công.')
      return
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false })
      streamRef.current = stream
      if (!videoRef.current) return
      videoRef.current.srcObject = stream
      await videoRef.current.play()
      const detector = new Detector({ formats: ['qr_code'] })
      setCameraMessage(undefined)
      scanTimer.current = window.setInterval(() => {
        if (!videoRef.current || submitting.current) return
        void detector.detect(videoRef.current).then((codes) => {
          const value = codes[0]?.rawValue
          if (value) submit(value)
        }).catch(() => setCameraMessage('Không thể đọc mã QR. Hãy thử lại hoặc nhập mã thủ công.'))
      }, 500)
    }
    catch {
      setCameraMessage('Không thể mở camera. Kiểm tra quyền camera hoặc nhập mã thủ công.')
      stopCamera()
    }
  }

  useEffect(() => stopCamera, [])

  const onManualSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    submit(manualCode)
  }

  return (
    <main className="min-h-screen bg-background p-4 sm:p-6">
      <section className="mx-auto max-w-xl">
        <p className="text-sm font-semibold text-primary">LAK STAFF</p>
        <h1 className="mt-1 text-3xl font-bold text-text-primary">Soát vé</h1>
        <p className="mt-2 text-sm text-text-secondary">Quét QR hoặc nhập mã vé. Kết quả luôn được xác nhận bởi hệ thống.</p>
        <div className="mt-6 overflow-hidden rounded-xl border border-border bg-surface">
          <video ref={videoRef} className="aspect-video w-full bg-slate-900 object-cover" muted playsInline aria-label="Khung quét mã QR" />
          <div className="p-4">
            <button onClick={() => void startCamera()} className="min-h-11 rounded-lg bg-primary px-4 text-sm font-semibold text-white hover:bg-primary-hover">Bật camera quét QR</button>
            {cameraMessage && <p role="status" className="mt-3 text-sm text-warning">{cameraMessage}</p>}
          </div>
        </div>
        <form onSubmit={onManualSubmit} className="mt-5 rounded-xl border border-border bg-surface p-4">
          <label htmlFor="ticket-code" className="text-sm font-semibold text-text-primary">Nhập mã vé thủ công</label>
          <div className="mt-2 flex flex-col gap-2 sm:flex-row">
            <input id="ticket-code" value={manualCode} onChange={(event) => setManualCode(event.target.value)} maxLength={256} className="min-h-11 flex-1 rounded-lg border border-border px-3" placeholder="Mã QR hoặc TKT-…" autoComplete="off" />
            <button disabled={state.kind === 'loading'} className="min-h-11 rounded-lg border border-primary px-4 text-sm font-semibold text-primary disabled:opacity-60">Xác thực vé</button>
          </div>
        </form>
        {state.kind === 'loading' && <div className="mt-5 rounded-xl border border-border bg-surface p-5" aria-busy="true">Đang xác thực vé…</div>}
        {state.kind === 'error' && <div role="alert" className="mt-5 rounded-xl border border-red-200 bg-red-50 p-5 text-error">{state.message}</div>}
        {state.kind === 'result' && <ScanResult result={state.data} onNext={() => { setManualCode(''); setState({ kind: 'idle' }) }} />}
      </section>
    </main>
  )
}

function ScanResult({ result, onNext }: { result: TicketScanResult; onNext: () => void }) {
  const used = result.result === 'USED'
  return (
    <section className={`mt-5 rounded-xl border p-5 ${used ? 'border-amber-200 bg-amber-50' : 'border-green-200 bg-green-50'}`} aria-live="polite">
      <p className={`font-bold ${used ? 'text-warning' : 'text-success'}`}>{used ? 'VÉ ĐÃ ĐƯỢC SỬ DỤNG' : 'VÉ HỢP LỆ'}</p>
      <h2 className="mt-3 text-xl font-bold text-text-primary">{result.movieTitle}</h2>
      <p className="mt-1 text-sm text-text-secondary">{dateTime.format(new Date(result.startAt))} · {result.auditoriumName}</p>
      <p className="mt-1 text-sm text-text-secondary">{result.cinemaName} · Ghế {result.seatLabels.join(', ')}</p>
      {used && <p className="mt-3 text-sm text-text-secondary">Quét lần đầu: {dateTime.format(new Date(result.firstScannedAt))}</p>}
      <button onClick={onNext} className="mt-5 min-h-11 rounded-lg border border-primary px-4 text-sm font-semibold text-primary">Quét vé tiếp theo</button>
    </section>
  )
}
