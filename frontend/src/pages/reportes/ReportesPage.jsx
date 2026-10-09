import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { EvidenceAttachment } from '../../components/shared/EvidenceAttachment.jsx'
import { clientDashboardService } from '../../services/clientDashboardService.js'

const currency = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
const statusLabels = { PENDIENTE: 'Pendiente', EN_PROGRESO: 'En progreso', COMPLETADO: 'Completado', CANCELADO: 'Cancelado' }

function Metric({ label, value, icon, tone }) {
  return <article className={`report-metric tone-${tone}`}><span className="material-symbols-outlined" aria-hidden="true">{icon}</span><div><strong>{value}</strong><p>{label}</p></div></article>
}

export function ReportesPage({ onLogout }) {
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const reload = async () => {
    setLoading(true)
    setError('')
    try {
      const { data } = await clientDashboardService.getReports()
      setReport(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudieron cargar los reportes.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    document.title = 'Reportes de actividad | ObraTech'
    let active = true
    clientDashboardService.getReports()
      .then(({ data }) => { if (active) setReport(data) })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.error || 'No se pudieron cargar los reportes.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const total = report?.totalProyectos || 0
  const states = [
    { label: 'Completados', value: report?.proyectosCompletados || 0, color: 'green' },
    { label: 'En progreso', value: report?.proyectosEnProgreso || 0, color: 'orange' },
    { label: 'Pendientes', value: report?.proyectosPendientes || 0, color: 'yellow' },
  ]
  const knownStates = states.reduce((sum, item) => sum + item.value, 0)
  if (total > knownStates) states.push({ label: 'Otros', value: total - knownStates, color: 'blue' })

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación de reportes" className="client-nav">
          <Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link>
          <Link to="/mis-proyectos"><span className="material-symbols-outlined" aria-hidden="true">folder_open</span>Mis proyectos</Link>
          <Link to="/proyectos/publicar"><span className="material-symbols-outlined" aria-hidden="true">add_circle</span>Publicar proyecto</Link>
          <Link className="is-current" to="/clientes/reportes"><span className="material-symbols-outlined" aria-hidden="true">bar_chart</span>Reportes</Link>
          <Link to="/perfil-cliente"><span className="material-symbols-outlined" aria-hidden="true">person</span>Mi perfil</Link>
        </nav>
        <div className="client-sidebar-footer"><button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>

      <main className="client-main">
        <header className="client-header">
          <div><p className="client-eyebrow">Actividad de la cuenta</p><h1>Reportes de actividad</h1><p>Consulta tus proyectos y los informes de avance enviados por tus contratistas.</p></div>
          <button aria-label="Actualizar reportes" className="admin-icon-button" onClick={reload} title="Actualizar" type="button"><span className="material-symbols-outlined" aria-hidden="true">refresh</span></button>
        </header>

        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {loading && !report && <div className="admin-loading" role="status">Cargando reportes…</div>}

        {report && (
          <>
            <section aria-label="Indicadores de actividad" className="report-metrics">
              <Metric icon="folder" label="Total proyectos" tone="blue" value={report.totalProyectos} />
              <Metric icon="task_alt" label="Completados" tone="green" value={report.proyectosCompletados} />
              <Metric icon="sync" label="En progreso" tone="orange" value={report.proyectosEnProgreso} />
              <Metric icon="hourglass_empty" label="Pendientes" tone="yellow" value={report.proyectosPendientes} />
              <Metric icon="engineering" label="Con contratista" tone="purple" value={report.proyectosConContratista} />
              <Metric icon="star" label="Calificaciones" tone="red" value={report.calificacionesDadas} />
            </section>

            <div className="report-content-grid">
              <section aria-labelledby="report-status-title" className="report-panel">
                <div className="client-section-heading"><div><p className="client-eyebrow">Distribución</p><h2 id="report-status-title">Estado de los proyectos</h2></div><span>{total} en total</span></div>
                {total ? <div className="report-state-list" role="img" aria-label={states.map((item) => `${item.label}: ${item.value}`).join(', ')}>
                  {states.map((item) => <div className="report-state-row" key={item.label}><span>{item.label}</span><div className="report-state-track"><span className={`report-state-bar tone-${item.color}`} style={{ width: `${Math.min(100, item.value * 100 / total)}%` }} /></div><strong>{item.value}</strong></div>)}
                </div> : <div className="report-empty">Aún no hay proyectos para mostrar.</div>}
              </section>

              <section className="report-callout">
                <p className="client-eyebrow">Tu actividad</p>
                <h2>{report.username}</h2>
                <p>Tienes {report.proyectosEnProgreso} proyectos en progreso. Mantén comunicación con tus contratistas para dar seguimiento a cada obra.</p>
                <div className="report-callout-actions"><Link className="button button-primary" to="/proyectos/publicar"><span className="material-symbols-outlined" aria-hidden="true">add</span>Nuevo proyecto</Link><Link className="button button-secondary" to="/mis-proyectos">Ver proyectos</Link></div>
              </section>
            </div>

            <section className="report-table-section">
              <div className="client-section-heading"><div><p className="client-eyebrow">Detalle</p><h2>Proyectos registrados</h2></div><span>{report.proyectos.length} resultados</span></div>
              {report.proyectos.length ? <div className="report-table-scroll"><table className="report-table"><thead><tr><th>Proyecto</th><th>Tipo</th><th>Estado</th><th>Contratista</th><th>Presupuesto</th><th><span className="sr-only">Acciones</span></th></tr></thead><tbody>
                {report.proyectos.map((project) => <tr key={project.id}><td>{project.titulo || 'Sin título'}</td><td>{project.tipoProyecto || '—'}</td><td><span className={`client-project-state state-${project.estado.toLowerCase()}`}>{statusLabels[project.estado] || project.estado}</span></td><td>{project.contratista || 'Sin asignar'}</td><td>{project.presupuesto == null ? '—' : currency.format(project.presupuesto)}</td><td><Link aria-label={`Ver ${project.titulo || 'proyecto'}`} className="report-view-link" to={`/proyectos/${project.id}`}>Ver</Link></td></tr>)}
              </tbody></table></div> : <div className="report-empty">No tienes proyectos registrados.</div>}
            </section>

            <section aria-label="Informes recibidos de contratistas" className="contractor-reports-section">
              <div className="client-section-heading"><div><p className="client-eyebrow">Seguimiento de obra</p><h2>Informes recibidos</h2></div><span>{report.informes?.length || 0} informes</span></div>
              {report.informes?.length ? <div className="contractor-report-list">{report.informes.map((item) => (
                <details className="contractor-report-item client-report-card" key={item.id}>
                  <summary className="client-report-summary">
                    <span><strong>{item.proyecto || 'Proyecto'}</strong><small>Contratista: {item.contratista || 'No disponible'}</small><time dateTime={item.creado}>{item.creado ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(item.creado)) : 'Fecha no disponible'}</time></span>
                    <span aria-hidden="true" className="material-symbols-outlined">expand_more</span>
                  </summary>
                  <div className="client-report-body">
                    <p>{item.contenido}</p>
                    {item.avancesTrabajador?.length > 0 && <div className="client-worker-advances">
                      <strong>Avances incluidos del equipo</strong>
                      {item.avancesTrabajador.map((advance) => <article className="client-worker-advance" key={advance.id}>
                        <div><strong>{advance.trabajador}</strong><time>{advance.creado ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(advance.creado)) : 'Fecha no disponible'}</time></div>
                        <p>{advance.contenido}</p>
                        <div className="evidence-attachment-list">{advance.evidencias.map((evidence) => <EvidenceAttachment evidence={evidence} key={evidence.fileId} />)}</div>
                      </article>)}
                    </div>}
                  </div>
                </details>
              ))}</div> : <div className="report-empty">Todavía no has recibido informes de tus contratistas.</div>}
            </section>
          </>
        )}
      </main>
    </div>
  )
}
