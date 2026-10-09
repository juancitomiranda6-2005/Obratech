import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { clientProjectsService } from '../../services/proyectoService.js'

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

function ProjectsList({ onLogout, inProgressOnly }) {
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
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
    clientProjectsService.listMine()
      .then(({ data }) => { if (active) setProjects(data) })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudieron cargar tus proyectos.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const inProgress = projects.filter((project) => project.estadoEjecucion === 'EN_PROGRESO').length
  const completed = projects.filter((project) => project.estadoEjecucion === 'COMPLETADO').length
  const visibleProjects = inProgressOnly
    ? projects.filter((project) => project.estadoEjecucion && project.estadoEjecucion !== 'COMPLETADO')
    : projects

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
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación cliente" className="client-nav">
          <Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Resumen</Link>
          <Link className="is-current" to="/mis-proyectos"><span className="material-symbols-outlined" aria-hidden="true">folder_open</span>Mis proyectos</Link>
          <Link to="/proyectos/publicar"><span className="material-symbols-outlined" aria-hidden="true">add_circle</span>Publicar proyecto</Link>
        </nav>
        <div className="client-sidebar-footer"><button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>

      <main className="client-main">
        <header className="client-header">
          <div><p className="client-eyebrow">Proyectos</p><h1>{inProgressOnly ? 'Proyectos en proceso' : 'Mis proyectos'}</h1><p>Administra y haz seguimiento al progreso de tus obras.</p></div>
          <div className="client-header-actions">
            <Link className="button button-primary" to="/proyectos/publicar"><span className="material-symbols-outlined" aria-hidden="true">add</span>Nuevo proyecto</Link>
            <button aria-label="Actualizar proyectos" className="admin-icon-button" onClick={reload} title="Actualizar" type="button"><span className="material-symbols-outlined" aria-hidden="true">refresh</span></button>
          </div>
        </header>

        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {loading && !projects.length && <div className="admin-loading" role="status">Cargando proyectos…</div>}

        <section aria-label="Estadísticas de proyectos" className="client-stats">
          <ProjectStat label="Total proyectos" value={projects.length} tone="orange" />
          <ProjectStat label="En progreso" value={inProgress} tone="blue" />
          <ProjectStat label="Completados" value={completed} tone="green" />
        </section>

        {!loading && visibleProjects.length === 0 ? (
          <div className="client-empty projects-empty"><span className="material-symbols-outlined" aria-hidden="true">post_add</span><h3>Aún no hay proyectos</h3><p>Publica lo que necesitas construir o remodelar y conéctate con profesionales.</p><Link className="button button-primary" to="/proyectos/publicar">Publicar mi primer proyecto</Link></div>
        ) : (
          <section aria-label="Lista de proyectos" className="projects-list">
            {visibleProjects.map((project) => (
              <article className="project-list-item" key={project.id}>
                <div className="project-list-header">
                  <span className={`client-project-state state-${project.estadoValidacion === 'PENDIENTE' ? 'pendiente' : project.estadoEjecucion.toLowerCase()}`}>
                    {validationLabels[project.estadoValidacion] || statusLabels[project.estadoEjecucion] || project.estadoEjecucion}
                  </span>
                  <span className="project-list-type">{project.tipoProyecto}</span>
                </div>
                <h2>{project.titulo}</h2>
                <p>{project.descripcion}</p>
                <div className="project-list-meta">
                  <span><span className="material-symbols-outlined" aria-hidden="true">location_on</span>{project.ubicacion}</span>
                  <span><span className="material-symbols-outlined" aria-hidden="true">event</span>{project.fechaInicio || 'Inicio pendiente'} – {project.fechaEntrega || 'Entrega pendiente'}</span>
                  <strong>{new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(project.presupuesto || 0)}</strong>
                </div>
                <div className="project-list-progress"><span>Avance {Math.round(project.progresoProyecto || 0)}%</span><progress aria-label={`Avance ${project.titulo}`} max="100" value={Math.min(100, Math.max(0, project.progresoProyecto || 0))} /><span>{project.totalPostulantes || 0} postulaciones · {project.miembrosEquipo || 0} integrantes</span></div>
                <div className="project-list-actions">
                  <Link aria-label={`Ver postulantes de ${project.titulo}`} className="project-action-button" title="Ver postulantes" to={`/proyectos/${project.id}/postulantes`}><span className="material-symbols-outlined" aria-hidden="true">group</span></Link>
                  <Link aria-label={`Ver ${project.titulo}`} className="project-action-button" title="Ver detalles" to={`/proyectos/${project.id}`}><span className="material-symbols-outlined" aria-hidden="true">visibility</span></Link>
                  <Link aria-label={`Editar ${project.titulo}`} className="project-action-button" title="Editar" to={`/proyectos/${project.id}/editar`}><span className="material-symbols-outlined" aria-hidden="true">edit</span></Link>
                  <button aria-label={`Eliminar ${project.titulo}`} className="project-action-button project-action-delete" onClick={() => setProjectToDelete(project)} title="Eliminar" type="button"><span className="material-symbols-outlined" aria-hidden="true">delete</span></button>
                </div>
              </article>
            ))}
          </section>
        )}

        {projectToDelete && (
          <div className="confirm-overlay" role="presentation">
            <section aria-labelledby="delete-project-title" aria-modal="true" className="confirm-dialog" role="dialog">
              <span className="material-symbols-outlined confirm-icon" aria-hidden="true">warning</span>
              <h2 id="delete-project-title">Eliminar proyecto</h2>
              <p>¿Eliminar “{projectToDelete.titulo}”? Esta acción no se puede deshacer.</p>
              <div className="confirm-actions">
                <button className="button button-secondary" disabled={deleting} onClick={() => setProjectToDelete(null)} type="button">Cancelar</button>
                <button className="button confirm-delete" disabled={deleting} onClick={deleteProject} type="button">{deleting ? 'Eliminando…' : 'Eliminar'}</button>
              </div>
            </section>
          </div>
        )}
      </main>
    </div>
  )
}

function ProjectStat({ label, value, tone }) {
  return <article className={`client-stat tone-${tone}`}><span className="material-symbols-outlined" aria-hidden="true">{tone === 'green' ? 'task_alt' : 'home_work'}</span><div><strong>{value}</strong><p>{label}</p></div></article>
}

export function MisProyectos({ onLogout, inProgressOnly = false }) {
  return <ProjectsList inProgressOnly={inProgressOnly} onLogout={onLogout} />
}
