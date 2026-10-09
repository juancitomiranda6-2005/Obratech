import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminService } from '../../services/adminService.js'

const sections = [
  { id: 'proyectos', label: 'Proyectos', icon: 'grading' },
  { id: 'contratistas', label: 'Contratistas', icon: 'engineering' },
  { id: 'clientes', label: 'Clientes', icon: 'person' },
  { id: 'verificaciones', label: 'Verificaciones', icon: 'verified_user' },
  { id: 'inactivos', label: 'Inactivos', icon: 'block' },
]

const initialSearch = {
  proyectos: '',
  contratistas: '',
  clientes: '',
  verificaciones: '',
  inactivos: '',
}

function matchesSearch(item, query) {
  const value = query.trim().toLocaleLowerCase()
  if (!value) return true
  return [item.nombre, item.username, item.titulo, item.cliente, item.descripcion]
    .filter(Boolean)
    .some((field) => field.toLocaleLowerCase().includes(value))
}

function roleLabel(roles = []) {
  return roles
    .filter((role) => role !== 'ROLE_USER')
    .map((role) => ({
      ROLE_CONTRACTOR: 'Contratista',
      ROLE_CLIENT: 'Cliente',
      ROLE_WORKER: 'Trabajador',
    })[role] || role.replace('ROLE_', ''))
    .join(', ')
}

function SearchField({ label, value, onChange }) {
  return (
    <label className="admin-search">
      <span className="material-symbols-outlined" aria-hidden="true">search</span>
      <span className="sr-only">{label}</span>
      <input
        onChange={(event) => onChange(event.target.value)}
        placeholder={label}
        type="search"
        value={value}
      />
    </label>
  )
}

function EmptyState({ children }) {
  return <p className="admin-empty">{children}</p>
}

function AdminDashboard({ onLogout }) {
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [search, setSearch] = useState(initialSearch)
  const [pendingAction, setPendingAction] = useState('')

  const loadDashboard = async () => {
    setLoading(true)
    setError('')
    try {
      const { data } = await adminService.getDashboard()
      setDashboard(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo cargar el panel administrativo.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    document.title = 'Panel de Administración | ObraTech'
    let active = true
    adminService.getDashboard()
      .then(({ data }) => {
        if (active) setDashboard(data)
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudo cargar el panel administrativo.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => { active = false }
  }, [])

  const runAction = async (actionKey, action, successMessage) => {
    setPendingAction(actionKey)
    setNotice('')
    setError('')
    try {
      await action()
      setNotice(successMessage)
      await loadDashboard()
    } catch (requestError) {
      const responseData = requestError.response?.data
      const missing = responseData?.missingRequirements
      const detail = Array.isArray(missing) && missing.length
        ? ` Faltan: ${missing.join(', ')}.`
        : ''
      setError(`${responseData?.error || 'No se pudo completar la acción.'}${detail}`)
    } finally {
      setPendingAction('')
    }
  }

  const updateSearch = (section, value) => setSearch((current) => ({ ...current, [section]: value }))
  const filtered = (items, section) => (items || []).filter((item) => matchesSearch(item, search[section]))

  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <a className="admin-brand" href="#top">
          <span className="admin-brand-icon material-symbols-outlined" aria-hidden="true">admin_panel_settings</span>
          <span>ObraTech <strong>Admin</strong></span>
        </a>
        <nav aria-label="Secciones del panel" className="admin-nav">
          <a href="#top"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Resumen general</a>
          <Link to="/admin/usuarios"><span className="material-symbols-outlined" aria-hidden="true">manage_accounts</span>Gestión de usuarios</Link>
          {sections.map((section) => (
            <a href={`#seccion-${section.id}`} key={section.id}>
              <span className="material-symbols-outlined" aria-hidden="true">{section.icon}</span>
              {section.label}
            </a>
          ))}
        </nav>
        <div className="admin-sidebar-footer">
          <div className="admin-user-badge">
            <span className="material-symbols-outlined" aria-hidden="true">shield</span>
            <div><strong>Administrador</strong><small>Sesión verificada</small></div>
          </div>
          <button className="admin-logout" onClick={onLogout} type="button">
            <span className="material-symbols-outlined" aria-hidden="true">logout</span>
            Cerrar sesión
          </button>
        </div>
      </aside>

      <main className="admin-main" id="top">
        <header className="admin-header">
          <div>
            <p className="admin-eyebrow">Moderación y operaciones</p>
            <h1>Centro de Moderación</h1>
            <p>Supervisa y valida las funciones globales de ObraTech.</p>
          </div>
          <div className="admin-header-actions">
            <span className={`admin-online ${dashboard ? 'is-online' : ''}`}>
              <span />{dashboard ? 'Sistema online' : 'Conectando'}
            </span>
            <button aria-label="Actualizar panel" className="admin-icon-button" onClick={loadDashboard} title="Actualizar" type="button">
              <span className="material-symbols-outlined" aria-hidden="true">refresh</span>
            </button>
          </div>
        </header>

        {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}

        {loading && !dashboard ? (
          <div className="admin-loading" role="status">Cargando datos administrativos…</div>
        ) : dashboard ? (
          <>
            <section aria-label="Resumen de actividad" className="admin-stats">
              <Stat label="Total usuarios" value={dashboard.totalUsuarios} tone="blue" />
              <Stat label="Proyectos" value={dashboard.totalProyectos} tone="orange" />
              <Stat label="Proyectos pendientes" value={dashboard.proyectosPendientes} tone="yellow" />
              <Stat label="Verificaciones pendientes" value={dashboard.usuariosPendientes} tone="red" />
            </section>

            <div className="admin-content-grid">
              <section className="admin-section" id="seccion-proyectos">
                <SectionHeader title="Proyectos pendientes" count={dashboard.proyectosPendientes} tone="yellow" />
                <SearchField label="Buscar proyecto o cliente" value={search.proyectos} onChange={(value) => updateSearch('proyectos', value)} />
                <div className="admin-list admin-list-scroll">
                  {filtered(dashboard.proyectos, 'proyectos').map((project) => (
                    <article className="admin-project" key={project.id}>
                      <div>
                        <h3>{project.titulo || 'Proyecto sin título'}</h3>
                        <p>{project.descripcion || 'Sin descripción disponible.'}</p>
                        <small>Cliente: {project.cliente || 'Genérico'}</small>
                      </div>
                      <div className="admin-action-row">
                        <button
                          className="admin-action admin-action-danger"
                          disabled={pendingAction === project.id}
                          onClick={() => runAction(project.id, () => adminService.updateProjectValidation(project.id, 'RECHAZADO'), 'Proyecto rechazado.')}
                          type="button"
                        >Rechazar</button>
                        <button
                          className="admin-action admin-action-success"
                          disabled={pendingAction === project.id}
                          onClick={() => runAction(project.id, () => adminService.updateProjectValidation(project.id, 'APROBADO'), 'Proyecto aprobado.')}
                          type="button"
                        >Aprobar</button>
                      </div>
                    </article>
                  ))}
                  {!filtered(dashboard.proyectos, 'proyectos').length && <EmptyState>No hay proyectos pendientes para mostrar.</EmptyState>}
                </div>
              </section>

              <section className="admin-section" id="seccion-contratistas">
                <SectionHeader title="Directorio de contratistas" count={dashboard.contratistas.length} tone="blue" />
                <SearchField label="Buscar por nombre o correo" value={search.contratistas} onChange={(value) => updateSearch('contratistas', value)} />
                <ProfileList
                  items={filtered(dashboard.contratistas, 'contratistas')}
                  pendingAction={pendingAction}
                  onToggle={(profile) => runAction(profile.id, () => adminService.updateProfileActive(profile.id, !profile.activo), profile.activo ? 'Contratista suspendido.' : 'Contratista activado.')}
                />
              </section>

              <section className="admin-section" id="seccion-clientes">
                <SectionHeader title="Directorio de clientes" count={dashboard.clientes.length} tone="green" />
                <SearchField label="Buscar por nombre o correo" value={search.clientes} onChange={(value) => updateSearch('clientes', value)} />
                <ProfileList
                  items={filtered(dashboard.clientes, 'clientes')}
                  pendingAction={pendingAction}
                  onToggle={(profile) => runAction(profile.id, () => adminService.updateProfileActive(profile.id, !profile.activo), profile.activo ? 'Cliente suspendido.' : 'Cliente activado.')}
                />
              </section>

              <section className="admin-section" id="seccion-verificaciones">
                <SectionHeader title="Verificación de usuarios" count={dashboard.usuariosPendientes} tone="purple" />
                <SearchField label="Filtrar por nombre o correo" value={search.verificaciones} onChange={(value) => updateSearch('verificaciones', value)} />
                <div className="admin-list admin-list-grid">
                  {filtered(dashboard.verificaciones, 'verificaciones').map((profile) => (
                    <article className="admin-profile" key={profile.id}>
                      <div className="admin-verification-content">
                        <ProfileSummary profile={profile} />
                        {profile.missingRequirements?.length ? (
                          <div className="admin-requirements">
                            <strong>Requisitos pendientes</strong>
                            <ul>{profile.missingRequirements.map((requirement) => <li key={requirement}>{requirement}</li>)}</ul>
                          </div>
                        ) : <p className="admin-requirements-ready">Perfil completo para verificación.</p>}
                      </div>
                      <button
                        className="admin-action admin-action-verify"
                        disabled={pendingAction === profile.id || Boolean(profile.missingRequirements?.length)}
                        onClick={() => runAction(profile.id, () => adminService.updateProfileVerified(profile.id, true), 'Usuario verificado.')}
                        title={profile.missingRequirements?.length ? 'El perfil aún tiene requisitos pendientes' : 'Verificar perfil'}
                        type="button"
                      >Verificar</button>
                    </article>
                  ))}
                  {!filtered(dashboard.verificaciones, 'verificaciones').length && <EmptyState>Todos los usuarios están verificados.</EmptyState>}
                </div>
              </section>

              <section className="admin-section admin-section-wide" id="seccion-inactivos">
                <SectionHeader title="Usuarios inactivos / suspendidos" count={dashboard.usuariosInactivos} tone="red" />
                <SearchField label="Buscar usuario inactivo" value={search.inactivos} onChange={(value) => updateSearch('inactivos', value)} />
                <ProfileList
                  items={filtered(dashboard.inactivos, 'inactivos')}
                  emptyText="No hay usuarios inactivos en este momento."
                  pendingAction={pendingAction}
                  onToggle={(profile) => runAction(profile.id, () => adminService.updateProfileActive(profile.id, true), 'Usuario activado.')}
                />
              </section>
            </div>
          </>
        ) : null}
      </main>
    </div>
  )
}

function Stat({ label, value, tone }) {
  return (
    <div className={`admin-stat tone-${tone}`}>
      <p>{label}</p>
      <strong>{value}</strong>
    </div>
  )
}

function SectionHeader({ title, count, tone }) {
  return (
    <div className="admin-section-heading">
      <div><span className={`section-mark tone-${tone}`} /><h2>{title}</h2></div>
      <span className={`admin-count tone-${tone}`}>{count}</span>
    </div>
  )
}

function ProfileSummary({ profile }) {
  return (
    <div className="admin-profile-summary">
      <span className="admin-avatar material-symbols-outlined" aria-hidden="true">{profile.roles.includes('ROLE_CONTRACTOR') ? 'engineering' : profile.roles.includes('ROLE_WORKER') ? 'work' : 'person'}</span>
      <div className="admin-profile-copy">
        <strong>{profile.nombre || profile.username}</strong>
        <span>{profile.username}</span>
        <small>{roleLabel(profile.roles)}</small>
      </div>
    </div>
  )
}

function ProfileList({ items, emptyText = 'No hay perfiles para mostrar.', onToggle, pendingAction }) {
  return (
    <div className="admin-list">
      {items.map((profile) => (
        <article className="admin-profile" key={profile.id}>
          <ProfileSummary profile={profile} />
          <div className="admin-profile-actions">
            <span className={`admin-state ${profile.activo ? 'state-active' : 'state-inactive'}`}>
              {profile.activo ? 'Activo' : 'Inactivo'}
            </span>
            <button
              aria-label={`${profile.activo ? 'Suspender' : 'Activar'} ${profile.nombre || profile.username}`}
              className={`admin-icon-button ${profile.activo ? 'icon-danger' : 'icon-success'}`}
              disabled={pendingAction === profile.id}
              onClick={() => onToggle(profile)}
              title={profile.activo ? 'Suspender usuario' : 'Activar usuario'}
              type="button"
            >
              <span className="material-symbols-outlined" aria-hidden="true">power_settings_new</span>
            </button>
          </div>
        </article>
      ))}
      {!items.length && <EmptyState>{emptyText}</EmptyState>}
    </div>
  )
}

export function DashboardAdmin({ onLogout }) {
  return <AdminDashboard onLogout={onLogout} />
}
