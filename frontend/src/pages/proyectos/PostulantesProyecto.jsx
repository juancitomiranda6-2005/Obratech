import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { clientApplicationsService } from '../../services/postulacionService.js'

const labels = { PENDING: 'Pendiente', OFFERED: 'Esperando confirmación del contratista', ACCEPTED: 'Aceptado', REJECTED: 'Rechazado' }

// Recibe la fecha que envio el backend y la presenta en el formato local del usuario.
function formatDate(date) {
  if (!date) return 'Fecha no disponible'
  return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(date))
}

// Vista para que el cliente consulte postulantes de un proyecto, abra sus perfiles
// y envie al backend una decision de aceptar, rechazar o retirar una postulacion.
function ApplicantList({ projectId }) {
  // Guarda el titulo del proyecto, su estado de asignacion y la lista de postulantes.
  const [data, setData] = useState(null)
  // Indica si la carga inicial de postulaciones sigue en curso.
  const [loading, setLoading] = useState(true)
  // Mensaje para errores de consulta o acciones fallidas.
  const [error, setError] = useState('')
  // Confirmacion de la ultima accion realizada por el cliente.
  const [notice, setNotice] = useState('')
  // ID de la postulacion que se esta aceptando, rechazando o eliminando.
  const [pendingAction, setPendingAction] = useState('')

  // Vuelve a pedir el listado a la API despues de una accion para mostrar estados recientes.
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

  // Al cambiar projectId consulta los postulantes de ese proyecto. La bandera active
  // evita que una respuesta atrasada cambie el estado si se abrio otra pagina.
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

  // Envia el ID de la postulacion y la decision al backend. Mientras espera deshabilita
  // las acciones de esa tarjeta y luego vuelve a cargar la lista para reflejar el resultado.
  const respond = async (applicationId, estado) => {
    setPendingAction(applicationId)
    setError('')
    try {
      await clientApplicationsService.respond(projectId, applicationId, estado)
      setNotice(estado === 'ACCEPTED'
        ? 'Oferta enviada al contratista para su confirmación.'
        : 'Postulación rechazada.')
      await reload()
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo actualizar la postulación.')
    } finally {
      setPendingAction('')
    }
  }

  // Muestra una confirmacion del navegador y, si el cliente acepta, elimina la postulacion
  // en el backend y vuelve a consultar los datos para actualizar la pantalla.
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
      {/* Muestra el nombre del proyecto y permite volver al detalle o abrir el perfil cliente. */}
      <header className="project-page-header">
        <div><p className="client-eyebrow">Selección</p><h1>{data?.projectTitle || 'Postulantes del proyecto'}</h1><p>{data ? `${data.applications.length} postulaciones recibidas` : 'Revisa los perfiles y responde las solicitudes.'}</p></div>
        <div className="project-page-header-actions"><Link className="button button-secondary" to="/perfil-cliente"><span className="material-symbols-outlined" aria-hidden="true">person</span>Ver perfil del cliente</Link><Link className="button button-secondary" to={`/proyectos/${projectId}`}><span className="material-symbols-outlined" aria-hidden="true">arrow_back</span>Volver al proyecto</Link></div>
      </header>
      {/* Informa que el cliente ya selecciono un contratista y espera su respuesta. */}
      {data?.assignmentState === 'SELECCIONADO_PENDIENTE_CONTRATACION' && <div className="admin-alert admin-alert-info" role="status">El contratista seleccionado debe confirmar la oferta.</div>}
      {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
      {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}
      {loading && <div className="admin-loading" role="status">Cargando postulantes…</div>}
      {data && (data.applications.length ? (
        /* Cada tarjeta reune identidad, mensaje, fecha, estado y acciones del postulante. */
        <section aria-label="Postulaciones recibidas" className="applicants-list">
          {data.applications.map((application) => {
            const pending = application.estado === 'PENDING'
            return (
              <article className="applicant-card" key={application.id}>
                {/* Muestra el nombre, rol, correo y enlace al perfil publico del postulante. */}
                <div className="applicant-identity">
                  <span className="admin-avatar material-symbols-outlined" aria-hidden="true">{application.rol === 'ROLE_CONTRACTOR' ? 'engineering' : 'person'}</span>
                  <div><h2>{application.perfilId ? <Link to={`/${application.rol === 'ROLE_WORKER' ? 'trabajadores' : 'contratistas'}/${application.perfilId}`}>{application.nombre}</Link> : application.nombre}{application.usuarioVerificado && <span className="material-symbols-outlined applicant-verified" aria-label="Verificado">verified</span>}</h2><p>{application.username}</p><small>{application.rol.replace('ROLE_', '')}</small></div>
                  {application.perfilId && application.rol === 'ROLE_CONTRACTOR' && <Link className="profile-open-link" to={`/contratistas/${application.perfilId}`}>Ver perfil del contratista<span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span></Link>}
                </div>
                {/* Texto opcional enviado por el postulante y momento de envio. */}
                <div className="applicant-message"><small>Mensaje</small><p>{application.mensaje || 'Sin mensaje'}</p><time dateTime={application.fechaPostulacion}>{formatDate(application.fechaPostulacion)}</time></div>
                {/* Presenta el estado actual y las acciones permitidas para ese estado. */}
                <div className="applicant-actions">
                  <span className={`application-status status-${application.estado.toLowerCase()}`}>{labels[application.estado] || application.estado}</span>
                  {pending && <div className="applicant-action-buttons">
                    {/* Selecciona al contratista; la API envia una oferta que el contratista confirma. */}
                    <button className="admin-action admin-action-success" disabled={pendingAction === application.id || data.contractorAssigned} onClick={() => respond(application.id, 'ACCEPTED')} type="button">Seleccionar contratista</button>
                    {/* Cambia la postulacion pendiente a rechazada en el backend. */}
                    <button className="admin-action admin-action-danger" disabled={pendingAction === application.id} onClick={() => respond(application.id, 'REJECTED')} type="button">Rechazar</button>
                  </div>}
                  {!pending && application.estado !== 'OFFERED' && <button className="admin-action admin-action-danger" disabled={pendingAction === application.id} onClick={() => remove(application)} type="button">Eliminar</button>}
                </div>
              </article>
            )
          })}
        </section>
      ) : <div className="client-empty applicants-empty"><span className="material-symbols-outlined" aria-hidden="true">mail</span><h3>No hay postulaciones todavía</h3><p>Cuando alguien se postule a este proyecto, aparecerá aquí.</p></div>)}
    </div>
  )
}

// Punto de entrada de la vista de postulaciones recibidas por proyecto.
export function PostulantesProyecto({ projectId }) {
  return <ApplicantList projectId={projectId} />
}