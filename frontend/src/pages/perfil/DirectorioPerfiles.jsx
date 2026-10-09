import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { profileDirectoryService } from '../../services/perfilService.js'

export function DirectorioPerfiles({ kind, specialty = '', availableOnly = false, onLogout }) {
  const isContractorDirectory = kind === 'contractors'
  const isWorkerDirectory = kind === 'workers'
  const [profiles, setProfiles] = useState([])
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    document.title = isContractorDirectory ? 'Directorio de contratistas | ObraTech' : isWorkerDirectory ? 'Directorio de trabajadores | ObraTech' : 'Directorio de clientes | ObraTech'
    let active = true
    const request = isContractorDirectory
      ? profileDirectoryService.listContractors(specialty)
      : isWorkerDirectory
        ? profileDirectoryService.listWorkers(availableOnly)
        : profileDirectoryService.listClients()
    request
      .then(({ data }) => { if (active) setProfiles(data) })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.error || 'No se pudo cargar el directorio.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [isContractorDirectory, isWorkerDirectory, specialty, availableOnly])

  const filtered = profiles.filter((profile) => [profile.nombre, profile.username, profile.email, profile.empresa, profile.especialidad]
    .filter(Boolean).some((value) => value.toLocaleLowerCase().includes(query.trim().toLocaleLowerCase())))

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación de directorios" className="client-nav">
          <Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link>
          <Link className={isContractorDirectory ? 'is-current' : ''} to="/contratistas"><span className="material-symbols-outlined" aria-hidden="true">engineering</span>Contratistas</Link>
          <Link className={isWorkerDirectory ? 'is-current' : ''} to="/trabajadores/disponibles"><span className="material-symbols-outlined" aria-hidden="true">construction</span>Trabajadores</Link>
          <Link className={!isContractorDirectory ? 'is-current' : ''} to="/clientes"><span className="material-symbols-outlined" aria-hidden="true">business</span>Clientes</Link>
        </nav>
        <div className="client-sidebar-footer"><button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>
      <main className="client-main">
        <header className="client-header">
          <div><p className="client-eyebrow">Red ObraTech</p><h1>{isContractorDirectory ? 'Directorio de contratistas' : isWorkerDirectory ? availableOnly ? 'Trabajadores disponibles' : 'Directorio de trabajadores' : 'Directorio de clientes'}</h1><p>{specialty ? `Especialidad: ${specialty}` : isContractorDirectory ? 'Consulta profesionales activos, su especialidad y reputación.' : isWorkerDirectory ? 'Consulta oficio, experiencia y disponibilidad de los trabajadores.' : 'Consulta los clientes activos de la plataforma.'}</p></div>
          <span className="market-result-count">{filtered.length} perfiles</span>
        </header>
        <section className="contractor-history-toolbar">
          <label className="admin-search"><span className="material-symbols-outlined" aria-hidden="true">search</span><span className="sr-only">Buscar perfil</span><input onChange={(event) => setQuery(event.target.value)} placeholder="Buscar por nombre, correo o especialidad" type="search" value={query} /></label>
        </section>
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
        {loading && <div className="admin-loading" role="status">Cargando directorio…</div>}
        {!loading && filtered.length > 0 && <section aria-label={isContractorDirectory ? 'Contratistas' : 'Clientes'} className="directory-grid">
          {filtered.map((profile) => <article className="directory-profile" key={profile.id}>
            <header><span className="team-member-avatar">{profile.nombre?.slice(0, 1).toUpperCase() || 'P'}</span><div><h2>{isContractorDirectory ? <Link to={`/contratistas/${profile.id}`}>{profile.nombre || profile.username}</Link> : isWorkerDirectory ? <Link to={`/trabajadores/${profile.id}`}>{profile.nombre || profile.username}</Link> : profile.nombre || profile.username}</h2><p>{isContractorDirectory ? profile.especialidad || 'Especialidad no registrada' : isWorkerDirectory ? profile.oficio || 'Oficio no registrado' : profile.empresa || 'Cliente particular'}</p></div>{profile.verificado && <span className="material-symbols-outlined team-member-verified" aria-label="Verificado">verified</span>}</header>
            <dl>{profile.email && <div><dt>Correo</dt><dd>{profile.email}</dd></div>}{profile.telefono && <div><dt>Teléfono</dt><dd>{profile.telefono}</dd></div>}{isContractorDirectory && <div><dt>Calificación</dt><dd><span className="material-symbols-outlined" aria-hidden="true">star</span> {profile.calificacionPromedio.toFixed(1)} / 5</dd></div>}{isWorkerDirectory && <><div><dt>Experiencia</dt><dd>{profile.experiencia ?? 0} años</dd></div><div><dt>Disponibilidad</dt><dd>{profile.disponible ? 'Disponible' : 'No disponible'}</dd></div></>}</dl>
          </article>)}
        </section>}
        {!loading && !error && filtered.length === 0 && <div className="client-empty"><span className="material-symbols-outlined" aria-hidden="true">search_off</span><h2>{profiles.length ? 'No hay coincidencias' : 'No hay perfiles registrados'}</h2><p>{profiles.length ? 'Prueba con otro criterio de búsqueda.' : 'Los perfiles activos aparecerán aquí.'}</p></div>}
      </main>
    </div>
  )
}