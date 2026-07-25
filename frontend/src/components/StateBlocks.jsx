export function ListSkeleton({ rows = 3 }) {
  const skeletonKeys = Array.from({ length: rows }, () => crypto.randomUUID())

  return (
    <div className="mt-4 grid gap-3">
      {skeletonKeys.map((key) => (
        <div key={key} className="rounded-2xl border border-slate-200 p-4">
          <div className="h-4 w-2/5 animate-pulse rounded bg-slate-200" />
          <div className="mt-3 h-3 w-4/5 animate-pulse rounded bg-slate-100" />
          <div className="mt-2 h-3 w-3/5 animate-pulse rounded bg-slate-100" />
        </div>
      ))}
    </div>
  )
}

export function EmptyState({ title, message }) {
  return (
    <div className="mt-4 rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-4 py-6 text-center">
      <p className="text-sm font-semibold text-slate-700">{title}</p>
      <p className="mt-1 text-sm text-slate-500">{message}</p>
    </div>
  )
}
