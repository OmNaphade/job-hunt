import { useEffect, useMemo, useState } from 'react'
import SectionCard from '../components/SectionCard'
import { EmptyState, ListSkeleton } from '../components/StateBlocks'
import { ErrorMessage, SuccessMessage } from '../components/Message'
import { api, getErrorMessage } from '../lib/api'

const SERVICES = [
  { key: 'gateway', label: 'API Gateway' },
  { key: 'auth', label: 'Auth Service' },
  { key: 'user', label: 'User Service' },
  { key: 'job', label: 'Job Service' },
  { key: 'company', label: 'Company Service' },
  { key: 'application', label: 'Application Service' },
  { key: 'notification', label: 'Notification Service' },
]

const DEFAULT_WIDGETS = {
  heap: true,
  cpu: true,
  dbConnections: true,
  requests: true,
  circuitBreaker: true,
  rateLimiting: true,
}

const ZIPKIN_URL = import.meta.env.VITE_ZIPKIN_URL || 'http://localhost:9411/zipkin/'

const WIDGET_STORAGE_KEY = 'jp_monitor_widgets'
const LAYOUT_STORAGE_KEY = 'jp_monitor_layout'
const ORDER_STORAGE_KEY = 'jp_monitor_order'
const PINNED_STORAGE_KEY = 'jp_monitor_pinned'
const LAST_N_POINTS = 24

function getLevel(value, warning, critical) {
  if (value >= critical) return 'critical'
  if (value >= warning) return 'warning'
  return 'healthy'
}

function getLevelClasses(level) {
  if (level === 'critical') return 'bg-rose-100 text-rose-700 border-rose-300'
  if (level === 'warning') return 'bg-amber-100 text-amber-700 border-amber-300'
  return 'bg-emerald-100 text-emerald-700 border-emerald-300'
}

function MetricBadge({ value, warning, critical }) {
  const level = getLevel(value, warning, critical)
  return (
    <span className={`rounded-full border px-2 py-0.5 text-[10px] font-bold uppercase ${getLevelClasses(level)}`}>
      {level}
    </span>
  )
}

function Sparkline({ values, colorClass = 'stroke-cyan-600' }) {
  if (!values.length) {
    return <div className="h-16 rounded-lg bg-slate-100" />
  }

  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = max - min || 1

  const points = values
    .map((value, index) => {
      const x = (index / Math.max(values.length - 1, 1)) * 100
      const y = 100 - ((value - min) / range) * 100
      return `${x},${y}`
    })
    .join(' ')

  return (
    <svg viewBox="0 0 100 100" preserveAspectRatio="none" className="h-16 w-full rounded-lg bg-slate-50">
      <polyline
        points={points}
        fill="none"
        strokeWidth="2"
        className={colorClass}
        vectorEffect="non-scaling-stroke"
      />
    </svg>
  )
}

export default function MonitoringPage() {
  const [serviceMetrics, setServiceMetrics] = useState([])
  const [selectedServices, setSelectedServices] = useState(SERVICES.map((x) => x.key))
  const [autoRefreshSec, setAutoRefreshSec] = useState(20)
  const [layoutMode, setLayoutMode] = useState(() => localStorage.getItem(LAYOUT_STORAGE_KEY) || 'expanded')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [history, setHistory] = useState({})
  const [cardOrder, setCardOrder] = useState(() => {
    const raw = localStorage.getItem(ORDER_STORAGE_KEY)
    const defaultOrder = SERVICES.map((x) => x.key)
    if (!raw) return defaultOrder
    try {
      const parsed = JSON.parse(raw)
      if (!Array.isArray(parsed)) return defaultOrder
      const unique = [...new Set(parsed)]
      return [...unique, ...defaultOrder.filter((key) => !unique.includes(key))]
    } catch {
      return defaultOrder
    }
  })
  const [pinnedCards, setPinnedCards] = useState(() => {
    const raw = localStorage.getItem(PINNED_STORAGE_KEY)
    if (!raw) return {}
    try {
      return JSON.parse(raw)
    } catch {
      return {}
    }
  })
  const [widgets, setWidgets] = useState(() => {
    const raw = localStorage.getItem(WIDGET_STORAGE_KEY)
    if (!raw) return DEFAULT_WIDGETS
    try {
      return { ...DEFAULT_WIDGETS, ...JSON.parse(raw) }
    } catch {
      return DEFAULT_WIDGETS
    }
  })

  const activeServices = useMemo(
    () => SERVICES.filter((x) => selectedServices.includes(x.key)),
    [selectedServices],
  )

  useEffect(() => {
    localStorage.setItem(WIDGET_STORAGE_KEY, JSON.stringify(widgets))
  }, [widgets])

  useEffect(() => {
    localStorage.setItem(LAYOUT_STORAGE_KEY, layoutMode)
  }, [layoutMode])

  useEffect(() => {
    localStorage.setItem(ORDER_STORAGE_KEY, JSON.stringify(cardOrder))
  }, [cardOrder])

  useEffect(() => {
    localStorage.setItem(PINNED_STORAGE_KEY, JSON.stringify(pinnedCards))
  }, [pinnedCards])

  async function fetchServiceMetrics(showLoader = false) {
    if (showLoader) setIsLoading(true)
    setError('')

    try {
      const params = new URLSearchParams()
      activeServices.forEach((service) => params.append('services', service.key))
      const response = await api.get(`/api/monitoring/summary?${params.toString()}`)
      const metrics = response.data?.services || []

      setServiceMetrics(metrics)
      setSuccess(`Monitoring refreshed for ${metrics.length} services`)

      setHistory((prev) => {
        const next = { ...prev }
        for (const m of metrics) {
          const existing = next[m.key] || {
            heapMb: [],
            cpuPercent: [],
            dbConnections: [],
            requestCount: [],
          }

          next[m.key] = {
            heapMb: [...existing.heapMb, m.heapMb].slice(-LAST_N_POINTS),
            cpuPercent: [...existing.cpuPercent, m.cpuPercent].slice(-LAST_N_POINTS),
            dbConnections: [...existing.dbConnections, m.dbConnections].slice(-LAST_N_POINTS),
            requestCount: [...existing.requestCount, m.requestCount].slice(-LAST_N_POINTS),
          }
        }
        return next
      })

      setCardOrder((prev) => {
        const known = new Set(prev)
        const incoming = metrics.map((m) => m.key)
        const appended = [...prev, ...incoming.filter((key) => !known.has(key))]
        return appended.filter((key) => incoming.includes(key) || SERVICES.some((s) => s.key === key))
      })
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchServiceMetrics(true)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedServices])

  useEffect(() => {
    if (autoRefreshSec <= 0) return undefined
    const interval = window.setInterval(() => {
      fetchServiceMetrics(false)
    }, autoRefreshSec * 1000)
    return () => window.clearInterval(interval)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoRefreshSec, activeServices.length])

  function toggleService(key) {
    setSelectedServices((prev) =>
      prev.includes(key) ? prev.filter((x) => x !== key) : [...prev, key],
    )
  }

  function toggleWidget(key) {
    setWidgets((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  function togglePin(key) {
    setPinnedCards((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  function moveCard(key, direction) {
    setCardOrder((prev) => {
      const idx = prev.indexOf(key)
      if (idx < 0) return prev
      const targetIdx = direction === 'up' ? idx - 1 : idx + 1
      if (targetIdx < 0 || targetIdx >= prev.length) return prev

      const next = [...prev]
      ;[next[idx], next[targetIdx]] = [next[targetIdx], next[idx]]
      return next
    })
  }

  function exportSnapshot() {
    const payload = {
      exportedAt: new Date().toISOString(),
      selectedServices,
      autoRefreshSec,
      widgets,
      serviceMetrics,
      history,
    }
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `monitoring-snapshot-${Date.now()}.json`
    link.click()
    window.URL.revokeObjectURL(url)
  }

  const allUp = serviceMetrics.every((x) => x.status === 'UP')
  const orderedMetrics = [...serviceMetrics].sort((a, b) => {
    const pinA = pinnedCards[a.key] ? 1 : 0
    const pinB = pinnedCards[b.key] ? 1 : 0
    if (pinA !== pinB) return pinB - pinA

    const idxA = cardOrder.indexOf(a.key)
    const idxB = cardOrder.indexOf(b.key)
    const normalizedA = idxA === -1 ? Number.MAX_SAFE_INTEGER : idxA
    const normalizedB = idxB === -1 ? Number.MAX_SAFE_INTEGER : idxB
    return normalizedA - normalizedB
  })

  return (
    <div className="space-y-6">
      <SectionCard
        title="Admin Monitoring Dashboard"
        subtitle="Service health, runtime metrics, and customizable observability widgets for local operations."
        right={
          <div className="flex items-center gap-2">
            <a
              href={ZIPKIN_URL}
              target="_blank"
              rel="noreferrer"
              className="btn btn-secondary"
            >
              Open Zipkin Traces
            </a>
            <button
              type="button"
              onClick={exportSnapshot}
              className="btn btn-secondary"
            >
              Export Snapshot
            </button>
            <button
              type="button"
              onClick={() => fetchServiceMetrics(true)}
              className="btn btn-dark"
            >
              Refresh Now
            </button>
          </div>
        }
      >
        <ErrorMessage text={error} />
        <SuccessMessage text={success} />

        <div className="mt-4 grid gap-4 lg:grid-cols-3">
          <div className="tile">
            <p className="text-xs uppercase tracking-widest text-slate-500">Environment Health</p>
            <p className={`mt-2 text-xl font-black ${allUp ? 'text-emerald-700' : 'text-amber-700'}`}>
              {allUp ? 'ALL SYSTEMS UP' : 'DEGRADED'}
            </p>
            <p className="text-sm text-slate-600">{serviceMetrics.length} monitored services</p>
          </div>

          <div className="tile">
            <p className="text-xs uppercase tracking-widest text-slate-500">Auto Refresh</p>
            <select
              value={autoRefreshSec}
              onChange={(e) => setAutoRefreshSec(Number(e.target.value))}
              className="input mt-2"
            >
              <option value={0}>Paused</option>
              <option value={10}>10 seconds</option>
              <option value={20}>20 seconds</option>
              <option value={30}>30 seconds</option>
              <option value={60}>60 seconds</option>
            </select>
          </div>

          <div className="tile">
            <p className="text-xs uppercase tracking-widest text-slate-500">Widget Controls</p>
            <div className="mt-2 flex flex-wrap gap-2">
              {Object.keys(widgets).map((key) => (
                <button
                  key={key}
                  type="button"
                  onClick={() => toggleWidget(key)}
                  className={`rounded-full px-3 py-1 text-xs font-semibold ${
                    widgets[key]
                      ? 'bg-slate-900 text-white'
                      : 'border border-slate-300 bg-white text-slate-600'
                  }`}
                >
                  {key}
                </button>
              ))}
            </div>
            <div className="mt-3 flex items-center gap-2">
              <button
                type="button"
                onClick={() => setLayoutMode('compact')}
                className={`rounded-full px-3 py-1 text-xs font-semibold ${
                  layoutMode === 'compact'
                    ? 'bg-slate-900 text-white'
                    : 'border border-slate-300 bg-white text-slate-600'
                }`}
              >
                Compact
              </button>
              <button
                type="button"
                onClick={() => setLayoutMode('expanded')}
                className={`rounded-full px-3 py-1 text-xs font-semibold ${
                  layoutMode === 'expanded'
                    ? 'bg-slate-900 text-white'
                    : 'border border-slate-300 bg-white text-slate-600'
                }`}
              >
                Expanded
              </button>
            </div>
          </div>
        </div>

        <div className="item-card mt-4">
          <p className="text-sm font-semibold text-slate-700">Service Scope</p>
          <div className="mt-2 flex flex-wrap gap-2">
            {SERVICES.map((service) => (
              <button
                key={service.key}
                type="button"
                onClick={() => toggleService(service.key)}
                className={`rounded-full px-3 py-1 text-xs font-semibold ${
                  selectedServices.includes(service.key)
                    ? 'bg-cyan-600 text-white'
                    : 'border border-slate-300 bg-white text-slate-600'
                }`}
              >
                {service.label}
              </button>
            ))}
          </div>
        </div>
      </SectionCard>

      {isLoading ? <ListSkeleton rows={4} /> : null}

      {!isLoading && serviceMetrics.length === 0 ? (
        <EmptyState
          title="No service metrics selected"
          message="Select one or more services in Service Scope to begin monitoring."
        />
      ) : null}

      {!isLoading && serviceMetrics.length > 0 ? (
        <div className={`grid gap-4 ${layoutMode === 'compact' ? 'lg:grid-cols-3' : 'lg:grid-cols-2'}`}>
          {orderedMetrics.map((m) => (
            <SectionCard
              key={m.key}
              title={m.label}
              subtitle={`Status: ${m.status} • Last sample: ${new Date(m.sampledAt).toLocaleTimeString()}`}
              right={
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => togglePin(m.key)}
                    className={`rounded-full px-2 py-1 text-[10px] font-bold uppercase ${
                      pinnedCards[m.key]
                        ? 'bg-cyan-600 text-white'
                        : 'border border-slate-300 bg-white text-slate-600'
                    }`}
                  >
                    {pinnedCards[m.key] ? 'Pinned' : 'Pin'}
                  </button>
                  {layoutMode === 'expanded' ? (
                    <>
                      <button
                        type="button"
                        onClick={() => moveCard(m.key, 'up')}
                        className="badge badge-neutral"
                      >
                        Up
                      </button>
                      <button
                        type="button"
                        onClick={() => moveCard(m.key, 'down')}
                        className="badge badge-neutral"
                      >
                        Down
                      </button>
                    </>
                  ) : null}
                </div>
              }
            >
              <div className={`grid gap-3 ${layoutMode === 'compact' ? '' : 'sm:grid-cols-2'}`}>
                {widgets.heap ? (
                  <div className="item-card-compact">
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-xs uppercase tracking-widest text-slate-500">Heap Memory</p>
                      <MetricBadge value={m.heapMb} warning={700} critical={1024} />
                    </div>
                    <p className="mt-1 text-lg font-black text-slate-900">{m.heapMb} MB</p>
                    <Sparkline values={history[m.key]?.heapMb || []} colorClass="stroke-indigo-600" />
                  </div>
                ) : null}

                {widgets.cpu ? (
                  <div className="item-card-compact">
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-xs uppercase tracking-widest text-slate-500">CPU Usage</p>
                      <MetricBadge value={m.cpuPercent} warning={60} critical={80} />
                    </div>
                    <p className="mt-1 text-lg font-black text-slate-900">{m.cpuPercent}%</p>
                    <Sparkline values={history[m.key]?.cpuPercent || []} colorClass="stroke-emerald-600" />
                  </div>
                ) : null}

                {widgets.dbConnections ? (
                  <div className="item-card-compact">
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-xs uppercase tracking-widest text-slate-500">DB Connections</p>
                      <MetricBadge value={m.dbConnections} warning={15} critical={30} />
                    </div>
                    <p className="mt-1 text-lg font-black text-slate-900">{m.dbConnections}</p>
                    <Sparkline
                      values={history[m.key]?.dbConnections || []}
                      colorClass="stroke-amber-600"
                    />
                  </div>
                ) : null}

                {widgets.requests ? (
                  <div className="item-card-compact">
                    <p className="text-xs uppercase tracking-widest text-slate-500">Request Counter</p>
                    <p className="mt-1 text-lg font-black text-slate-900">{Math.round(m.requestCount)}</p>
                    <Sparkline
                      values={history[m.key]?.requestCount || []}
                      colorClass="stroke-cyan-600"
                    />
                  </div>
                ) : null}
              </div>

              {widgets.circuitBreaker ? (
                <div className="mt-3 grid gap-3 rounded-xl border border-slate-200 bg-slate-50 p-3 sm:grid-cols-3">
                  <div>
                    <p className="text-xs uppercase tracking-widest text-slate-500">Circuit Breakers Open</p>
                    <p className={`mt-1 text-lg font-black ${m.circuitOpen > 0 ? 'text-rose-700' : 'text-emerald-700'}`}>
                      {m.circuitOpen}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-widest text-slate-500">Live Threads</p>
                    <p className="mt-1 text-lg font-black text-slate-900">{Math.round(m.threads)}</p>
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-widest text-slate-500">GC Pauses</p>
                    <p className="mt-1 text-lg font-black text-slate-900">{Math.round(m.gcPauses)}</p>
                  </div>
                </div>
              ) : null}

              {widgets.rateLimiting && m.key === 'gateway' ? (
                <div className="item-card-compact mt-3">
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-xs uppercase tracking-widest text-slate-500">Rate Limit Rejections (429s)</p>
                    <MetricBadge value={m.rateLimitRejected} warning={1} critical={20} />
                  </div>
                  <p className="mt-1 text-lg font-black text-slate-900">{Math.round(m.rateLimitRejected)}</p>
                  <p className="mt-1 text-xs text-slate-500">
                    Cumulative since startup, across the login (5/min) and standard (100/min) buckets.
                  </p>
                </div>
              ) : null}
            </SectionCard>
          ))}
        </div>
      ) : null}
    </div>
  )
}
