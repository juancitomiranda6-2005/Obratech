import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { clientProfileService } from '../../services/perfilService.js'

function ClientProfile({ editing }) {
  const navigate = useNavigate()
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    document.title = editing ? 'Editar Perfil | ObraTech' : 'Perfil Cliente | ObraTech'
    let active = true
    clientProfileService.getMine()
      .then(({ data }) => {
        if (!active) return
        setProfile(data)
        setForm({ nombre: data.nombre, apellido: data.apellido, telefono: data.telefono, empresa: data.empresa })
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudo cargar el perfil.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [editing])

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const save = async (event) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const { data } = await clientProfileService.updateMine(form)
      setProfile(data)
      setNotice('Tus datos se guardaron correctamente.')
      navigate('/perfil-cliente')
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo guardar el perfil.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="project-page-shell"><p className="admin-loading" role="status">Cargando perfil…</p></div>

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación cliente" className="client-nav"><Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Resumen</Link><Link to="/mis-proyectos"><span className="material-symbols-outlined" aria-hidden="true">folder_open</span>Mis proyectos</Link><Link to="/perfil-cliente"><span className="material-symbols-outlined" aria-hidden="true">person</span>Mi perfil</Link></nav>
      </aside>

      <main className="client-main">
        <header className="client-header"><div><p className="client-eyebrow">Cuenta cliente</p><h1>{editing ? 'Editar perfil' : 'Mi perfil'}</h1><p>Información de contacto y verificación de la cuenta.</p></div>{!editing && <Link className="button button-primary" to="/perfil-cliente/editar"><span className="material-symbols-outlined" aria-hidden="true">edit</span>Editar perfil</Link>}</header>
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}
        {profile && editing && form && (
          <form className="publish-form profile-edit-form" onSubmit={save}>
            <label className="publish-field"><span>Nombre</span><input autoComplete="given-name" maxLength={100} name="nombre" onChange={updateField} required value={form.nombre} /></label>
            <label className="publish-field"><span>Apellido</span><input autoComplete="family-name" maxLength={100} name="apellido" onChange={updateField} required value={form.apellido} /></label>
            <label className="publish-field"><span>Empresa</span><input maxLength={140} name="empresa" onChange={updateField} required value={form.empresa} /></label>
            <label className="publish-field"><span>Teléfono de contacto</span><input autoComplete="tel" maxLength={24} name="telefono" onChange={updateField} required type="tel" value={form.telefono} /></label>
            <div className="publish-actions publish-field-full"><button className="button button-primary" disabled={saving} type="submit"><span className="material-symbols-outlined" aria-hidden="true">save</span>{saving ? 'Guardando…' : 'Guardar cambios'}</button><Link className="button button-secondary" to="/perfil-cliente">Cancelar</Link></div>
          </form>
        )}
        {profile && !editing && (
          <div className="client-profile-page-grid">
            <section className="client-profile identity-profile">
              <span className="client-avatar profile-large-avatar">{(profile.nombre || profile.username).slice(0, 1).toUpperCase()}</span>
              <p className="client-eyebrow">Cliente</p>
              <h2>{`${profile.nombre} ${profile.apellido}`.trim() || profile.username}</h2>
              <span className={`profile-verification ${profile.verificado ? 'is-verified' : ''}`}><span className="material-symbols-outlined" aria-hidden="true">{profile.verificado ? 'verified' : 'pending'}</span>{profile.verificado ? 'Cliente verificado' : 'Pendiente de verificación'}</span>
            </section>
            <section className="client-profile profile-details">
              <p className="client-eyebrow">Información</p><h2>Detalles de la cuenta</h2>
              <ProfileField label="Correo electrónico" value={profile.username} icon="mail" />
              <ProfileField label="Teléfono" value={profile.telefono || 'No especificado'} icon="call" />
              <ProfileField label="Empresa" value={profile.empresa || 'Independiente'} icon="business" />
              <ProfileField label="Miembro desde" value={profile.creado ? new Intl.DateTimeFormat('es-CO', { month: 'long', year: 'numeric' }).format(new Date(profile.creado)) : 'Recientemente'} icon="calendar_today" />
            </section>
            <section className="client-stats profile-stats">
              <ProfileStat label="Proyectos publicados" value={profile.proyectosPublicados} />
              <ProfileStat label="En progreso" value={profile.proyectosEnProgreso} />
            </section>
          </div>
        )}
      </main>
    </div>
  )
}

function ProfileField({ label, value, icon }) {
  return <div className="profile-field-row"><span className="material-symbols-outlined" aria-hidden="true">{icon}</span><div><small>{label}</small><strong>{value}</strong></div></div>
}

function ProfileStat({ label, value }) {
  return <article className="client-stat tone-orange"><span className="material-symbols-outlined" aria-hidden="true">folder_open</span><div><strong>{value}</strong><p>{label}</p></div></article>
}

export function PerfilCliente({ editing = false }) {
  return <ClientProfile editing={editing} />
}
