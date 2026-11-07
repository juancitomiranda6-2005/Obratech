import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { clientProjectsService } from '../../services/proyectoService.js'
import { SettingsMenu } from '../../components/layout/SettingsMenu.jsx'
import { ProfileAvatar } from '../../components/shared/ProfileAvatar.jsx'
import { useAuth } from '../../hooks/useAuth.js'

const statusLabels = {
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

function formatDisplayName(user) {
  if (user?.nombre && user.nombre.trim()) return user.nombre.trim()
  const rawEmail = user?.email || user?.username || ''
  if (rawEmail.includes('@')) {
    const raw = rawEmail.split('@')[0]
    return raw.replace(/[._]/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
  }
  return rawEmail || 'Cliente'
}

function ProjectsList({ onLogout: _onLogout, inProgressOnly }) {
  const { user } = useAuth()
  const displayName = formatDisplayName(user)

  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [searchQuery, setSearchQuery] = useState('')
  const [projectToDelete, setProjectToDelete] = useState(null)
  const [deleting, setDeleting] = useState(false)

  const reload = async () => {
    setLoading(true)
    setError('')
    try {
      const { data } = await clientProjectsService.listMine()
      setProjects(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudieron cargar tus proyectos.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    document.title = 'Mis Proyectos | ObraTech'
    let active = true
    clientProjectsService
      .listMine()
      .then(({ data }) => {
        if (active) setProjects(data)
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudieron cargar tus proyectos.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  const inProgress = useMemo(
    () => projects.filter((project) => project.estadoEjecucion === 'EN_PROGRESO').length,
    [projects]
  )
  const completed = useMemo(
    () => projects.filter((project) => project.estadoEjecucion === 'COMPLETADO').length,
    [projects]
  )
  const pending = useMemo(
    () =>
      projects.filter(
        (project) => project.estadoValidacion === 'PENDIENTE' || project.estadoEjecucion === 'PENDIENTE'
      ).length,
    [projects]
  )
  const totalBudget = useMemo(
    () => projects.reduce((sum, p) => sum + (p.presupuesto || 0), 0),
    [projects]
  )

  const visibleProjects = useMemo(() => {
    return projects.filter((project) => {
      if (inProgressOnly && project.estadoEjecucion === 'COMPLETADO') return false
      if (statusFilter === 'EN_PROGRESO' && project.estadoEjecucion !== 'EN_PROGRESO') return false
      if (statusFilter === 'COMPLETADO' && project.estadoEjecucion !== 'COMPLETADO') return false
      if (
        statusFilter === 'PENDIENTE' &&
        project.estadoValidacion !== 'PENDIENTE' &&
        project.estadoEjecucion !== 'PENDIENTE'
      )
        return false

      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase().trim()
        const matchTitle = (project.titulo || '').toLowerCase().includes(query)
        const matchLoc = (project.ubicacion || '').toLowerCase().includes(query)
        const matchType = (project.tipoProyecto || '').toLowerCase().includes(query)
        const matchDesc = (project.descripcion || '').toLowerCase().includes(query)
        if (!matchTitle && !matchLoc && !matchType && !matchDesc) return false
      }
      return true
    })
  }, [projects, statusFilter, searchQuery, inProgressOnly])

  const deleteProject = async () => {
    if (!projectToDelete) return
    setDeleting(true)
    setError('')
    try {
      await clientProjectsService.deleteMine(projectToDelete.id)
      setProjects((current) => current.filter((project) => project.id !== projectToDelete.id))
      setProjectToDelete(null)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo eliminar el proyecto.')
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="client-layout">
      {/* ── Sidebar Cliente estructurado ──────────────────────── */}
      <aside className="client-sidebar">
        <Link className="client-brand" to="/">
          <span className="material-symbols-outlined" aria-hidden="true">construction</span>
          ObraTech
        </Link>

        <nav aria-label="Navegación cliente" className="client-nav">
          {/* GESTIÓN */}
          <p className="client-nav-group-label">Gestión</p>
          <Link to="/">
            <span className="material-symbols-outlined" aria-hidden="true">dashboard</span>
            Resumen
          </Link>
          <Link className="is-current" to="/mis-proyectos">
            <span className="material-symbols-outlined" aria-hidden="true">folder_open</span>
            Mis proyectos
          </Link>
          <Link to="/proyectos/publicar">
            <span className="material-symbols-outlined" aria-hidden="true">add_circle</span>
            Publicar proyecto
          </Link>
          <Link to="/clientes/mis-postulaciones">
            <span className="material-symbols-outlined" aria-hidden="true">mail</span>
            Postulaciones recibidas
          </Link>

          {/* PERSONAS */}
          <p className="client-nav-group-label">Personas</p>
          <Link to="/clientes/mis-contratistas">
            <span className="material-symbols-outlined" aria-hidden="true">engineering</span>
            Mis contratistas
          </Link>
          <Link to="/contratistas">
            <span className="material-symbols-outlined" aria-hidden="true">search</span>
            Explorar contratistas
          </Link>
          <Link to="/trabajadores/disponibles">
            <span className="material-symbols-outlined" aria-hidden="true">construction</span>
            Trabajadores disponibles
          </Link>

          {/* ANÁLISIS */}
          <p className="client-nav-group-label">Análisis</p>
          <Link to="/clientes/reportes">
            <span className="material-symbols-outlined" aria-hidden="true">bar_chart</span>
            Reportes
          </Link>
        </nav>

        <div className="client-sidebar-footer">
          <SettingsMenu
            inline
            triggerContent={(
              <span className="client-user">
                <ProfileAvatar className="client-avatar" name={displayName} profileId={user?.email || user?.username} />
                <span>
                  <strong>{displayName}</strong>
                  <small>Cliente</small>
                </span>
              </span>
            )}
            triggerLabel={`Opciones de cuenta de ${displayName}`}
          />
        </div>
      </aside>

      {/* ── Contenido Principal ──────────────────────────────── */}
      <main className="client-main">
        <header className="client-header">
          <div>
            <p className="client-eyebrow">Área de cliente · Gestión</p>
            <h1>{inProgressOnly ? 'Proyectos en proceso' : 'Mis proyectos'}</h1>
            <p>Administra, monitorea y haz seguimiento al ciclo de vida de tus obras.</p>
          </div>
          <div className="client-header-actions">
            <Link className="button button-primary" to="/proyectos/publicar">
              <span className="material-symbols-outlined" aria-hidden="true">add</span>
              Nuevo proyecto
            </Link>
            <button
              aria-label="Actualizar proyectos"
              className="admin-icon-button"
              onClick={reload}
              title="Actualizar"
              type="button"
            >
              <span className="material-symbols-outlined" aria-hidden="true">refresh</span>
            </button>
          </div>
        </header>

        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {loading && !projects.length && <div className="admin-loading" role="status">Cargando proyectos…</div>}

        {/* Tarjetas de Estadísticas / KPIs */}
        <section aria-label="Estadísticas de proyectos" className="client-stats">
          <ProjectStat label="Total proyectos" tone="orange" value={projects.length} />
          <ProjectStat label="En progreso" tone="blue" value={inProgress} />
          <ProjectStat label="En verificación / Pendientes" tone="orange" value={pending} />
          <ProjectStat label="Presupuesto total" tone="green" value={currencyFormatter.format(totalBudget)} />
        </section>

        {/* Barra de Filtros y Búsqueda */}
        <div className="projects-filter-bar">
          <div className="projects-filter-chips" role="group" aria-label="Filtro de proyectos por estado">
            <button
              type="button"
              className={`projects-filter-chip ${statusFilter === 'ALL' ? 'is-active' : ''}`}
              onClick={() => setStatusFilter('ALL')}
            >
              <span>Todos</span>
              <span className="chip-count">{projects.length}</span>
            </button>
            <button
              type="button"
              className={`projects-filter-chip ${statusFilter === 'EN_PROGRESO' ? 'is-active' : ''}`}
              onClick={() => setStatusFilter('EN_PROGRESO')}
            >
              <span>En progreso</span>
              <span className="chip-count">{inProgress}</span>
            </button>
            <button
              type="button"
              className={`projects-filter-chip ${statusFilter === 'PENDIENTE' ? 'is-active' : ''}`}
              onClick={() => setStatusFilter('PENDIENTE')}
            >
              <span>En verificación</span>
              <span className="chip-count">{pending}</span>
            </button>
            <button
              type="button"
              className={`projects-filter-chip ${statusFilter === 'COMPLETADO' ? 'is-active' : ''}`}
              onClick={() => setStatusFilter('COMPLETADO')}
            >
              <span>Completados</span>
              <span className="chip-count">{completed}</span>
            </button>
          </div>

          <div className="projects-search-box">
            <span className="material-symbols-outlined" aria-hidden="true">search</span>
            <input
              aria-label="Buscar proyectos"
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Buscar por título, ubicación o tipo…"
              type="search"
              value={searchQuery}
            />
            {searchQuery && (
              <button
                aria-label="Limpiar búsqueda"
                className="projects-search-clear"
                onClick={() => setSearchQuery('')}
                title="Limpiar búsqueda"
                type="button"
              >
                <span className="material-symbols-outlined" aria-hidden="true">close</span>
              </button>
            )}
          </div>
        </div>

        {/* Lista de Proyectos */}
        {!loading && projects.length === 0 ? (
          <div className="client-empty projects-empty">
            <span className="material-symbols-outlined" aria-hidden="true">post_add</span>
            <h3>Aún no hay proyectos</h3>
            <p>Publica lo que necesitas construir o remodelar y conéctate con profesionales calificados.</p>
            <Link className="button button-primary" to="/proyectos/publicar">
              Publicar mi primer proyecto
            </Link>
          </div>
        ) : !loading && visibleProjects.length === 0 ? (
          <div className="client-empty projects-empty">
            <span className="material-symbols-outlined" aria-hidden="true">filter_alt_off</span>
            <h3>No se encontraron proyectos</h3>
            <p>No hay proyectos que coincidan con los filtros o la búsqueda ingresada.</p>
            <button
              className="button button-secondary"
              onClick={() => {
                setStatusFilter('ALL')
                setSearchQuery('')
              }}
              type="button"
            >
              Restablecer filtros
            </button>
          </div>
        ) : (
          <section aria-label="Lista de proyectos" className="projects-list">
            {visibleProjects.map((project) => (
              <article className="project-list-item" key={project.id}>
                <div className="project-list-header">
                  <span
                    className={`client-project-state state-${
                      project.estadoValidacion === 'PENDIENTE'
                        ? 'pendiente'
                        : (project.estadoEjecucion || 'pendiente').toLowerCase()
                    }`}
                  >
                    {validationLabels[project.estadoValidacion] ||
                      statusLabels[project.estadoEjecucion] ||
                      project.estadoEjecucion}
                  </span>
                  <span className="project-list-type">{project.tipoProyecto}</span>
                  {project.estadoEjecucion === 'EN_PROGRESO' && (
                    <span className="project-phase-pill">
                      <span className="material-symbols-outlined" aria-hidden="true">play_circle</span>
                      En ejecución
                    </span>
                  )}
                </div>

                <h2>{project.titulo}</h2>
                <p>{project.descripcion}</p>

                <div className="project-list-meta">
                  <span>
                    <span className="material-symbols-outlined" aria-hidden="true">location_on</span>
                    {project.ubicacion}
                  </span>
                  <span>
                    <span className="material-symbols-outlined" aria-hidden="true">event</span>
                    {project.fechaInicio || 'Inicio pendiente'} – {project.fechaEntrega || 'Entrega pendiente'}
                  </span>
                  <strong>{currencyFormatter.format(project.presupuesto || 0)}</strong>
                </div>

                <div className="project-list-progress">
                  <span>Avance {Math.round(project.progresoProyecto || 0)}%</span>
                  <progress
                    aria-label={`Avance de ${project.titulo}`}
                    max="100"
                    value={Math.min(100, Math.max(0, project.progresoProyecto || 0))}
                  />
                  <span>
                    {project.totalPostulantes || 0} postulaciones · {project.miembrosEquipo || 0} integrantes
                  </span>
                </div>

                <div className="project-list-actions">
                  <Link
                    aria-label={`Ver fases y avance de ${project.titulo}`}
                    className="button button-secondary project-view-phases-btn"
                    title="Ver fases del proyecto (30/50/20%)"
                    to={`/proyectos/${project.id}?tab=fases`}
                  >
                    <span className="material-symbols-outlined" aria-hidden="true">view_timeline</span>
                    Ver fases
                  </Link>
                  <Link
                    aria-label={`Ver postulantes de ${project.titulo}`}
                    className="project-action-button"
                    title="Ver postulantes"
                    to={`/proyectos/${project.id}/postulantes`}
                  >
                    <span className="material-symbols-outlined" aria-hidden="true">group</span>
                  </Link>
                  <Link
                    aria-label={`Ver detalles de ${project.titulo}`}
                    className="project-action-button"
                    title="Ver detalles completos"
                    to={`/proyectos/${project.id}`}
                  >
                    <span className="material-symbols-outlined" aria-hidden="true">visibility</span>
                  </Link>
                  <Link
                    aria-label={`Editar ${project.titulo}`}
                    className="project-action-button"
                    title="Editar"
                    to={`/proyectos/${project.id}/editar`}
                  >
                    <span className="material-symbols-outlined" aria-hidden="true">edit</span>
                  </Link>
                  <button
                    aria-label={`Eliminar ${project.titulo}`}
                    className="project-action-button project-action-delete"
                    onClick={() => setProjectToDelete(project)}
                    title="Eliminar"
                    type="button"
                  >
                    <span className="material-symbols-outlined" aria-hidden="true">delete</span>
                  </button>
                </div>
              </article>
            ))}
          </section>
        )}

        {/* Modal de Confirmación de Eliminación */}
        {projectToDelete && (
          <div className="confirm-overlay" role="presentation">
            <section
              aria-labelledby="delete-project-title"
              aria-modal="true"
              className="confirm-dialog"
              role="dialog"
            >
              <span className="material-symbols-outlined confirm-icon" aria-hidden="true">warning</span>
              <h2 id="delete-project-title">Eliminar proyecto</h2>
              <p>¿Eliminar “{projectToDelete.titulo}”? Esta acción no se puede deshacer y borrará los datos asociados.</p>
              <div className="confirm-actions">
                <button
                  className="button button-secondary"
                  disabled={deleting}
                  onClick={() => setProjectToDelete(null)}
                  type="button"
                >
                  Cancelar
                </button>
                <button
                  className="button confirm-delete"
                  disabled={deleting}
                  onClick={deleteProject}
                  type="button"
                >
                  {deleting ? 'Eliminando…' : 'Eliminar'}
                </button>
              </div>
            </section>
          </div>
        )}
      </main>
    </div>
  )
}

function ProjectStat({ label, value, tone }) {
  const icon =
    tone === 'green'
      ? 'task_alt'
      : tone === 'blue'
      ? 'engineering'
      : tone === 'purple'
      ? 'payments'
      : 'home_work'
  return (
    <article className={`client-stat tone-${tone}`}>
      <span className="material-symbols-outlined" aria-hidden="true">{icon}</span>
      <div>
        <strong>{value}</strong>
        <p>{label}</p>
      </div>
    </article>
  )
}

export function MisProyectos({ onLogout, inProgressOnly = false }) {
  return <ProjectsList inProgressOnly={inProgressOnly} onLogout={onLogout} />
}
