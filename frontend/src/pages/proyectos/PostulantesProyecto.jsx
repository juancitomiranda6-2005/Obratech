import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { clientApplicationsService } from '../../services/postulacionService.js'

const labels = { PENDING: 'Pendiente', ACCEPTED: 'Aceptado', REJECTED: 'Rechazado' }

function formatDate(date) {
  if (!date) return 'Fecha no disponible'
  return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(date))
}

function ApplicantList({ projectId }) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [pendingAction, setPendingAction] = useState('')

  const reload = async () => {
    setError('')
    try {
      const { data: response } = await clientApplicationsService.listForProject(projectId)
      setData(response)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudieron cargar las postulaciones.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    document.title = 'Postulantes | ObraTech'
    let active = true
    clientApplicationsService.listForProject(projectId)
      .then(({ data: response }) => { if (active) setData(response) })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudieron cargar las postulaciones.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [projectId])

  const respond = async (applicationId, estado) => {
    setPendingAction(applicationId)
    setError('')
    try {
      await clientApplicationsService.respond(projectId, applicationId, estado)
      await reload()
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo actualizar la postulación.')
    } finally {
      setPendingAction('')
    }
  }

  const remove = async (application) => {
    if (!window.confirm(`¿Eliminar la postulación de ${application.nombre}?`)) return
    setPendingAction(application.id)
    setError('')
    setNotice('')
    try {
      await clientApplicationsService.remove(projectId, application.id)
      setNotice('Postulación eliminada.')
      await reload()
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo eliminar la postulación.')
    } finally {
      setPendingAction('')
    }
  }

  return (
    <div className="project-page-shell">
      <header className="project-page-header">
        <div><p className="client-eyebrow">Selección</p><h1>{data?.projectTitle || 'Postulantes del proyecto'}</h1><p>{data ? `${data.applications.length} postulaciones recibidas` : 'Revisa los perfiles y responde las solicitudes.'}</p></div>
        <Link className="button button-secondary" to={`/proyectos/${projectId}`}><span className="material-symbols-outlined" aria-hidden="true">arrow_back</span>Volver al proyecto</Link>
      </header>
      {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
      {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}
      {loading && <div className="admin-loading" role="status">Cargando postulantes…</div>}
      {data && (data.applications.length ? (
        <section aria-label="Postulaciones recibidas" className="applicants-list">
          {data.applications.map((application) => {
            const pending = application.estado === 'PENDING'
            return (
              <article className="applicant-card" key={application.id}>
                <div className="applicant-identity">
                  <span className="admin-avatar material-symbols-outlined" aria-hidden="true">{application.rol === 'ROLE_CONTRACTOR' ? 'engineering' : 'person'}</span>
                  <div><h2>{application.perfilId ? <Link to={`/${application.rol === 'ROLE_WORKER' ? 'trabajadores' : 'contratistas'}/${application.perfilId}`}>{application.nombre}</Link> : application.nombre}{application.usuarioVerificado && <span className="material-symbols-outlined applicant-verified" aria-label="Verificado">verified</span>}</h2><p>{application.username}</p><small>{application.rol.replace('ROLE_', '')}</small></div>
                </div>
                <div className="applicant-message"><small>Mensaje</small><p>{application.mensaje || 'Sin mensaje'}</p><time dateTime={application.fechaPostulacion}>{formatDate(application.fechaPostulacion)}</time></div>
                <div className="applicant-actions">
                  <span className={`application-status status-${application.estado.toLowerCase()}`}>{labels[application.estado] || application.estado}</span>
                  {pending && <div className="applicant-action-buttons">
                    <button className="admin-action admin-action-success" disabled={pendingAction === application.id || data.contractorAssigned} onClick={() => respond(application.id, 'ACCEPTED')} type="button">Aceptar</button>
                    <button className="admin-action admin-action-danger" disabled={pendingAction === application.id} onClick={() => respond(application.id, 'REJECTED')} type="button">Rechazar</button>
                  </div>}
                  {!pending && <button className="admin-action admin-action-danger" disabled={pendingAction === application.id} onClick={() => remove(application)} type="button">Eliminar</button>}
                </div>
              </article>
            )
          })}
        </section>
      ) : <div className="client-empty applicants-empty"><span className="material-symbols-outlined" aria-hidden="true">mail</span><h3>No hay postulaciones todavía</h3><p>Cuando alguien se postule a este proyecto, aparecerá aquí.</p></div>)}
    </div>
  )
}

export function PostulantesProyecto({ projectId }) {
  return <ApplicantList projectId={projectId} />
}