import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { clientDashboardService } from '../../services/clientDashboardService.js'

const stateLabels = { PENDIENTE: 'Pendiente', EN_PROGRESO: 'En progreso', COMPLETADO: 'Completado', CANCELADO: 'Cancelado' }

function ContractorCard({ contractor }) {
  const initials = contractor.nombre?.split(/\s+/).filter(Boolean).slice(0, 2).map((word) => word[0]).join('').toUpperCase() || 'C'
  return (
    <article className="contractor-history-card">
      <header className="contractor-history-header">
        <span className="contractor-history-avatar">{initials}</span>
        <div><h2><Link to={`/contratistas/${contractor.id}`}>{contractor.nombre || contractor.username}</Link></h2><p>{contractor.especialidad || 'Sin especialidad'}</p></div>
        {contractor.calificacionPromedio != null && <span className="contractor-history-rating"><span className="material-symbols-outlined" aria-hidden="true">star</span>{contractor.calificacionPromedio.toFixed(1)}</span>}
      </header>
      <dl className="contractor-history-contact">
        {contractor.email && <div><dt>Correo</dt><dd>{contractor.email}</dd></div>}
        {contractor.telefono && <div><dt>Teléfono</dt><dd>{contractor.telefono}</dd></div>}
        {contractor.ubicacion && <div><dt>Ubicación</dt><dd>{contractor.ubicacion}</dd></div>}
      </dl>
      <section className="contractor-history-projects">
        <div className="client-section-heading"><h3>Proyectos compartidos</h3><span>{contractor.proyectos.length}</span></div>
        <ul>{contractor.proyectos.map((project) => <li key={project.id}><span>{project.titulo || 'Proyecto sin título'}</span><small className={`client-project-state state-${project.estado.toLowerCase()}`}>{stateLabels[project.estado] || project.estado}</small><Link aria-label={`Calificar ${contractor.nombre} por ${project.titulo || 'este proyecto'}`} className="contractor-rate-link" to={`/calificaciones/crear/${project.id}/${contractor.id}`}>{project.estado === 'COMPLETADO' ? 'Calificar' : 'Valorar'}</Link></li>)}</ul>
      </section>
    </article>
  )
}

export function MisContratistasPage({ onLogout }) {
  const [contractors, setContractors] = useState([])
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')

  const reload = async (showRefresh = false) => {
    if (showRefresh) setRefreshing(true)
    setError('')
    try {
      const { data } = await clientDashboardService.getContractors()
      setContractors(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudieron cargar tus contratistas.')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    document.title = 'Mis contratistas | ObraTech'
    let active = true
    clientDashboardService.getContractors()
      .then(({ data }) => { if (active) setContractors(data) })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.error || 'No se pudieron cargar tus contratistas.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const normalizedQuery = query.trim().toLocaleLowerCase()
  const filtered = contractors.filter((contractor) => [contractor.nombre, contractor.username, contractor.email, contractor.especialidad]
    .filter(Boolean).some((value) => value.toLocaleLowerCase().includes(normalizedQuery)))

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación de cliente" className="client-nav">
          <Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link>
          <Link to="/mis-proyectos"><span className="material-symbols-outlined" aria-hidden="true">folder_open</span>Mis proyectos</Link>
          <Link className="is-current" to="/clientes/mis-contratistas"><span className="material-symbols-outlined" aria-hidden="true">engineering</span>Mis contratistas</Link>
          <Link to="/clientes/reportes"><span className="material-symbols-outlined" aria-hidden="true">bar_chart</span>Reportes</Link>
          <Link to="/perfil-cliente"><span className="material-symbols-outlined" aria-hidden="true">person</span>Mi perfil</Link>
        </nav>
        <div className="client-sidebar-footer"><button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>

      <main className="client-main">
        <header className="client-header">
          <div><p className="client-eyebrow">Relaciones de trabajo</p><h1>Mis contratistas</h1><p>Contratistas asignados a tus proyectos.</p></div>
          <button aria-label="Actualizar contratistas" className="admin-icon-button" disabled={refreshing} onClick={() => reload(true)} title="Actualizar" type="button"><span className="material-symbols-outlined" aria-hidden="true">refresh</span></button>
        </header>

        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        <section className="contractor-history-toolbar">
          <label className="admin-search"><span className="material-symbols-outlined" aria-hidden="true">search</span><span className="sr-only">Buscar contratista</span><input onChange={(event) => setQuery(event.target.value)} placeholder="Buscar por nombre, correo o especialidad" type="search" value={query} /></label>
          {!loading && <span>{filtered.length} de {contractors.length} contratistas</span>}
        </section>
        {loading && <div className="admin-loading" role="status">Cargando contratistas…</div>}
        {!loading && !contractors.length && <div className="client-empty"><span className="material-symbols-outlined" aria-hidden="true">engineering</span><h2>Aún no tienes contratistas asignados</h2><p>Cuando tus proyectos tengan un contratista asignado, aparecerá aquí.</p><Link className="button button-secondary" to="/mis-proyectos">Ver mis proyectos</Link></div>}
        {!loading && contractors.length > 0 && filtered.length === 0 && <div className="client-empty"><span className="material-symbols-outlined" aria-hidden="true">search_off</span><h2>No hay coincidencias</h2><p>Prueba con otro nombre, correo o especialidad.</p></div>}
        {filtered.length > 0 && <section aria-label="Contratistas asignados" className="contractor-history-grid">{filtered.map((contractor) => <ContractorCard contractor={contractor} key={contractor.id} />)}</section>}
      </main>
    </div>
  )
}