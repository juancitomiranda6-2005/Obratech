import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { clientDashboardService } from '../../services/clientDashboardService.js'

const currencyFormatter = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
})

const stateLabels = {
  PENDIENTE: 'Pendiente',
  EN_PROGRESO: 'En progreso',
  COMPLETADO: 'Completado',
  CANCELADO: 'Cancelado',
}

function ClientDashboard({ onLogout }) {
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const reload = async () => {
    setLoading(true)
    setError('')
    try {
      const { data } = await clientDashboardService.getDashboard()
      setDashboard(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo cargar tu resumen de proyectos.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    document.title = 'Dashboard Cliente | ObraTech'
    let active = true
    clientDashboardService.getDashboard()
      .then(({ data }) => {
        if (active) setDashboard(data)
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudo cargar tu resumen de proyectos.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [])

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <a className="client-brand" href="#resumen">
          <span className="material-symbols-outlined" aria-hidden="true">construction</span>
          ObraTech
        </a>
        <nav aria-label="Secciones del dashboard" className="client-nav">
          <a href="#resumen"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Resumen</a>
          <Link to="/mis-proyectos"><span className="material-symbols-outlined" aria-hidden="true">folder_open</span>Mis proyectos</Link>
          <Link to="/clientes/mis-postulaciones"><span className="material-symbols-outlined" aria-hidden="true">mail</span>Postulaciones recibidas</Link>
          <Link to="/proyectos/publicar"><span className="material-symbols-outlined" aria-hidden="true">add_circle</span>Publicar proyecto</Link>
          <Link to="/clientes/reportes"><span className="material-symbols-outlined" aria-hidden="true">bar_chart</span>Reportes</Link>
          <Link to="/clientes/mis-contratistas"><span className="material-symbols-outlined" aria-hidden="true">engineering</span>Mis contratistas</Link>
          <Link to="/contratistas"><span className="material-symbols-outlined" aria-hidden="true">search</span>Explorar contratistas</Link>
          <Link to="/trabajadores/disponibles"><span className="material-symbols-outlined" aria-hidden="true">construction</span>Trabajadores disponibles</Link>
          <Link to="/perfil-cliente"><span className="material-symbols-outlined" aria-hidden="true">person</span>Mi perfil</Link>
        </nav>
        <div className="client-sidebar-footer">
          <div className="client-user">
            <span className="client-avatar">{dashboard?.username?.slice(0, 1).toUpperCase() || 'C'}</span>
            <div><strong>{dashboard?.username || 'Cliente'}</strong><small>Cliente</small></div>
          </div>
          <button className="client-logout" onClick={onLogout} type="button">
            <span className="material-symbols-outlined" aria-hidden="true">logout</span>
            Cerrar sesión
          </button>
        </div>
      </aside>

      <main className="client-main" id="resumen">
        <header className="client-header">
          <div>
            <p className="client-eyebrow">Área de cliente</p>
            <h1>Bienvenido, {dashboard?.username || '…'}</h1>
            <p>Gestiona tus proyectos de construcción de forma eficiente.</p>
          </div>
          <div className="client-header-actions">
            <Link className="button button-primary" to="/proyectos/publicar"><span className="material-symbols-outlined" aria-hidden="true">add</span>Nuevo proyecto</Link>
            <button aria-label="Actualizar dashboard" className="admin-icon-button" onClick={reload} title="Actualizar" type="button">
              <span className="material-symbols-outlined" aria-hidden="true">refresh</span>
            </button>
          </div>
        </header>

        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {loading && !dashboard && <div className="admin-loading" role="status">Cargando tus proyectos…</div>}

        {dashboard && (
          <>
            {!dashboard.usuarioVerificado && (
              <section className="client-verification" aria-label="Estado de verificación">
                <span className="material-symbols-outlined" aria-hidden="true">gpp_maybe</span>
                <div>
                  <h2>Cuenta pendiente de verificación</h2>
                  <p>El administrador debe verificar tu información antes de habilitar todas las interacciones de la plataforma.</p>
                  {dashboard.perfilIncompleto && dashboard.requisitosFaltantes.length > 0 && (
                    <>
                      <p className="client-missing">Completa estos campos: {dashboard.requisitosFaltantes.join(', ')}.</p>
                      <Link className="client-verification-action" to="/perfil-cliente/editar"><span className="material-symbols-outlined" aria-hidden="true">edit</span>Completar perfil</Link>
                    </>
                  )}
                </div>
                <span className="client-review-badge">En revisión</span>
              </section>
            )}

            <section aria-label="Estadísticas de proyectos" className="client-stats">
              <ClientStat icon="construction" label="Proyectos activos" value={dashboard.proyectosEnProgreso} tone="orange" />
              <ClientStat icon="groups" label="Miembros de equipo" value={dashboard.miembrosEquipo} tone="blue" />
              <ClientStat icon="payments" label="Presupuesto total" value={currencyFormatter.format(dashboard.presupuestoTotal)} tone="purple" />
              <ClientStat icon="task_alt" label="Tasa de finalización" value={`${dashboard.tasaExito}%`} tone="green" />
            </section>

            <div className="client-lower-grid">
              <section className="client-project-section" id="proyectos">
                <div className="client-section-heading">
                  <div><p className="client-eyebrow">Actividad</p><h2>Proyectos recientes</h2></div>
                  <span>{dashboard.totalProyectos} en total</span>
                </div>
                {dashboard.proyectosRecientes.length ? (
                  <div className="client-project-list">
                    {dashboard.proyectosRecientes.map((project) => (
                      <article className="client-project" key={project.id}>
                        <div className="client-project-topline">
                          <span className={`client-project-state state-${project.estado.toLowerCase()}`}>
                            {stateLabels[project.estado] || project.estado}
                          </span>
                          {project.tipoProyecto && <span className="client-project-type">{project.tipoProyecto}</span>}
                        </div>
                        <h3>{project.titulo || 'Proyecto sin título'}</h3>
                        <p>{project.descripcion || 'Sin descripción disponible.'}</p>
                        <div className="client-project-meta">
                          <span><span className="material-symbols-outlined" aria-hidden="true">location_on</span>{project.ubicacion || 'Ubicación pendiente'}</span>
                          <span>{project.miembrosEquipo} miembros</span>
                          <strong>{project.presupuesto == null ? 'Sin presupuesto' : currencyFormatter.format(project.presupuesto)}</strong>
                        </div>
                        <Link className="profile-open-link" to={`/proyectos/${project.id}/postulantes`}>
                          Ver postulantes<span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
                        </Link>
                      </article>
                    ))}
                  </div>
                ) : (
                  <div className="client-empty">
                    <span className="material-symbols-outlined" aria-hidden="true">folder_open</span>
                    <h3>Aún no tienes proyectos</h3>
                    <p>Cuando publiques tu primer proyecto, aparecerá aquí con su estado y presupuesto.</p>
                  </div>
                )}
              </section>

              <section className="client-profile" id="perfil">
                <p className="client-eyebrow">Cuenta</p>
                <h2>Estado del perfil</h2>
                <div className="client-profile-email">
                  <span className="material-symbols-outlined" aria-hidden="true">mail</span>
                  <span>{dashboard.username}</span>
                </div>
                <div className="client-profile-status">
                  <span className={`client-status-dot ${dashboard.usuarioVerificado ? 'is-verified' : ''}`} />
                  <span>{dashboard.usuarioVerificado ? 'Cuenta verificada' : 'Pendiente de verificación'}</span>
                </div>
                <div className="client-profile-total">
                  <span>Proyectos registrados</span><strong>{dashboard.totalProyectos}</strong>
                </div>
                <Link className="profile-open-link" to="/perfil-cliente">Ver perfil completo<span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span></Link>
              </section>
            </div>
          </>
        )}
      </main>
    </div>
  )
}

function ClientStat({ icon, label, value, tone }) {
  return (
    <article className={`client-stat tone-${tone}`}>
      <span className="material-symbols-outlined" aria-hidden="true">{icon}</span>
      <div><strong>{value}</strong><p>{label}</p></div>
    </article>
  )
}

export function DashboardCliente({ onLogout }) {
  return <ClientDashboard onLogout={onLogout} />
}
