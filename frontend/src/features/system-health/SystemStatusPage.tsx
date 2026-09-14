import { useCallback, useEffect, useState } from 'react'
import { getSystemHealth } from './api/healthApi'
import type { HealthState, SystemHealthResponse } from './types'

type ViewState =
  | { kind: 'loading' }
  | { kind: 'loaded'; health: SystemHealthResponse }
  | { kind: 'error'; message: string }

type DisplayState = HealthState | 'UNKNOWN'

const services = [
  { key: 'backend', name: 'Backend API', description: 'Spring Boot application' },
  { key: 'database', name: 'PostgreSQL', description: 'Nguồn dữ liệu chính' },
  { key: 'redis', name: 'Redis', description: 'Cache và realtime support' },
] as const

const statusStyles: Record<DisplayState, string> = {
  UP: 'border-green-200 bg-green-50 text-green-700',
  DOWN: 'border-red-200 bg-red-50 text-red-700',
  UNKNOWN: 'border-slate-200 bg-slate-100 text-slate-600',
}

const statusLabels: Record<DisplayState, string> = {
  UP: 'Hoạt động',
  DOWN: 'Gián đoạn',
  UNKNOWN: 'Chưa xác định',
}

function readableError(error: unknown): string {
  return error instanceof Error
    ? error.message
    : 'Không thể kết nối tới backend. Vui lòng thử lại.'
}

function ServiceCard({
  name,
  description,
  status,
}: {
  name: string
  description: string
  status: DisplayState
}) {
  return (
    <article className="rounded-xl border border-border bg-surface p-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-text-primary">{name}</h2>
          <p className="mt-1 text-sm leading-5 text-text-secondary">{description}</p>
        </div>
        <span
          className={`inline-flex min-h-7 items-center gap-2 rounded-full border px-3 py-1 text-xs font-semibold ${statusStyles[status]}`}
        >
          <span className="size-2 rounded-full bg-current" aria-hidden="true" />
          {statusLabels[status]}
        </span>
      </div>
    </article>
  )
}

export function SystemStatusPage() {
  const [viewState, setViewState] = useState<ViewState>({ kind: 'loading' })

  const refreshHealth = useCallback(async () => {
    setViewState({ kind: 'loading' })
    try {
      const health = await getSystemHealth()
      setViewState({ kind: 'loaded', health })
    } catch (error) {
      setViewState({ kind: 'error', message: readableError(error) })
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    void getSystemHealth(controller.signal).then(
      (health) => setViewState({ kind: 'loaded', health }),
      (error: unknown) => {
        if (!controller.signal.aborted) {
          setViewState({ kind: 'error', message: readableError(error) })
        }
      },
    )
    return () => controller.abort()
  }, [])

  const health = viewState.kind === 'loaded' ? viewState.health : null
  const checkedAt = health
    ? new Intl.DateTimeFormat('vi-VN', {
        dateStyle: 'medium',
        timeStyle: 'medium',
        timeZone: 'Asia/Ho_Chi_Minh',
      }).format(new Date(health.timestamp))
    : null

  return (
    <main className="min-h-screen bg-background px-4 py-10 sm:px-6 lg:py-16">
      <div className="mx-auto max-w-5xl">
        <header className="rounded-2xl border border-rose-100 bg-gradient-to-br from-white to-primary-soft p-6 sm:p-8">
          <div className="flex size-12 items-center justify-center rounded-xl bg-primary text-xl font-bold text-white">
            L
          </div>
          <p className="mt-6 text-sm font-semibold uppercase tracking-[0.16em] text-primary">
            LAK Movie Ticket Booking
          </p>
          <h1 className="mt-3 text-3xl font-bold leading-10 text-text-primary sm:text-4xl sm:leading-12">
            Trạng thái hệ thống
          </h1>
          <p className="mt-3 max-w-2xl text-base leading-6 text-text-secondary">
            Kiểm tra kết nối end-to-end giữa giao diện, backend và hạ tầng dữ liệu.
          </p>
        </header>

        <section className="mt-8" aria-live="polite" aria-busy={viewState.kind === 'loading'}>
          <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-xl font-semibold text-text-primary">Dịch vụ</h2>
              <p className="mt-1 text-sm text-text-secondary">
                {checkedAt ? `Cập nhật lúc ${checkedAt}` : 'Đang lấy trạng thái mới nhất'}
              </p>
            </div>
            <button
              type="button"
              onClick={() => void refreshHealth()}
              disabled={viewState.kind === 'loading'}
              className="min-h-11 rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition-colors duration-150 hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
            >
              {viewState.kind === 'loading' ? 'Đang kiểm tra…' : 'Kiểm tra lại'}
            </button>
          </div>

          {viewState.kind === 'error' && (
            <div role="alert" className="mb-4 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
              <strong>Không thể kết nối:</strong> {viewState.message}
            </div>
          )}

          <div className="grid gap-4 md:grid-cols-3">
            {services.map((service) => (
              <ServiceCard
                key={service.key}
                name={service.name}
                description={service.description}
                status={health?.services[service.key].status ?? 'UNKNOWN'}
              />
            ))}
          </div>
        </section>
      </div>
    </main>
  )
}
