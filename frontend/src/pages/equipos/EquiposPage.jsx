import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { EvidenceAttachment } from '../../components/shared/EvidenceAttachment.jsx'
import { teamService } from '../../services/teamService.js'

const contractor = 'ROLE_CONTRACTOR'

function displayName(member) {
  return member.nombre || member.username || 'Integrante'
}

function TeamCard({
  team,
  role,
  saving,
  progressValue,
  project,
  allAssignedInProject,
  onProgressChange,
  onSaveProgress,
  onSubmitProgressReport,
  onUpdateProgressReport,
  workerReports = [],
  reportsAnchor = false,
  currentTime,
  onDelete,
  onAddMember,
  onRemoveMember,
}) {
  const isWorker = role !== contractor
  const isContractor = role === contractor
  const [showAddBox, setShowAddBox] = useState(false)
  const [showRemoveBox, setShowRemoveBox] = useState(false)
  const [selectedMemberId, setSelectedMemberId] = useState('')
  const [selectedRemoveId, setSelectedRemoveId] = useState('')
  const [progressReport, setProgressReport] = useState('')
  const [progressEvidence, setProgressEvidence] = useState([])
  const [editingReportId, setEditingReportId] = useState('')
  const [editingReportContent, setEditingReportContent] = useState('')

  // Trabajadores del proyecto que NO pertenecen a NINGÚN equipo en este proyecto
  const assignedIds = allAssignedInProject || []
  const availableMembers = project?.integrantesDisponibles?.filter(
    (pMember) => !assignedIds.includes(pMember.id),
  ) || []

  const handleAddSubmit = (e) => {
    e.preventDefault()
    if (!selectedMemberId) return
    onAddMember(team, selectedMemberId)
    setSelectedMemberId('')
    setShowAddBox(false)
  }

  const handleRemoveSubmit = (e) => {
    e.preventDefault()
    if (!selectedRemoveId) return
    onRemoveMember(team, selectedRemoveId)
    setSelectedRemoveId('')
    setShowRemoveBox(false)
  }

  return (
    <article className="team-card">
      <header className="team-card-header">
        <div>
          <p className="client-eyebrow">{team.proyecto}</p>
          <h2>{team.nombre || 'Equipo de trabajo'}</h2>
          <p>{team.actividad || 'Actividad no especificada'}</p>
        </div>
        <span className="team-progress-value">{Math.round(team.porcentajeAvance)}%</span>
      </header>

      <div aria-label={`Avance ${Math.round(team.porcentajeAvance)} por ciento`} className="team-progress-track">
        <span style={{ width: `${Math.max(0, Math.min(100, team.porcentajeAvance))}%` }} />
      </div>

      {isWorker && (
        <>
          <form className="team-progress-form" onSubmit={(event) => { event.preventDefault(); onSaveProgress(team) }}>
            <label>
              <span>Actualizar avance</span>
              <input max="100" min="0" onChange={(event) => onProgressChange(team.id, event.target.value)} step="0.1" type="number" value={progressValue ?? team.porcentajeAvance} />
            </label>
            <button className="button button-primary" disabled={saving} type="submit">{saving ? 'Guardando…' : 'Guardar avance'}</button>
          </form>
          <form className="worker-progress-report-form" onSubmit={async (event) => {
            event.preventDefault()
            const form = event.currentTarget
            const saved = await onSubmitProgressReport(team, progressReport, progressEvidence)
            if (saved) {
              setProgressReport('')
              setProgressEvidence([])
              form.reset()
            }
          }}>
            <label htmlFor={`worker-report-${team.id}`}>Reportar avance al contratista</label>
            <textarea id={`worker-report-${team.id}`} maxLength={3000} minLength={10} onChange={(event) => setProgressReport(event.target.value)} required rows={3} value={progressReport} />
            <label className="worker-evidence-picker" htmlFor={`worker-evidence-${team.id}`}>
              <span className="material-symbols-outlined" aria-hidden="true">add_a_photo</span>
              <span>{progressEvidence.length ? `${progressEvidence.length} archivos seleccionados` : 'Adjuntar fotos o PDF'}</span>
            </label>
            <input accept="image/jpeg,image/png,image/webp,application/pdf" id={`worker-evidence-${team.id}`} multiple onChange={(event) => setProgressEvidence(Array.from(event.target.files || []))} type="file" />
            <small>Adjunta de 1 a 5 evidencias; máximo 10 MB por archivo.</small>
            <button className="button button-primary" disabled={saving || progressEvidence.length === 0} type="submit">{saving ? 'Enviando…' : 'Enviar avance y evidencias'}</button>
          </form>
          <section className="worker-submitted-reports" id={reportsAnchor ? 'mis-informes' : undefined}>
            <div className="client-section-heading"><h3>Mis avances enviados</h3><span>{workerReports.length}</span></div>
            {workerReports.length ? workerReports.map((report) => {
              const editableUntil = report.editableHasta ? new Date(report.editableHasta) : null
              const canEdit = report.editable
                && !(currentTime && editableUntil && currentTime >= editableUntil.getTime())
              return <article className="worker-submitted-report" key={report.id}>
              {editingReportId === report.id ? <form onSubmit={async (event) => {
                event.preventDefault()
                const saved = await onUpdateProgressReport(team, report.id, editingReportContent)
                if (saved) setEditingReportId('')
              }}>
                <label htmlFor={`edit-worker-report-${report.id}`}>Editar avance</label>
                <textarea id={`edit-worker-report-${report.id}`} maxLength={3000} minLength={10} onChange={(event) => setEditingReportContent(event.target.value)} required rows={3} value={editingReportContent} />
                <div className="team-add-member-actions">
                  <button className="button button-secondary" onClick={() => setEditingReportId('')} type="button">Cancelar</button>
                  <button className="button button-primary" disabled={saving} type="submit">Guardar cambios</button>
                </div>
              </form> : <>
                <header><strong>{report.creado ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(report.creado)) : 'Avance enviado'}</strong>
                  <button aria-label="Editar mi informe de avance" className="report-edit-button" disabled={!canEdit} onClick={() => { setEditingReportId(report.id); setEditingReportContent(report.contenido) }} title={canEdit ? `Puedes editar hasta ${new Intl.DateTimeFormat('es-CO', { timeStyle: 'short' }).format(editableUntil)}` : 'El plazo de edición de 30 minutos terminó'} type="button"><span className="material-symbols-outlined" aria-hidden="true">{canEdit ? 'edit' : 'lock'}</span></button>
                </header>
                <small className="worker-report-edit-deadline">{canEdit ? `Editable hasta ${new Intl.DateTimeFormat('es-CO', { dateStyle: 'short', timeStyle: 'short' }).format(editableUntil)}` : 'Plazo de edición finalizado (30 minutos)'}</small>
                <p>{report.contenido}</p>
                <div className="evidence-attachment-list">{report.evidencias.map((evidence) => <EvidenceAttachment evidence={evidence} key={evidence.fileId} />)}</div>
              </>}
            </article>
            }) : <p className="worker-report-history-empty">Aún no has enviado avances a tus contratistas.</p>}
          </section>
        </>
      )}

      <section className="team-members">
        <div className="client-section-heading">
          <h3>Integrantes</h3>
          <span>{team.integrantes.length}</span>
        </div>

        {team.integrantes.length ? (
          <ul>
            {team.integrantes.map((member) => (
              <li key={member.id}>
                <span className="team-member-avatar">{displayName(member).slice(0, 1).toUpperCase()}</span>
                <div>
                  <strong>{displayName(member)}</strong>
                  <small>{member.oficio || 'Operario'}</small>
                </div>
                {member.verificado && (
                  <span aria-label="Perfil verificado" className="material-symbols-outlined team-member-verified" title="Perfil verificado">
                    verified
                  </span>
                )}
                {isContractor && (
                  <button
                    aria-label={`Quitar ${displayName(member)} del equipo`}
                    className="team-member-remove-btn"
                    disabled={saving}
                    onClick={() => onRemoveMember(team, member.id)}
                    title="Quitar de este equipo"
                    type="button"
                  >
                    <span className="material-symbols-outlined" aria-hidden="true">close</span>
                  </button>
                )}
              </li>
            ))}
          </ul>
        ) : (
          <p className="team-empty-members">Aún no hay integrantes registrados en esta cuadrilla.</p>
        )}

        {isContractor && showAddBox && (
          <form className="team-add-member-box" onSubmit={handleAddSubmit}>
            <label htmlFor={`add-member-${team.id}`}>Seleccionar trabajador del proyecto:</label>
            {availableMembers.length > 0 ? (
              <>
                <select
                  id={`add-member-${team.id}`}
                  onChange={(e) => setSelectedMemberId(e.target.value)}
                  required
                  value={selectedMemberId}
                >
                  <option value="">-- Selecciona un trabajador --</option>
                  {availableMembers.map((m) => (
                    <option key={m.id} value={m.id}>
                      {displayName(m)} ({m.oficio || 'Operario'})
                    </option>
                  ))}
                </select>
                <div className="team-add-member-actions">
                  <button className="button button-secondary" onClick={() => setShowAddBox(false)} type="button">
                    Cancelar
                  </button>
                  <button className="button button-primary" disabled={!selectedMemberId || saving} type="submit">
                    {saving ? 'Agregando…' : 'Agregar'}
                  </button>
                </div>
              </>
            ) : (
              <div className="team-no-more-members-hint">
                No hay más trabajadores disponibles en este proyecto.
                <Link to="/trabajadores">Contratar o invitar más trabajadores</Link>
                <div style={{ marginTop: '8px' }}>
                  <button className="button button-secondary" onClick={() => setShowAddBox(false)} type="button">
                    Cerrar
                  </button>
                </div>
              </div>
            )}
          </form>
        )}

        {isContractor && showRemoveBox && (
          <form className="team-add-member-box" onSubmit={handleRemoveSubmit}>
            <label htmlFor={`remove-member-${team.id}`}>Seleccionar trabajador a retirar:</label>
            {team.integrantes.length > 0 ? (
              <>
                <select
                  id={`remove-member-${team.id}`}
                  onChange={(e) => setSelectedRemoveId(e.target.value)}
                  required
                  value={selectedRemoveId}
                >
                  <option value="">-- Selecciona quién saldrá del equipo --</option>
                  {team.integrantes.map((m) => (
                    <option key={m.id} value={m.id}>
                      {displayName(m)} ({m.oficio || 'Operario'})
                    </option>
                  ))}
                </select>
                <div className="team-add-member-actions">
                  <button className="button button-secondary" onClick={() => setShowRemoveBox(false)} type="button">
                    Cancelar
                  </button>
                  <button className="button confirm-delete" disabled={!selectedRemoveId || saving} style={{ padding: '5px 12px', fontSize: '11px' }} type="submit">
                    {saving ? 'Retirando…' : 'Retirar del equipo'}
                  </button>
                </div>
              </>
            ) : (
              <div className="team-no-more-members-hint">
                No hay integrantes en esta cuadrilla para retirar.
                <div style={{ marginTop: '8px' }}>
                  <button className="button button-secondary" onClick={() => setShowRemoveBox(false)} type="button">
                    Cerrar
                  </button>
                </div>
              </div>
            )}
          </form>
        )}
      </section>

      {isContractor && (
        <footer className="team-card-actions">
          <button
            className="team-add-member-btn"
            disabled={saving}
            onClick={() => {
              setShowAddBox((prev) => !prev)
              setShowRemoveBox(false)
            }}
            type="button"
          >
            <span className="material-symbols-outlined" aria-hidden="true">
              {showAddBox ? 'close' : 'person_add'}
            </span>
            {showAddBox ? 'Cerrar' : 'Agregar trabajador'}
          </button>

          <button
            className="team-remove-member-btn"
            disabled={saving || team.integrantes.length === 0}
            onClick={() => {
              setShowRemoveBox((prev) => !prev)
              setShowAddBox(false)
            }}
            title={team.integrantes.length === 0 ? 'No hay integrantes en esta cuadrilla' : 'Retirar integrante de esta cuadrilla'}
            type="button"
          >
            <span className="material-symbols-outlined" aria-hidden="true">
              {showRemoveBox ? 'close' : 'person_remove'}
            </span>
            {showRemoveBox ? 'Cerrar' : 'Retirar trabajador'}
          </button>

          <button className="team-delete-button" disabled={saving} onClick={() => onDelete(team)} type="button">
            <span className="material-symbols-outlined" aria-hidden="true">delete</span>
            Eliminar equipo
          </button>
        </footer>
      )}
    </article>
  )
}

export function EquiposPage({ role, onLogout, projectId: initialProjectId = '' }) {
  const location = useLocation()
  const isContractor = role === contractor
  const [currentTime, setCurrentTime] = useState(0)
  const [teams, setTeams] = useState([])
  const [workerReportsByTeam, setWorkerReportsByTeam] = useState({})
  const [projects, setProjects] = useState([])
  const [projectId, setProjectId] = useState(initialProjectId)
  const [name, setName] = useState('')
  const [activity, setActivity] = useState('')
  const [memberIds, setMemberIds] = useState([])
  const [progressValues, setProgressValues] = useState({})
  const [loading, setLoading] = useState(true)
  const [pending, setPending] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [teamToDelete, setTeamToDelete] = useState(null)

  const loadData = async (showLoading = true) => {
    if (showLoading) setLoading(true)
    setError('')
    try {
      if (isContractor) {
        const [teamResponse, projectResponse] = await Promise.all([
          teamService.getContractorTeams(),
          teamService.getContractorOptions(),
        ])
        setTeams(teamResponse.data)
        setProjects(projectResponse.data)
        setProjectId((current) => projectResponse.data.some((project) => project.id === current)
          ? current
          : projectResponse.data.find((project) => project.id === initialProjectId)?.id || projectResponse.data[0]?.id || '')
      } else {
        const { data } = await teamService.getWorkerTeams()
        setTeams(data)
        const reports = await Promise.all(data.map(async (team) => {
          const response = await teamService.getWorkerReports(team.id)
          return [team.id, response.data]
        }))
        setWorkerReportsByTeam(Object.fromEntries(reports))
      }
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudieron cargar los equipos de trabajo.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    document.title = isContractor ? 'Equipos de trabajo | ObraTech' : 'Mi equipo de trabajo | ObraTech'
    let active = true
    const load = async () => {
      try {
        if (isContractor) {
          const [teamResponse, projectResponse] = await Promise.all([
            teamService.getContractorTeams(),
            teamService.getContractorOptions(),
          ])
          if (active) {
            setTeams(teamResponse.data)
            setProjects(projectResponse.data)
            setProjectId(projectResponse.data.find((project) => project.id === initialProjectId)?.id || projectResponse.data[0]?.id || '')
          }
        } else {
          const { data } = await teamService.getWorkerTeams()
          const reports = await Promise.all(data.map(async (team) => {
            const response = await teamService.getWorkerReports(team.id)
            return [team.id, response.data]
          }))
          if (active) {
            setTeams(data)
            setWorkerReportsByTeam(Object.fromEntries(reports))
          }
        }
      } catch (requestError) {
        if (active) setError(requestError.response?.data?.error || 'No se pudieron cargar los equipos de trabajo.')
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [isContractor, initialProjectId])

  useEffect(() => {
    if (!loading && !isContractor && location.hash === '#mis-informes') {
      document.getElementById('mis-informes')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [isContractor, loading, location.hash])

  useEffect(() => {
    if (isContractor) return undefined
    const timer = window.setInterval(() => setCurrentTime(Date.now()), 15000)
    return () => window.clearInterval(timer)
  }, [isContractor])

  const selectedProject = projects.find((project) => project.id === projectId)
  const toggleMember = (id) => setMemberIds((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id])

  const createTeam = async (event) => {
    event.preventDefault()
    setPending('create')
    setError('')
    setNotice('')
    try {
      await teamService.createContractorTeam(projectId, { nombre: name, actividad: activity, integrantesIds: memberIds })
      setName('')
      setActivity('')
      setMemberIds([])
      setNotice('Equipo creado correctamente.')
      await loadData(false)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo crear el equipo.')
    } finally {
      setPending('')
    }
  }

  const handleAddMember = async (team, memberId) => {
    setPending(team.id)
    setError('')
    setNotice('')
    try {
      const { data } = await teamService.addMemberToTeam(team.proyectoId, team.id, memberId)
      setTeams((current) => current.map((item) => (item.id === team.id ? data : item)))
      setNotice('Trabajador agregado al equipo.')
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo agregar el trabajador al equipo.')
    } finally {
      setPending('')
    }
  }

  const handleRemoveMember = async (team, memberId) => {
    setPending(team.id)
    setError('')
    setNotice('')
    try {
      const { data } = await teamService.removeMemberFromTeam(team.proyectoId, team.id, memberId)
      setTeams((current) => current.map((item) => (item.id === team.id ? data : item)))
      setNotice('Trabajador retirado del equipo.')
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo retirar el trabajador.')
    } finally {
      setPending('')
    }
  }

  const saveProgress = async (team) => {
    const value = Number(progressValues[team.id] ?? team.porcentajeAvance)
    setPending(team.id)
    setError('')
    setNotice('')
    try {
      const { data } = await teamService.updateWorkerProgress(team.id, value)
      setTeams((current) => current.map((item) => item.id === team.id ? data : item))
      setNotice('Avance actualizado.')
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo actualizar el avance.')
    } finally {
      setPending('')
    }
  }

  const submitProgressReport = async (team, content, files) => {
    setPending(team.id)
    setError('')
    setNotice('')
    try {
      const { data } = await teamService.createWorkerReport(team.id, content, files)
      setWorkerReportsByTeam((current) => ({
        ...current,
        [team.id]: [data, ...(current[team.id] || [])],
      }))
      setNotice('Avance y evidencias enviados al contratista.')
      return true
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo enviar el avance.')
      return false
    } finally {
      setPending('')
    }
  }

  const updateProgressReport = async (team, reportId, content) => {
    setPending(team.id)
    setError('')
    setNotice('')
    try {
      const { data } = await teamService.updateWorkerReport(team.id, reportId, content)
      setWorkerReportsByTeam((current) => ({
        ...current,
        [team.id]: (current[team.id] || []).map((report) => report.id === reportId ? data : report),
      }))
      setNotice('Informe de avance actualizado.')
      return true
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo editar el informe de avance.')
      return false
    } finally {
      setPending('')
    }
  }

  const deleteTeam = async () => {
    if (!teamToDelete) return
    setPending(teamToDelete.id)
    setError('')
    try {
      await teamService.deleteContractorTeam(teamToDelete.proyectoId, teamToDelete.id)
      setTeams((current) => current.filter((item) => item.id !== teamToDelete.id))
      setTeamToDelete(null)
      setNotice('Equipo eliminado.')
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo eliminar el equipo.')
    } finally {
      setPending('')
    }
  }

  const hasCreatableProject = projects.some((project) => project.integrantesDisponibles.length > 0)

  // Mapa de miembros asignados a equipos por proyecto
  const assignedByProject = {}
  const memberAssignedTeamName = {}
  teams.forEach((t) => {
    if (!assignedByProject[t.proyectoId]) {
      assignedByProject[t.proyectoId] = []
    }
    t.integrantes.forEach((m) => {
      assignedByProject[t.proyectoId].push(m.id)
      memberAssignedTeamName[`${t.proyectoId}_${m.id}`] = t.nombre || 'otro equipo'
    })
  })

  return (
    <div className="client-layout">
      <aside className="client-sidebar">
        <Link className="client-brand" to="/"><span className="material-symbols-outlined" aria-hidden="true">construction</span>ObraTech</Link>
        <nav aria-label="Navegación de equipos" className="client-nav">
          <Link to="/"><span className="material-symbols-outlined" aria-hidden="true">dashboard</span>Dashboard</Link>
          {isContractor ? <Link className="is-current" to="/contratistas/mi-equipo"><span className="material-symbols-outlined" aria-hidden="true">groups</span>Mi equipo</Link> : <><Link className="is-current" to="/trabajadores/mi-equipo"><span className="material-symbols-outlined" aria-hidden="true">groups</span>Mi equipo de trabajo</Link><a href="#mis-informes"><span className="material-symbols-outlined" aria-hidden="true">description</span>Mis informes enviados</a></>}
          {isContractor ? <Link to="/postulaciones"><span className="material-symbols-outlined" aria-hidden="true">assignment_turned_in</span>Proyectos disponibles</Link> : <Link to="/perfil-trabajador"><span className="material-symbols-outlined" aria-hidden="true">person</span>Mi perfil</Link>}
        </nav>
        <div className="client-sidebar-footer"><button className="client-logout" onClick={onLogout} type="button"><span className="material-symbols-outlined" aria-hidden="true">logout</span>Cerrar sesión</button></div>
      </aside>

      <main className="client-main">
        <header className="client-header">
          <div><p className="client-eyebrow">{isContractor ? 'Gestión de cuadrillas' : 'Trabajo asignado'}</p><h1>{isContractor ? 'Equipos de trabajo' : 'Mi equipo de trabajo'}</h1><p>{isContractor ? 'Organiza integrantes y actividades de tus proyectos.' : 'Consulta tus actividades y reporta el avance de tu equipo.'}</p></div>
          <button aria-label="Actualizar equipos" className="admin-icon-button" disabled={loading} onClick={() => loadData()} title="Actualizar" type="button"><span className="material-symbols-outlined" aria-hidden="true">refresh</span></button>
        </header>

        {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}

        {isContractor && hasCreatableProject && (
          <form className="team-create-panel" onSubmit={createTeam}>
            <div><p className="client-eyebrow">Nueva cuadrilla</p><h2>Crear equipo</h2></div>
            <div className="team-create-fields">
              <label><span>Proyecto</span><select onChange={(event) => { setProjectId(event.target.value); setMemberIds([]) }} required value={projectId}>{projects.filter((project) => project.integrantesDisponibles.length).map((project) => <option key={project.id} value={project.id}>{project.titulo || 'Proyecto sin título'}</option>)}</select></label>
              <label><span>Nombre del equipo</span><input maxLength={100} onChange={(event) => setName(event.target.value)} required value={name} /></label>
              <label><span>Actividad asignada</span><input maxLength={160} onChange={(event) => setActivity(event.target.value)} required value={activity} /></label>
            </div>
            <fieldset className="team-member-picker">
              <legend>Integrantes del proyecto</legend>
              {selectedProject?.integrantesDisponibles.map((member) => {
                const alreadyInTeam = memberAssignedTeamName[`${projectId}_${member.id}`]
                return (
                  <label
                    key={member.id}
                    style={alreadyInTeam ? { opacity: 0.5, cursor: 'not-allowed' } : {}}
                    title={alreadyInTeam ? `Ya pertenece a la cuadrilla "${alreadyInTeam}"` : ''}
                  >
                    <input
                      checked={memberIds.includes(member.id)}
                      disabled={Boolean(alreadyInTeam)}
                      onChange={() => toggleMember(member.id)}
                      type="checkbox"
                    />
                    <span>
                      {displayName(member)}
                      <small>
                        {alreadyInTeam ? `(En: ${alreadyInTeam})` : member.oficio || 'Operario'}
                      </small>
                    </span>
                  </label>
                )
              })}
            </fieldset>
            <button className="button button-primary" disabled={pending === 'create' || memberIds.length === 0} type="submit"><span className="material-symbols-outlined" aria-hidden="true">group_add</span>{pending === 'create' ? 'Creando…' : 'Crear equipo'}</button>
          </form>
        )}

        {isContractor && !loading && !hasCreatableProject && <div className="team-create-hint">Para crear una cuadrilla, primero asigna trabajadores a uno de tus proyectos.</div>}
        {loading && <div className="admin-loading" role="status">Cargando equipos…</div>}
        {!loading && teams.length === 0 && <div className="client-empty team-empty"><span className="material-symbols-outlined" aria-hidden="true">group_off</span><h2>{isContractor ? 'Aún no tienes equipos' : 'Sin equipos asignados'}</h2><p>{isContractor ? 'Crea una cuadrilla cuando tengas trabajadores asignados a un proyecto.' : 'Cuando te asignen a una cuadrilla, sus actividades y compañeros aparecerán aquí.'}</p><Link className="button button-secondary" to="/">Volver al dashboard</Link></div>}

        {teams.length > 0 && (
          <section aria-label="Equipos de trabajo" className="team-grid">
            {teams.map((team, index) => (
              <TeamCard
                key={team.id}
                allAssignedInProject={assignedByProject[team.proyectoId] || []}
                onAddMember={handleAddMember}
                onDelete={setTeamToDelete}
                onProgressChange={(id, value) => setProgressValues((current) => ({ ...current, [id]: value }))}
                onRemoveMember={handleRemoveMember}
                onSaveProgress={saveProgress}
                onSubmitProgressReport={submitProgressReport}
                onUpdateProgressReport={updateProgressReport}
                workerReports={workerReportsByTeam[team.id] || []}
                reportsAnchor={!isContractor && index === 0}
                currentTime={currentTime}
                project={projects.find((p) => p.id === team.proyectoId)}
                progressValue={progressValues[team.id]}
                role={role}
                saving={pending === team.id}
                team={team}
              />
            ))}
          </section>
        )}
      </main>

      {teamToDelete && <div className="confirm-overlay" role="presentation"><section aria-labelledby="delete-team-title" aria-modal="true" className="confirm-dialog" role="dialog"><span className="material-symbols-outlined confirm-icon" aria-hidden="true">warning</span><h2 id="delete-team-title">Eliminar equipo</h2><p>¿Eliminar “{teamToDelete.nombre}”? Los integrantes seguirán vinculados al proyecto.</p><div className="confirm-actions"><button className="button button-secondary" disabled={Boolean(pending)} onClick={() => setTeamToDelete(null)} type="button">Cancelar</button><button className="button confirm-delete" disabled={Boolean(pending)} onClick={deleteTeam} type="button">{pending ? 'Eliminando…' : 'Eliminar equipo'}</button></div></section></div>}
    </div>
  )
}