import { useEffect, useRef, type ReactNode } from 'react'
import { X } from 'lucide-react'

type Props = {
  title: string
  description?: string
  children: ReactNode
  onClose: () => void
  size?: 'default' | 'large'
}

function focusableElements(container: HTMLElement): HTMLElement[] {
  return [...container.querySelectorAll<HTMLElement>(
    'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
  )]
}

export function AdminDialog({ title, description, children, onClose, size = 'default' }: Props) {
  const dialogRef = useRef<HTMLElement>(null)

  useEffect(() => {
    const previouslyFocused = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const dialog = dialogRef.current
    dialog?.querySelector<HTMLElement>('button, input, select, textarea, [tabindex]:not([tabindex="-1"])')?.focus()

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        onClose()
        return
      }
      if (event.key !== 'Tab' || !dialog) return
      const focusable = focusableElements(dialog)
      if (!focusable.length) return
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      previouslyFocused?.focus()
    }
  }, [onClose])

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/40 p-4" role="presentation">
      <button type="button" aria-label="Đóng hộp thoại" onClick={onClose} className="absolute inset-0 cursor-default" />
      <section ref={dialogRef} role="dialog" aria-modal="true" aria-labelledby="admin-dialog-title" className={`relative z-10 max-h-[calc(100vh-2rem)] w-full overflow-y-auto rounded-2xl border border-border bg-surface shadow-xl ${size === 'large' ? 'max-w-4xl' : 'max-w-xl'}`}>
        <header className="sticky top-0 z-10 flex items-start justify-between gap-4 border-b border-border bg-surface px-5 py-4">
          <div>
            <h2 id="admin-dialog-title" className="text-lg font-bold text-text-primary">{title}</h2>
            {description && <p className="mt-1 text-sm text-text-secondary">{description}</p>}
          </div>
          <button type="button" onClick={onClose} className="grid size-11 shrink-0 place-items-center rounded-xl text-text-secondary hover:bg-primary-soft hover:text-primary focus-visible:outline-2 focus-visible:outline-primary" aria-label="Đóng hộp thoại"><X size={20} aria-hidden="true" /></button>
        </header>
        <div className="p-5">{children}</div>
      </section>
    </div>
  )
}
