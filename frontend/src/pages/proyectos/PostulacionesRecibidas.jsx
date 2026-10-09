import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { clientApplicationsService } from '../../services/postulacionService.js'

const statusLabels = { PENDING: 'Pendiente', ACCEPTED: 'Aceptada', REJECTED: 'Rechazada' }
const money = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

function ApplicantProfileLink({ application }) {
  if (!application.perfilId) return <strong>{application.nombre || application.username}</strong>
  const path = application.rol === 'ROLE_WORKER' ? 'trabajadores' : 'contratistas'
  return <Link to={`/${path}/${application.perfilId}`}>{application.nombre || application.username}</Link>
}

export function PostulacionesRecibidas({ onLogout }) {
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    document.title = 'Postulaciones recibidas | ObraTech'
    let active = true
    clientApplicationsService.listForClient()
      .then(({ data }) => { if (active) setProjects(data) })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.error || 'No se pudieron cargar las postulaciones recibidas.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const projectsWithApplications = projects.filter((project) => project.applications.length > 0)
  const totalApplications = projects.reduce((total, project) => total + project.applications.length, 0)

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación de cliente" className="client-nav">
          <Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link>
          <Link to="/mis-proyectos"><span className="material-symbols-outlined" aria-hidden="true">folder_open</span>Mis proyectos</Link>
          <Link className="is-current" to="/clientes/mis-postulaciones"><span className="material-symbols-outlined" aria-hidden="true">mail</span>Postulaciones recibidas</Link>
        </nav>
        <div className="client-sidebar-footer"><button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>

      <main className="client-main">
        <header className="client-header">
          <div><p className="client-eyebrow">Selección de profesionales</p><h1>Postulaciones recibidas</h1><p>{loading ? 'Cargando solicitudes…' : `${totalApplications} solicitudes en ${projectsWithApplications.length} proyectos`}</p></div>
          <Link className="button button-secondary" to="/mis-proyectos"><span className="material-symbols-outlined" aria-hidden="true">arrow_back</span>Mis proyectos</Link>
        </header>
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {loading && <div className="admin-loading" role="status">Cargando postulaciones…</div>}
        {!loading && !error && totalApplications === 0 && (
          <div className="client-empty"><span className="material-symbols-outlined" aria-hidden="true">mail</span><h2>No has recibido postulaciones todavía</h2><p>Las solicitudes aparecerán aquí cuando alguien se postule a uno de tus proyectos.</p></div>
        )}
        {!loading && projectsWithApplications.length > 0 && (
          <section aria-label="Postulaciones por proyecto" className="client-project-list">
            {projectsWithApplications.map((project) => (
              <article className="client-project" key={project.projectId}>
                <div className="client-section-heading">
                  <div><p className="client-eyebrow">{project.ubicacion || 'Ubicación no registrada'}</p><h2>{project.projectTitle || 'Proyecto sin título'}</h2></div>
                  <span>{project.applications.length} solicitudes</span>
                </div>
                {project.presupuesto != null && <p className="client-project-meta"><strong>Presupuesto: {money.format(project.presupuesto)}</strong></p>}
                <ul className="received-applications-list">
                  {project.applications.map((application) => (
                    <li key={application.id}>
                      <div><ApplicantProfileLink application={application} />{application.usuarioVerificado && <span className="material-symbols-outlined applicant-verified" aria-label="Verificado">verified</span>}<small>{application.rol.replace('ROLE_', '') || 'Postulante'} · {application.username}</small></div>
                      <span className={`application-status status-${application.estado.toLowerCase()}`}>{statusLabels[application.estado] || application.estado}</span>
                    </li>
                  ))}
                </ul>
                <Link className="profile-open-link" to={`/proyectos/${project.projectId}/postulantes`}>Gestionar postulaciones<span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span></Link>
              </article>
            ))}
          </section>
        )}
      </main>
    </div>
  )
}