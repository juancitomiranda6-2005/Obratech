import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { calificacionService } from '../../services/calificacionService.js'

const scoreLabels = { 5: 'Excelente', 4: 'Muy bueno', 3: 'Bueno', 2: 'Regular', 1: 'Malo' }

export function CalificarContratista({ projectId, contractorId, ratingId, onLogout }) {
  const [rating, setRating] = useState(null)
  const [score, setScore] = useState('5')
  const [comment, setComment] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    document.title = 'Calificar contratista | ObraTech'
    let active = true
    (ratingId
      ? calificacionService.getClientRatingById(ratingId)
      : contractorId
        ? calificacionService.getClientRating(projectId, contractorId)
        : calificacionService.getRatingForProject(projectId))
      .then(({ data }) => {
        if (!active) return
        setRating(data)
        if (data.puntuacion != null) setScore(String(data.puntuacion))
        setComment(data.comentario || '')
      })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.error || 'No se pudo cargar el formulario de calificación.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [projectId, contractorId, ratingId])

  const submit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const { data } = await calificacionService.saveClientRating(rating?.projectId || projectId, rating?.contractorId || contractorId, {
        puntuacion: Number(score),
        comentario: comment.trim(),
      })
      setRating(data)
      setNotice('Calificación guardada correctamente.')
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo guardar la calificación.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación de calificación" className="client-nav">
          <Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link>
          <Link to="/clientes/mis-contratistas"><span className="material-symbols-outlined" aria-hidden="true">engineering</span>Mis contratistas</Link>
          <Link to="/mis-proyectos"><span className="material-symbols-outlined" aria-hidden="true">folder_open</span>Mis proyectos</Link>
        </nav>
        <div className="client-sidebar-footer"><button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>

      <main className="client-main">
        <header className="client-header"><div><p className="client-eyebrow">Opinión del cliente</p><h1>{rating?.puntuacion ? 'Editar calificación' : 'Calificar contratista'}</h1><p>Tu opinión ayuda a otros clientes a elegir profesionales.</p></div></header>
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}
        {loading ? <div className="admin-loading" role="status">Cargando calificación…</div> : rating && (
          <section className="rating-panel">
            <div className="rating-subject"><span className="material-symbols-outlined" aria-hidden="true">engineering</span><div><p className="client-eyebrow">{rating.projectTitle}</p><h2>{rating.contractorName || 'Contratista'}</h2><p>{rating.specialty || 'Profesional asignado al proyecto'}</p></div></div>
            <form className="rating-form" onSubmit={submit}>
              <label><span>Puntuación</span><select onChange={(event) => setScore(event.target.value)} value={score}>{Object.entries(scoreLabels).map(([value, label]) => <option key={value} value={value}>{value} - {label}</option>)}</select></label>
              <label><span>Comentario (opcional)</span><textarea maxLength={1000} onChange={(event) => setComment(event.target.value)} placeholder="Cuéntanos cómo fue tu experiencia con este contratista." rows={5} value={comment} /></label>
              <div className="rating-form-footer"><span>{comment.length}/1000</span><div><Link className="button button-secondary" to="/clientes/mis-contratistas">Volver</Link><button className="button button-primary" disabled={saving} type="submit"><span className="material-symbols-outlined" aria-hidden="true">star</span>{saving ? 'Guardando…' : rating.puntuacion ? 'Actualizar calificación' : 'Enviar calificación'}</button></div></div>
            </form>
          </section>
        )}
      </main>
    </div>
  )
}