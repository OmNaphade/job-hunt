export default function SectionCard({ id, title, subtitle, children, right }) {
  return (
    <section id={id} className="card scroll-mt-24 p-5 md:p-6">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3 border-b border-slate-100 pb-4">
        <div className="flex items-start gap-3">
          <span className="mt-1 h-8 w-1.5 shrink-0 rounded-full bg-gradient-to-b from-indigo-500 to-violet-600" />
          <div>
            <h2 className="text-lg font-extrabold tracking-tight text-slate-900 md:text-xl">{title}</h2>
            {subtitle ? <p className="mt-1 text-sm text-slate-500">{subtitle}</p> : null}
          </div>
        </div>
        {right ? <div className="flex flex-wrap items-center gap-2">{right}</div> : null}
      </div>
      {children}
    </section>
  )
}
