import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { participantProjectsService } from '../../services/participantProjectsService.js'

const states = { PENDING: 'Pendiente', ACCEPTED: 'Aceptada', REJECTED: 'Rechazada' }

function MyApplications({ onLogout, role }) {
  const [applications, setApplications] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    document.title = 'Mis postulaciones | ObraTech'
    let active = true
    participantProjectsService.listMyApplications()
      .then(({ data }) => { if (active) setApplications(data) })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.error || 'No se pudieron cargar tus postulaciones.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación de postulaciones" className="client-nav"><Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link><Link to="/postulaciones"><span className="material-symbols-outlined" aria-hidden="true">assignment_turned_in</span>Proyectos disponibles</Link><Link className="is-current" to="/mis-postulaciones"><span className="material-symbols-outlined" aria-hidden="true">mail</span>Mis postulaciones</Link></nav>
        <div className="client-sidebar-footer"><button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>
      <main className="client-main">
        <header className="client-header"><div><p className="client-eyebrow">{role === 'ROLE_CONTRACTOR' ? 'Contratista' : 'Trabajador'}</p><h1>Mis postulaciones</h1><p>Consulta el estado de tus solicitudes a proyectos.</p></div><Link className="button button-primary" to="/postulaciones">Explorar proyectos</Link></header>
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {loading && <div className="admin-loading" role="status">Cargando postulaciones…</div>}
        {!loading && !applications.length && <div className="client-empty market-empty"><span className="material-symbols-outlined" aria-hidden="true">mail</span><h3>No has enviado postulaciones</h3><p>Explora los proyectos disponibles para enviar tu primera solicitud.</p><Link className="button button-primary" to="/postulaciones">Explorar proyectos</Link></div>}
        {applications.length > 0 && <section aria-label="Mis solicitudes" className="my-applications-list">{applications.map((application) => <Link className="my-application-card" key={application.id} to={`/mis-postulaciones/${application.id}`}><div className="client-project-topline"><span className={`application-status status-${application.estado.toLowerCase()}`}>{states[application.estado] || application.estado}</span></div><h2>{application.projectTitle}</h2><p>{application.ubicacion || 'Ubicación pendiente'}</p><div className="client-project-meta"><span>Postulación enviada: {application.fechaPostulacion ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' }).format(new Date(application.fechaPostulacion)) : 'Fecha no disponible'}</span><strong>{application.presupuesto == null ? 'Presupuesto no disponible' : new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(application.presupuesto)}</strong></div><span className="application-detail-link">Ver detalle<span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span></span></Link>)}</section>}
      </main>
    </div>
  )
}

export function MisPostulaciones({ onLogout, role }) {
  return <MyApplications onLogout={onLogout} role={role} />
}