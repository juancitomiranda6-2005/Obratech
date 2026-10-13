import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { participantProjectsService } from '../../services/participantProjectsService.js'

const money = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

// Mercado de obras: consulta oportunidades, aplica filtros y envia postulaciones.
function AvailableProjects({ onLogout, role, projectId = '' }) {
  // Datos del mercado, incluidos requisitos del perfil y proyectos disponibles.
  const [market, setMarket] = useState(null)
  // Estado de carga para las consultas del mercado.
  const [loading, setLoading] = useState(true)
  // Error visible si falla la consulta o el envio de una postulacion.
  const [error, setError] = useState('')
  // Texto usado para buscar por titulo o descripcion.
  const [search, setSearch] = useState('')
  // Filtro elegido para el tipo de proyecto.
  const [type, setType] = useState('')
  // Filtro elegido para la ubicacion del proyecto.
  const [location, setLocation] = useState('')
  // Presupuesto minimo seleccionado en los filtros.
  const [minimumBudget, setMinimumBudget] = useState(0)
  // Mensaje opcional que se adjunta a la postulacion.
  const [applicationMessage, setApplicationMessage] = useState('')
  // ID del proyecto cuya postulacion se esta enviando.
  const [applyingTo, setApplyingTo] = useState('')
  // Confirmacion del ultimo envio correcto.
  const [notice, setNotice] = useState('')

  // Repite la peticion del mercado cuando el usuario pulsa recargar y sustituye los datos.
  const reload = async () => {
    setLoading(true)
    setError('')
    try {
      const { data } = await participantProjectsService.listAvailable()
      setMarket(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudieron cargar los proyectos disponibles.')
    } finally {
      setLoading(false)
    }
  }

  // Pide al backend los proyectos disponibles al abrir la vista. La bandera active
  // evita actualizar el componente si el usuario navega antes de recibir la respuesta.
  useEffect(() => {
    document.title = 'Proyectos Disponibles | ObraTech'
    let active = true
    participantProjectsService.listAvailable()
      .then(({ data }) => { if (active) setMarket(data) })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudieron cargar los proyectos disponibles.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const projects = market?.proyectos || []
  // Obtiene ubicaciones sin repetirlas para llenar el filtro de ubicacion.
  const locations = [...new Set(projects.map((project) => project.ubicacion).filter(Boolean))].sort()
  // Aplica en memoria el ID de ruta, texto de busqueda, tipo, ubicacion y presupuesto.
  const filtered = projects.filter((project) => {
    if (projectId && project.id !== projectId) return false
    const query = search.trim().toLocaleLowerCase()
    return (!query || `${project.titulo} ${project.descripcion}`.toLocaleLowerCase().includes(query))
      && (!type || project.tipoProyecto === type)
      && (!location || project.ubicacion === location)
      && (project.presupuesto || 0) >= minimumBudget
  })

  // Envia el mensaje y el ID del proyecto a la API. Si se guarda correctamente,
  // limpia el mensaje, avisa al usuario y vuelve a consultar el mercado.
  const apply = async (project) => {
    setApplyingTo(project.id)
    setError('')
    setNotice('')
    try {
      await participantProjectsService.apply(project.id, applicationMessage)
      setApplicationMessage('')
      setNotice(`Postulación enviada para “${project.titulo}”.`)
      await reload()
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo enviar la postulación.')
    } finally {
      setApplyingTo('')
    }
  }
  const selectedProject = projects.find((project) => project.id === projectId)

  const roleName = role === 'ROLE_CONTRACTOR' ? 'Contratista' : 'Trabajador'
  const canApply = role !== 'ROLE_CONTRACTOR' || market?.perfilCompleto

  return (
    <div className="client-layout">
      {/* Navegacion de las oportunidades y postulaciones */}
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación de oportunidades" className="client-nav">
          <Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link>
          <Link className="is-current" to="/postulaciones"><span className="material-symbols-outlined" aria-hidden="true">assignment_turned_in</span>Proyectos disponibles</Link>
          <Link to="/mis-postulaciones"><span className="material-symbols-outlined" aria-hidden="true">mail</span>Mis postulaciones</Link>
        </nav>
        <div className="client-sidebar-footer"><button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>

      <main className="client-main">
        {/* Titulo del mercado y boton para actualizar proyectos */}
        <header className="client-header">
          <div><p className="client-eyebrow">Mercado laboral · {roleName}</p><h1>Proyectos disponibles</h1><p>Explora las obras aprobadas y postúlate a las que te interesan.</p></div>
          {/* Boton para volver a consultar las oportunidades */}
          <button aria-label="Actualizar proyectos" className="admin-icon-button" disabled={loading} onClick={reload} title={loading ? 'Actualizando…' : 'Actualizar'} type="button"><span className={`material-symbols-outlined ${loading ? 'is-refreshing' : ''}`} aria-hidden="true">refresh</span></button>
        </header>

        {/* Mensajes de carga, estado de cuenta, errores y confirmacion del envio. */}
        {loading && market && <p className="market-refresh-status" role="status">Actualizando proyectos…</p>}
        {market && !market.usuarioVerificado && <div className="admin-alert admin-alert-info" role="status">Tu cuenta está activa. Completa tu perfil para obtener la insignia de verificación.</div>}
        {market && role === 'ROLE_CONTRACTOR' && !market.perfilCompleto && <div className="client-verification" role="status"><span className="material-symbols-outlined" aria-hidden="true">gpp_maybe</span><div><h2>Completa tu perfil profesional</h2><p>{market.requisitosFaltantes.join(' ')}</p></div></div>}
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}
        {loading && !market && <div className="admin-loading" role="status">Cargando proyectos disponibles…</div>}

        {market && (
          <>
            {/* Datos completos de la obra seleccionada antes de enviar una postulacion. */}
            {projectId && selectedProject && <article className="market-project-detail">
              <Link className="button button-secondary" to="/postulaciones"><span className="material-symbols-outlined" aria-hidden="true">arrow_back</span>Volver al mercado</Link>
              <div className="client-project-topline"><span className="client-project-state state-en_progreso">Aprobado</span><span className="client-project-type">{selectedProject.tipoProyecto}</span></div>
              <h2>{selectedProject.titulo}</h2>
              <p className="market-project-description">{selectedProject.descripcion}</p>
              {/* Resume cliente, presupuesto, ubicacion, fechas, plazo y area de la obra. */}
              <dl className="market-project-specs">
                <div><dt>Cliente</dt><dd>{selectedProject.clientePerfilId ? <Link to={`/clientes/perfil/${selectedProject.clientePerfilId}`}><span className="material-symbols-outlined" aria-hidden="true">person</span>Ver perfil del cliente</Link> : selectedProject.clienteNombre || 'Cliente no disponible'}</dd></div>
                <div><dt>Ubicación</dt><dd>{selectedProject.ubicacion || 'No especificada'}</dd></div>
                <div><dt>Presupuesto</dt><dd>{money.format(selectedProject.presupuesto || 0)}</dd></div>
                <div><dt>Plazo estimado</dt><dd>{selectedProject.plazoEstimado ? `${selectedProject.plazoEstimado} meses` : 'Por definir'}</dd></div>
                <div><dt>Área total</dt><dd>{selectedProject.areaTotal ? `${selectedProject.areaTotal} m²` : 'Por definir'}</dd></div>
                <div><dt>Inicio</dt><dd>{selectedProject.fechaInicio || 'Por definir'}</dd></div>
                <div><dt>Entrega</dt><dd>{selectedProject.fechaEntrega || 'Por definir'}</dd></div>
                <div><dt>Límite de postulación</dt><dd>{selectedProject.fechaLimitePostulacion || 'Abierto'}</dd></div>
              </dl>
              {selectedProject.observaciones && <section className="market-project-scope"><h3>Detalles adicionales</h3><p>{selectedProject.observaciones}</p></section>}
              {/* Mensaje opcional que acompana la postulacion y se envia con ella a la API. */}
              {!selectedProject.yaPostulado && <label className="market-application-message"><span>Mensaje para el cliente (opcional)</span><textarea maxLength={1000} onChange={(event) => setApplicationMessage(event.target.value)} placeholder="Presenta brevemente tu experiencia para este proyecto." rows={4} value={applicationMessage} /></label>}
              {/* Permite volver al mercado o enviar la postulacion si cumple los requisitos. */}
              <div className="market-detail-actions"><Link className="button button-secondary" to="/postulaciones">Cancelar</Link><button className="button button-primary" disabled={!canApply || selectedProject.yaPostulado || applyingTo === selectedProject.id} onClick={() => apply(selectedProject)} type="button"><span className="material-symbols-outlined" aria-hidden="true">{selectedProject.yaPostulado ? 'check' : 'send'}</span>{selectedProject.yaPostulado ? 'Ya postulaste' : applyingTo === selectedProject.id ? 'Enviando…' : 'Postularme'}</button></div>
            </article>}
            {projectId && !selectedProject && !loading && <div className="client-empty"><span className="material-symbols-outlined" aria-hidden="true">search_off</span><h2>Proyecto no disponible</h2><p>La obra no está abierta para postulaciones o ya fue asignada.</p><Link className="button button-secondary" to="/postulaciones">Volver al mercado</Link></div>}
            {/* Controles que cambian el estado de filtros usado para reducir la lista. */}
            {!projectId && <section aria-label="Filtros de proyectos" className="market-filters">
              <label className="market-search"><span className="material-symbols-outlined" aria-hidden="true">search</span><span className="sr-only">Buscar por título o descripción</span><input onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por título o descripción" type="search" value={search} /></label>
              <label><span className="sr-only">Tipo de proyecto</span><select onChange={(event) => setType(event.target.value)} value={type}><option value="">Todos los tipos</option>{['Residencial', 'Comercial', 'Industrial', 'Institucional', 'Infraestructura'].map((option) => <option key={option}>{option}</option>)}</select></label>
              <label><span className="sr-only">Ubicación</span><select onChange={(event) => setLocation(event.target.value)} value={location}><option value="">Todas las ubicaciones</option>{locations.map((option) => <option key={option}>{option}</option>)}</select></label>
              <label><span className="sr-only">Presupuesto mínimo</span><select onChange={(event) => setMinimumBudget(Number(event.target.value))} value={minimumBudget}><option value={0}>Cualquier presupuesto</option><option value={1000000}>Más de $1.000.000</option><option value={5000000}>Más de $5.000.000</option><option value={20000000}>Más de $20.000.000</option></select></label>
              <span className="market-result-count">{filtered.length} de {projects.length} proyectos</span>
            </section>}

            {/* Tarjetas resumidas de las obras filtradas y su boton de postulacion. */}
            {!projectId && filtered.length ? (
              <section aria-label="Proyectos que coinciden" className="market-project-grid">
                {filtered.map((project) => (
                  <article className="market-project" key={project.id}>
                    <div className="client-project-topline"><span className="client-project-state state-en_progreso">Aprobado</span><span className="client-project-type">{project.tipoProyecto}</span></div>
                    <h2>{project.titulo}</h2>
                    <p>{project.descripcion}</p>
                    <p className="market-project-client">Cliente: {project.clientePerfilId ? <Link to={`/clientes/perfil/${project.clientePerfilId}`}>Ver perfil del cliente</Link> : project.clienteNombre || 'No disponible'}</p>
                    <div className="market-project-info"><span><span className="material-symbols-outlined" aria-hidden="true">location_on</span>{project.ubicacion || 'Ubicación pendiente'}</span><strong>{money.format(project.presupuesto || 0)}</strong></div>
                    <div className="market-project-dates"><span>Inicio: {project.fechaInicio || 'Por definir'}</span><span>Límite: {project.fechaLimitePostulacion || 'Abierto'}</span></div>
                    {/* Envia una postulacion a esta obra sin abrir antes su detalle. */}
                    <button className="button button-primary market-apply" disabled={!canApply || project.yaPostulado || applyingTo === project.id} onClick={() => apply(project)} type="button">
                      <span className="material-symbols-outlined" aria-hidden="true">{project.yaPostulado ? 'check' : 'send'}</span>
                      {project.yaPostulado ? 'Ya postulaste' : applyingTo === project.id ? 'Enviando…' : 'Postularme'}
                    </button>
                  </article>
                ))}
              </section>
            ) : !projectId ? (
              <div className="client-empty market-empty"><span className="material-symbols-outlined" aria-hidden="true">search_off</span><h3>{projects.length ? 'No hay coincidencias' : 'No hay proyectos disponibles'}</h3><p>{projects.length ? 'Prueba con otros filtros.' : 'Los proyectos publicados aparecerán aquí cuando el cliente los publique, sean aprobados y queden abiertos a postulaciones.'}</p></div>
            ) : null}
          </>
        )}
      </main>
    </div>
  )
}

export function ProyectosDisponibles({ onLogout, role, projectId }) {
  return <AvailableProjects onLogout={onLogout} role={role} projectId={projectId} />
}