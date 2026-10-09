import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { teamService } from '../../services/teamService.js'
import { workerRecruitmentService } from '../../services/workerRecruitmentService.js'

export function MercadoTrabajadores({ onLogout, workerId = '' }) {
  const [workers, setWorkers] = useState([])
  const [projects, setProjects] = useState([])
  const [projectId, setProjectId] = useState('')
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [pendingId, setPendingId] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadMarket = async (selectedProjId = projectId) => {
    setLoading(true)
    setError('')
    try {
      const [projectResponse] = await Promise.all([
        teamService.getContractorOptions(),
      ])
      const activeProjects = projectResponse.data.filter((project) => project.estadoProyecto !== 'COMPLETADO')
      setProjects(activeProjects)
      
      const activeProjId = selectedProjId || activeProjects[0]?.id || ''
      setProjectId(activeProjId)

      const { data: workerData } = await workerRecruitmentService.listAvailable(activeProjId)
      setWorkers(workerData)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo cargar el mercado de trabajadores.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    document.title = 'Mercado de trabajadores | ObraTech'
    let active = true
    teamService.getContractorOptions()
      .then(async (projectResponse) => {
        if (!active) return
        const activeProjects = projectResponse.data.filter((project) => project.estadoProyecto !== 'COMPLETADO')
        setProjects(activeProjects)
        const initialProjId = activeProjects[0]?.id || ''
        setProjectId(initialProjId)

        const { data: workerData } = await workerRecruitmentService.listAvailable(initialProjId)
        if (active) setWorkers(workerData)
      })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.error || 'No se pudo cargar el mercado de trabajadores.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const handleProjectChange = async (newProjectId) => {
    setProjectId(newProjectId)
    setLoading(true)
    setError('')
    try {
      const { data: workerData } = await workerRecruitmentService.listAvailable(newProjectId)
      setWorkers(workerData)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudieron actualizar los estados de los trabajadores.')
    } finally {
      setLoading(false)
    }
  }

  const invite = async (worker) => {
    setPendingId(worker.id)
    setError('')
    setNotice('')
    try {
      await workerRecruitmentService.invite(worker.id, projectId)
      setWorkers((current) => current.map((item) => (item.id === worker.id ? { ...item, estado: 'INVITADO', invitado: true } : item)))
      setNotice(`Invitación enviada a ${worker.nombre}.`)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo enviar la invitación.')
    } finally {
      setPendingId('')
    }
  }

  const normalizedQuery = query.trim().toLocaleLowerCase()
  const filtered = workers.filter((worker) => (!workerId || worker.id === workerId)
    && [worker.nombre, worker.email, worker.oficio]
      .filter(Boolean).some((value) => value.toLocaleLowerCase().includes(normalizedQuery)))

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación contratista" className="client-nav">
          <Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link>
          <Link className="is-current" to="/trabajadores"><span className="material-symbols-outlined" aria-hidden="true">person_search</span>Trabajadores</Link>
          <Link to="/contratistas/mi-equipo"><span className="material-symbols-outlined" aria-hidden="true">groups</span>Mi equipo</Link>
          <Link to="/postulaciones"><span className="material-symbols-outlined" aria-hidden="true">assignment_turned_in</span>Proyectos disponibles</Link>
        </nav>
        <div className="client-sidebar-footer"><button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>

      <main className="client-main">
        <header className="client-header">
          <div><p className="client-eyebrow">Talento disponible</p><h1>Mercado de trabajadores</h1><p>Encuentra trabajadores para tus proyectos y envíales una invitación.</p></div>
          <button aria-label="Actualizar trabajadores" className="admin-icon-button" disabled={loading} onClick={() => loadMarket(projectId)} title="Actualizar" type="button"><span className="material-symbols-outlined" aria-hidden="true">refresh</span></button>
        </header>
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}

        <section className="worker-market-controls">
          <label className="admin-search"><span className="material-symbols-outlined" aria-hidden="true">search</span><span className="sr-only">Buscar trabajadores</span><input onChange={(event) => setQuery(event.target.value)} placeholder="Buscar por nombre, correo u oficio" type="search" value={query} /></label>
          <label className="worker-project-select"><span>Invitar al proyecto</span><select disabled={!projects.length} onChange={(event) => handleProjectChange(event.target.value)} value={projectId}>{projects.length ? projects.map((project) => <option key={project.id} value={project.id}>{project.titulo || 'Proyecto sin título'}</option>) : <option value="">No tienes proyectos activos</option>}</select></label>
          <span className="worker-market-count">{filtered.length} registrados</span>
        </section>

        {loading && <div className="admin-loading" role="status">Cargando trabajadores…</div>}
        {!loading && !projects.length && <div className="client-verification"><span className="material-symbols-outlined" aria-hidden="true">info</span><div><h2>No tienes proyectos activos</h2><p>Debes tener un proyecto asignado y activo para invitar trabajadores.</p><Link className="client-verification-action" to="/">Volver al dashboard</Link></div></div>}
        {!loading && filtered.length > 0 && <section aria-label="Trabajadores disponibles" className="worker-market-grid">{filtered.map((worker) => (
          <article className="worker-market-card" key={worker.id}>
            <header>
              <span className="team-member-avatar">{worker.nombre.slice(0, 1).toUpperCase()}</span>
              <div>
                <h2><Link to={`/trabajadores/${worker.id}`}>{worker.nombre}</Link></h2>
                <p>{worker.oficio || 'Oficio no registrado'}</p>
              </div>
              {worker.verificado && <span className="material-symbols-outlined team-member-verified" title="Verificado">verified</span>}
            </header>

            <dl>
              <div><dt>Experiencia</dt><dd>{worker.experiencia} años</dd></div>
              {worker.email && <div><dt>Correo</dt><dd>{worker.email}</dd></div>}
              {worker.telefono && <div><dt>Teléfono</dt><dd>{worker.telefono}</dd></div>}
            </dl>

            {worker.contratado || worker.estado === 'CONTRATADO' ? (
              <div className="worker-status-badge is-contracted">
                <span className="material-symbols-outlined" aria-hidden="true">check_circle</span>
                Contratado
              </div>
            ) : worker.invitado || worker.estado === 'INVITADO' ? (
              <div className="worker-status-badge is-invited">
                <span className="material-symbols-outlined" aria-hidden="true">schedule</span>
                Invitación enviada
              </div>
            ) : (
              <button
                className="button button-primary"
                disabled={!projectId || pendingId === worker.id}
                onClick={() => invite(worker)}
                type="button"
              >
                <span className="material-symbols-outlined" aria-hidden="true">person_add</span>
                {pendingId === worker.id ? 'Enviando…' : 'Invitar al proyecto'}
              </button>
            )}
          </article>
        ))}</section>}
        {!loading && projects.length > 0 && filtered.length === 0 && <div className="client-empty"><span className="material-symbols-outlined" aria-hidden="true">search_off</span><h2>{workers.length ? 'No hay coincidencias' : 'No hay trabajadores disponibles'}</h2><p>{workers.length ? 'Cambia el criterio de búsqueda.' : 'Los trabajadores disponibles aparecerán aquí.'}</p></div>}
      </main>
    </div>
  )
}