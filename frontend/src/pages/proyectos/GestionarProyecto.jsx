import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { clientProjectsService } from '../../services/proyectoService.js'
import { PhasePanel, PhaseRequirementActions } from '../../components/PhasePanel.jsx'
import { ProjectRiskPanel } from '../../components/ProjectRiskPanel.jsx'
import { PredictiveObservationPanel } from '../../components/PredictiveObservationPanel.jsx'

const projectTypes = ['Residencial', 'Comercial', 'Industrial', 'Institucional', 'Infraestructura', 'Mixto', 'Ambiental', 'Energético']
const stateLabels = {
  PENDIENTE: 'Pendiente',
  EN_PROGRESO: 'En progreso',
  COMPLETADO: 'Completado',
  CANCELADO: 'Cancelado',
}
const validationLabels = {
  PENDIENTE: 'En verificación',
  APROBADO: 'Aprobado',
  RECHAZADO: 'Rechazado',
}

const currencyFormatter = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
})

const phaseDefinitions = [
  { id: 'PLANEACION', label: 'Fase 1 · Planeación', weight: 30, desc: 'Estudios, licencias, replanteo y cimentación inicial.' },
  { id: 'DESARROLLO', label: 'Fase 2 · Desarrollo', weight: 50, desc: 'Obra gris, mampostería, estructuras e instalaciones técnicas.' },
  { id: 'DETALLE_Y_ENTREGA_FINAL', label: 'Fase 3 · Detalles y entrega', weight: 20, desc: 'Acabados, carpintería, pintura, limpieza y entrega formal.' },
]

const validTabs = ['general', 'fases', 'pagos', 'equipo', 'documentos', 'riesgos']

const phaseLabel = (phase) =>
  phaseDefinitions.find((item) => item.id === phase)?.label || phase || 'En planeación'

function ProjectWorkspace({ id, editing }) {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const tabFromUrl = searchParams.get('tab')
  const activeTab = validTabs.includes(tabFromUrl) ? tabFromUrl : 'general'

  const [project, setProject] = useState(null)
  const [form, setForm] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [advancingPhase, setAdvancingPhase] = useState(false)
  const [updatingPhaseRequirements, setUpdatingPhaseRequirements] = useState(false)
  const [error, setError] = useState('')
  const [documentPreviewUrl, setDocumentPreviewUrl] = useState('')
  const [phaseStatus, setPhaseStatus] = useState(null)

  const changeTab = (tab) => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        next.set('tab', tab)
        return next
      },
      { replace: true }
    )
  }

  useEffect(() => {
    document.title = editing ? 'Editar Proyecto | ObraTech' : 'Detalles del Proyecto | ObraTech'
    let active = true

    const loadProject = async () => {
      try {
        const workspaceResponse = await clientProjectsService.getWorkspace(id)
        if (!active) return
        const projectData = workspaceResponse.data
        setProject(projectData)
        setForm({
          titulo: projectData.titulo || '',
          descripcion: projectData.descripcion || '',
          ubicacionGeografica: projectData.ubicacionGeografica || {
            departamento: '',
            municipio: '',
            detalle: projectData.ubicacion || '',
          },
          tipoProyecto: projectData.tipoProyecto || '',
          presupuesto: projectData.presupuesto ?? '',
          fechaInicio: projectData.fechaInicio || '',
          fechaEntrega: projectData.fechaEntrega || '',
          plazoEstimado: projectData.plazoEstimado ?? '',
          areaTotal: projectData.areaTotal ?? '',
          observaciones: projectData.observaciones || '',
        })

        const phaseResponse = await clientProjectsService.getPhaseStatus(id)
        if (active) setPhaseStatus(phaseResponse.data)
      } catch (requestError) {
        if (active) setError(requestError.response?.data?.error || 'No se pudo cargar el proyecto solicitado.')
      } finally {
        if (active) setLoading(false)
      }
    }

    loadProject()
    return () => {
      active = false
    }
  }, [id, editing])

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const updateLocation = (event) => {
    const { name, value } = event.target
    setForm((current) => ({
      ...current,
      ubicacionGeografica: { ...current.ubicacionGeografica, [name]: value },
    }))
  }

  const save = async (event) => {
    event.preventDefault()
    setError('')
    if (new Date(form.fechaEntrega) < new Date(form.fechaInicio)) {
      setError('La fecha de entrega no puede ser anterior a la de inicio.')
      return
    }
    setSaving(true)
    try {
      await clientProjectsService.updateMine(id, {
        ...form,
        presupuesto: Number(form.presupuesto),
        plazoEstimado: form.plazoEstimado ? Number(form.plazoEstimado) : null,
        areaTotal: form.areaTotal ? Number(form.areaTotal) : null,
      })
      navigate(`/proyectos/${id}`)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudieron guardar los cambios.')
    } finally {
      setSaving(false)
    }
  }

  const openLegalDocument = async (download) => {
    setError('')
    try {
      const { data } = await clientProjectsService.getLegalDocument(id, download)
      const url = URL.createObjectURL(data)
      if (download) {
        const anchor = document.createElement('a')
        anchor.href = url
        anchor.download = project.documentoLegalNombre || 'documento-legal'
        anchor.click()
        window.setTimeout(() => URL.revokeObjectURL(url), 60000)
      } else {
        setDocumentPreviewUrl(url)
      }
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo abrir el documento legal.')
    }
  }

  const closeLegalDocument = () => {
    URL.revokeObjectURL(documentPreviewUrl)
    setDocumentPreviewUrl('')
  }

  const advancePhase = async () => {
    if (!id) return
    setAdvancingPhase(true)
    setError('')
    try {
      const { data } = await clientProjectsService.advancePhase(id)
      setPhaseStatus(data)
      setProject((current) =>
        current ? { ...current, faseActual: data.faseActual, estadoFaseActual: data.estadoFaseActual } : current
      )
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo avanzar la fase de la obra.')
    } finally {
      setAdvancingPhase(false)
    }
  }

  const updatePhaseApproval = async (aprobada) => {
    setUpdatingPhaseRequirements(true)
    setError('')
    try {
      const { data } = await clientProjectsService.updatePhaseApproval(id, aprobada)
      setPhaseStatus(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo actualizar la aprobación de la fase.')
    } finally {
      setUpdatingPhaseRequirements(false)
    }
  }

  const registerPhaseInstallment = async () => {
    setUpdatingPhaseRequirements(true)
    setError('')
    try {
      const { data } = await clientProjectsService.registerPhaseInstallment(id)
      setPhaseStatus(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo registrar la cuota.')
    } finally {
      setUpdatingPhaseRequirements(false)
    }
  }

  if (loading) {
    return (
      <div className="project-page-shell">
        <p className="admin-loading" role="status">Cargando proyecto…</p>
      </div>
    )
  }

  if (!project || !form) {
    return (
      <div className="project-page-shell">
        <div className="admin-alert admin-alert-error" role="alert">{error || 'No se encontró el proyecto.'}</div>
        <Link className="button button-secondary project-back-link" to="/mis-proyectos">
          Volver a mis proyectos
        </Link>
      </div>
    )
  }

  return (
    <div className="project-page-shell">
      {/* ── Encabezado Principal ─────────────────────────────── */}
      <header className="project-page-header">
        <div>
          <p className="client-eyebrow">
            {editing ? 'Edición de obra' : 'Área de Cliente · Proyecto'}
          </p>
          <h1>{editing ? 'Editar proyecto' : project.titulo}</h1>
          <p>
            {editing
              ? 'Actualiza los datos, presupuesto y cronograma de esta obra.'
              : `${project.tipoProyecto} · ${project.ubicacion}`}
          </p>
        </div>
        <div className="project-page-header-actions">
          <Link className="button button-secondary" to="/mis-proyectos">
            <span className="material-symbols-outlined" aria-hidden="true">arrow_back</span>
            Mis proyectos
          </Link>
          {!editing && project.propietario && (
            <Link className="button button-secondary" to={`/proyectos/${id}/postulantes`}>
              <span className="material-symbols-outlined" aria-hidden="true">group</span>
              Postulantes ({project.totalPostulantes || 0})
            </Link>
          )}
          {!editing && project.propietario && (
            <Link className="button button-primary" to={`/proyectos/${id}/editar`}>
              <span className="material-symbols-outlined" aria-hidden="true">edit</span>
              Editar
            </Link>
          )}
        </div>
      </header>

      {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}

      {/* ── Formulario de Edición ────────────────────────────── */}
      {editing ? (
        <form className="publish-form project-edit-form" onSubmit={save}>
          <label className="publish-field publish-field-full"><span>Título del proyecto</span><input maxLength={120} name="titulo" onChange={updateField} required value={form.titulo} /></label>
          <label className="publish-field publish-field-full"><span>Descripción</span><textarea maxLength={4000} name="descripcion" onChange={updateField} required rows={5} value={form.descripcion} /></label>
          <label className="publish-field"><span>Ubicación</span><input maxLength={160} name="ubicacion" onChange={updateField} required value={form.ubicacion} /></label>
          <label className="publish-field"><span>Tipo de proyecto</span><select name="tipoProyecto" onChange={updateField} required value={form.tipoProyecto}><option value="">Selecciona un tipo…</option>{projectTypes.map((type) => <option key={type} value={type}>{type}</option>)}</select></label>
          <label className="publish-field"><span>Presupuesto (COP)</span><input min="1000000" name="presupuesto" onChange={updateField} required step="100000" type="number" value={form.presupuesto} /></label>
          <label className="publish-field"><span>Plazo estimado (meses)</span><input min="1" name="plazoEstimado" onChange={updateField} type="number" value={form.plazoEstimado} /></label>
          <label className="publish-field"><span>Fecha de inicio</span><input name="fechaInicio" onChange={updateField} required type="date" value={form.fechaInicio} /></label>
          <label className="publish-field"><span>Fecha de entrega</span><input min={form.fechaInicio || undefined} name="fechaEntrega" onChange={updateField} required type="date" value={form.fechaEntrega} /></label>
          <label className="publish-field"><span>Área total (m²)</span><input min="0" name="areaTotal" onChange={updateField} step="0.01" type="number" value={form.areaTotal} /></label>
          <label className="publish-field publish-field-full"><span>Observaciones del proyecto</span><textarea maxLength={2000} name="observaciones" onChange={updateField} rows={3} value={form.observaciones} /></label>
          <div className="publish-actions publish-field-full"><button className="button button-primary" disabled={saving} type="submit"><span className="material-symbols-outlined" aria-hidden="true">save</span>{saving ? 'Guardando…' : 'Guardar cambios'}</button><Link className="button button-secondary" to={`/proyectos/${id}`}>Cancelar</Link></div>
        </form>
      ) : (
        <main className="project-detail-grid">
          <section className="project-detail-main">
            <div className="project-detail-titlebar">
              <div><span className="project-list-type">{project.tipoProyecto}</span><h2>{project.titulo}</h2></div>
              <span className={`client-project-state state-${project.estadoValidacion === 'PENDIENTE' ? 'pendiente' : project.estadoEjecucion.toLowerCase()}`}>{validationLabels[project.estadoValidacion] || stateLabels[project.estadoEjecucion]}</span>
            </div>
            <section className="project-detail-section"><h3>Descripción del proyecto</h3><p>{project.descripcion}</p></section>
            <section className="project-spec-grid">
              <ProjectSpec label="Ubicación" value={project.ubicacion} icon="location_on" />
              <ProjectSpec label="Presupuesto" value={new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(project.presupuesto || 0)} icon="payments" />
              <ProjectSpec label="Fecha de inicio" value={project.fechaInicio || 'Pendiente'} icon="event" />
              <ProjectSpec label="Fecha de entrega" value={project.fechaEntrega || 'Pendiente'} icon="event_available" />
              <ProjectSpec label="Plazo estimado" value={project.plazoEstimado ? `${project.plazoEstimado} meses` : 'Sin definir'} icon="schedule" />
              <ProjectSpec label="Área total" value={project.areaTotal ? `${project.areaTotal} m²` : 'Sin definir'} icon="square_foot" />
            </section>
            {project.observaciones && <section className="project-detail-section"><h3>Observaciones</h3><p>{project.observaciones}</p></section>}

            {phaseStatus && (
              <>
                <PhasePanel
                  advancing={advancingPhase}
                  canAdvance={Boolean(project.propietario || project.administrador)}
                  onAdvance={advancePhase}
                  phaseStatus={phaseStatus}
                />
                <PhaseRequirementActions
                  canApprove={Boolean(project.propietario || project.administrador)}
                  onApprove={updatePhaseApproval}
                  onRegisterInstallment={registerPhaseInstallment}
                  pending={updatingPhaseRequirements}
                  phaseStatus={phaseStatus}
                />
              </>
            )}
            {project.indicadores && (
              <ProjectRiskPanel fechaEntrega={project.fechaEntrega} indicators={project.indicadores} />
            )}

            <section className="project-detail-section project-progress-section">
              <div><h3>Avance real</h3><strong>{Math.round(project.progresoProyecto)}%</strong></div>
              <progress aria-label="Avance real del proyecto" max="100" value={Math.min(100, Math.max(0, project.progresoProyecto))} />
              <p>{project.miembrosEquipo} trabajadores vinculados · {project.totalPostulantes} postulaciones recibidas</p>
            </section>
          </section>
        </div>
      )}
    </div>
  )
}

function ProjectSpec({ label, value, icon }) {
  return (
    <div className="project-spec">
      <span className="material-symbols-outlined" aria-hidden="true">{icon}</span>
      <div>
        <small>{label}</small>
        <strong>{value || '—'}</strong>
      </div>
    </div>
  )
}

export function DetalleProyecto({ id }) {
  return <ProjectWorkspace editing={false} id={id} />
}

export function EditarProyecto({ id }) {
  return <ProjectWorkspace editing id={id} />
}
