import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminService } from '../../services/adminService.js'

const roleLabels = { ROLE_CLIENT: 'Cliente', ROLE_CONTRACTOR: 'Contratista', ROLE_WORKER: 'Trabajador' }
const money = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

function DetailField({ label, value }) {
  return <div className="admin-detail-field"><small>{label}</small><strong>{value || 'No registrado'}</strong></div>
}

export function AdminPerfilDetalle({ id, onLogout }) {
  const [detail, setDetail] = useState(null)
  const [loading, setLoading] = useState(true)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const profile = detail?.perfil
  const roles = (profile?.roles || []).map((role) => roleLabels[role] || role.replace('ROLE_', '')).join(', ')

  const load = async () => {
    setError('')
    try {
      const { data } = await adminService.getProfile(id)
      setDetail(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo cargar el perfil.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    document.title = 'Detalle de perfil | ObraTech Admin'
    let active = true
    adminService.getProfile(id)
      .then(({ data }) => { if (active) setDetail(data) })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.error || 'No se pudo cargar el perfil.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id])

  const runAction = async (action, successMessage) => {
    setPending(true)
    setError('')
    setNotice('')
    try {
      await action()
      setNotice(successMessage)
      await load()
    } catch (requestError) {
      const missing = requestError.response?.data?.missingRequirements
      setError(`${requestError.response?.data?.error || 'No se pudo completar la acción.'}${Array.isArray(missing) && missing.length ? ` Faltan: ${missing.join(', ')}.` : ''}`)
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <Link className="admin-brand" to="/"><span className="admin-brand-icon material-symbols-outlined" aria-hidden="true">admin_panel_settings</span><span>ObraTech <strong>Admin</strong></span></Link>
        <nav aria-label="Navegación administrativa" className="admin-nav"><Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Resumen general</Link><Link to="/admin/usuarios"><span className="material-symbols-outlined" aria-hidden="true">manage_accounts</span>Gestión de usuarios</Link></nav>
        <div className="admin-sidebar-footer"><div className="admin-user-badge"><span className="material-symbols-outlined" aria-hidden="true">shield</span><div><strong>Administrador</strong><small>Sesión verificada</small></div></div><button className="admin-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>
      <main className="admin-main">
        <header className="admin-header"><div><p className="admin-eyebrow">Detalle administrativo</p><h1>{profile?.nombre || profile?.username || 'Perfil'}</h1><p>{roles}</p></div><Link className="admin-icon-button" aria-label="Volver a usuarios" title="Volver a usuarios" to="/admin/usuarios"><span className="material-symbols-outlined" aria-hidden="true">arrow_back</span></Link></header>
        {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {loading && <div className="admin-loading" role="status">Cargando perfil…</div>}
        {detail && profile && <>
          <section aria-label="Estado de cuenta" className="admin-detail-status">
            <span className={`admin-state ${profile.activo ? 'state-active' : 'state-inactive'}`}>{profile.activo ? 'Activo' : 'Suspendido'}</span>
            <span className={`admin-state ${profile.verificado ? 'state-active' : 'state-pending'}`}>{profile.verificado ? 'Verificado' : 'Pendiente de verificación'}</span>
            <div className="admin-user-actions"><button className={`admin-action ${profile.activo ? 'admin-action-danger' : 'admin-action-success'}`} disabled={pending} onClick={() => runAction(() => adminService.updateProfileActive(profile.id, !profile.activo), profile.activo ? 'Usuario suspendido.' : 'Usuario activado.')} type="button">{profile.activo ? 'Suspender' : 'Activar'}</button><button className="admin-action admin-action-verify" disabled={pending || (!profile.verificado && profile.missingRequirements?.length > 0)} onClick={() => runAction(() => adminService.updateProfileVerified(profile.id, !profile.verificado), profile.verificado ? 'Verificación retirada.' : 'Usuario verificado.')} type="button">{profile.verificado ? 'Quitar verificación' : 'Verificar'}</button></div>
          </section>
          {!profile.verificado && profile.missingRequirements?.length > 0 && <div className="admin-alert admin-alert-error">Requisitos faltantes: {profile.missingRequirements.join(', ')}.</div>}
          <div className="admin-detail-grid">
            <section className="admin-section admin-detail-section"><p className="admin-eyebrow">Cuenta</p><h2>Información personal</h2><DetailField label="Correo" value={detail.email || profile.username} /><DetailField label="Teléfono" value={detail.telefono} /><DetailField label="Roles" value={roles} /><DetailField label="Empresa" value={detail.empresa} /></section>
            <section className="admin-section admin-detail-section"><p className="admin-eyebrow">Profesional</p><h2>Datos de perfil</h2><DetailField label="Especialidad" value={detail.especialidad} /><DetailField label="Oficio" value={detail.oficio} /><DetailField label="Experiencia" value={detail.experiencia == null ? null : `${detail.experiencia} años`} /><DetailField label="Ubicación" value={detail.ubicacion} /><DetailField label="Descripción" value={detail.descripcion} />{detail.cvUrl && <DetailField label="CV" value="Documento registrado" />}</section>
          </div>
          <section className="admin-section admin-detail-projects"><div className="admin-section-heading"><div><span className="section-mark tone-orange" /><h2>Proyectos asociados</h2></div><span className="admin-count tone-orange">{detail.proyectos.length}</span></div>
            {detail.proyectos.length ? <div className="admin-detail-project-list">{detail.proyectos.map((project) => <article key={project.id}><div><strong>{project.titulo || 'Proyecto sin título'}</strong><small>{project.ubicacion || 'Ubicación no registrada'} · {project.estado}</small></div><span>{project.presupuesto == null ? '—' : money.format(project.presupuesto)}</span></article>)}</div> : <p className="admin-empty">Este perfil aún no tiene proyectos asociados.</p>}
          </section>
        </>}
      </main>
    </div>
  )
}
