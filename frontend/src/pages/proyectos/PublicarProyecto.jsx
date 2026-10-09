import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { clientProjectsService } from '../../services/proyectoService.js'

const projectTypes = [
  { value: 'Residencial', icon: 'home' },
  { value: 'Comercial', icon: 'storefront' },
  { value: 'Industrial', icon: 'factory' },
  { value: 'Institucional', icon: 'school' },
  { value: 'Infraestructura', icon: 'traffic' },
]

const emptyForm = {
  titulo: '',
  descripcion: '',
  ubicacion: '',
  tipoProyecto: '',
  presupuesto: '',
  fechaInicio: '',
  fechaEntrega: '',
  plazoEstimado: '',
  areaTotal: '',
}

function PublicarProyectoForm() {
  const navigate = useNavigate()
  const [form, setForm] = useState(emptyForm)
  const [documentFile, setDocumentFile] = useState(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    document.title = 'Publicar Proyecto | ObraTech'
  }, [])

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    if (new Date(form.fechaEntrega) < new Date(form.fechaInicio)) {
      setError('La fecha de entrega no puede ser anterior a la de inicio.')
      return
    }

    setSubmitting(true)
    try {
      await clientProjectsService.createForCurrentClient({
        ...form,
        presupuesto: Number(form.presupuesto),
        plazoEstimado: form.plazoEstimado ? Number(form.plazoEstimado) : null,
        areaTotal: form.areaTotal ? Number(form.areaTotal) : null,
      }, documentFile)
      navigate('/mis-proyectos')
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo publicar el proyecto.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="publish-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación cliente" className="client-nav">
          <Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Resumen</Link>
          <Link to="/mis-proyectos"><span className="material-symbols-outlined" aria-hidden="true">folder_open</span>Mis proyectos</Link>
          <Link className="is-current" to="/proyectos/publicar"><span className="material-symbols-outlined" aria-hidden="true">add_circle</span>Publicar proyecto</Link>
        </nav>
      </aside>

      <main className="publish-main">
        <header className="publish-header">
          <div><p className="client-eyebrow">Área de cliente</p><h1>Publicar nuevo proyecto</h1><p>Completa los detalles de la obra que deseas realizar.</p></div>
          <Link className="button button-secondary" to="/"><span className="material-symbols-outlined" aria-hidden="true">arrow_back</span>Volver al resumen</Link>
        </header>

        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}

        <form className="publish-form" onSubmit={handleSubmit}>
          <label className="publish-field publish-field-full">
            <span>Título del proyecto</span>
            <input autoComplete="off" maxLength={120} name="titulo" onChange={updateField} placeholder="Ej. Construcción de edificio comercial" required value={form.titulo} />
          </label>

          <label className="publish-field publish-field-full">
            <span>Descripción</span>
            <textarea maxLength={4000} name="descripcion" onChange={updateField} placeholder="Describe los detalles y objetivos del proyecto…" required rows={5} value={form.descripcion} />
          </label>

          <label className="publish-field">
            <span>Ubicación</span>
            <input maxLength={160} name="ubicacion" onChange={updateField} placeholder="Ej. Cartagena, Bolívar" required value={form.ubicacion} />
          </label>

          <fieldset className="publish-field publish-field-full">
            <legend>Tipo de proyecto</legend>
            <div className="project-type-grid">
              {projectTypes.map((type) => (
                <label className={`project-type-option ${form.tipoProyecto === type.value ? 'is-selected' : ''}`} key={type.value}>
                  <input checked={form.tipoProyecto === type.value} name="tipoProyecto" onChange={updateField} required type="radio" value={type.value} />
                  <span className="material-symbols-outlined" aria-hidden="true">{type.icon}</span>
                  <strong>{type.value}</strong>
                </label>
              ))}
            </div>
          </fieldset>

          <label className="publish-field">
            <span>Presupuesto estimado (COP)</span>
            <input min="1000000" name="presupuesto" onChange={updateField} placeholder="Ej. 50000000" required step="100000" type="number" value={form.presupuesto} />
            <small>Mínimo: $1.000.000 COP</small>
          </label>

          <label className="publish-field">
            <span>Plazo estimado (meses)</span>
            <input min="1" name="plazoEstimado" onChange={updateField} placeholder="Ej. 6" type="number" value={form.plazoEstimado} />
          </label>

          <label className="publish-field">
            <span>Fecha de inicio</span>
            <input name="fechaInicio" onChange={updateField} required type="date" value={form.fechaInicio} />
          </label>

          <label className="publish-field">
            <span>Fecha de entrega</span>
            <input min={form.fechaInicio || undefined} name="fechaEntrega" onChange={updateField} required type="date" value={form.fechaEntrega} />
          </label>

          <label className="publish-field">
            <span>Área total (m²)</span>
            <input min="0" name="areaTotal" onChange={updateField} placeholder="Ej. 5000" step="0.01" type="number" value={form.areaTotal} />
          </label>

          <label className="publish-document publish-field-full">
            <span className="material-symbols-outlined" aria-hidden="true">description</span>
            <span className="publish-document-copy"><strong>Documento legal</strong><small>{documentFile ? documentFile.name : 'PDF, DOC o DOCX, hasta 50 MB (opcional)'}</small></span>
            <span className="publish-document-action">Seleccionar archivo</span>
            <input accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document" onChange={(event) => setDocumentFile(event.target.files?.[0] || null)} type="file" />
          </label>

          <div className="publish-actions publish-field-full">
            <button className="button button-primary" disabled={submitting} type="submit">
              <span className="material-symbols-outlined" aria-hidden="true">publish</span>
              {submitting ? 'Publicando…' : 'Publicar proyecto'}
            </button>
            <Link className="button button-secondary" to="/">Cancelar</Link>
          </div>
        </form>
      </main>
    </div>
  )
}

export function PublicarProyecto() {
  return <PublicarProyectoForm />
}