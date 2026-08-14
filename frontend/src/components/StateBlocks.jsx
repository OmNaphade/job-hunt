export function ListSkeleton({ rows = 3 }) {
  const skeletonKeys = Array.from({ length: rows }, () => crypto.randomUUID())

  return (
    <div className="mt-4 grid gap-3">
      {skeletonKeys.map((key) => (
        <div key={key} className="item-card overflow-hidden">
          <div className="skeleton h-4 w-2/5 rounded-full" />
          <div className="skeleton mt-3 h-3 w-4/5 rounded-full" />
          <div className="skeleton mt-2 h-3 w-3/5 rounded-full" />
        </div>
      ))}
    </div>
  )
}

export function EmptyState({ title, message }) {
  return (
    <div className="mt-4 rounded-2xl border border-dashed border-slate-300 bg-slate-50/70 px-4 py-8 text-center">
      <div className="mx-auto mb-3 flex h-10 w-10 items-center justify-center rounded-full bg-slate-200/80 text-slate-500">
        <svg viewBox="0 0 24 24" fill="none" className="h-5 w-5">
          <path
            d="M4 7h16M4 12h16M4 17h10"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeDasharray="1 4"
          />
        </svg>
      </div>
      <p className="text-sm font-semibold text-slate-700">{title}</p>
      <p className="mt-1 text-sm text-slate-500">{message}</p>
    </div>
  )
}
