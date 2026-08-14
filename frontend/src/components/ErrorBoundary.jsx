import { Component } from 'react'

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error) {
    return { error }
  }

  componentDidCatch(error, info) {
    console.error('Unhandled UI error:', error, info)
  }

  render() {
    if (!this.state.error) {
      return this.props.children
    }

    return (
      <div className="flex min-h-screen items-center justify-center px-4">
        <div className="card w-full max-w-lg p-6">
          <p className="eyebrow text-rose-500">Something went wrong</p>
          <h1 className="mt-2 text-xl font-extrabold tracking-tight text-slate-900">
            This page hit an unexpected error
          </h1>
          <p className="mt-2 text-sm text-slate-600">
            The interface crashed while rendering. Reloading usually clears it; if it keeps happening, the
            details below are worth sharing with whoever&rsquo;s debugging it.
          </p>

          <details className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-3 text-xs text-slate-600">
            <summary className="cursor-pointer font-semibold text-slate-700">Technical details</summary>
            <pre className="mt-2 max-h-48 overflow-auto whitespace-pre-wrap break-words">
              {String(this.state.error?.stack || this.state.error?.message || this.state.error)}
            </pre>
          </details>

          <div className="mt-5 flex flex-wrap gap-2">
            <button type="button" onClick={() => window.location.reload()} className="btn btn-primary">
              Reload page
            </button>
            <a href="/" className="btn btn-secondary">
              Go to homepage
            </a>
          </div>
        </div>
      </div>
    )
  }
}
