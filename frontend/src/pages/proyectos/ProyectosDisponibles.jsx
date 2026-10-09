import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { participantProjectsService } from '../../services/participantProjectsService.js'

const money = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

function AvailableProjects({ onLogout, role, projectId = '' }) {
  const [market, setMarket] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [type, setType] = useState('')
  const [location, setLocation] = useState('')
  const [minimumBudget, setMinimumBudget] = useState(0)
  const [applicationMessage, setApplicationMessage] = useState('')
  const [applyingTo, setApplyingTo] = useState('')
  const [notice, setNotice] = useState('')

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
  const locations = [...new Set(projects.map((project) => project.ubicacion).filter(Boolean))].sort()
  const filtered = projects.filter((project) => {
    if (projectId && project.id !== projectId) return false
    const query = search.trim().toLocaleLowerCase()
    return (!query || `${project.titulo} ${project.descripcion}`.toLocaleLowerCase().includes(query))
      && (!type || project.tipoProyecto === type)
      && (!location || project.ubicacion === location)
      && (project.presupuesto || 0) >= minimumBudget
  })

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
  const canApply = market?.usuarioVerificado && (role !== 'ROLE_CONTRACTOR' || market?.perfilCompleto)

  return (
    <div className="client-layout">
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
        <header className="client-header">
          <div><p className="client-eyebrow">Mercado laboral · {roleName}</p><h1>Proyectos disponibles</h1><p>Explora las obras aprobadas y postúlate a las que te interesan.</p></div>
          <button aria-label="Actualizar proyectos" className="admin-icon-button" onClick={reload} title="Actualizar" type="button"><span className="material-symbols-outlined" aria-hidden="true">refresh</span></button>
        </header>

        {market && !market.usuarioVerificado && <div className="admin-alert admin-alert-error" role="status">Tu cuenta debe estar verificada para postularte.</div>}
        {market && market.usuarioVerificado && !market.perfilCompleto && <div className="client-verification" role="status"><span className="material-symbols-outlined" aria-hidden="true">gpp_maybe</span><div><h2>Completa tu perfil</h2><p>{market.requisitosFaltantes.join(' ')}</p></div></div>}
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}
        {loading && !market && <div className="admin-loading" role="status">Cargando proyectos disponibles…</div>}

        {market && (
          <>
            {projectId && selectedProject && <article className="market-project-detail">
              <Link className="button button-secondary" to="/postulaciones"><span className="material-symbols-outlined" aria-hidden="true">arrow_back</span>Volver al mercado</Link>
              <div className="client-project-topline"><span className="client-project-state state-en_progreso">Aprobado</span><span className="client-project-type">{selectedProject.tipoProyecto}</span></div>
              <h2>{selectedProject.titulo}</h2>
              <p className="market-project-description">{selectedProject.descripcion}</p>
              <dl className="market-project-specs">
                <div><dt>Ubicación</dt><dd>{selectedProject.ubicacion || 'No especificada'}</dd></div>
                <div><dt>Presupuesto</dt><dd>{money.format(selectedProject.presupuesto || 0)}</dd></div>
                <div><dt>Inicio</dt><dd>{selectedProject.fechaInicio || 'Por definir'}</dd></div>
                <div><dt>Entrega</dt><dd>{selectedProject.fechaEntrega || 'Por definir'}</dd></div>
                <div><dt>Límite de postulación</dt><dd>{selectedProject.fechaLimitePostulacion || 'Abierto'}</dd></div>
              </dl>
              {!selectedProject.yaPostulado && <label className="market-application-message"><span>Mensaje para el cliente (opcional)</span><textarea maxLength={1000} onChange={(event) => setApplicationMessage(event.target.value)} placeholder="Presenta brevemente tu experiencia para este proyecto." rows={4} value={applicationMessage} /></label>}
              <div className="market-detail-actions"><Link className="button button-secondary" to="/postulaciones">Cancelar</Link><button className="button button-primary" disabled={!canApply || selectedProject.yaPostulado || applyingTo === selectedProject.id} onClick={() => apply(selectedProject)} type="button"><span className="material-symbols-outlined" aria-hidden="true">{selectedProject.yaPostulado ? 'check' : 'send'}</span>{selectedProject.yaPostulado ? 'Ya postulaste' : applyingTo === selectedProject.id ? 'Enviando…' : 'Postularme'}</button></div>
            </article>}
            {projectId && !selectedProject && !loading && <div className="client-empty"><span className="material-symbols-outlined" aria-hidden="true">search_off</span><h2>Proyecto no disponible</h2><p>La obra no está abierta para postulaciones o ya fue asignada.</p><Link className="button button-secondary" to="/postulaciones">Volver al mercado</Link></div>}
            {!projectId && <section aria-label="Filtros de proyectos" className="market-filters">
              <label className="market-search"><span className="material-symbols-outlined" aria-hidden="true">search</span><span className="sr-only">Buscar por título o descripción</span><input onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por título o descripción" type="search" value={search} /></label>
              <label><span className="sr-only">Tipo de proyecto</span><select onChange={(event) => setType(event.target.value)} value={type}><option value="">Todos los tipos</option>{['Residencial', 'Comercial', 'Industrial', 'Institucional', 'Infraestructura'].map((option) => <option key={option}>{option}</option>)}</select></label>
              <label><span className="sr-only">Ubicación</span><select onChange={(event) => setLocation(event.target.value)} value={location}><option value="">Todas las ubicaciones</option>{locations.map((option) => <option key={option}>{option}</option>)}</select></label>
              <label><span className="sr-only">Presupuesto mínimo</span><select onChange={(event) => setMinimumBudget(Number(event.target.value))} value={minimumBudget}><option value={0}>Cualquier presupuesto</option><option value={1000000}>Más de $1.000.000</option><option value={5000000}>Más de $5.000.000</option><option value={20000000}>Más de $20.000.000</option></select></label>
              <span className="market-result-count">{filtered.length} de {projects.length} proyectos</span>
            </section>}

            {!projectId && filtered.length ? (
              <section aria-label="Proyectos que coinciden" className="market-project-grid">
                {filtered.map((project) => (
                  <article className="market-project" key={project.id}>
                    <div className="client-project-topline"><span className="client-project-state state-en_progreso">Aprobado</span><span className="client-project-type">{project.tipoProyecto}</span></div>
                    <h2>{project.titulo}</h2>
                    <p>{project.descripcion}</p>
                    <div className="market-project-info"><span><span className="material-symbols-outlined" aria-hidden="true">location_on</span>{project.ubicacion || 'Ubicación pendiente'}</span><strong>{money.format(project.presupuesto || 0)}</strong></div>
                    <div className="market-project-dates"><span>Inicio: {project.fechaInicio || 'Por definir'}</span><span>Límite: {project.fechaLimitePostulacion || 'Abierto'}</span></div>
                    <button className="button button-primary market-apply" disabled={!canApply || project.yaPostulado || applyingTo === project.id} onClick={() => apply(project)} type="button">
                      <span className="material-symbols-outlined" aria-hidden="true">{project.yaPostulado ? 'check' : 'send'}</span>
                      {project.yaPostulado ? 'Ya postulaste' : applyingTo === project.id ? 'Enviando…' : 'Postularme'}
                    </button>
                  </article>
                ))}
              </section>
            ) : !projectId ? (
              <div className="client-empty market-empty"><span className="material-symbols-outlined" aria-hidden="true">search_off</span><h3>{projects.length ? 'No hay coincidencias' : 'No hay proyectos disponibles'}</h3><p>{projects.length ? 'Prueba con otros filtros.' : 'Los proyectos aprobados aparecerán aquí cuando estén abiertos a postulaciones.'}</p></div>
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