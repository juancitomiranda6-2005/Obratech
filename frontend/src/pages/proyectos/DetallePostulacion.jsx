import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { participantProjectsService } from '../../services/participantProjectsService.js'

const labels = { PENDING: 'Pendiente', ACCEPTED: 'Aceptada', REJECTED: 'Rechazada' }
const date = new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' })
const money = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

export function DetallePostulacion({ id, role, onLogout }) {
  const [application, setApplication] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    document.title = 'Detalle de postulación | ObraTech'
    let active = true
    participantProjectsService.listMyApplications()
      .then(({ data }) => {
        if (!active) return
        const found = data.find((item) => item.id === id)
        if (found) setApplication(found)
        else setError('No se encontró esta postulación en tu cuenta.')
      })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.error || 'No se pudo cargar el detalle.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id])

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación de postulaciones" className="client-nav"><Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link><Link to="/postulaciones"><span className="material-symbols-outlined" aria-hidden="true">assignment_turned_in</span>Proyectos disponibles</Link><Link className="is-current" to="/mis-postulaciones"><span className="material-symbols-outlined" aria-hidden="true">mail</span>Mis postulaciones</Link></nav>
        <div className="client-sidebar-footer"><button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>
      <main className="client-main">
        <header className="client-header"><div><p className="client-eyebrow">{role === 'ROLE_CONTRACTOR' ? 'Contratista' : 'Trabajador'}</p><h1>Detalle de postulación</h1><p>Consulta el estado y los datos enviados de tu solicitud.</p></div><Link className="button button-secondary" to="/mis-postulaciones"><span className="material-symbols-outlined" aria-hidden="true">arrow_back</span>Volver</Link></header>
        {loading && <div className="admin-loading" role="status">Cargando postulación…</div>}
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {application && <article className="application-detail-panel"><div className="client-project-topline"><span className={`application-status status-${application.estado.toLowerCase()}`}>{labels[application.estado] || application.estado}</span></div><h2>{application.projectTitle || 'Proyecto no disponible'}</h2><dl><div><dt>Ubicación</dt><dd>{application.ubicacion || 'No especificada'}</dd></div><div><dt>Presupuesto</dt><dd>{application.presupuesto == null ? 'No disponible' : money.format(application.presupuesto)}</dd></div><div><dt>Fecha de envío</dt><dd>{application.fechaPostulacion ? date.format(new Date(application.fechaPostulacion)) : 'No disponible'}</dd></div></dl><section><h3>Mensaje enviado</h3><p>{application.mensaje || 'No agregaste un mensaje a esta postulación.'}</p></section></article>}
      </main>
    </div>
  )
}