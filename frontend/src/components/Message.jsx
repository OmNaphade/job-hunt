export function ErrorMessage({ text }) {
  if (!text) return null
  return (
    <p className="flex items-start gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm font-medium text-rose-700">
      <span aria-hidden className="mt-0.5">⚠</span>
      <span>{text}</span>
    </p>
  )
}

export function SuccessMessage({ text }) {
  if (!text) return null
  return (
    <p className="flex items-start gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm font-medium text-emerald-700">
      <span aria-hidden className="mt-0.5">✓</span>
      <span>{text}</span>
    </p>
  )
}
