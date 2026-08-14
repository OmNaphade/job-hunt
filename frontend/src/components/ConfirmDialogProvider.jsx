import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'

const ConfirmDialogContext = createContext(null)

const defaultOptions = {
  title: 'Are you sure?',
  message: '',
  confirmLabel: 'Confirm',
  cancelLabel: 'Cancel',
  tone: 'danger',
}

export function ConfirmDialogProvider({ children }) {
  const [request, setRequest] = useState(null)
  const resolverRef = useRef(null)
  const confirmButtonRef = useRef(null)

  const confirm = useCallback((options = {}) => {
    return new Promise((resolve) => {
      resolverRef.current = resolve
      setRequest({ ...defaultOptions, ...options })
    })
  }, [])

  const settle = useCallback((result) => {
    if (resolverRef.current) {
      resolverRef.current(result)
      resolverRef.current = null
    }
    setRequest(null)
  }, [])

  useEffect(() => {
    if (!request) return undefined

    confirmButtonRef.current?.focus()

    function handleKey(event) {
      if (event.key === 'Escape') settle(false)
    }
    document.addEventListener('keydown', handleKey)
    return () => document.removeEventListener('keydown', handleKey)
  }, [request, settle])

  const value = useMemo(() => ({ confirm }), [confirm])

  return (
    <ConfirmDialogContext.Provider value={value}>
      {children}
      {request ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/55 p-4 backdrop-blur-sm"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) settle(false)
          }}
        >
          <div
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="confirm-dialog-title"
            className="dropdown-panel dropdown-in w-full max-w-sm p-5"
          >
            <h2 id="confirm-dialog-title" className="text-base font-extrabold text-slate-900">
              {request.title}
            </h2>
            {request.message ? <p className="mt-2 text-sm text-slate-600">{request.message}</p> : null}
            <div className="mt-5 flex justify-end gap-2">
              <button type="button" onClick={() => settle(false)} className="btn btn-secondary btn-sm">
                {request.cancelLabel}
              </button>
              <button
                ref={confirmButtonRef}
                type="button"
                onClick={() => settle(true)}
                className={`btn btn-sm ${request.tone === 'danger' ? 'btn-danger' : 'btn-primary'}`}
              >
                {request.confirmLabel}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </ConfirmDialogContext.Provider>
  )
}

export function useConfirm() {
  const context = useContext(ConfirmDialogContext)
  if (!context) {
    throw new Error('useConfirm must be used within ConfirmDialogProvider')
  }
  return context.confirm
}
