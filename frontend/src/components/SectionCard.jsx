export default function SectionCard({ title, subtitle, children, right }) {
  return (
    <section className="rounded-3xl border border-slate-200/90 bg-white/95 p-5 shadow-[0_8px_24px_rgba(15,23,42,0.06)] backdrop-blur">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-xl font-extrabold tracking-tight text-slate-900 md:text-[1.32rem]">{title}</h2>
          {subtitle ? <p className="mt-1 text-sm text-slate-600">{subtitle}</p> : null}
        </div>
        {right}
      </div>
      {children}
    </section>
  )
}
