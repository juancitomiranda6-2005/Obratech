import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { clientProjectsService } from '../../services/proyectoService.js'
import { PhasePanel, PhaseRequirementActions } from '../../components/PhasePanel.jsx'
import { ProjectRiskPanel } from '../../components/ProjectRiskPanel.jsx'

const projectTypes = ['Residencial', 'Comercial', 'Industrial', 'Institucional', 'Infraestructura']
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
          ubicacion: projectData.ubicacion || '',
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
          <label className="publish-field publish-field-full">
            <span>Título del proyecto</span>
            <input maxLength={120} name="titulo" onChange={updateField} required value={form.titulo} />
          </label>
          <label className="publish-field publish-field-full">
            <span>Descripción</span>
            <textarea maxLength={4000} name="descripcion" onChange={updateField} required rows={5} value={form.descripcion} />
          </label>
          <label className="publish-field">
            <span>Ubicación</span>
            <input maxLength={160} name="ubicacion" onChange={updateField} required value={form.ubicacion} />
          </label>
          <label className="publish-field">
            <span>Tipo de proyecto</span>
            <select name="tipoProyecto" onChange={updateField} required value={form.tipoProyecto}>
              <option value="">Selecciona un tipo…</option>
              {projectTypes.map((type) => (
                <option key={type} value={type}>{type}</option>
              ))}
            </select>
          </label>
          <label className="publish-field">
            <span>Presupuesto (COP)</span>
            <input min="1000000" name="presupuesto" onChange={updateField} required step="100000" type="number" value={form.presupuesto} />
          </label>
          <label className="publish-field">
            <span>Plazo estimado (meses)</span>
            <input min="1" name="plazoEstimado" onChange={updateField} type="number" value={form.plazoEstimado} />
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
            <input min="0" name="areaTotal" onChange={updateField} step="0.01" type="number" value={form.areaTotal} />
          </label>
          <label className="publish-field publish-field-full">
            <span>Observaciones del proyecto</span>
            <textarea maxLength={2000} name="observaciones" onChange={updateField} rows={3} value={form.observaciones} />
          </label>
          <div className="publish-actions publish-field-full">
            <button className="button button-primary" disabled={saving} type="submit">
              <span className="material-symbols-outlined" aria-hidden="true">save</span>
              {saving ? 'Guardando…' : 'Guardar cambios'}
            </button>
            <Link className="button button-secondary" to={`/proyectos/${id}`}>
              Cancelar
            </Link>
          </div>
        </form>
      ) : (
        /* ── Vista Detalle Modular con Pestañas ─────────────── */
        <div className="project-detail-tabs-wrap">
          {/* Navegación por pestañas */}
          <nav aria-label="Secciones del proyecto" className="contractor-follow-up-tabs">
            <button
              className={activeTab === 'general' ? 'is-active' : ''}
              onClick={() => changeTab('general')}
              type="button"
            >
              <span className="material-symbols-outlined" aria-hidden="true">info</span>
              <span>General</span>
            </button>

            <button
              className={activeTab === 'fases' ? 'is-active' : ''}
              onClick={() => changeTab('fases')}
              type="button"
            >
              <span className="material-symbols-outlined" aria-hidden="true">view_timeline</span>
              <span>Fases (30/50/20%)</span>
              {phaseStatus && (
                <span className="project-tab-badge">
                  {Math.round(phaseStatus.avanceObra || 0)}%
                </span>
              )}
            </button>

            <button
              className={activeTab === 'pagos' ? 'is-active' : ''}
              onClick={() => changeTab('pagos')}
              type="button"
            >
              <span className="material-symbols-outlined" aria-hidden="true">payments</span>
              <span>Pagos y Finanzas</span>
              {phaseStatus?.cuotasEsperadasEnFase > 0 && (
                <span className="project-tab-badge">
                  {phaseStatus.cuotasPagadasEnFase || 0}/{phaseStatus.cuotasEsperadasEnFase}
                </span>
              )}
            </button>

            <button
              className={activeTab === 'equipo' ? 'is-active' : ''}
              onClick={() => changeTab('equipo')}
              type="button"
            >
              <span className="material-symbols-outlined" aria-hidden="true">groups</span>
              <span>Equipo</span>
              {(project.equipos?.length > 0 || project.poolTrabajadores?.length > 0) && (
                <span className="project-tab-badge">
                  {(project.equipos?.length || 0) + (project.poolTrabajadores?.length || 0)}
                </span>
              )}
            </button>

            <button
              className={activeTab === 'documentos' ? 'is-active' : ''}
              onClick={() => changeTab('documentos')}
              type="button"
            >
              <span className="material-symbols-outlined" aria-hidden="true">description</span>
              <span>Documentos</span>
              {project.documentoLegalUrl && <span className="project-tab-badge">1</span>}
            </button>

            <button
              className={activeTab === 'riesgos' ? 'is-active' : ''}
              onClick={() => changeTab('riesgos')}
              type="button"
            >
              <span className="material-symbols-outlined" aria-hidden="true">warning</span>
              <span>Riesgos</span>
              {project.indicadores?.riesgo?.score != null && (
                <span className="project-tab-badge">
                  {Math.round(project.indicadores.riesgo.score)}%
                </span>
              )}
            </button>
          </nav>

          <div className="project-tab-content">
            {/* ── Pestaña 1: GENERAL ──────────────────────────── */}
            {activeTab === 'general' && (
              <main className="project-detail-grid">
                <section className="project-detail-main">
                  <div className="project-detail-titlebar">
                    <div>
                      <span className="project-list-type">{project.tipoProyecto}</span>
                      <h2>{project.titulo}</h2>
                    </div>
                    <span
                      className={`client-project-state state-${
                        project.estadoValidacion === 'PENDIENTE'
                          ? 'pendiente'
                          : (project.estadoEjecucion || 'pendiente').toLowerCase()
                      }`}
                    >
                      {validationLabels[project.estadoValidacion] ||
                        stateLabels[project.estadoEjecucion] ||
                        project.estadoEjecucion}
                    </span>
                  </div>

                  <section className="project-detail-section">
                    <h3>Descripción del proyecto</h3>
                    <p>{project.descripcion}</p>
                  </section>

                  <section className="project-spec-grid">
                    <ProjectSpec icon="location_on" label="Ubicación" value={project.ubicacion} />
                    <ProjectSpec
                      icon="payments"
                      label="Presupuesto"
                      value={currencyFormatter.format(project.presupuesto || 0)}
                    />
                    <ProjectSpec icon="event" label="Fecha de inicio" value={project.fechaInicio || 'Pendiente'} />
                    <ProjectSpec icon="event_available" label="Fecha de entrega" value={project.fechaEntrega || 'Pendiente'} />
                    <ProjectSpec
                      icon="schedule"
                      label="Plazo estimado"
                      value={project.plazoEstimado ? `${project.plazoEstimado} meses` : 'Sin definir'}
                    />
                    <ProjectSpec
                      icon="square_foot"
                      label="Área total"
                      value={project.areaTotal ? `${project.areaTotal} m²` : 'Sin definir'}
                    />
                  </section>

                  {project.observaciones && (
                    <section className="project-detail-section">
                      <h3>Observaciones y especificaciones técnicas</h3>
                      <p>{project.observaciones}</p>
                    </section>
                  )}

                  <section className="project-detail-section project-progress-section">
                    <div>
                      <h3>Avance real consolidado</h3>
                      <strong>{Math.round(project.progresoProyecto || 0)}%</strong>
                    </div>
                    <progress
                      aria-label="Avance real del proyecto"
                      max="100"
                      value={Math.min(100, Math.max(0, project.progresoProyecto || 0))}
                    />
                    <p>
                      {project.miembrosEquipo || 0} trabajadores vinculados · {project.totalPostulantes || 0} postulaciones recibidas
                    </p>
                  </section>

                  {/* Resumen del modelo de fases 30/50/20 */}
                  <section className="project-detail-section">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                      <h3>Progreso del modelo de fases (30 / 50 / 20%)</h3>
                      <button
                        className="button button-secondary"
                        onClick={() => changeTab('fases')}
                        style={{ fontSize: 10, minHeight: 30, padding: '0 10px' }}
                        type="button"
                      >
                        Ver fases detalladas
                        <span className="material-symbols-outlined" style={{ fontSize: 14 }} aria-hidden="true">arrow_forward</span>
                      </button>
                    </div>
                    <div className="project-phases-explainer">
                      {phaseDefinitions.map((def, idx) => {
                        const isCurrent = phaseStatus?.faseActual === def.id
                        return (
                          <div className={`project-phase-step-card ${isCurrent ? 'is-current' : ''}`} key={def.id}>
                            <span className="step-number">Etapa {idx + 1} {isCurrent && '· Activa'}</span>
                            <h4>{def.label}</h4>
                            <p>{def.desc}</p>
                            <span className="step-weight">{def.weight}% del total</span>
                          </div>
                        )
                      })}
                    </div>
                  </section>
                </section>

                <aside className="project-detail-side">
                  {project.contratista ? (
                    <section className="project-detail-section">
                      <p className="client-eyebrow">Contratista asignado</p>
                      <h2>{project.contratista.nombre}</h2>
                      <p>
                        {project.contratista.especialidad || 'Especialidad general'} · ★{' '}
                        {project.contratista.calificacionPromedio
                          ? project.contratista.calificacionPromedio.toFixed(1)
                          : '5.0'}{' '}
                        / 5
                      </p>
                      <Link className="profile-open-link" to={`/contratistas/${project.contratista.id}`}>
                        Ver perfil
                        <span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
                      </Link>
                      {project.propietario && (
                        <Link
                          className="button button-secondary"
                          style={{ marginTop: 8 }}
                          to={`/calificaciones/crear/${id}/${project.contratista.id}`}
                        >
                          <span className="material-symbols-outlined" aria-hidden="true">star</span>
                          Calificar contratista
                        </Link>
                      )}
                    </section>
                  ) : (
                    <section className="project-detail-section">
                      <p className="client-eyebrow">Equipo en obra</p>
                      <h2>Sin contratista asignado</h2>
                      <p>Aún no has seleccionado un contratista para liderar esta obra.</p>
                      {project.propietario && (
                        <Link className="button button-primary" to={`/proyectos/${id}/postulantes`}>
                          <span className="material-symbols-outlined" aria-hidden="true">group</span>
                          Ver postulantes ({project.totalPostulantes || 0})
                        </Link>
                      )}
                    </section>
                  )}

                  {project.documentoLegalUrl && (
                    <section className="project-detail-section">
                      <p className="client-eyebrow">Documentación</p>
                      <h2>{project.documentoLegalNombre || 'Documento legal'}</h2>
                      <div className="project-document-actions">
                        <button className="button button-secondary" onClick={() => openLegalDocument(false)} type="button">
                          <span className="material-symbols-outlined" aria-hidden="true">visibility</span>
                          Previsualizar
                        </button>
                        <button className="button button-secondary" onClick={() => openLegalDocument(true)} type="button">
                          <span className="material-symbols-outlined" aria-hidden="true">download</span>
                          Descargar
                        </button>
                      </div>
                    </section>
                  )}

                  {project.propietario && (
                    <Link className="button button-secondary project-applications-link" to={`/proyectos/${id}/postulantes`}>
                      <span className="material-symbols-outlined" aria-hidden="true">group</span>
                      Ver postulantes ({project.totalPostulantes || 0})
                    </Link>
                  )}
                  {project.propietario && (
                    <Link className="button button-primary" to={`/proyectos/${id}/editar`}>
                      <span className="material-symbols-outlined" aria-hidden="true">edit</span>
                      Editar proyecto
                    </Link>
                  )}
                </aside>
              </main>
            )}

            {/* ── Pestaña 2: FASES (30 / 50 / 20%) ─────────────── */}
            {activeTab === 'fases' && (
              <div className="project-tab-single">
                <div className="project-phases-explainer">
                  {phaseDefinitions.map((def, idx) => {
                    const isCurrent = phaseStatus?.faseActual === def.id
                    return (
                      <div className={`project-phase-step-card ${isCurrent ? 'is-current' : ''}`} key={def.id}>
                        <span className="step-number">Etapa {idx + 1} {isCurrent && '· Activa'}</span>
                        <h4>{def.label}</h4>
                        <p>{def.desc}</p>
                        <span className="step-weight">{def.weight}% del presupuesto</span>
                      </div>
                    )
                  })}
                </div>

                {phaseStatus ? (
                  <>
                    <PhasePanel
                      advancing={advancingPhase}
                      canAdvance={Boolean(project.propietario || project.administrador)}
                      onAdvance={advancePhase}
                      phaseStatus={phaseStatus}
                      sections={['summary', 'phases']}
                    />
                    <PhaseRequirementActions
                      canApprove={Boolean(project.propietario || project.administrador)}
                      onApprove={updatePhaseApproval}
                      onRegisterInstallment={registerPhaseInstallment}
                      pending={updatingPhaseRequirements}
                      phaseStatus={phaseStatus}
                    />
                  </>
                ) : (
                  <div className="project-info-card">
                    <p>Cargando información de fases…</p>
                  </div>
                )}
              </div>
            )}

            {/* ── Pestaña 3: PAGOS Y FINANZAS ─────────────────── */}
            {activeTab === 'pagos' && (
              <div className="project-tab-single">
                <div className="project-payments-overview">
                  <div className="project-payment-kpi">
                    <small>Presupuesto Total</small>
                    <strong>{currencyFormatter.format(project.presupuesto || 0)}</strong>
                    <span>Valor total pactado</span>
                  </div>
                  <div className="project-payment-kpi">
                    <small>Fase Activa</small>
                    <strong style={{ fontSize: 16 }}>{phaseLabel(phaseStatus?.faseActual)}</strong>
                    <span>{phaseStatus?.estadoFaseActual || 'En ejecución'}</span>
                  </div>
                  <div className="project-payment-kpi">
                    <small>Cuotas en Fase</small>
                    <strong>
                      {phaseStatus?.cuotasPagadasEnFase || 0} / {phaseStatus?.cuotasEsperadasEnFase || 0}
                    </strong>
                    <span>Semanas liquidadas</span>
                  </div>
                </div>

                {phaseStatus ? (
                  <>
                    <PhasePanel phaseStatus={phaseStatus} sections={['payments']} />

                    {project.propietario && (
                      <div className="project-info-card" style={{ marginTop: 12 }}>
                        <h3>Registro de cuotas semanales</h3>
                        <p style={{ marginBottom: 14 }}>
                          Como propietario del proyecto, puedes registrar cada cuota semanal entregada al contratista para mantener la bitácora financiera sincronizada con el cronograma de obra.
                        </p>
                        <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
                          <button
                            className="button button-primary"
                            disabled={updatingPhaseRequirements || !phaseStatus.puedeRegistrarCuota}
                            onClick={registerPhaseInstallment}
                            type="button"
                          >
                            <span className="material-symbols-outlined" aria-hidden="true">check_circle</span>
                            {updatingPhaseRequirements
                              ? 'Registrando…'
                              : `Confirmar cuota semanal #${(phaseStatus.cuotasPagadasEnFase || 0) + 1}`}
                          </button>
                          {!phaseStatus.puedeRegistrarCuota && (
                            <span style={{ fontSize: 11, color: 'var(--ot-muted)' }}>
                              Se han registrado todas las cuotas previstas para la fase actual.
                            </span>
                          )}
                        </div>
                      </div>
                    )}
                  </>
                ) : (
                  <div className="project-info-card">
                    <p>Cargando plan de pagos…</p>
                  </div>
                )}

                <div className="project-disclaimer-note">
                  <span className="material-symbols-outlined" aria-hidden="true">verified_user</span>
                  <div>
                    <strong>Bitácora de pagos y trazabilidad ObraTech</strong>
                    <p>
                      ObraTech no procesa transacciones bancarias directas. El panel de cuotas semanales sirve como registro transparente y mutuo entre cliente y contratista para auditar el cumplimiento financiero de cada etapa.
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* ── Pestaña 4: EQUIPO ───────────────────────────── */}
            {activeTab === 'equipo' && (
              <div className="project-tab-single">
                <div className="project-info-card">
                  <p className="client-eyebrow">Líder técnico del proyecto</p>
                  {project.contratista ? (
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap', marginTop: 8 }}>
                      <div>
                        <h3 style={{ margin: 0, fontSize: 18 }}>{project.contratista.nombre}</h3>
                        <p style={{ margin: '4px 0 0' }}>
                          {project.contratista.especialidad || 'Especialidad general'} · Calificación ★{' '}
                          {project.contratista.calificacionPromedio ? project.contratista.calificacionPromedio.toFixed(1) : '5.0'} / 5
                        </p>
                      </div>
                      <div style={{ display: 'flex', gap: 8 }}>
                        <Link className="button button-secondary" to={`/contratistas/${project.contratista.id}`}>
                          Ver perfil
                        </Link>
                        {project.propietario && (
                          <Link className="button button-secondary" to={`/calificaciones/crear/${id}/${project.contratista.id}`}>
                            <span className="material-symbols-outlined" aria-hidden="true">star</span>
                            Calificar
                          </Link>
                        )}
                      </div>
                    </div>
                  ) : (
                    <div style={{ marginTop: 8 }}>
                      <h3>Sin contratista asignado</h3>
                      <p>Evalúa las postulaciones recibidas para asignar al contratista líder de la obra.</p>
                      {project.propietario && (
                        <Link className="button button-primary" style={{ marginTop: 12, width: 'fit-content' }} to={`/proyectos/${id}/postulantes`}>
                          <span className="material-symbols-outlined" aria-hidden="true">group</span>
                          Ver postulantes ({project.totalPostulantes || 0})
                        </Link>
                      )}
                    </div>
                  )}
                </div>

                <div className="project-info-card">
                  <h3>Equipos de trabajo en obra ({project.equipos?.length || 0})</h3>
                  {project.equipos?.length ? (
                    <ul className="project-workforce-list" style={{ marginTop: 12 }}>
                      {project.equipos.map((team) => (
                        <li key={team.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid var(--ot-line)' }}>
                          <div>
                            <strong>{team.nombre}</strong>
                            <div style={{ fontSize: 11, color: 'var(--ot-muted)' }}>{team.actividad}</div>
                            <small style={{ fontSize: 10, color: 'var(--ot-muted)' }}>
                              Integrantes: {team.integrantes?.map((m) => m.nombre).join(', ') || 'Sin integrantes asignados'}
                            </small>
                          </div>
                          <strong style={{ color: 'var(--ot-orange-text)', fontSize: 13 }}>
                            {Math.round(team.porcentajeAvance || 0)}%
                          </strong>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p style={{ marginTop: 8 }}>No se han configurado cuadrillas o equipos para este proyecto aún.</p>
                  )}
                </div>

                <div className="project-info-card">
                  <h3>Trabajadores vinculados ({project.poolTrabajadores?.length || 0})</h3>
                  {project.poolTrabajadores?.length ? (
                    <ul className="project-workforce-list" style={{ marginTop: 12 }}>
                      {project.poolTrabajadores.map((worker) => (
                        <li key={worker.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0', borderBottom: '1px solid var(--ot-line)' }}>
                          <Link style={{ fontWeight: 600 }} to={`/trabajadores/${worker.id}`}>
                            {worker.nombre}
                          </Link>
                          <span style={{ fontSize: 11, color: 'var(--ot-muted)' }}>
                            {worker.oficio || 'Oficio general'}
                          </span>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p style={{ marginTop: 8 }}>Aún no hay trabajadores en el pool del proyecto.</p>
                  )}
                </div>
              </div>
            )}

            {/* ── Pestaña 5: DOCUMENTOS Y EVIDENCIAS ───────────── */}
            {activeTab === 'documentos' && (
              <div className="project-tab-single">
                <div className="project-info-card">
                  <h3>Documentación contractual y legal</h3>
                  {project.documentoLegalUrl ? (
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap', marginTop: 12 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                        <span className="material-symbols-outlined" style={{ fontSize: 32, color: 'var(--ot-orange-text)' }} aria-hidden="true">
                          description
                        </span>
                        <div>
                          <strong>{project.documentoLegalNombre || 'Documento legal de la obra'}</strong>
                          <p style={{ fontSize: 10, margin: '2px 0 0' }}>Archivo contractual adjunto por el cliente</p>
                        </div>
                      </div>
                      <div style={{ display: 'flex', gap: 8 }}>
                        <button className="button button-secondary" onClick={() => openLegalDocument(false)} type="button">
                          <span className="material-symbols-outlined" aria-hidden="true">visibility</span>
                          Previsualizar
                        </button>
                        <button className="button button-secondary" onClick={() => openLegalDocument(true)} type="button">
                          <span className="material-symbols-outlined" aria-hidden="true">download</span>
                          Descargar
                        </button>
                      </div>
                    </div>
                  ) : (
                    <p style={{ marginTop: 8 }}>Este proyecto no cuenta con un documento legal o contrato digital cargado.</p>
                  )}
                </div>

                <div className="project-info-card">
                  <h3>Evidencias de avance en obra</h3>
                  <p>
                    Total de evidencias fotográficas e informes registrados: <strong>{phaseStatus?.totalEvidencias || 0}</strong>
                  </p>
                  <p style={{ marginTop: 6, fontSize: 10 }}>
                    Los contratistas y equipos adjuntan evidencias fotográficas en cada reporte de avance semanal, las cuales son requeridas para la validación y cierre de cada fase.
                  </p>
                </div>
              </div>
            )}

            {/* ── Pestaña 6: RIESGOS ──────────────────────────── */}
            {activeTab === 'riesgos' && (
              <div className="project-tab-single">
                {project.indicadores ? (
                  <ProjectRiskPanel fechaEntrega={project.fechaEntrega} indicators={project.indicadores} />
                ) : (
                  <div className="project-info-card">
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, color: '#4ade80', marginBottom: 8 }}>
                      <span className="material-symbols-outlined" style={{ fontSize: 24 }} aria-hidden="true">check_circle</span>
                      <h3 style={{ margin: 0, color: '#4ade80' }}>Sin alertas de retraso detectadas</h3>
                    </div>
                    <p>
                      El modelo predictivo heurístico analiza continuamente el ritmo de trabajo de los equipos, los días restantes hasta la fecha de entrega y el pronóstico climático de la ciudad. Actualmente no se detectan desviaciones críticas.
                    </p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Modal de Previsualización de Documento */}
      {documentPreviewUrl && (
        <div className="project-preview-overlay" role="presentation">
          <section aria-label="Previsualización del documento legal" aria-modal="true" className="project-preview-dialog" role="dialog">
            <header>
              <h2>{project.documentoLegalNombre || 'Documento legal'}</h2>
              <button aria-label="Cerrar previsualización" className="admin-icon-button" onClick={closeLegalDocument} type="button">
                <span className="material-symbols-outlined" aria-hidden="true">close</span>
              </button>
            </header>
            <iframe src={documentPreviewUrl} title="Documento legal del proyecto" />
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
