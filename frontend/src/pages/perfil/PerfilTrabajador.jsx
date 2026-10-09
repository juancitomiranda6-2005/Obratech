import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { workerProfileService } from '../../services/perfilService.js'

export function PerfilTrabajador({ editing = false }) {
  const navigate = useNavigate()
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
    const [cvFile, setCvFile] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    document.title = editing ? 'Editar Perfil Trabajador | ObraTech' : 'Perfil Trabajador | ObraTech'
    let active = true
    workerProfileService.getMine()
      .then(({ data }) => {
        if (!active) return
        setProfile(data)
        setForm({
          nombre: data.nombre || '',
          apellido: data.apellido || '',
          telefono: data.telefono || '',
          oficio: data.oficio || '',
          experiencia: data.experiencia ?? 0,
          disponible: Boolean(data.disponible),
          descripcion: data.descripcion || '',
        })
      })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudo cargar el perfil del trabajador.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [editing])

  const updateField = (event) => {
    const { name, value, type, checked } = event.target
    setForm((current) => ({
      ...current,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const openCv = async () => {
    if (!profile?.cvUrl) return
    try {
      const { data } = await workerProfileService.getCv(profile.cvUrl)
      const url = URL.createObjectURL(data)
      window.open(url, '_blank', 'noopener,noreferrer')
      window.setTimeout(() => URL.revokeObjectURL(url), 60000)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo abrir el CV.')
    }
  }

  const save = async (event) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const payload = {
        ...form,
        experiencia: Number(form.experiencia),
        disponible: Boolean(form.disponible),
      }
      const { data } = await workerProfileService.updateMine(payload, cvFile)
      setProfile(data)
      navigate('/perfil-trabajador')
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
        <nav aria-label="Navegación trabajador" className="client-nav">
          <Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link>
          <Link to="/postulaciones"><span className="material-symbols-outlined" aria-hidden="true">assignment_turned_in</span>Proyectos disponibles</Link>
          <Link to="/mis-postulaciones"><span className="material-symbols-outlined" aria-hidden="true">mail</span>Mis postulaciones</Link>
          <Link className="is-current" to="/perfil-trabajador"><span className="material-symbols-outlined" aria-hidden="true">person</span>Mi perfil</Link>
        </nav>
      </aside>

      <main className="client-main">
        <header className="client-header">
          <div>
            <p className="client-eyebrow">Perfil profesional</p>
            <h1>{editing ? 'Editar perfil laboral' : `${profile?.nombre || ''} ${profile?.apellido || ''}`.trim() || 'Mi perfil'}</h1>
            <p>{editing ? 'Actualiza tus datos y disponibilidad para recibir proyectos.' : profile?.oficio || 'Trabajador'}</p>
          </div>
          {!editing && <Link className="button button-primary" to="/perfil-trabajador/editar"><span className="material-symbols-outlined" aria-hidden="true">edit</span>Editar perfil</Link>}
        </header>

        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}

        {profile && editing && form && (
          <form className="publish-form profile-edit-form" onSubmit={save}>
            <label className="publish-field"><span>Nombre</span><input maxLength={100} name="nombre" onChange={updateField} required value={form.nombre} /></label>
            <label className="publish-field"><span>Apellido</span><input maxLength={100} name="apellido" onChange={updateField} required value={form.apellido} /></label>
            <label className="publish-field"><span>Teléfono</span><input maxLength={24} name="telefono" onChange={updateField} required type="tel" value={form.telefono} /></label>
            <label className="publish-field"><span>Oficio</span><input maxLength={120} name="oficio" onChange={updateField} required value={form.oficio} /></label>
            <label className="publish-field"><span>Años de experiencia</span><input min="0" name="experiencia" onChange={updateField} required type="number" value={form.experiencia} /></label>
            <label className="publish-field publish-field-full"><span>Descripción profesional</span><textarea maxLength={2000} name="descripcion" onChange={updateField} rows={5} value={form.descripcion} /></label>
                        <label className="publish-field publish-field-full"><span>Hoja de vida (PDF, DOC o DOCX, máximo 10 MB)</span><input accept=".pdf,.doc,.docx" onChange={(event) => setCvFile(event.target.files?.[0] || null)} type="file" />{profile.cvUrl && <small>Ya tienes un CV cargado. Solo se reemplaza si seleccionas otro archivo.</small>}</label>
            <label className="publish-field publish-field-full boolean-field-row">
              <input checked={form.disponible} name="disponible" onChange={updateField} type="checkbox" />
              <span>Estoy disponible para recibir proyectos</span>
            </label>
            <div className="publish-actions publish-field-full">
              <button className="button button-primary" disabled={saving} type="submit"><span className="material-symbols-outlined" aria-hidden="true">save</span>{saving ? 'Guardando…' : 'Guardar cambios'}</button>
              <Link className="button button-secondary" to="/perfil-trabajador">Cancelar</Link>
            </div>
          </form>
        )}

        {profile && !editing && (
          <div className="client-profile-page-grid">
            <section className="client-profile identity-profile">
              <span className="client-avatar profile-large-avatar">{(profile.nombre || profile.username).slice(0, 1).toUpperCase()}</span>
              <p className="client-eyebrow">Trabajador</p>
              <h2>{`${profile.nombre} ${profile.apellido}`.trim() || profile.username}</h2>
              <span className={`profile-verification ${profile.verificado ? 'is-verified' : ''}`}><span className="material-symbols-outlined" aria-hidden="true">{profile.verificado ? 'verified' : 'pending'}</span>{profile.verificado ? 'Perfil verificado' : 'Pendiente de verificación'}</span>
            </section>

            <section className="client-profile profile-details">
              <p className="client-eyebrow">Información</p>
              <h2>Datos profesionales</h2>
              <ProfileField label="Correo electrónico" value={profile.username} icon="mail" />
              <ProfileField label="Teléfono" value={profile.telefono || 'No especificado'} icon="call" />
              <ProfileField label="Oficio" value={profile.oficio || 'No especificado'} icon="build" />
              <ProfileField label="Experiencia" value={`${profile.experiencia ?? 0} años`} icon="work_history" />
              <ProfileField label="Disponibilidad" value={profile.disponible ? 'Disponible para proyectos' : 'No disponible'} icon={profile.disponible ? 'check_circle' : 'schedule'} />
                          <div className="profile-field-row"><span className="material-symbols-outlined" aria-hidden="true">description</span><div><small>Hoja de vida</small>{profile.cvUrl ? <button className="profile-file-link" onClick={openCv} type="button">Abrir CV<span className="material-symbols-outlined" aria-hidden="true">open_in_new</span></button> : <strong>Sin CV cargado</strong>}</div></div>
            </section>

            <section className="client-stats profile-stats">
              <ProfileStat label="Disponibilidad" value={profile.disponible ? 'Sí' : 'No'} />
              <ProfileStat label="Experiencia" value={`${profile.experiencia ?? 0} años`} />
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
  return <article className="client-stat tone-orange"><span className="material-symbols-outlined" aria-hidden="true">person</span><div><strong>{value}</strong><p>{label}</p></div></article>
}
