import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { EvidenceAttachment } from '../../components/shared/EvidenceAttachment.jsx'
import { contractorDashboardService } from '../../services/professionalDashboardService.js'

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

function ContractorDashboard({ onLogout }) {
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
          setWorkerReportsByProject(await loadProjectWorkerReports(dashboardResponse.data.proyectos))
        }
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudo cargar tu resumen de trabajo.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

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
    <div className="client-layout">
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
          <a href="#proyectos"><span className="material-symbols-outlined" aria-hidden="true">folder_special</span>Proyectos asignados</a>
          <a href="#proyectos"><span className="material-symbols-outlined" aria-hidden="true">description</span>Hacer reporte</a>
          <a href="#perfil"><span className="material-symbols-outlined" aria-hidden="true">person</span>Mi perfil</a>
        </nav>
        <div className="client-sidebar-footer">
          <div className="client-user"><span className="client-avatar">{dashboard?.nombre?.slice(0, 1).toUpperCase() || 'C'}</span><div><strong>{dashboard?.nombre || 'Contratista'}</strong><small>Contratista</small></div></div>
          <button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button>
        </div>
      </aside>

      <main className="client-main" id="resumen">
        <header className="client-header">
          <div><p className="client-eyebrow">Área de contratista</p><h1>Bienvenido, {dashboard?.nombre || '…'}</h1><p>Gestiona tu equipo y tus proyectos de construcción.</p></div>
          <button aria-label="Actualizar dashboard" className="admin-icon-button" onClick={reload} title="Actualizar" type="button"><span className="material-symbols-outlined" aria-hidden="true">refresh</span></button>
        </header>

        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}
        {loading && !dashboard && <div className="admin-loading" role="status">Cargando proyectos asignados…</div>}

        {dashboard && (
          <>
            {!dashboard.usuarioVerificado && (
              <section className="client-verification" aria-label="Estado de verificación">
                <span className="material-symbols-outlined" aria-hidden="true">gpp_maybe</span>
                <div><h2>Cuenta pendiente de verificación</h2><p>El administrador debe validar tu información antes de habilitar las postulaciones y otras interacciones.</p>{dashboard.perfilIncompleto && <><p className="client-missing">Completa estos campos: {dashboard.requisitosFaltantes.join(', ')}.</p><Link className="client-verification-action" to="/perfil-contratista/editar"><span className="material-symbols-outlined" aria-hidden="true">edit</span>Completar perfil</Link></>}</div>
                <span className="client-review-badge">En revisión</span>
              </section>
            )}

            <section aria-label="Estadísticas del contratista" className="client-stats">
              <ContractorStat icon="groups" label="Trabajadores en equipo" value={dashboard.miembrosEquipo} tone="blue" />
              <ContractorStat icon="construction" label="Proyectos activos" value={dashboard.proyectosEnProgreso} tone="orange" />
              <ContractorStat icon="star" label="Calificación promedio" value={dashboard.calificacionPromedio.toFixed(1)} tone="purple" />
              <ContractorStat icon="task_alt" label="Proyectos completados" value={dashboard.proyectosCompletados} tone="green" />
            </section>

            <div className="client-lower-grid contractor-lower-grid">
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
                ) : <div className="client-empty"><span className="material-symbols-outlined" aria-hidden="true">folder_open</span><h3>No tienes proyectos asignados</h3><p>Los proyectos asignados aparecerán aquí con su estado y ubicación.</p></div>}
              </section>

              <section className="client-profile" id="perfil">
                <p className="client-eyebrow">Perfil profesional</p><h2>{dashboard.especialidad || 'Contratista general'}</h2>
                <div className="client-profile-email"><span className="material-symbols-outlined" aria-hidden="true">location_on</span><span>{dashboard.ubicacion || 'Ubicación no definida'}</span></div>
                <div className="client-profile-status"><span className={`client-status-dot ${dashboard.usuarioVerificado ? 'is-verified' : ''}`} /><span>{dashboard.usuarioVerificado ? 'Cuenta verificada' : 'Pendiente de verificación'}</span></div>
                <div className="client-profile-total"><span>Proyectos completados</span><strong>{dashboard.proyectosCompletados}</strong></div>
                <Link className="profile-open-link" to="/perfil-contratista">Ver perfil completo<span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span></Link>
              </section>
            </div>

            <section aria-label="Historial de informes" className="contractor-reports-section" id="informes">
              <div className="client-section-heading"><div><p className="client-eyebrow">Seguimiento</p><h2>Informes enviados</h2></div><span>{reports.length} informes</span></div>
              {reports.length ? <div className="contractor-report-list">{reports.map((report) => (
                <article className="contractor-report-item" key={report.id}>
                  <div><strong>{report.proyecto || 'Proyecto'}</strong><time dateTime={report.creado}>{report.creado ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(report.creado)) : 'Fecha no disponible'}</time>
                    <button aria-label="Editar informe enviado al cliente" className="report-edit-button" onClick={() => { setEditingReportId(report.id); setEditingReportContent(report.contenido) }} title="Editar informe" type="button"><span className="material-symbols-outlined" aria-hidden="true">edit</span></button>
                  </div>
                  {editingReportId === report.id ? <form className="contractor-report-edit-form" onSubmit={(event) => updateReport(event, report.id)}>
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
          </>
        )}
      </main>
    </div>
  )
}

function ContractorStat({ icon, label, value, tone }) {
  return <article className={`client-stat tone-${tone}`}><span className="material-symbols-outlined" aria-hidden="true">{icon}</span><div><strong>{value}</strong><p>{label}</p></div></article>
}

export function DashboardContratista({ onLogout }) {
  return <ContractorDashboard onLogout={onLogout} />
}
