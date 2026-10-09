import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { contractorProfileService } from '../../services/perfilService.js'

const subSpecialties = ['RETIE', 'Alta Tensión', 'Subestaciones', 'Cableado', 'Mantenimiento', 'Seguridad Industrial']
const specialties = ['Albañilería', 'Plomería', 'Electricidad', 'Carpintería', 'Pintura', 'Herrería', 'Demolición', 'Excavación', 'Gestión de Proyectos']

function ContractorProfile({ editing }) {
  const navigate = useNavigate()
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState(null)
  const [files, setFiles] = useState({})
  const [photoUrl, setPhotoUrl] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    document.title = editing ? 'Editar Perfil Contratista | ObraTech' : 'Perfil Contratista | ObraTech'
    let active = true
    contractorProfileService.getMine()
      .then(({ data }) => {
        if (!active) return
        setProfile(data)
        setForm({
          nombre: data.nombre || '', apellido: data.apellido || '', telefono: data.telefono || '',
          especialidad: data.especialidad || '', descripcion: data.descripcion || '', ciudad: data.ciudad || '',
          departamento: data.departamento || '', matriculaProfesional: data.matriculaProfesional || '',
          experiencia: data.experiencia ?? '', subespecialidades: data.subespecialidades || [],
        })
      })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.error || 'No se pudo cargar el perfil.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [editing])

  useEffect(() => {
    if (!profile?.fotoPerfilUrl) return undefined
    let objectUrl = ''
    let active = true
    contractorProfileService.getFile(profile.fotoPerfilUrl)
      .then(({ data }) => {
        if (active) {
          objectUrl = URL.createObjectURL(data)
          setPhotoUrl(objectUrl)
        }
      })
      .catch(() => setPhotoUrl(''))
    return () => {
      active = false
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [profile?.fotoPerfilUrl])

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const toggleSpecialty = (value) => {
    setForm((current) => ({
      ...current,
      subespecialidades: current.subespecialidades.includes(value)
        ? current.subespecialidades.filter((item) => item !== value)
        : [...current.subespecialidades, value],
    }))
  }

  const selectFile = (event) => {
    const { name, files: selectedFiles } = event.target
    setFiles((current) => ({ ...current, [name]: selectedFiles?.[0] || null }))
  }

  const save = async (event) => {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      const payload = { ...form, experiencia: Number(form.experiencia) }
      const { data } = await contractorProfileService.updateMine(payload, files)
      setProfile(data)
      navigate('/perfil-contratista')
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo guardar el perfil.')
    } finally {
      setSaving(false)
    }
  }

  const download = async (fileId) => {
    try {
      const { data } = await contractorProfileService.getFile(fileId)
      const url = URL.createObjectURL(data)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = 'documento-contratista'
      anchor.click()
      URL.revokeObjectURL(url)
    } catch {
      setError('No se pudo abrir el documento.')
    }
  }

  if (loading) return <div className="project-page-shell"><p className="admin-loading" role="status">Cargando perfil profesional…</p></div>

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación contratista" className="client-nav"><Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link><Link to="/postulaciones"><span className="material-symbols-outlined" aria-hidden="true">assignment_turned_in</span>Proyectos disponibles</Link><Link to="/mis-postulaciones"><span className="material-symbols-outlined" aria-hidden="true">mail</span>Mis postulaciones</Link><Link className="is-current" to="/perfil-contratista"><span className="material-symbols-outlined" aria-hidden="true">person</span>Mi perfil</Link></nav>
      </aside>

      <main className="client-main">
        <header className="client-header"><div><p className="client-eyebrow">Perfil profesional</p><h1>{editing ? 'Editar perfil' : `${profile?.nombre || ''} ${profile?.apellido || ''}`.trim() || profile?.username}</h1><p>{editing ? 'Mantén tu información y documentación al día.' : profile?.especialidad || 'Contratista'}</p></div>{!editing && <Link className="button button-primary" to="/perfil-contratista/editar"><span className="material-symbols-outlined" aria-hidden="true">edit</span>Editar perfil</Link>}</header>
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}

        {profile && editing && form && (
          <form className="publish-form contractor-profile-form" onSubmit={save}>
            <label className="publish-field"><span>Nombre</span><input maxLength={100} name="nombre" onChange={updateField} required value={form.nombre} /></label>
            <label className="publish-field"><span>Apellido</span><input maxLength={100} name="apellido" onChange={updateField} required value={form.apellido} /></label>
            <label className="publish-field"><span>Correo electrónico</span><input readOnly value={profile.email || profile.username} /></label>
            <label className="publish-field"><span>Teléfono</span><input maxLength={24} name="telefono" onChange={updateField} required type="tel" value={form.telefono} /></label>
            <label className="publish-field"><span>Especialidad</span><select name="especialidad" onChange={updateField} required value={form.especialidad}><option value="">Selecciona una especialidad</option>{specialties.map((item) => <option key={item}>{item}</option>)}</select></label>
            <label className="publish-field"><span>Años de experiencia</span><input min="0" name="experiencia" onChange={updateField} required type="number" value={form.experiencia} /></label>
            <label className="publish-field"><span>Ciudad</span><input maxLength={100} name="ciudad" onChange={updateField} required value={form.ciudad} /></label>
            <label className="publish-field"><span>Departamento / región</span><input maxLength={100} name="departamento" onChange={updateField} required value={form.departamento} /></label>
            <label className="publish-field publish-field-full"><span>Presentación profesional (mínimo 150 caracteres)</span><textarea minLength={150} maxLength={2000} name="descripcion" onChange={updateField} required rows={5} value={form.descripcion} /></label>
            <label className="publish-field publish-field-full"><span>Matrícula / tarjeta profesional</span><input maxLength={100} name="matriculaProfesional" onChange={updateField} required value={form.matriculaProfesional} /></label>
            <fieldset className="publish-field publish-field-full"><legend>Subespecialidades técnicas (mínimo dos)</legend><div className="contractor-specialty-grid">{subSpecialties.map((item) => <label key={item}><input checked={form.subespecialidades.includes(item)} onChange={() => toggleSpecialty(item)} type="checkbox" />{item}</label>)}</div></fieldset>
            <div className="contractor-upload-grid publish-field-full">
              <FileField label="Foto de perfil (JPG/PNG, 5 MB máx.)" name="fotoFile" accept=".jpg,.jpeg,.png,image/jpeg,image/png" file={files.fotoFile} onChange={selectFile} />
              <FileField label="PDF de matrícula (10 MB máx.)" name="matriculaFile" accept=".pdf,application/pdf" file={files.matriculaFile} onChange={selectFile} />
              <FileField label="Hoja de vida (PDF/DOC/DOCX, 10 MB máx.)" name="cvFile" accept=".pdf,.doc,.docx" file={files.cvFile} onChange={selectFile} />
              <FileField label="Estatutos / RUT (PDF/DOC/DOCX, 10 MB máx.)" name="estatutosFile" accept=".pdf,.doc,.docx" file={files.estatutosFile} onChange={selectFile} />
            </div>
            <div className="publish-actions publish-field-full"><button className="button button-primary" disabled={saving} type="submit"><span className="material-symbols-outlined" aria-hidden="true">save</span>{saving ? 'Guardando…' : 'Guardar cambios'}</button><Link className="button button-secondary" to="/perfil-contratista">Cancelar</Link></div>
          </form>
        )}

        {profile && !editing && (
          <div className="contractor-profile-grid">
            <section className="client-profile contractor-identity">
              {photoUrl ? <img alt="Foto de perfil" className="contractor-profile-photo" src={photoUrl} /> : <span className="client-avatar profile-large-avatar">{(profile.nombre || profile.username).slice(0, 1).toUpperCase()}</span>}
              <p className="client-eyebrow">Contratista</p><h2>{profile.especialidad || 'Especialidad pendiente'}</h2>
              <span className={`profile-verification ${profile.usuarioVerificado ? 'is-verified' : ''}`}><span className="material-symbols-outlined" aria-hidden="true">{profile.usuarioVerificado ? 'verified' : 'pending'}</span>{profile.usuarioVerificado ? 'Verificado' : 'Pendiente de verificación'}</span>
              <div className="contractor-profile-rating"><span className="material-symbols-outlined" aria-hidden="true">star</span><strong>{profile.calificacionPromedio.toFixed(1)}</strong><small>{profile.reviews.length} reseñas</small></div>
              <div className="client-profile-total"><span>Proyectos completados</span><strong>{profile.proyectosCompletados}</strong></div>
              <div className="client-profile-total"><span>En progreso</span><strong>{profile.proyectosEnProgreso}</strong></div>
            </section>
            <section className="client-profile contractor-profile-details">
              <p className="client-eyebrow">Presentación</p><h2>Perfil profesional</h2><p className="contractor-bio">{profile.descripcion || 'Aún no has añadido una presentación profesional.'}</p>
              <ProfileLine label="Correo" value={profile.email || profile.username} icon="mail" />
              <ProfileLine label="Teléfono" value={profile.telefono} icon="call" />
              <ProfileLine label="Cobertura" value={`${profile.ciudad || 'Ciudad pendiente'} · ${profile.departamento || 'Región pendiente'}`} icon="location_on" />
              <ProfileLine label="Experiencia" value={`${profile.experiencia ?? 0} años`} icon="work_history" />
              <ProfileLine label="Matrícula profesional" value={profile.matriculaProfesional} icon="verified" />
              <div className="contractor-specialty-list">{profile.subespecialidades.map((item) => <span key={item}>{item}</span>)}</div>
              <div className="contractor-document-list">
                <DocumentButton label="Foto de perfil" id={profile.fotoPerfilUrl} onOpen={download} />
                <DocumentButton label="Matrícula profesional" id={profile.matriculaDocumentoUrl} onOpen={download} />
                <DocumentButton label="Hoja de vida" id={profile.cvUrl} onOpen={download} />
                <DocumentButton label="Estatutos / RUT" id={profile.estatutosUrl} onOpen={download} />
              </div>
            </section>
            <section className="contractor-reviews"><div><p className="client-eyebrow">Reputación</p><h2>Reseñas recibidas</h2></div>{profile.reviews.length ? profile.reviews.map((review) => <article className="contractor-review" key={review.id}><div><strong>{review.proyecto}</strong><span><span className="material-symbols-outlined" aria-hidden="true">star</span>{review.puntuacion}/5</span></div><p>{review.comentario || 'Sin comentario.'}</p><small>{review.fecha ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' }).format(new Date(review.fecha)) : ''}</small></article>) : <p className="contractor-no-reviews">Aún no has recibido reseñas.</p>}</section>
          </div>
        )}
      </main>
    </div>
  )
}

function FileField({ label, name, accept, file, onChange }) {
  return <label className="contractor-file-field"><span className="material-symbols-outlined" aria-hidden="true">upload_file</span><span>{file?.name || label}</span><input accept={accept} name={name} onChange={onChange} type="file" /></label>
}

function DocumentButton({ label, id, onOpen }) {
  return <button className="contractor-document-button" disabled={!id} onClick={() => onOpen(id)} type="button"><span className="material-symbols-outlined" aria-hidden="true">{id ? 'description' : 'block'}</span><span>{label}</span><small>{id ? 'Abrir' : 'No disponible'}</small></button>
}

function ProfileLine({ label, value, icon }) {
  return <div className="profile-field-row"><span className="material-symbols-outlined" aria-hidden="true">{icon}</span><div><small>{label}</small><strong>{value || 'No registrado'}</strong></div></div>
}

export function PerfilContratista({ editing = false }) {
  return <ContractorProfile editing={editing} />
}
