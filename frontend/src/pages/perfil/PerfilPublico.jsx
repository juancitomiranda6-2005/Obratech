import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { publicProfileService } from '../../services/perfilService.js'
import { ProfileAvatar } from '../../components/shared/ProfileAvatar.jsx'

const labels = { ROLE_CLIENT: 'Cliente', ROLE_CONTRACTOR: 'Contratista', ROLE_WORKER: 'Trabajador' }
const projectStates = { PENDIENTE: 'Pendiente', EN_PROGRESO: 'En progreso', COMPLETADO: 'Completado', CANCELADO: 'Cancelado' }
const date = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' })

export function PerfilPublico({ id, onLogout, viewerRole }) {
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [documentError, setDocumentError] = useState('')
  const isWorker = profile?.roles?.includes('ROLE_WORKER')
  const isContractor = profile?.roles?.includes('ROLE_CONTRACTOR')
  const isClient = profile?.roles?.includes('ROLE_CLIENT')
  const canViewContractorDocuments = viewerRole === 'ROLE_CLIENT' && isContractor
  const canViewContractorStatutes = viewerRole === 'ROLE_WORKER' && isContractor && profile.estatutosDisponibles
  const canViewWorkerCv = viewerRole === 'ROLE_CONTRACTOR' && isWorker && profile.cvDisponible
  const profileType = (profile?.roles || []).map((role) => labels[role] || role).join(', ')

  useEffect(() => {
    document.title = 'Perfil profesional | ObraTech'
    let active = true
    publicProfileService.getById(id)
      .then(({ data }) => { if (active) setProfile(data) })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.error || 'No se pudo cargar el perfil público.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id])

  const openDocument = async (request) => {
    setDocumentError('')
    try {
      const { data } = await request()
      const url = URL.createObjectURL(data)
      const link = document.createElement('a')
      link.href = url
      link.target = '_blank'
      link.rel = 'noopener noreferrer'
      link.click()
      window.setTimeout(() => URL.revokeObjectURL(url), 60000)
    } catch (requestError) {
      setDocumentError(requestError.response?.data?.error || 'No se pudo abrir el documento.')
    }
  }

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación de perfil" className="client-nav"><Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link>{isWorker ? <Link to="/trabajadores"><span className="material-symbols-outlined" aria-hidden="true">person_search</span>Trabajadores</Link> : <Link to="/clientes/mis-contratistas"><span className="material-symbols-outlined" aria-hidden="true">engineering</span>Mis contratistas</Link>}</nav>
        <div className="client-sidebar-footer"><button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>
      <main className="client-main">
        <header className="client-header"><div><p className="client-eyebrow">Directorio ObraTech</p><h1>{isClient ? 'Perfil del cliente' : 'Perfil público'}</h1><p>{profileType}</p></div></header>
        {loading && <div className="admin-loading" role="status">Cargando perfil…</div>}
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {profile && <>
          <section className="public-profile-hero"><ProfileAvatar className="public-profile-avatar" name={profile.nombre || profile.username} photoAvailable={profile.fotoDisponible} profileId={profile.id} /><div><p className="client-eyebrow">{profileType}</p><h2>{profile.nombre || profile.username}</h2><p>{isWorker ? profile.oficio || 'Oficio no especificado' : isContractor ? profile.especialidad || 'Especialidad no especificada' : 'Cliente de ObraTech'}</p></div><span className={`profile-verification ${profile.verificado ? 'is-verified' : ''}`}><span className="material-symbols-outlined" aria-hidden="true">{profile.verificado ? 'verified' : 'pending'}</span>{profile.verificado ? 'Verificado' : 'En revisión'}</span></section>
          <div className="public-profile-grid">
            <section className="public-profile-panel"><p className="client-eyebrow">Información</p><h2>{isClient ? 'Datos de contacto' : 'Datos profesionales'}</h2><ProfileInfo label="Correo" value={profile.email || profile.username} /><ProfileInfo label="Teléfono" value={profile.telefono} /><ProfileInfo label="Ubicación" value={profile.ubicacion} />{!isClient && <ProfileInfo label="Experiencia" value={profile.experiencia == null ? null : `${profile.experiencia} años`} />}{isWorker && <ProfileInfo label="Disponibilidad" value={profile.disponible ? 'Disponible' : 'No disponible'} />}{isContractor && <ProfileInfo label="Calificación promedio" value={`${profile.calificacionPromedio.toFixed(1)} / 5`} />}{profile.descripcion && <div className="public-profile-description"><h3>{isClient ? 'Sobre el cliente' : 'Presentación'}</h3><p>{profile.descripcion}</p></div>}</section>
            {!isClient && <section className="public-profile-panel"><div className="client-section-heading"><div><p className="client-eyebrow">Actividad</p><h2>{isWorker ? 'Proyectos asociados' : 'Proyectos asignados'}</h2></div><span>{profile.proyectos.length}</span></div>{profile.proyectos.length ? <ul className="public-profile-projects">{profile.proyectos.map((project) => <li key={project.id}><div><strong>{project.titulo || 'Proyecto'}</strong><small>{project.ubicacion || 'Ubicación no registrada'}</small></div><span className={`client-project-state state-${project.estado.toLowerCase()}`}>{projectStates[project.estado] || project.estado}</span></li>)}</ul> : <div className="report-empty">Aún no hay proyectos asociados.</div>}</section>}
          </div>
          {(canViewContractorDocuments || canViewContractorStatutes || canViewWorkerCv) && <section className="public-profile-panel public-profile-documents"><div className="client-section-heading"><div><p className="client-eyebrow">Documentación</p><h2>Documentos</h2></div></div><div className="contractor-document-list">
            {canViewContractorDocuments && profile.cvDisponible && <button className="profile-file-link" onClick={() => openDocument(() => publicProfileService.getContractorDocument(profile.id, 'cv'))} type="button">Hoja de vida</button>}
            {canViewContractorDocuments && profile.matriculaDisponible && <button className="profile-file-link" onClick={() => openDocument(() => publicProfileService.getContractorDocument(profile.id, 'matricula'))} type="button">Matrícula profesional</button>}
            {canViewContractorDocuments && profile.estatutosDisponibles && <button className="profile-file-link" onClick={() => openDocument(() => publicProfileService.getContractorDocument(profile.id, 'estatutos'))} type="button">Estatutos / RUT</button>}
            {canViewContractorStatutes && <button className="profile-file-link" onClick={() => openDocument(() => publicProfileService.getContractorStatutes(profile.id))} type="button">Estatutos / RUT</button>}
            {canViewWorkerCv && <button className="profile-file-link" onClick={() => openDocument(() => publicProfileService.getWorkerCv(profile.id))} type="button">Hoja de vida</button>}
          </div>{documentError && <div className="admin-alert admin-alert-error" role="alert">{documentError}</div>}</section>}
          {isContractor && <section className="public-profile-panel public-profile-reviews"><div className="client-section-heading"><div><p className="client-eyebrow">Opiniones de clientes</p><h2>Calificaciones</h2></div><span>{profile.calificaciones.length}</span></div>{profile.calificaciones.length ? <div className="public-review-list">{profile.calificaciones.map((review) => <article key={review.id}><div><strong>{review.proyecto}</strong><span className="review-stars">{'★'.repeat(review.puntuacion)}{'☆'.repeat(5-review.puntuacion)}</span></div><p>{review.comentario || 'Sin comentario.'}</p><small>{review.fecha ? date.format(new Date(review.fecha)) : 'Fecha no disponible'}</small></article>)}</div> : <div className="report-empty">Todavía no hay calificaciones.</div>}</section>}
        </>}
      </main>
    </div>
  )
}

function ProfileInfo({ label, value }) {
  return <div className="public-profile-info"><span>{label}</span><strong>{value || 'No registrado'}</strong></div>
}