import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { clientDashboardService } from '../../services/clientDashboardService.js'
import { ProfileAvatar } from '../../components/shared/ProfileAvatar.jsx'
import { ProjectRiskPanel } from '../../components/ProjectRiskPanel.jsx'
import { SettingsMenu } from '../../components/layout/SettingsMenu.jsx'

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

/* ── Helpers ─────────────────────────────────────────── */

function deriveAttentionItems(dashboard) {
  const items = []

  // Perfil incompleto
  if (dashboard.perfilIncompleto && dashboard.requisitosFaltantes?.length > 0) {
    items.push({
      key: 'perfil',
      icon: 'person_alert',
      label: 'Perfil incompleto',
      detail: `Faltan: ${dashboard.requisitosFaltantes.join(', ')}`,
      to: '/perfil-cliente/editar',
    })
  }

  // Proyectos en estado PENDIENTE (sin arrancar)
  const pendientes = (dashboard.proyectosRecientes || []).filter(
    (p) => p.estado === 'PENDIENTE',
  )
  if (pendientes.length > 0) {
    items.push({
      key: 'pendientes',
      icon: 'pending_actions',
      label: `${pendientes.length} proyecto${pendientes.length > 1 ? 's' : ''} sin iniciar`,
      detail: pendientes.map((p) => p.titulo || 'Sin título').join(', '),
      to: '/mis-proyectos',
    })
  }

  // Proyectos con indicadores de riesgo alto
  const conRiesgo = (dashboard.proyectosRecientes || []).filter(
    (p) =>
      p.indicadores &&
      (p.indicadores.nivelRiesgo === 'ALTO' ||
        p.indicadores.nivelRiesgo === 'CRITICO'),
  )
  if (conRiesgo.length > 0) {
    items.push({
      key: 'riesgo',
      icon: 'warning',
      label: `${conRiesgo.length} proyecto${conRiesgo.length > 1 ? 's' : ''} con riesgo elevado`,
      detail: conRiesgo.map((p) => p.titulo || 'Sin título').join(', '),
      to: '/mis-proyectos',
    })
  }

  return items
}

function calcProfileCompleteness(dashboard) {
  const checks = [
    !!dashboard.username,
    dashboard.usuarioVerificado,
    !dashboard.perfilIncompleto,
    (dashboard.totalProyectos || 0) > 0,
  ]
  const done = checks.filter(Boolean).length
  return Math.round((done / checks.length) * 100)
}

/* ── Sub-components ──────────────────────────────────── */

function ClientStat({ icon, label, value, tone }) {
  return (
    <article className={`client-stat tone-${tone}`}>
      <span className="material-symbols-outlined" aria-hidden="true">{icon}</span>
      <div><strong>{value}</strong><p>{label}</p></div>
    </article>
  )
}

function AttentionBand({ items }) {
  if (!items.length) {
    return (
      <div className="client-attention-empty" role="status">
        <span className="material-symbols-outlined" aria-hidden="true">check_circle</span>
        <span>Todo al día — no hay elementos urgentes</span>
      </div>
    )
  }

  return (
    <section className="client-attention-band" aria-label="Requiere tu atención">
      <p className="client-attention-band-label">
        <span className="material-symbols-outlined" aria-hidden="true">notifications_active</span>
        Requiere tu atención
      </p>
      <ul className="client-attention-list" role="list">
        {items.map((item) => (
          <li className="client-attention-item" key={item.key}>
            <span className="material-symbols-outlined" aria-hidden="true">{item.icon}</span>
            <span className="client-attention-text">
              <strong>{item.label}</strong>
              {item.detail && <span>{item.detail}</span>}
            </span>
            {item.to && (
              <Link className="client-attention-action" to={item.to}>
                Ver
                <span className="material-symbols-outlined" aria-hidden="true">chevron_right</span>
              </Link>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}

function ProfileCompletenessBar({ pct }) {
  return (
    <div className="client-completeness-wrap">
      <div className="client-completeness-header">
        <span>Completitud del perfil</span>
        <strong>{pct}%</strong>
      </div>
      <div className="client-completeness-track" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
        <span className="client-completeness-fill" style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}

function ProjectProgressBar({ estado }) {
  // Segment widths based on estado
  const config = {
    PENDIENTE:   { done: 0,  active: 0,  pending: 100 },
    EN_PROGRESO: { done: 35, active: 40, pending: 25 },
    COMPLETADO:  { done: 100, active: 0, pending: 0 },
    CANCELADO:   { done: 0,  active: 0,  pending: 100 },
  }
  const { done, active, pending } = config[estado] || config.PENDIENTE

  return (
    <div className="client-progress-mini" aria-label={`Progreso: ${stateLabels[estado] || estado}`}>
      {done > 0   && <span className="cpm-done"    style={{ width: `${done}%`    }} />}
      {active > 0 && <span className="cpm-active"  style={{ width: `${active}%`  }} />}
      {pending > 0 && <span className="cpm-pending" style={{ width: `${pending}%` }} />}
    </div>
  )
}

/* ── Main dashboard ──────────────────────────────────── */

function ClientDashboard() {
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

  const attentionItems = dashboard ? deriveAttentionItems(dashboard) : []
  const completeness   = dashboard ? calcProfileCompleteness(dashboard) : 0

  return (
    <div className="client-layout">
      {/* ── Sidebar ────────────────────────────────────── */}
      <aside className="client-sidebar">
        <a className="client-brand" href="#resumen">
          <span className="material-symbols-outlined" aria-hidden="true">construction</span>
          ObraTech
        </a>

        <nav aria-label="Secciones del dashboard" className="client-nav">
          {/* GESTIÓN */}
          <p className="client-nav-group-label">Gestión</p>
          <a href="#resumen"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Resumen</a>
          <Link to="/mis-proyectos"><span className="material-symbols-outlined" aria-hidden="true">folder_open</span>Mis proyectos</Link>
          <Link to="/proyectos/publicar"><span className="material-symbols-outlined" aria-hidden="true">add_circle</span>Publicar proyecto</Link>
          <Link to="/clientes/mis-postulaciones"><span className="material-symbols-outlined" aria-hidden="true">mail</span>Postulaciones recibidas</Link>

          {/* PERSONAS */}
          <p className="client-nav-group-label">Personas</p>
          <Link to="/clientes/mis-contratistas"><span className="material-symbols-outlined" aria-hidden="true">engineering</span>Mis contratistas</Link>
          <Link to="/contratistas"><span className="material-symbols-outlined" aria-hidden="true">search</span>Explorar contratistas</Link>
          <Link to="/trabajadores/disponibles"><span className="material-symbols-outlined" aria-hidden="true">construction</span>Trabajadores disponibles</Link>

          {/* ANÁLISIS */}
          <p className="client-nav-group-label">Análisis</p>
          <Link to="/clientes/reportes"><span className="material-symbols-outlined" aria-hidden="true">bar_chart</span>Reportes</Link>
        </nav>

        <div className="client-sidebar-footer">
          <SettingsMenu
            inline
            triggerContent={(
              <span className="client-user">
                <ProfileAvatar className="client-avatar" name={dashboard?.username || 'Cliente'} profileId={dashboard?.username} />
                <span><strong>{dashboard?.username || 'Cliente'}</strong><small>Cliente</small></span>
              </span>
            )}
            triggerLabel={`Opciones de cuenta de ${dashboard?.username || 'Cliente'}`}
          />
        </div>
      </aside>

      {/* ── Main content ───────────────────────────────── */}
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
            {/* ── Banda de atención ── */}
            <AttentionBand items={attentionItems} />

            {/* ── Verificación de cuenta (si aplica) ── */}
            {!dashboard.usuarioVerificado && (
              <section className="client-verification" aria-label="Estado de verificación">
                <span className="material-symbols-outlined" aria-hidden="true">gpp_maybe</span>
                <div>
                  <h2>Completa tu perfil para obtener la insignia</h2>
                  <p>Tu cuenta está activa. La insignia se habilitará automáticamente cuando completes los requisitos.</p>
                  {dashboard.perfilIncompleto && dashboard.requisitosFaltantes.length > 0 && (
                    <>
                      <p className="client-missing">Completa estos campos: {dashboard.requisitosFaltantes.join(', ')}.</p>
                      <Link className="client-verification-action" to="/perfil-cliente/editar"><span className="material-symbols-outlined" aria-hidden="true">edit</span>Completar perfil</Link>
                    </>
                  )}
                </div>
                <span className="client-review-badge">Activa</span>
              </section>
            )}

            {/* ── KPIs ── */}
            <section aria-label="Estadísticas de proyectos" className="client-stats">
              <ClientStat icon="construction" label="Proyectos activos"    value={dashboard.proyectosEnProgreso}                       tone="orange" />
              <ClientStat icon="groups"       label="Miembros de equipo"   value={dashboard.miembrosEquipo}                             tone="blue"   />
              <ClientStat icon="payments"     label="Presupuesto total"    value={currencyFormatter.format(dashboard.presupuestoTotal)} tone="purple" />
              <ClientStat icon="task_alt"     label="Tasa de finalización" value={`${dashboard.tasaExito}%`}                           tone="green"  />
            </section>

            {/* ── Grid principal ── */}
            <div className="client-lower-grid">

              {/* Proyectos recientes */}
              <section className="client-project-section" id="proyectos">
                <div className="client-section-heading">
                  <div><p className="client-eyebrow">Actividad</p><h2>Proyectos recientes</h2></div>
                  <span>{dashboard.totalProyectos} en total</span>
                </div>

                {dashboard.proyectosRecientes.length ? (
                  <div className="client-project-list">
                    {dashboard.proyectosRecientes.map((project) => (
                      <article className="client-project" key={project.id}>
                        {/* Topline: estado + tipo */}
                        <div className="client-project-topline">
                          <span className={`client-project-state state-${project.estado.toLowerCase()}`}>
                            {stateLabels[project.estado] || project.estado}
                          </span>
                          {project.tipoProyecto && <span className="client-project-type">{project.tipoProyecto}</span>}
                        </div>

                        <h3>{project.titulo || 'Proyecto sin título'}</h3>
                        <p>{project.descripcion || 'Sin descripción disponible.'}</p>

                        {/* Barra de progreso visual */}
                        <ProjectProgressBar estado={project.estado} />

                        {/* Meta: ubicación, equipo, presupuesto */}
                        <div className="client-project-meta">
                          <span><span className="material-symbols-outlined" aria-hidden="true">location_on</span>{project.ubicacion || 'Ubicación pendiente'}</span>
                          <span>{project.miembrosEquipo} miembros</span>
                          <strong>{project.presupuesto == null ? 'Sin presupuesto' : currencyFormatter.format(project.presupuesto)}</strong>
                        </div>

                        {/* Panel de riesgos compacto */}
                        <ProjectRiskPanel compact fechaEntrega={project.fechaEntrega} indicators={project.indicadores} />

                        <Link className="profile-open-link" to={`/proyectos/${project.id}`}>
                          Abrir proyecto y gestionar fases
                          <span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
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

              {/* Sidebar derecho: perfil + próximos hitos + actividad */}
              <section className="client-profile" id="perfil">
                <p className="client-eyebrow">Cuenta</p>
                <h2>Estado del perfil</h2>

                {/* Email / username */}
                <div className="client-profile-email">
                  <span className="material-symbols-outlined" aria-hidden="true">mail</span>
                  <span>{dashboard.username}</span>
                </div>

                {/* Estado verificación */}
                <div className="client-profile-status">
                  <span className={`client-status-dot ${dashboard.usuarioVerificado ? 'is-verified' : ''}`} />
                  <span>{dashboard.usuarioVerificado ? 'Cuenta verificada' : 'Insignia pendiente'}</span>
                </div>

                {/* Barra de completitud */}
                <ProfileCompletenessBar pct={completeness} />

                {/* Total proyectos */}
                <div className="client-profile-total">
                  <span>Proyectos registrados</span><strong>{dashboard.totalProyectos}</strong>
                </div>

                {/* Próximos hitos */}
                <div className="client-milestones">
                  <p className="client-milestones-title">
                    <span className="material-symbols-outlined" aria-hidden="true">event</span>
                    Próximos hitos
                  </p>
                  {dashboard.proyectosRecientes.filter((p) => p.estado === 'EN_PROGRESO').length > 0 ? (
                    <ul className="client-milestone-list" role="list">
                      {dashboard.proyectosRecientes
                        .filter((p) => p.estado === 'EN_PROGRESO')
                        .slice(0, 3)
                        .map((p) => (
                          <li className="client-milestone-item" key={p.id}>
                            <span className="material-symbols-outlined" aria-hidden="true">radio_button_unchecked</span>
                            <span>
                              <strong>{p.titulo || 'Sin título'}</strong>
                              {p.fechaEntrega && (
                                <small>
                                  Entrega:{' '}
                                  {new Date(p.fechaEntrega).toLocaleDateString('es-CO', {
                                    day: '2-digit',
                                    month: 'short',
                                    year: 'numeric',
                                  })}
                                </small>
                              )}
                            </span>
                          </li>
                        ))}
                    </ul>
                  ) : (
                    <p className="client-milestones-empty">No hay proyectos activos en este momento.</p>
                  )}
                </div>

                <Link className="profile-open-link" to="/perfil-cliente">
                  Ver perfil completo
                  <span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
                </Link>
              </section>
            </div>
          </>
        )}
      </main>
    </div>
  )
}

export function DashboardCliente() {
  return <ClientDashboard />
}
