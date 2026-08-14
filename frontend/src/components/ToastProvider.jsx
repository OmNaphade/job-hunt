import { createContext, useCallback, useContext, useMemo, useState } from 'react'

const ToastContext = createContext(null)

function toneClasses(tone) {
  switch (tone) {
    case 'success':
      return 'border-emerald-200 bg-emerald-50 text-emerald-800'
    case 'error':
      return 'border-rose-200 bg-rose-50 text-rose-800'
    case 'warning':
      return 'border-amber-200 bg-amber-50 text-amber-900'
    default:
      return 'border-slate-200 bg-white text-slate-800'
  }
}

function toneIcon(tone) {
  switch (tone) {
    case 'success':
      return '✓'
    case 'error':
      return '⚠'
    case 'warning':
      return '!'
    default:
      return 'i'
  }
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((item) => item.id !== id))
  }, [])

  const showToast = useCallback((message, tone = 'info', durationMs = 2800) => {
    const id = crypto.randomUUID()
    setToasts((prev) => [...prev, { id, message, tone }])

    window.setTimeout(() => {
      removeToast(id)
    }, durationMs)
  }, [removeToast])

  const value = useMemo(() => ({ showToast }), [showToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed right-4 top-4 z-50 flex w-[min(92vw,360px)] flex-col gap-2">
        {toasts.map((toast) => (
          <output
            key={toast.id}
            className={`pointer-events-auto rounded-xl border px-3 py-2.5 text-sm font-semibold shadow-lg shadow-slate-900/10 toast-in ${toneClasses(toast.tone)}`}
            aria-live="polite"
          >
            <div className="flex items-start gap-2.5">
              <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-black/5 text-xs">
                {toneIcon(toast.tone)}
              </span>
              <p className="flex-1">{toast.message}</p>
              <button
                type="button"
                onClick={() => removeToast(toast.id)}
                className="rounded-md px-1 text-xs text-slate-500 hover:bg-black/5"
                aria-label="Close notification"
              >
                x
              </button>
            </div>
          </output>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return context
}
