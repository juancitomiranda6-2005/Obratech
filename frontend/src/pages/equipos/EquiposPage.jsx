import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { EvidenceAttachment } from '../../components/shared/EvidenceAttachment.jsx'
import { ProfileAvatar } from '../../components/shared/ProfileAvatar.jsx'
import { teamService } from '../../services/teamService.js'

const contractor = 'ROLE_CONTRACTOR'
const phaseOptions = [
  { value: 'PLANEACION', label: 'Fase 1 · Planeación' },
  { value: 'DESARROLLO', label: 'Fase 2 · Desarrollo' },
  { value: 'DETALLE_Y_ENTREGA_FINAL', label: 'Fase 3 · Detalles y entrega' },
]

function phaseName(phase) {
  return phaseOptions.find((option) => option.value === phase)?.label || 'Fase no definida'
}

// Elige el nombre visible del integrante y usa el usuario como alternativa si falta.
function displayName(member) {
  return member.nombre || member.username || 'Integrante'
}

// Muestra una cuadrilla. Recibe sus datos y las funciones del padre para guardar
// cambios; el contenido cambia segun la persona conectada sea trabajador o contratista.
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
  onUpdateTeam,
}) {
  const isWorker = role !== contractor
  const isContractor = role === contractor
  // Indica si esta abierto el formulario que permite elegir a otro integrante.
  const [showAddBox, setShowAddBox] = useState(false)
  // Indica si esta abierto el formulario para retirar a un integrante existente.
  const [showRemoveBox, setShowRemoveBox] = useState(false)
  // Guarda el ID elegido en el selector antes de pedirle al padre que lo agregue.
  const [selectedMemberId, setSelectedMemberId] = useState('')
  // Guarda el ID elegido en el selector antes de pedirle al padre que lo retire.
  const [selectedRemoveId, setSelectedRemoveId] = useState('')
  // Conserva el texto escrito por el trabajador hasta enviar el informe.
  const [progressReport, setProgressReport] = useState('')
  // Conserva las fotos o PDF elegidos para adjuntarlos al informe.
  const [progressEvidence, setProgressEvidence] = useState([])
  // Tiene el ID del informe que se esta editando; vacio significa que ninguno.
  const [editingReportId, setEditingReportId] = useState('')
  // Guarda el texto del informe mientras el trabajador hace cambios.
  const [editingReportContent, setEditingReportContent] = useState('')
  const [editingTeam, setEditingTeam] = useState(false)
  const [teamForm, setTeamForm] = useState({
    nombre: team.nombre || '',
    actividad: team.actividad || '',
    fase: team.fase || 'PLANEACION',
  })

  // Usa todos los IDs ya asignados en este proyecto para no ofrecer duplicados.
  const assignedIds = allAssignedInProject || []
  // Deja disponibles solo integrantes del proyecto que aun no estan en una cuadrilla.
  const availableMembers = project?.integrantesDisponibles?.filter(
    (pMember) => !assignedIds.includes(pMember.id),
  ) || []

  // Evita que el navegador recargue el formulario. Si se eligio un trabajador,
  // envia la cuadrilla y su ID al padre, que realiza la peticion a la API;
  // despues limpia el selector y cierra el formulario.
  const handleAddSubmit = (e) => {
    e.preventDefault()
    if (!selectedMemberId) return
    onAddMember(team, selectedMemberId)
    setSelectedMemberId('')
    setShowAddBox(false)
  }

  // Envía al padre la cuadrilla y el ID elegido para quitarlo mediante la API.
  // Si no se eligio a nadie, no hace nada; si se completa, limpia y cierra el selector.
  const handleRemoveSubmit = (e) => {
    e.preventDefault()
    if (!selectedRemoveId) return
    onRemoveMember(team, selectedRemoveId)
    setSelectedRemoveId('')
    setShowRemoveBox(false)
  }

  return (
    <article className="team-card">
      {/* Identificacion de la cuadrilla y porcentaje de avance */}
      <header className="team-card-header">
        <div>
          <p className="client-eyebrow">{team.proyecto}</p>
          <h2>{team.nombre || 'Equipo de trabajo'}</h2>
          <p>{team.actividad || 'Actividad no especificada'}</p>
          <small>{phaseName(team.fase)}</small>
        </div>
        <span className="team-progress-value">{Math.round(team.porcentajeAvance)}%</span>
      </header>

      {/* Barra visual de progreso del equipo */}
      <div aria-label={`Avance ${Math.round(team.porcentajeAvance)} por ciento`} className="team-progress-track">
        <span style={{ width: `${Math.max(0, Math.min(100, team.porcentajeAvance))}%` }} />
      </div>

      {isWorker && !team.proyectoDisponible && <p className="team-project-unavailable" role="status">Proyecto no disponible. Puedes consultar el historial, pero ya no enviar avances.</p>}

      {isWorker && team.proyectoDisponible && (
        <>
          {/* El trabajador cambia el porcentaje y el padre lo guarda en el backend. */}
          <form className="team-progress-form" onSubmit={(event) => { event.preventDefault(); onSaveProgress(team) }}>
            <label>
              <span>Actualizar avance</span>
              <input max="100" min="0" onChange={(event) => onProgressChange(team.id, event.target.value)} step="0.1" type="number" value={progressValue ?? team.porcentajeAvance} />
            </label>
            <button className="button button-primary" disabled={saving} type="submit">{saving ? 'Guardando…' : 'Guardar avance'}</button>
          </form>
          {/* Recoge el texto y los archivos; solo limpia el formulario si el envio tuvo exito. */}
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
        </>
      )}

      {isWorker && (
        /* Historial de informes del trabajador; tambien permite editar mientras el plazo siga abierto. */
        <section className="worker-submitted-reports" id={reportsAnchor ? 'mis-informes' : undefined}>
            <div className="client-section-heading"><h3>Mis avances enviados</h3><span>{workerReports.length}</span></div>
            {workerReports.length ? workerReports.map((report) => {
              const editableUntil = report.editableHasta ? new Date(report.editableHasta) : null
              const canEdit = team.proyectoDisponible && report.editable
                && !(currentTime && editableUntil && currentTime >= editableUntil.getTime())
              return <article className="worker-submitted-report" key={report.id}>
              {editingReportId === report.id && team.proyectoDisponible ? <form onSubmit={async (event) => {
                event.preventDefault()
                // Guarda el texto editado y cierra el formulario solamente si la API responde bien.
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
                <small className="worker-report-edit-deadline">{!team.proyectoDisponible ? 'Proyecto eliminado o no disponible; informe de solo lectura.' : canEdit ? `Editable hasta ${new Intl.DateTimeFormat('es-CO', { dateStyle: 'short', timeStyle: 'short' }).format(editableUntil)}` : 'Plazo de edición finalizado (30 minutos)'}</small>
                <p>{report.contenido}</p>
                <div className="evidence-attachment-list">{report.evidencias.map((evidence) => <EvidenceAttachment evidence={evidence} key={evidence.fileId} />)}</div>
              </>}
            </article>
            }) : <p className="worker-report-history-empty">Aún no has enviado avances a tus contratistas.</p>}
        </section>
      )}

      <section className="team-members">
        {/* Lista de integrantes y acciones individuales */}
        <div className="client-section-heading">
          <h3>Integrantes</h3>
          <span>{team.integrantes.length}</span>
        </div>

        {team.integrantes.length ? (
          <ul>
            {team.integrantes.map((member) => (
              <li key={member.id}>
                <ProfileAvatar className="team-member-avatar" name={displayName(member)} profileId={member.id} />
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
          /* Selector de trabajadores disponibles en el proyecto actual */
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
                  {/* Boton que cancela el alta de integrante */}
                  <button className="button button-secondary" onClick={() => setShowAddBox(false)} type="button">
                    Cancelar
                  </button>
                  {/* Boton que agrega el trabajador seleccionado */}
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
          /* Selector para retirar un integrante de esta cuadrilla */
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
                  {/* Boton que cancela el retiro */}
                  <button className="button button-secondary" onClick={() => setShowRemoveBox(false)} type="button">
                    Cancelar
                  </button>
                  {/* Boton que confirma el retiro del integrante */}
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

        {isContractor && editingTeam && (
          <form className="team-add-member-box" onSubmit={async (event) => {
            event.preventDefault()
            const saved = await onUpdateTeam(team, teamForm)
            if (saved) setEditingTeam(false)
          }}>
            <label><span>Nombre del equipo</span><input maxLength={100} onChange={(event) => setTeamForm((current) => ({ ...current, nombre: event.target.value }))} required value={teamForm.nombre} /></label>
            <label><span>Actividad</span><input maxLength={160} onChange={(event) => setTeamForm((current) => ({ ...current, actividad: event.target.value }))} required value={teamForm.actividad} /></label>
            <label><span>Fase de obra</span><select onChange={(event) => setTeamForm((current) => ({ ...current, fase: event.target.value }))} required value={teamForm.fase}>{phaseOptions.map((phase) => <option key={phase.value} value={phase.value}>{phase.label}</option>)}</select></label>
            <div className="team-add-member-actions">
              <button className="button button-secondary" onClick={() => setEditingTeam(false)} type="button">Cancelar</button>
              <button className="button button-primary" disabled={saving} type="submit">{saving ? 'Guardando…' : 'Guardar equipo'}</button>
            </div>
          </form>
        )}
      </section>

      {isContractor && (
        /* Acciones para administrar los integrantes y la cuadrilla */
        <footer className="team-card-actions">
          <button className="team-add-member-btn" disabled={saving} onClick={() => {
            setTeamForm({ nombre: team.nombre || '', actividad: team.actividad || '', fase: team.fase || 'PLANEACION' })
            setEditingTeam((current) => !current)
          }} type="button">
            <span className="material-symbols-outlined" aria-hidden="true">edit</span>
            {editingTeam ? 'Cerrar edición' : 'Editar equipo'}
          </button>
          {/* Boton que abre o cierra el selector para agregar */}
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

          {/* Boton que abre o cierra el selector para retirar */}
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

          {/* Boton que solicita eliminar la cuadrilla */}
          <button className="team-delete-button" disabled={saving} onClick={() => onDelete(team)} type="button">
            <span className="material-symbols-outlined" aria-hidden="true">delete</span>
            Eliminar equipo
          </button>
        </footer>
      )}
    </article>
  )
}

// Vista principal de equipos: consulta datos al backend, muestra cuadrillas y
// permite al contratista organizarlas o al trabajador reportar sus avances.
export function EquiposPage({ role, onLogout, projectId: initialProjectId = '' }) {
  const location = useLocation()
  const isContractor = role === contractor
  // Hora actualizada periodicamente para habilitar o bloquear la edicion de informes.
  const [currentTime, setCurrentTime] = useState(0)
  // Equipos que llegaron de la API; si la ruta trae proyecto, pertenecen solo a ese proyecto.
  const [teams, setTeams] = useState([])
  // Informes agrupados por ID de equipo para mostrarlos en la tarjeta correcta.
  const [workerReportsByTeam, setWorkerReportsByTeam] = useState({})
  // Opciones de proyecto e integrantes que el contratista puede asignar.
  const [projects, setProjects] = useState([])
  // ID del proyecto seleccionado en el formulario para crear una cuadrilla.
  const [projectId, setProjectId] = useState(initialProjectId)
  // Texto del campo de nombre de la nueva cuadrilla.
  const [name, setName] = useState('')
  // Texto que describe la actividad que realizara la cuadrilla.
  const [activity, setActivity] = useState('')
  const [teamPhase, setTeamPhase] = useState('PLANEACION')
  // IDs marcados en la lista de integrantes disponibles.
  const [memberIds, setMemberIds] = useState([])
  // Cambios de avance aun no guardados, indexados por ID de equipo.
  const [progressValues, setProgressValues] = useState({})
  // Controla indicadores de carga mientras se consultan o actualizan datos.
  const [loading, setLoading] = useState(true)
  // ID de la accion en curso para deshabilitar solo los controles afectados.
  const [pending, setPending] = useState('')
  // Texto del error recibido de la API o producido al guardar.
  const [error, setError] = useState('')
  // Confirmacion visible despues de crear o actualizar un equipo o informe.
  const [notice, setNotice] = useState('')
  // Equipo que se mostrara en el dialogo antes de confirmar su eliminacion.
  const [teamToDelete, setTeamToDelete] = useState(null)

  // Consulta los equipos (y opciones del contratista) y actualiza el estado de la pagina.
  // Si se abrio desde un proyecto, envia ese ID para que el backend no mezcle cuadrillas.
  const loadData = async (showLoading = true) => {
    if (showLoading) setLoading(true)
    setError('')
    try {
      if (isContractor) {
        const [teamResponse, projectResponse] = await Promise.all([
          teamService.getContractorTeams(initialProjectId || undefined),
          teamService.getContractorOptions(),
        ])
        setTeams(teamResponse.data)
        setProjects(projectResponse.data)
        const selected = projectResponse.data.find((project) => project.id === projectId)
          || projectResponse.data.find((project) => project.id === initialProjectId)
          || projectResponse.data[0]
        setProjectId(selected?.id || '')
        setTeamPhase(selected?.faseActual || 'PLANEACION')
      } else {
        const { data } = await teamService.getWorkerTeams(initialProjectId || undefined)
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

  // Al abrir la vista consulta las cuadrillas y proyectos. La bandera active evita
  // actualizar React si el usuario sale de la pagina antes de recibir la respuesta.
  useEffect(() => {
    document.title = isContractor ? 'Equipos de trabajo | ObraTech' : 'Mi equipo de trabajo | ObraTech'
    let active = true
    const load = async () => {
      try {
        if (isContractor) {
          const [teamResponse, projectResponse] = await Promise.all([
            teamService.getContractorTeams(initialProjectId || undefined),
            teamService.getContractorOptions(),
          ])
          if (active) {
            setTeams(teamResponse.data)
            setProjects(projectResponse.data)
            const selected = projectResponse.data.find((project) => project.id === initialProjectId)
              || projectResponse.data[0]
            setProjectId(selected?.id || '')
            setTeamPhase(selected?.faseActual || 'PLANEACION')
          }
        } else {
          const { data } = await teamService.getWorkerTeams(initialProjectId || undefined)
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

  // Cuando el usuario abre el enlace #mis-informes, desplaza la pagina a esa seccion.
  useEffect(() => {
    if (!loading && !isContractor && location.hash === '#mis-informes') {
      document.getElementById('mis-informes')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [isContractor, loading, location.hash])

  // Actualiza el reloj cada 15 segundos para que venza visualmente el plazo de edicion.
  // El temporizador se limpia al salir de la vista y no se inicia para contratistas.
  useEffect(() => {
    if (isContractor) return undefined
    const timer = window.setInterval(() => setCurrentTime(Date.now()), 15000)
    return () => window.clearInterval(timer)
  }, [isContractor])

  const selectedProject = projects.find((project) => project.id === projectId)
  // Marca o desmarca un ID; asi el formulario permite elegir varios integrantes.
  const toggleMember = (id) => setMemberIds((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id])

  // Envia nombre, actividad e integrantes al endpoint del proyecto seleccionado.
  // Si la API confirma la creacion, limpia el formulario y vuelve a cargar los equipos.
  const createTeam = async (event) => {
    event.preventDefault()
    setPending('create')
    setError('')
    setNotice('')
    try {
      await teamService.createContractorTeam(projectId, {
        nombre: name,
        actividad: activity,
        integrantesIds: memberIds,
        fase: teamPhase,
      })
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

  const updateTeam = async (team, values) => {
    setPending(team.id)
    setError('')
    setNotice('')
    try {
      const { data } = await teamService.updateContractorTeam(team.proyectoId, team.id, values)
      setTeams((current) => current.map((item) => item.id === team.id ? data : item))
      setNotice('Equipo actualizado.')
      return true
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo actualizar el equipo.')
      return false
    } finally {
      setPending('')
    }
  }

  // Pide al backend agregar el trabajador a la cuadrilla del proyecto y reemplaza
  // la tarjeta local con la respuesta actualizada que devuelve la API.
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

  // Envia proyecto, equipo e integrante al endpoint de retiro y actualiza la tarjeta.
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

  // Convierte el valor del input a numero, lo guarda en la API y sincroniza la tarjeta.
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

  // Construye y envia el informe con sus archivos; agrega la respuesta al historial
  // local para que aparezca sin tener que recargar toda la pagina.
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

  // Guarda la version corregida de un informe y reemplaza ese elemento en el historial.
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

  // Elimina el equipo seleccionado en el dialogo y lo quita de la lista local.
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
  // Segundo filtro de seguridad visual: una ruta con proyecto nunca muestra otras cuadrillas.
  const visibleTeams = initialProjectId
    ? teams.filter((team) => team.proyectoId === initialProjectId)
    : teams

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
      {/* Navegacion principal del area de equipos */}
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
        {/* Encabezado y recarga manual de equipos */}
        <header className="client-header">
          <div><p className="client-eyebrow">{isContractor ? 'Gestión de cuadrillas' : 'Trabajo asignado'}</p><h1>{isContractor ? 'Equipos de trabajo' : 'Mi equipo de trabajo'}</h1><p>{isContractor ? 'Organiza integrantes y actividades de tus proyectos.' : 'Consulta tus actividades y reporta el avance de tu equipo.'}</p></div>
          <button aria-label="Actualizar equipos" className="admin-icon-button" disabled={loading} onClick={() => loadData()} title="Actualizar" type="button"><span className="material-symbols-outlined" aria-hidden="true">refresh</span></button>
        </header>

        {notice && <div className="admin-alert admin-alert-success" role="status">{notice}</div>}
        {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}

        {isContractor && hasCreatableProject && (
          /* Formulario para crear una cuadrilla dentro del proyecto seleccionado */
          <form className="team-create-panel" onSubmit={createTeam}>
            <div><p className="client-eyebrow">Nueva cuadrilla</p><h2>Crear equipo</h2></div>
            <div className="team-create-fields">
              <label><span>Proyecto</span><select onChange={(event) => { setProjectId(event.target.value); setMemberIds([]); setTeamPhase(projects.find((project) => project.id === event.target.value)?.faseActual || 'PLANEACION') }} required value={projectId}>{projects.filter((project) => project.integrantesDisponibles.length).map((project) => <option key={project.id} value={project.id}>{project.titulo || 'Proyecto sin título'}</option>)}</select></label>
              <label><span>Nombre del equipo</span><input maxLength={100} onChange={(event) => setName(event.target.value)} required value={name} /></label>
              <label><span>Actividad asignada</span><input maxLength={160} onChange={(event) => setActivity(event.target.value)} required value={activity} /></label>
              <label><span>Fase de obra</span><select onChange={(event) => setTeamPhase(event.target.value)} required value={teamPhase}>{phaseOptions.map((phase) => <option key={phase.value} value={phase.value}>{phase.label}</option>)}</select></label>
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
            {/* Boton para guardar el equipo con los integrantes seleccionados */}
            <button className="button button-primary" disabled={pending === 'create' || memberIds.length === 0} type="submit"><span className="material-symbols-outlined" aria-hidden="true">group_add</span>{pending === 'create' ? 'Creando…' : 'Crear equipo'}</button>
          </form>
        )}

        {isContractor && !loading && !hasCreatableProject && <div className="team-create-hint">Para crear una cuadrilla, primero asigna trabajadores a uno de tus proyectos.</div>}
        {loading && <div className="admin-loading" role="status">Cargando equipos…</div>}
        {!loading && visibleTeams.length === 0 && <div className="client-empty team-empty"><span className="material-symbols-outlined" aria-hidden="true">group_off</span><h2>{isContractor ? 'Aún no tienes equipos' : 'Sin equipos asignados'}</h2><p>{isContractor ? 'Crea una cuadrilla cuando tengas trabajadores asignados a un proyecto.' : 'Cuando te asignen a una cuadrilla, sus actividades y compañeros aparecerán aquí.'}</p><Link className="button button-secondary" to="/">Volver al dashboard</Link></div>}

        {/* Tarjetas de equipos limitadas al proyecto de la ruta, si aplica */}
        {visibleTeams.length > 0 && (
          <section aria-label="Equipos de trabajo" className="team-grid">
            {visibleTeams.map((team, index) => (
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
                onUpdateTeam={updateTeam}
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