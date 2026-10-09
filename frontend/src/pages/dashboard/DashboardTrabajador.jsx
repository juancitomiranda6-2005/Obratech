import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { workerDashboardService } from '../../services/professionalDashboardService.js'

function WorkerDashboard({ onLogout }) {
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [pendingInvitation, setPendingInvitation] = useState('')

  const reload = async () => {
    setLoading(true)
    setError('')
    try {
      const { data } = await workerDashboardService.getDashboard()
      setDashboard(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo cargar tu espacio de trabajo.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    document.title = 'Espacio de Trabajo | ObraTech'
    let active = true
    workerDashboardService.getDashboard()
      .then(({ data }) => { if (active) setDashboard(data) })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudo cargar tu espacio de trabajo.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const respondToInvitation = async (invitationId, state) => {
    setPendingInvitation(invitationId)
    setError('')
    try {
      await workerDashboardService.respondToInvitation(invitationId, state)
      await reload()
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo responder la invitación.')
    } finally {
      setPendingInvitation('')
    }
  }

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <a className="client-brand" href="#resumen"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</a>
        <nav aria-label="Secciones del dashboard" className="client-nav">
          <a href="#resumen"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Mi resumen</a>
          <Link to="/postulaciones"><span className="material-symbols-outlined" aria-hidden="true">assignment_turned_in</span>Proyectos disponibles</Link>
          <Link to="/mis-postulaciones"><span className="material-symbols-outlined" aria-hidden="true">mail</span>Mis postulaciones</Link>
          <Link to="/trabajadores/mi-equipo"><span className="material-symbols-outlined" aria-hidden="true">groups</span>Mi equipo de trabajo</Link>
          <Link to="/trabajadores/mi-equipo#mis-informes"><span className="material-symbols-outlined" aria-hidden="true">description</span>Mis informes enviados</Link>
          <Link to="/perfil-trabajador"><span className="material-symbols-outlined" aria-hidden="true">person</span>Mi perfil</Link>
          <a href="#invitaciones"><span className="material-symbols-outlined" aria-hidden="true">notifications</span>Invitaciones</a>
          <a href="#proyectos"><span className="material-symbols-outlined" aria-hidden="true">folder_special</span>Proyectos asignados</a>
        </nav>
        <div className="client-sidebar-footer">
          <div className="client-user"><span className="client-avatar">{dashboard?.nombre?.slice(0, 1).toUpperCase() || 'T'}</span><div><strong>{dashboard?.nombre || 'Trabajador'}</strong><small>Trabajador</small></div></div>
          <button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button>
        </div>
      </aside>

      <main className="client-main" id="resumen">
        <header className="client-header">
          <div><p className="client-eyebrow">Espacio de trabajo</p><h1>Hola, {dashboard?.nombre || '…'}</h1><p>Revisa tus proyectos asignados y tu estado laboral.</p></div>
          <div className="client-header-actions">
            <Link className="button button-primary" to="/perfil-trabajador/editar"><span className="material-symbols-outlined" aria-hidden="true">edit</span>Editar perfil</Link>
            <div className={`worker-availability ${dashboard?.disponible ? 'is-available' : ''}`}><span />{dashboard?.disponible ? 'Disponible' : 'Ocupado'}</div>
          </div>
        </header>

        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {loading && !dashboard && <div className="admin-loading" role="status">Cargando invitaciones y proyectos…</div>}

        {dashboard && (
          <>
            {!dashboard.usuarioVerificado && (
              <section className="client-verification" aria-label="Estado de verificación">
                <span className="material-symbols-outlined" aria-hidden="true">gpp_maybe</span>
                <div><h2>Cuenta pendiente de verificación</h2><p>El administrador debe validar tu información antes de habilitar las postulaciones y otras interacciones.</p>{dashboard.perfilIncompleto && <><p className="client-missing">Completa estos campos: {dashboard.requisitosFaltantes.join(', ')}.</p><Link className="client-verification-action" to="/perfil-trabajador/editar"><span className="material-symbols-outlined" aria-hidden="true">edit</span>Completar perfil</Link></>}</div>
                <span className="client-review-badge">En revisión</span>
              </section>
            )}

            <section className="worker-summary-grid">
              <article className="client-profile"><p className="client-eyebrow">Tu perfil</p><h2>{dashboard.oficio || 'Oficio general'}</h2><div className="client-profile-total"><span>Experiencia</span><strong>{dashboard.experiencia} años</strong></div><div className="client-profile-status"><span className={`client-status-dot ${dashboard.usuarioVerificado ? 'is-verified' : ''}`} /><span>{dashboard.usuarioVerificado ? 'Cuenta verificada' : 'Pendiente de verificación'}</span></div><div className="client-profile-status"><span className={`client-status-dot ${dashboard.cvDisponible ? 'is-verified' : ''}`} /><span>{dashboard.cvDisponible ? 'CV registrado' : 'Sin CV registrado'}</span></div></article>
              <article className="client-profile worker-count"><p className="client-eyebrow">Proyectos</p><h2>Asignados</h2><strong>{dashboard.proyectosAsignados.length}</strong><span>proyectos en tu equipo</span></article>
            </section>

            <section className="worker-section" id="invitaciones">
              <div className="client-section-heading"><div><p className="client-eyebrow">Notificaciones</p><h2>Invitaciones de trabajo</h2></div><span>{dashboard.invitacionesPendientes.length} pendientes</span></div>
              {dashboard.invitacionesPendientes.length ? (
                <div className="worker-invitation-list">
                  {dashboard.invitacionesPendientes.map((invitation) => (
                    <article className="worker-invitation" key={invitation.id}>
                      <div><p className="client-eyebrow">Nueva invitación</p><h3>{invitation.proyecto}</h3><p>{invitation.ubicacion || 'Ubicación pendiente'} · Invitado por {invitation.contratista}</p></div>
                      <div className="worker-invitation-actions">
                        <button className="worker-accept" disabled={pendingInvitation === invitation.id} onClick={() => respondToInvitation(invitation.id, 'ACEPTADA')} type="button">Aceptar</button>
                        <button className="worker-reject" disabled={pendingInvitation === invitation.id} onClick={() => respondToInvitation(invitation.id, 'RECHAZADA')} type="button">Rechazar</button>
                      </div>
                    </article>
                  ))}
                </div>
              ) : <div className="client-empty"><span className="material-symbols-outlined" aria-hidden="true">notifications_off</span><h3>No tienes invitaciones pendientes</h3><p>Las invitaciones de los contratistas aparecerán aquí.</p></div>}
            </section>

            <section className="worker-section" id="proyectos">
              <div className="client-section-heading"><div><p className="client-eyebrow">Trabajo actual</p><h2>Proyectos asignados</h2></div><span>{dashboard.proyectosAsignados.length} activos</span></div>
              {dashboard.proyectosAsignados.length ? (
                <div className="client-project-list">
                  {dashboard.proyectosAsignados.map((project) => (
                    <article className="client-project" key={project.id}>
                      <div className="client-project-topline"><span className={`client-project-state state-${project.estado.toLowerCase()}`}>{project.estado.replaceAll('_', ' ')}</span></div>
                      <h3>{project.titulo || 'Proyecto sin título'}</h3><p>{project.descripcion || 'Sin descripción disponible.'}</p>
                      <div className="client-project-meta"><span><span className="material-symbols-outlined" aria-hidden="true">location_on</span>{project.ubicacion || 'Ubicación pendiente'}</span><span>Contratista: {project.contratista}</span></div>
                    </article>
                  ))}
                </div>
              ) : <div className="client-empty"><span className="material-symbols-outlined" aria-hidden="true">folder_open</span><h3>Bandeja vacía</h3><p>No estás asignado a ningún proyecto en este momento.</p></div>}
            </section>
          </>
        )}
      </main>
    </div>
  )
}

export function DashboardTrabajador({ onLogout }) {
  return <WorkerDashboard onLogout={onLogout} />
}
