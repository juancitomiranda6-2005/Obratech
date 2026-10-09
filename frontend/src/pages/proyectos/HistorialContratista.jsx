import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { contractorDashboardService } from '../../services/professionalDashboardService.js'

const currency = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
const date = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeZone: 'UTC' })

export function HistorialContratista({ onLogout }) {
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')

  const loadHistory = async (showRefresh = false) => {
    if (showRefresh) setRefreshing(true)
    setError('')
    try {
      const { data } = await contractorDashboardService.getHistory()
      setProjects(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo cargar el historial de proyectos.')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    document.title = 'Historial de proyectos | ObraTech'
    let active = true
    contractorDashboardService.getHistory()
      .then(({ data }) => { if (active) setProjects(data) })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.error || 'No se pudo cargar el historial de proyectos.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const totalBudget = projects.reduce((sum, project) => sum + (project.presupuesto || 0), 0)

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación de contratista" className="client-nav">
          <Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link>
          <Link to="/contratistas/mi-equipo"><span className="material-symbols-outlined" aria-hidden="true">groups</span>Mi equipo</Link>
          <Link to="/postulaciones"><span className="material-symbols-outlined" aria-hidden="true">assignment_turned_in</span>Proyectos disponibles</Link>
          <Link to="/mis-postulaciones"><span className="material-symbols-outlined" aria-hidden="true">mail</span>Mis postulaciones</Link>
          <Link className="is-current" to="/contratistas/historial"><span className="material-symbols-outlined" aria-hidden="true">history</span>Historial</Link>
        </nav>
        <div className="client-sidebar-footer"><button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>

      <main className="client-main">
        <header className="client-header">
          <div><p className="client-eyebrow">Trayectoria profesional</p><h1>Historial de proyectos</h1><p>Consulta las obras que completaste y sus datos principales.</p></div>
          <button aria-label="Actualizar historial" className="admin-icon-button" disabled={refreshing} onClick={() => loadHistory(true)} title="Actualizar" type="button"><span className="material-symbols-outlined" aria-hidden="true">refresh</span></button>
        </header>

        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {loading && <div className="admin-loading" role="status">Cargando historial…</div>}
        {!loading && (
          <>
            <section aria-label="Resumen de proyectos completados" className="client-stats history-stats">
              <article className="client-stat tone-green"><span className="material-symbols-outlined" aria-hidden="true">task_alt</span><div><strong>{projects.length}</strong><p>Proyectos completados</p></div></article>
              <article className="client-stat tone-orange"><span className="material-symbols-outlined" aria-hidden="true">payments</span><div><strong>{currency.format(totalBudget)}</strong><p>Presupuesto acumulado</p></div></article>
            </section>
            {projects.length ? <section aria-label="Proyectos completados" className="history-project-list">{projects.map((project) => <article className="history-project" key={project.id}>
              <div className="history-project-main"><div className="client-project-topline"><span className="client-project-state state-completado">Completado</span></div><h2>{project.titulo || 'Proyecto sin título'}</h2><p>{project.descripcion || 'Sin descripción disponible.'}</p>
                <div className="client-project-meta"><span><span className="material-symbols-outlined" aria-hidden="true">location_on</span>{project.ubicacion || 'Ubicación no disponible'}</span><span><span className="material-symbols-outlined" aria-hidden="true">person</span>{project.cliente}</span>{project.fechaEntrega && <span><span className="material-symbols-outlined" aria-hidden="true">event_available</span>{date.format(new Date(`${project.fechaEntrega}T00:00:00Z`))}</span>}</div>
              </div>
              <strong className="history-project-budget">{project.presupuesto == null ? 'Sin presupuesto' : currency.format(project.presupuesto)}</strong>
            </article>)}</section> : <div className="client-empty history-empty"><span className="material-symbols-outlined" aria-hidden="true">workspace_premium</span><h2>Aún no hay historial</h2><p>Los proyectos completados aparecerán aquí.</p><Link className="button button-secondary" to="/">Volver al dashboard</Link></div>}
          </>
        )}
      </main>
    </div>
  )
}