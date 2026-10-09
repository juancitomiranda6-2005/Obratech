export function DocumentPreview({ title = 'Vista previa', url = '#' }) {
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-4">
      <p className="mb-3 text-sm uppercase tracking-[0.2em] text-slate-400">{title}</p>
      <iframe title={title} src={url} className="h-72 w-full rounded-xl border border-slate-700 bg-slate-950" />
    </div>
  )
}
