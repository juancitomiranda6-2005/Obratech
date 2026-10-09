import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminService } from '../../services/adminService.js'

const roleLabels = {
  ROLE_CLIENT: 'Cliente',
  ROLE_CONTRACTOR: 'Contratista',
  ROLE_WORKER: 'Trabajador',
}

function labelRoles(roles = []) {
  return roles.map((role) => roleLabels[role] || role.replace('ROLE_', '')).join(', ')
}

function UserCard({ profile, pending, onActive, onVerification, onDelete }) {
  const displayName = profile.nombre || profile.username
  const missing = profile.missingRequirements || []

  return (
    <article className="admin-user-row">
      <div className="admin-profile-summary">
        <span className="admin-avatar material-symbols-outlined" aria-hidden="true">
          {profile.roles.includes('ROLE_CONTRACTOR') ? 'engineering' : profile.roles.includes('ROLE_WORKER') ? 'work' : 'person'}
        </span>
        <div className="admin-profile-copy">
          <Link className="admin-user-detail-link" to={`/admin/perfiles/${profile.id}`}>{displayName}</Link>
          <span>{profile.username}</span>
          <small>{labelRoles(profile.roles)}</small>
        </div>
      </div>
      <div className="admin-user-statuses">
        <span className={`admin-state ${profile.activo ? 'state-active' : 'state-inactive'}`}>{profile.activo ? 'Activo' : 'Suspendido'}</span>
        <span className={`admin-state ${profile.verificado ? 'state-active' : 'state-pending'}`}>{profile.verificado ? 'Verificado' : 'Pendiente'}</span>
      </div>
      <div className="admin-user-actions">
        <button aria-label={`Eliminar ${displayName}`} className="admin-icon-button icon-danger" disabled={pending} onClick={() => onDelete(profile)} title="Eliminar usuario" type="button"><span className="material-symbols-outlined" aria-hidden="true">delete</span></button>
        <button
          aria-label={`${profile.activo ? 'Suspender' : 'Activar'} ${displayName}`}
          className={`admin-icon-button ${profile.activo ? 'icon-danger' : 'icon-success'}`}
          disabled={pending}
          onClick={() => onActive(profile)}
          title={profile.activo ? 'Suspender usuario' : 'Activar usuario'}
          type="button"
        ><span className="material-symbols-outlined" aria-hidden="true">power_settings_new</span></button>
        <button
          className="admin-action admin-action-verify"
          disabled={pending || (!profile.verificado && missing.length > 0)}
          onClick={() => onVerification(profile)}
          title={!profile.verificado && missing.length ? `Requisitos pendientes: ${missing.join(', ')}` : profile.verificado ? 'Quitar verificación' : 'Verificar usuario'}
          type="button"
        >{profile.verificado ? 'Quitar verificación' : 'Verificar'}</button>
      </div>
      {!profile.verificado && missing.length > 0 && (
        <p className="admin-user-missing">Pendiente: {missing.join(', ')}</p>
      )}
    </article>
  )
}

export function Usuarios({ onLogout, createWorkerInitially = false }) {
  const [dashboard, setDashboard] = useState(null)
  const [query, setQuery] = useState('')
  const [role, setRole] = useState('')
  const [state, setState] = useState('')
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [pendingId, setPendingId] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [userToDelete, setUserToDelete] = useState(null)
  const [workerFormOpen, setWorkerFormOpen] = useState(createWorkerInitially)
  const [workerForm, setWorkerForm] = useState({ email: '', nombre: '', apellido: '', telefono: '', oficio: '', experiencia: 0 })
  const [creatingWorker, setCreatingWorker] = useState(false)
  const [createdWorker, setCreatedWorker] = useState(null)
  const [copiedPassword, setCopiedPassword] = useState(false)

  const loadUsers = async (showLoading = false) => {
    if (showLoading) setRefreshing(true)
    setError('')
    try {
      const { data } = await adminService.getDashboard()
      setDashboard(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudieron cargar los usuarios.')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    document.title = 'Gestión de usuarios | ObraTech'
    let active = true
    adminService.getDashboard()
      .then(({ data }) => { if (active) setDashboard(data) })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.error || 'No se pudieron cargar los usuarios.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const runAction = async (profile, action, successMessage) => {
    setPendingId(profile.id)
    setNotice('')
    setError('')
    try {
      await action()
      setNotice(successMessage)
      await loadUsers()
    } catch (requestError) {
      const responseData = requestError.response?.data
      const missing = Array.isArray(responseData?.missingRequirements) ? ` Faltan: ${responseData.missingRequirements.join(', ')}.` : ''
      setError(`${responseData?.error || 'No se pudo completar la acción.'}${missing}`)
    } finally {
      setPendingId('')
    }
  }

  const users = (dashboard?.usuarios || []).filter((profile) => {
    const matchesQuery = `${profile.nombre} ${profile.username} ${labelRoles(profile.roles)}`.toLocaleLowerCase().includes(query.trim().toLocaleLowerCase())
    const matchesRole = !role || profile.roles.includes(role)
    const matchesState = !state || (state === 'active' ? profile.activo : !profile.activo)
    return matchesQuery && matchesRole && matchesState
  })
  const allUsers = dashboard?.usuarios || []

  const deleteUser = async () => {
    if (!userToDelete) return
    await runAction(
      userToDelete,
      () => adminService.deleteUser(userToDelete.id),
      'Usuario eliminado del sistema.',
    )
    setUserToDelete(null)
  }

  const createWorker = async (event) => {
    event.preventDefault()
    setCreatingWorker(true)
    setError('')
    setNotice('')
    try {
      const { data } = await adminService.createWorker({ ...workerForm, experiencia: Number(workerForm.experiencia) })
      setCreatedWorker(data)
      setWorkerFormOpen(false)
      setWorkerForm({ email: '', nombre: '', apellido: '', telefono: '', oficio: '', experiencia: 0 })
      await loadUsers()
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo crear el trabajador.')
    } finally {
      setCreatingWorker(false)
    }
  }

  const copyTemporaryPassword = async () => {
    try {
      await navigator.clipboard.writeText(createdWorker.temporaryPassword)
      setCopiedPassword(true)
    } catch {
      setError('No se pudo copiar la contraseña temporal.')
    }
  }

  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <Link className="admin-brand" to="/">
          <span className="admin-brand-icon material-symbols-outlined" aria-hidden="true">admin_panel_settings</span>
          <span>ObraTech <strong>Admin</strong></span>
        </Link>
        <nav aria-label="Navegación administrativa" className="admin-nav">
          <Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Resumen general</Link>
          <Link className="is-current" to="/admin/usuarios"><span className="material-symbols-outlined" aria-hidden="true">manage_accounts</span>Gestión de usuarios</Link>
        </nav>
        <div className="admin-sidebar-footer">
          <div className="admin-user-badge"><span className="material-symbols-outlined" aria-hidden="true">shield</span><div><strong>Administrador</strong><small>Sesión verificada</small></div></div>
          <button className="admin-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button>
        </div>
      </aside>

      <main className="admin-main">
        <header className="admin-header">
          <div><p className="admin-eyebrow">Administración</p><h1>Usuarios y perfiles</h1><p>Consulta cuentas, estados y requisitos de verificación.</p></div>
          <div className="admin-header-actions"><button className="admin-action admin-action-success" onClick={() => setWorkerFormOpen(true)} type="button"><span className="material-symbols-outlined" aria-hidden="true">person_add</span>Crear trabajador</button><button aria-label="Actualizar usuarios" className="admin-icon-button" disabled={refreshing} onClick={() => loadUsers(true)} title="Actualizar" type="button"><span className="material-symbols-outlined" aria-hidden="true">refresh</span></button></div>
        </header>

        {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}

        <section aria-label="Resumen de usuarios" className="admin-stats">
          <div className="admin-stat tone-blue"><p>Total de perfiles</p><strong>{allUsers.length}</strong></div>
          <div className="admin-stat tone-green"><p>Verificados</p><strong>{allUsers.filter((profile) => profile.verificado).length}</strong></div>
          <div className="admin-stat tone-yellow"><p>Pendientes</p><strong>{allUsers.filter((profile) => !profile.verificado).length}</strong></div>
          <div className="admin-stat tone-red"><p>Suspendidos</p><strong>{allUsers.filter((profile) => !profile.activo).length}</strong></div>
        </section>

        <section className="admin-users-section">
          <div className="admin-user-filters">
            <label className="admin-search"><span className="material-symbols-outlined" aria-hidden="true">search</span><span className="sr-only">Buscar usuario</span><input onChange={(event) => setQuery(event.target.value)} placeholder="Buscar por nombre, correo o rol" type="search" value={query} /></label>
            <label><span className="sr-only">Filtrar por rol</span><select className="admin-user-select" onChange={(event) => setRole(event.target.value)} value={role}><option value="">Todos los roles</option><option value="ROLE_CLIENT">Clientes</option><option value="ROLE_CONTRACTOR">Contratistas</option><option value="ROLE_WORKER">Trabajadores</option></select></label>
            <label><span className="sr-only">Filtrar por estado</span><select className="admin-user-select" onChange={(event) => setState(event.target.value)} value={state}><option value="">Todos los estados</option><option value="active">Activos</option><option value="inactive">Suspendidos</option></select></label>
            <span className="admin-user-result-count">{users.length} de {allUsers.length} perfiles</span>
          </div>
          {loading ? <div className="admin-loading" role="status">Cargando usuarios…</div> : (
            <div className="admin-user-list">
              {users.map((profile) => (
                <UserCard
                  key={profile.id}
                  onActive={(item) => runAction(item, () => adminService.updateProfileActive(item.id, !item.activo), item.activo ? 'Usuario suspendido.' : 'Usuario activado.')}
                  onVerification={(item) => runAction(item, () => adminService.updateProfileVerified(item.id, !item.verificado), item.verificado ? 'Verificación retirada.' : 'Usuario verificado.')}
                  onDelete={setUserToDelete}
                  pending={pendingId === profile.id}
                  profile={profile}
                />
              ))}
              {!users.length && <p className="admin-empty">No hay perfiles que coincidan con esos filtros.</p>}
            </div>
          )}
        </section>
      </main>
      {workerFormOpen && <div className="confirm-overlay" role="presentation"><section aria-labelledby="create-worker-title" aria-modal="true" className="admin-worker-dialog" role="dialog"><header><div><p className="admin-eyebrow">Nueva cuenta</p><h2 id="create-worker-title">Crear trabajador</h2></div><button aria-label="Cerrar" className="admin-icon-button" disabled={creatingWorker} onClick={() => setWorkerFormOpen(false)} type="button"><span className="material-symbols-outlined" aria-hidden="true">close</span></button></header><form className="admin-worker-form" onSubmit={createWorker}><label><span>Correo electrónico</span><input autoComplete="email" onChange={(event) => setWorkerForm((current) => ({ ...current, email: event.target.value }))} required type="email" value={workerForm.email} /></label><div className="admin-worker-fields"><label><span>Nombre</span><input onChange={(event) => setWorkerForm((current) => ({ ...current, nombre: event.target.value }))} required value={workerForm.nombre} /></label><label><span>Apellido</span><input onChange={(event) => setWorkerForm((current) => ({ ...current, apellido: event.target.value }))} required value={workerForm.apellido} /></label></div><label><span>Teléfono</span><input onChange={(event) => setWorkerForm((current) => ({ ...current, telefono: event.target.value }))} required type="tel" value={workerForm.telefono} /></label><div className="admin-worker-fields"><label><span>Oficio</span><input onChange={(event) => setWorkerForm((current) => ({ ...current, oficio: event.target.value }))} required value={workerForm.oficio} /></label><label><span>Años de experiencia</span><input min="0" onChange={(event) => setWorkerForm((current) => ({ ...current, experiencia: event.target.value }))} required type="number" value={workerForm.experiencia} /></label></div><div className="confirm-actions"><button className="button button-secondary" disabled={creatingWorker} onClick={() => setWorkerFormOpen(false)} type="button">Cancelar</button><button className="button button-primary" disabled={creatingWorker} type="submit">{creatingWorker ? 'Creando…' : 'Crear cuenta'}</button></div></form></section></div>}
      {createdWorker && <div className="confirm-overlay" role="presentation"><section aria-labelledby="worker-created-title" aria-modal="true" className="confirm-dialog" role="dialog"><span className="material-symbols-outlined confirm-icon" aria-hidden="true">check_circle</span><h2 id="worker-created-title">Trabajador creado</h2><p>Cuenta: <strong>{createdWorker.trabajador.username}</strong></p><p>Contraseña temporal (se muestra una sola vez):</p><div className="temporary-password">{createdWorker.temporaryPassword}</div><div className="confirm-actions"><button className="button button-secondary" onClick={copyTemporaryPassword} type="button"><span className="material-symbols-outlined" aria-hidden="true">content_copy</span>{copiedPassword ? 'Copiada' : 'Copiar contraseña'}</button><button className="button button-primary" onClick={() => { setCreatedWorker(null); setCopiedPassword(false) }} type="button">Listo</button></div></section></div>}
      {userToDelete && <div className="confirm-overlay" role="presentation"><section aria-labelledby="delete-user-title" aria-modal="true" className="confirm-dialog" role="dialog"><span className="material-symbols-outlined confirm-icon" aria-hidden="true">warning</span><h2 id="delete-user-title">Eliminar usuario</h2><p>¿Eliminar a “{userToDelete.nombre || userToDelete.username}”? También se eliminará su perfil asociado. Esta acción no se puede deshacer.</p><div className="confirm-actions"><button className="button button-secondary" disabled={Boolean(pendingId)} onClick={() => setUserToDelete(null)} type="button">Cancelar</button><button className="button confirm-delete" disabled={Boolean(pendingId)} onClick={deleteUser} type="button">{pendingId ? 'Eliminando…' : 'Eliminar usuario'}</button></div></section></div>}
    </div>
  )
}
