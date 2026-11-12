import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { EvidenceAttachment } from '../../components/shared/EvidenceAttachment.jsx'
import { ProfileAvatar } from '../../components/shared/ProfileAvatar.jsx'
import { contractorDashboardService } from '../../services/professionalDashboardService.js'
import { participantProjectsService } from '../../services/participantProjectsService.js'
import { PhasePanel } from '../../components/PhasePanel.jsx'
import { ProjectRiskPanel } from '../../components/ProjectRiskPanel.jsx'
import { SettingsMenu } from '../../components/layout/SettingsMenu.jsx'

async function loadProjectWorkerReports(projects = []) {
  const entries = await Promise.all(projects.map(async (project) => {
    try {
      const { data } = await contractorDashboardService.getWorkerReports(project.id)
      return [project.id, data]
    } catch {
      return [project.id, []]
    }
  }))
  return Object.fromEntries(entries)
}

function ContractorDashboard() {
  const [dashboard, setDashboard] = useState(null)
  const [reports, setReports] = useState([])
  const [workerReportsByProject, setWorkerReportsByProject] = useState({})
  const [reportDrafts, setReportDrafts] = useState({})
  const [selectedWorkerReports, setSelectedWorkerReports] = useState({})
  const [pendingReport, setPendingReport] = useState('')
  const [editingReportId, setEditingReportId] = useState('')
  const [editingReportContent, setEditingReportContent] = useState('')
  const [pendingEdit, setPendingEdit] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [selectedProjectId, setSelectedProjectId] = useState('')
  const [reportsShowAll, setReportsShowAll] = useState(false)
  const [selectedPhaseStatus, setSelectedPhaseStatus] = useState(null)
  const [phaseLoading, setPhaseLoading] = useState(false)
  const [phaseError, setPhaseError] = useState('')
  const [activeFollowUpTab, setActiveFollowUpTab] = useState('overview')
  const visibleReports = reportsShowAll
    ? reports
    : reports.filter((report) => report.proyectoId === selectedProjectId)
  const followUpTabs = [
    { id: 'overview', label: 'Vista General', icon: 'rocket_launch' },
    { id: 'phases', label: 'Fases', icon: 'description' },
    { id: 'risks', label: 'Riesgos', icon: 'monitoring' },
    { id: 'payments', label: 'Pagos y Finanzas', icon: 'account_balance_wallet' },
    { id: 'teams', label: 'Equipos', icon: 'groups' },
  ]

  const openProjects = (event) => {
    event.preventDefault()
    setActiveFollowUpTab('overview')
    window.requestAnimationFrame(() => {
      window.requestAnimationFrame(() => {
        document.getElementById('proyectos')?.scrollIntoView({ behavior: 'smooth' })
      })
    })
  }

  const reload = async () => {
    setLoading(true)
    setError('')
    try {
      const [dashboardResponse, reportsResponse] = await Promise.all([
        contractorDashboardService.getDashboard(),
        contractorDashboardService.getReports(),
      ])
      setDashboard(dashboardResponse.data)
      setReports(reportsResponse.data)
      const nextProjectId = dashboardResponse.data.proyectos.some((project) => project.id === selectedProjectId)
        ? selectedProjectId : dashboardResponse.data.proyectos[0]?.id || ''
      setSelectedProjectId(nextProjectId)
      if (!nextProjectId) setSelectedPhaseStatus(null)
      setPhaseLoading(Boolean(nextProjectId))
      setWorkerReportsByProject(await loadProjectWorkerReports(dashboardResponse.data.proyectos))
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo cargar tu resumen de trabajo.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    document.title = 'Dashboard Contratista | ObraTech'
    let active = true
    Promise.all([contractorDashboardService.getDashboard(), contractorDashboardService.getReports()])
      .then(async ([dashboardResponse, reportsResponse]) => {
        if (active) {
          setDashboard(dashboardResponse.data)
          setReports(reportsResponse.data)
          setSelectedProjectId(dashboardResponse.data.proyectos[0]?.id || '')
          setPhaseLoading(Boolean(dashboardResponse.data.proyectos[0]?.id))
          setWorkerReportsByProject(await loadProjectWorkerReports(dashboardResponse.data.proyectos))
        }
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudo cargar tu resumen de trabajo.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!selectedProjectId) {
      return undefined
    }
    let active = true
    participantProjectsService.getPhaseStatus(selectedProjectId)
      .then(({ data }) => { if (active) setSelectedPhaseStatus(data) })
      .catch((requestError) => {
        if (active) setPhaseError(requestError.response?.data?.error || 'No se pudo cargar el estado de fases.')
      })
      .finally(() => { if (active) setPhaseLoading(false) })
    return () => { active = false }
  }, [dashboard, selectedProjectId])

  const createReport = async (event, projectId) => {
    event.preventDefault()
    const contenido = reportDrafts[projectId]?.trim() || ''
    if (contenido.length < 10) {
      setError('Escribe un informe de al menos 10 caracteres.')
      return
    }
    setPendingReport(projectId)
    setError('')
    setNotice('')
    try {
      await contractorDashboardService.createReport(projectId, contenido, selectedWorkerReports[projectId] || [])
      setReportDrafts((current) => ({ ...current, [projectId]: '' }))
      setSelectedWorkerReports((current) => ({ ...current, [projectId]: [] }))
      setNotice('Informe registrado correctamente.')
      const { data } = await contractorDashboardService.getReports()
      setReports(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo registrar el informe.')
    } finally {
      setPendingReport('')
    }
  }

  const updateReport = async (event, reportId) => {
    event.preventDefault()
    const contenido = editingReportContent.trim()
    if (contenido.length < 10 || contenido.length > 3000) {
      setError('El informe debe tener entre 10 y 3000 caracteres.')
      return
    }
    setPendingEdit(true)
    setError('')
    setNotice('')
    try {
      await contractorDashboardService.updateReport(reportId, contenido)
      const { data } = await contractorDashboardService.getReports()
      setReports(data)
      setEditingReportId('')
      setNotice('Informe actualizado correctamente.')
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo actualizar el informe.')
    } finally {
      setPendingEdit(false)
    }
  }

  return (
    <div className="client-layout contractor-dashboard">
      <aside className="client-sidebar">
        <a className="client-brand" href="#resumen"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</a>
        <nav aria-label="Secciones del dashboard" className="client-nav">
          <a href="#resumen"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Resumen</a>
          <Link to="/postulaciones"><span className="material-symbols-outlined" aria-hidden="true">assignment_turned_in</span>Proyectos disponibles</Link>
          <Link to="/mis-postulaciones"><span className="material-symbols-outlined" aria-hidden="true">mail</span>Mis postulaciones</Link>
          <Link to="/trabajadores"><span className="material-symbols-outlined" aria-hidden="true">person_search</span>Mercado de trabajadores</Link>
          <Link to="/clientes"><span className="material-symbols-outlined" aria-hidden="true">business</span>Directorio de clientes</Link>
          <Link to="/contratistas/mi-equipo"><span className="material-symbols-outlined" aria-hidden="true">groups</span>Mi equipo</Link>
          <Link to="/contratistas/historial"><span className="material-symbols-outlined" aria-hidden="true">history</span>Historial de proyectos</Link>
          <a href="#proyectos" onClick={openProjects}><span className="material-symbols-outlined" aria-hidden="true">folder_special</span>Proyectos asignados</a>
          <a href="#proyectos" onClick={openProjects}><span className="material-symbols-outlined" aria-hidden="true">description</span>Hacer reporte</a>
        </nav>
        <div className="client-sidebar-footer">
          <SettingsMenu
            inline
            triggerContent={(
              <span className="client-user">
                <ProfileAvatar className="client-avatar" name={dashboard?.nombre || 'Contratista'} profileId={dashboard?.username} />
                <span><strong>{dashboard?.nombre || 'Contratista'}</strong><small>Contratista</small></span>
              </span>
            )}
            triggerLabel={`Opciones de cuenta de ${dashboard?.nombre || 'Contratista'}`}
          />
        </div>
      </aside>

      <main className="client-main" id="resumen">
        <header className="client-header contractor-dashboard-header">
          <div><p className="client-eyebrow">Área de contratista</p><h1>Bienvenido, {dashboard?.nombre || '…'}</h1><p>Gestiona tu equipo y tus proyectos de construcción.</p></div>
          <button aria-label="Actualizar dashboard" className="admin-icon-button" disabled={loading} onClick={reload} title={loading ? 'Actualizando…' : 'Actualizar'} type="button"><span className={`material-symbols-outlined ${loading ? 'is-refreshing' : ''}`} aria-hidden="true">refresh</span></button>
        </header>

        <nav aria-label="Secciones del seguimiento" className="contractor-follow-up-tabs">
          {followUpTabs.map((tab) => (
            <button
              aria-pressed={activeFollowUpTab === tab.id}
              className={activeFollowUpTab === tab.id ? 'is-active' : ''}
              key={tab.id}
              onClick={() => setActiveFollowUpTab(tab.id)}
              type="button"
            >
              <span aria-hidden="true" className="material-symbols-outlined">{tab.icon}</span>
              <span>{tab.label}</span>
            </button>
          ))}
        </nav>

        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}
        {loading && !dashboard && <div className="admin-loading" role="status">Cargando proyectos asignados…</div>}

        {dashboard && (
          <>
            {activeFollowUpTab === 'overview' && (
              <>
            {!dashboard.usuarioVerificado && (
              <section className="client-verification" aria-label="Estado de verificación">
                <span className="material-symbols-outlined" aria-hidden="true">gpp_maybe</span>
                <div>
                  <h2>Completa tu perfil para obtener la insignia</h2>
                  <p>Tu cuenta está activa. Al completar los requisitos se habilitará automáticamente tu insignia de verificación.</p>
                  {dashboard.perfilIncompleto && (
                    <div className="client-missing-chips">
                      <div className="client-missing-tags">
                        {dashboard.requisitosFaltantes.slice(0, 4).map((req, i) => (
                          <span className="client-req-tag" key={i}>
                            <span className="material-symbols-outlined" style={{ fontSize: '13px' }} aria-hidden="true">pending</span>
                            {req.length > 35 ? req.substring(0, 33) + '…' : req}
                          </span>
                        ))}
                        {dashboard.requisitosFaltantes.length > 4 && (
                          <span className="client-req-tag-more">
                            +{dashboard.requisitosFaltantes.length - 4} requisitos más
                          </span>
                        )}
                      </div>
                      <Link className="client-verification-action" to="/perfil-contratista/editar">
                        <span className="material-symbols-outlined" aria-hidden="true">edit</span>
                        Completar requisitos
                      </Link>
                    </div>
                  )}
                </div>
                <span className="client-review-badge">Activa</span>
              </section>
            )}

            <section aria-label="Estadísticas del contratista" className="client-stats">
              <ContractorStat icon="groups" label="Trabajadores en equipo" value={dashboard.miembrosEquipo} tone="blue" />
              <ContractorStat icon="construction" label="Proyectos activos" value={dashboard.proyectosEnProgreso} tone="orange" />
              <ContractorStat icon="star" label="Calificación promedio" value={dashboard.calificacionPromedio.toFixed(1)} tone="purple" />
              <ContractorStat icon="task_alt" label="Proyectos completados" value={dashboard.proyectosCompletados} tone="green" />
            </section>

            <div className="contractor-overview-projects">
              <section className="client-project-section" id="proyectos">
                <div className="client-section-heading"><div><p className="client-eyebrow">Trabajo</p><h2>Proyectos asignados</h2></div><span>{dashboard.proyectosEnProgreso} activos</span></div>
                {dashboard.proyectos.length ? (
                  <div className="client-project-list">
                    {dashboard.proyectos.map((project) => {
                      const workerReports = workerReportsByProject[project.id] || []
                      const selectedIds = selectedWorkerReports[project.id] || []
                      return <article className="client-project" key={project.id}>
                        <div className="client-project-topline"><span className={`client-project-state state-${project.estado.toLowerCase()}`}>{project.estado.replaceAll('_', ' ')}</span></div>
                        <h3>{project.titulo || 'Proyecto sin título'}</h3>
                        <p>{project.descripcion || 'Sin descripción disponible.'}</p>
                        <div className="client-project-meta"><span><span className="material-symbols-outlined" aria-hidden="true">location_on</span>{project.ubicacion || 'Ubicación pendiente'}</span><span>{project.contratista}</span></div>
                        <Link className="profile-open-link" to={`/proyectos/asignados/${project.id}`}>Ver fases del proyecto<span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span></Link>
                        <details className="contractor-report-composer">
                          <summary><span className="material-symbols-outlined" aria-hidden="true">edit_note</span>Hacer reporte para el cliente</summary>
                          <form onSubmit={(event) => createReport(event, project.id)}>
                            <fieldset className="contractor-worker-reports">
                              <legend>Avances y evidencias del equipo</legend>
                              {workerReports.length ? workerReports.map((workerReport) => (
                                <article className="contractor-worker-report" key={workerReport.id}>
                                  <label>
                                    <input checked={selectedIds.includes(workerReport.id)} onChange={() => setSelectedWorkerReports((current) => {
                                      const ids = current[project.id] || []
                                      return { ...current, [project.id]: ids.includes(workerReport.id) ? ids.filter((id) => id !== workerReport.id) : [...ids, workerReport.id] }
                                    })} type="checkbox" />
                                    <span><strong>{workerReport.trabajador}</strong><time>{workerReport.creado ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(workerReport.creado)) : 'Fecha no disponible'}</time></span>
                                  </label>
                                  <p>{workerReport.contenido}</p>
                                  <div className="evidence-attachment-list">{workerReport.evidencias.map((evidence) => <EvidenceAttachment evidence={evidence} key={evidence.fileId} />)}</div>
                                </article>
                              )) : <p className="contractor-worker-report-empty">Todavía no hay avances enviados por trabajadores.</p>}
                            </fieldset>
                            <label htmlFor={`report-${project.id}`}>Avances, novedades o bloqueos</label>
                            <textarea id={`report-${project.id}`} maxLength={3000} minLength={10} onChange={(event) => setReportDrafts((current) => ({ ...current, [project.id]: event.target.value }))} required rows={3} value={reportDrafts[project.id] || ''} />
                            <button className="button button-primary" disabled={pendingReport === project.id} type="submit">{pendingReport === project.id ? 'Guardando…' : 'Guardar informe'}</button>
                          </form>
                        </details>
                      </article>
                    })}
                  </div>
                ) : <div className="client-empty projects-empty-card">
                        <span className="material-symbols-outlined" aria-hidden="true">engineering</span>
                        <h3>No tienes proyectos asignados actualmente</h3>
                        <p>Explora las oportunidades laborales disponibles en la plataforma y postúlate para liderar nuevas obras.</p>
                        <Link className="button button-primary" to="/proyectos/postular" style={{ marginTop: 14 }}>
                          <span className="material-symbols-outlined" aria-hidden="true">search</span>
                          Explorar proyectos disponibles
                        </Link>
                      </div>}
              </section>
            </div>
              </>
            )}

            {activeFollowUpTab !== 'overview' && <section aria-label="Seguimiento del proyecto" className="contractor-follow-up">
              <header className="contractor-view-header">
                <div className="client-section-heading">
                  <div>
                    <p className="client-eyebrow">Seguimiento del proyecto</p>
                    <h2>{followUpTabs.find((tab) => tab.id === activeFollowUpTab)?.label}</h2>
                  </div>
                  <label className="contractor-phase-select">
                    <span>Proyecto seleccionado</span>
                    <select
                      disabled={!dashboard.proyectos.length}
                      onChange={(event) => {
                        setSelectedProjectId(event.target.value)
                        setReportsShowAll(false)
                        setSelectedPhaseStatus(null)
                        setPhaseLoading(Boolean(event.target.value))
                        setPhaseError('')
                      }}
                      value={selectedProjectId}
                    >
                      {!dashboard.proyectos.length && <option value="">Sin proyectos asignados</option>}
                      {dashboard.proyectos.map((project) => <option key={project.id} value={project.id}>{project.titulo}</option>)}
                    </select>
                  </label>
                </div>
              </header>

              {phaseError && <div className="admin-alert admin-alert-error" role="alert">{phaseError}</div>}
              {phaseLoading && <p className="admin-loading" role="status">Cargando fases e indicadores…</p>}
              {!selectedProjectId && !phaseLoading && <p className="contractor-follow-up-empty">No tienes proyectos asignados para mostrar el seguimiento.</p>}
              {!phaseLoading && selectedPhaseStatus && (
                <div className={`contractor-tab-content contractor-tab-${activeFollowUpTab}`}>
                  {activeFollowUpTab === 'phases' && (
                    <PhasePanel phaseStatus={selectedPhaseStatus} sections={['summary', 'phases']} />
                  )}
                  {activeFollowUpTab === 'risks' && (
                    selectedPhaseStatus.indicadores
                      ? <div className="contractor-risk-view">
                          <ProjectRiskPanel
                            fechaEntrega={selectedPhaseStatus.fechaEntrega}
                            indicators={selectedPhaseStatus.indicadores}
                            sections={['summary', 'factors', 'actions', 'delivery', 'weather']}
                          />
                        </div>
                      : <ProjectRiskPanel indicators={null} />
                  )}
                  {activeFollowUpTab === 'payments' && (
                    <PhasePanel phaseStatus={selectedPhaseStatus} sections={['payments']} />
                  )}
                  {activeFollowUpTab === 'teams' && (
                    selectedPhaseStatus.indicadores
                      ? <ProjectRiskPanel
                          fechaEntrega={selectedPhaseStatus.fechaEntrega}
                          indicators={selectedPhaseStatus.indicadores}
                          sections={['teams']}
                        />
                      : <p className="contractor-follow-up-empty">Los datos de equipos no están disponibles.</p>
                  )}
                </div>
              )}
            </section>}

            {activeFollowUpTab === 'overview' && <details className="contractor-overview-reports" id="informes">
              <summary>Ver historial de informes<span>{visibleReports.length} informes</span></summary>
              <section aria-label="Historial de informes" className="contractor-reports-section">
              <div className="client-section-heading">
                <div><p className="client-eyebrow">Seguimiento</p><h2>Informes enviados</h2></div>
                <label className="contractor-report-filter">
                  <span>Proyecto</span>
                  <select
                    aria-label="Filtrar informes por proyecto"
                    onChange={(event) => {
                      if (event.target.value === 'all') {
                        setReportsShowAll(true)
                      } else {
                        setReportsShowAll(false)
                        setSelectedProjectId(event.target.value)
                        setSelectedPhaseStatus(null)
                        setPhaseLoading(Boolean(event.target.value))
                        setPhaseError('')
                      }
                    }}
                    value={reportsShowAll ? 'all' : selectedProjectId}
                  >
                    {!dashboard.proyectos.length && <option value="">Sin proyectos asignados</option>}
                    {dashboard.proyectos.map((project) => <option key={project.id} value={project.id}>{project.titulo}</option>)}
                    <option value="all">Ver todos</option>
                  </select>
                </label>
                <span>{visibleReports.length} informes</span>
              </div>
              {visibleReports.length ? <div className="contractor-report-list">{visibleReports.map((report) => (
                <article className="contractor-report-item" key={report.id}>
                  <div><strong>{report.proyecto || 'Proyecto'}</strong><time dateTime={report.creado}>{report.creado ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(report.creado)) : 'Fecha no disponible'}</time>
                    {report.proyectoDisponible !== false ? <button aria-label="Editar informe enviado al cliente" className="report-edit-button" onClick={() => { setEditingReportId(report.id); setEditingReportContent(report.contenido) }} title="Editar informe" type="button"><span className="material-symbols-outlined" aria-hidden="true">edit</span></button> : <span className="report-project-unavailable">Proyecto eliminado · solo lectura</span>}
                  </div>
                  {editingReportId === report.id && report.proyectoDisponible !== false ? <form className="contractor-report-edit-form" onSubmit={(event) => updateReport(event, report.id)}>
                    <label htmlFor={`edit-contractor-report-${report.id}`}>Editar informe enviado al cliente</label>
                    <textarea id={`edit-contractor-report-${report.id}`} maxLength={3000} minLength={10} onChange={(event) => setEditingReportContent(event.target.value)} required rows={4} value={editingReportContent} />
                    <div className="team-add-member-actions">
                      <button className="button button-secondary" onClick={() => setEditingReportId('')} type="button">Cancelar</button>
                      <button className="button button-primary" disabled={pendingEdit} type="submit">{pendingEdit ? 'Guardando…' : 'Guardar cambios'}</button>
                    </div>
                  </form> : <p>{report.contenido}</p>}
                  {report.avancesTrabajador?.length > 0 && <details className="contractor-report-advances">
                    <summary>Avances incluidos ({report.avancesTrabajador.length})</summary>
                    {report.avancesTrabajador.map((advance) => <div className="contractor-report-advance" key={advance.id}>
                      <strong>{advance.trabajador}</strong><p>{advance.contenido}</p>
                    </div>)}
                  </details>}
                  <div className="evidence-attachment-list">{report.evidencias?.map((evidence) => <EvidenceAttachment evidence={evidence} key={evidence.fileId} />)}</div>
                </article>
              ))}</div> : <div className="client-empty"><span className="material-symbols-outlined" aria-hidden="true">description</span><h3>Aún no has enviado informes</h3><p>Registra avances, novedades o bloqueos desde cualquiera de tus proyectos asignados.</p></div>}
              </section>
            </details>}
          </>
        )}
      </main>
    </div>
  )
}

function ContractorStat({ icon, label, value, tone }) {
  return <article className={`client-stat tone-${tone}`}><span className="material-symbols-outlined" aria-hidden="true">{icon}</span><div><strong>{value}</strong><p>{label}</p></div></article>
}

export function DashboardContratista() {
  return <ContractorDashboard />
}
