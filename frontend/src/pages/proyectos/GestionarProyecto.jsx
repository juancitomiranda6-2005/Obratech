import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { clientProjectsService } from '../../services/proyectoService.js'

const projectTypes = ['Residencial', 'Comercial', 'Industrial', 'Institucional', 'Infraestructura']
const stateLabels = { PENDIENTE: 'Pendiente', EN_PROGRESO: 'En progreso', COMPLETADO: 'Completado', CANCELADO: 'Cancelado' }
const validationLabels = { PENDIENTE: 'En verificación', APROBADO: 'Aprobado', RECHAZADO: 'Rechazado' }

function ProjectWorkspace({ id, editing }) {
  const navigate = useNavigate()
  const [project, setProject] = useState(null)
  const [form, setForm] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [documentPreviewUrl, setDocumentPreviewUrl] = useState('')

  useEffect(() => {
    document.title = editing ? 'Editar Proyecto | ObraTech' : 'Detalles del Proyecto | ObraTech'
    let active = true
    clientProjectsService.getWorkspace(id)
      .then(({ data }) => {
        if (!active) return
        setProject(data)
        setForm({
          titulo: data.titulo || '',
          descripcion: data.descripcion || '',
          ubicacion: data.ubicacion || '',
          tipoProyecto: data.tipoProyecto || '',
          presupuesto: data.presupuesto ?? '',
          fechaInicio: data.fechaInicio || '',
          fechaEntrega: data.fechaEntrega || '',
          plazoEstimado: data.plazoEstimado ?? '',
          areaTotal: data.areaTotal ?? '',
          observaciones: data.observaciones || '',
        })
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudo cargar el proyecto solicitado.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id, editing])

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
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

  if (loading) {
    return <div className="project-page-shell"><p className="admin-loading" role="status">Cargando proyecto…</p></div>
  }

  if (!project || !form) {
    return (
      <div className="project-page-shell">
        <div className="admin-alert admin-alert-error" role="alert">{error || 'No se encontró el proyecto.'}</div>
        <Link className="button button-secondary project-back-link" to="/mis-proyectos">Volver a mis proyectos</Link>
      </div>
    )
  }

  return (
    <div className="project-page-shell">
      <header className="project-page-header">
        <div><p className="client-eyebrow">{editing ? 'Edición' : 'Detalle'}</p><h1>{editing ? 'Editar proyecto' : project.titulo}</h1><p>{editing ? 'Actualiza los datos de esta obra.' : `${project.tipoProyecto} · ${project.ubicacion}`}</p></div>
        <Link className="button button-secondary" to="/mis-proyectos"><span className="material-symbols-outlined" aria-hidden="true">arrow_back</span>Mis proyectos</Link>
      </header>

      {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}

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
            <section className="project-detail-section project-progress-section">
              <div><h3>Avance real</h3><strong>{Math.round(project.progresoProyecto)}%</strong></div>
              <progress aria-label="Avance real del proyecto" max="100" value={Math.min(100, Math.max(0, project.progresoProyecto))} />
              <p>{project.miembrosEquipo} trabajadores vinculados · {project.totalPostulantes} postulaciones recibidas</p>
            </section>
          </section>
          <aside className="project-detail-side">
            {project.contratista && <section className="project-detail-section">
              <p className="client-eyebrow">Contratista asignado</p>
              <h2>{project.contratista.nombre}</h2>
              <p>{project.contratista.especialidad || 'Sin especialidad'} · {project.contratista.calificacionPromedio.toFixed(1)} / 5</p>
              <Link className="profile-open-link" to={`/contratistas/${project.contratista.id}`}>Ver perfil<span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span></Link>
              {project.propietario && <Link className="button button-secondary" to={`/calificaciones/crear/${id}/${project.contratista.id}`}><span className="material-symbols-outlined" aria-hidden="true">star</span>Calificar</Link>}
            </section>}
            <section className="project-detail-section">
              <p className="client-eyebrow">Pool de trabajadores</p><h2>{project.poolTrabajadores.length} disponibles</h2>
              {project.poolTrabajadores.length ? <ul className="project-workforce-list">{project.poolTrabajadores.map((worker) => <li key={worker.id}><Link to={`/trabajadores/${worker.id}`}>{worker.nombre}</Link><span>{worker.oficio || 'Oficio no registrado'}</span></li>)}</ul> : <p>Aún no hay trabajadores en el pool del proyecto.</p>}
            </section>
            <section className="project-detail-section">
              <p className="client-eyebrow">Equipos de trabajo</p><h2>{project.equipos.length} equipos</h2>
              {project.equipos.length ? <ul className="project-workforce-list">{project.equipos.map((team) => <li key={team.id}><div><strong>{team.nombre}</strong><span>{team.actividad}</span><small>{team.integrantes.map((member) => member.nombre).join(', ') || 'Sin integrantes'}</small></div><strong>{Math.round(team.porcentajeAvance)}%</strong></li>)}</ul> : <p>No se han creado equipos para este proyecto.</p>}
              {project.contratistaAsignado && <Link className="profile-open-link" to="/contratistas/mi-equipo">Gestionar equipos<span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span></Link>}
            </section>
            {project.documentoLegalUrl && <section className="project-detail-section">
              <p className="client-eyebrow">Documentación</p><h2>{project.documentoLegalNombre || 'Documento legal'}</h2>
              <div className="project-document-actions"><button className="button button-secondary" disabled={saving} onClick={() => openLegalDocument(false)} type="button"><span className="material-symbols-outlined" aria-hidden="true">visibility</span>Previsualizar</button><button className="button button-secondary" disabled={saving} onClick={() => openLegalDocument(true)} type="button"><span className="material-symbols-outlined" aria-hidden="true">download</span>Descargar</button></div>
            </section>}
            {project.propietario && <Link className="button button-secondary project-applications-link" to={`/proyectos/${id}/postulantes`}><span className="material-symbols-outlined" aria-hidden="true">group</span>Ver postulantes</Link>}
            {project.propietario && <Link className="button button-primary" to={`/proyectos/${id}/editar`}><span className="material-symbols-outlined" aria-hidden="true">edit</span>Editar proyecto</Link>}
          </aside>
        </main>
      )}
      {documentPreviewUrl && <div className="project-preview-overlay" role="presentation"><section aria-label="Previsualización del documento legal" aria-modal="true" className="project-preview-dialog" role="dialog"><header><h2>{project.documentoLegalNombre || 'Documento legal'}</h2><button aria-label="Cerrar previsualización" className="admin-icon-button" onClick={closeLegalDocument} type="button"><span className="material-symbols-outlined" aria-hidden="true">close</span></button></header><iframe title="Documento legal del proyecto" src={documentPreviewUrl} /></section></div>}
    </div>
  )
}

function ProjectSpec({ label, value, icon }) {
  return <div className="project-spec"><span className="material-symbols-outlined" aria-hidden="true">{icon}</span><div><small>{label}</small><strong>{value || '—'}</strong></div></div>
}

export function DetalleProyecto({ id }) {
  return <ProjectWorkspace id={id} editing={false} />
}

export function EditarProyecto({ id }) {
  return <ProjectWorkspace id={id} editing />
}