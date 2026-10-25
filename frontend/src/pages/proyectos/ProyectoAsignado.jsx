import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PhasePanel, PhaseRequirementActions } from '../../components/PhasePanel.jsx'
import { ProjectRiskPanel } from '../../components/ProjectRiskPanel.jsx'
import { participantProjectsService } from '../../services/participantProjectsService.js'
import { clientProjectsService } from '../../services/proyectoService.js'

function AssignedProject({ id, role }) {
  const [phaseStatus, setPhaseStatus] = useState(null)
  const [loading, setLoading] = useState(true)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')

  const reload = async () => {
    setError('')
    const { data } = await participantProjectsService.getPhaseStatus(id)
    setPhaseStatus(data)
  }

  useEffect(() => {
    document.title = 'Proyecto asignado | ObraTech'
    let active = true
    participantProjectsService.getPhaseStatus(id)
      .then(({ data }) => { if (active) setPhaseStatus(data) })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudo cargar este proyecto asignado.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id])

  const updateTasks = async (completadas, limpiezaProfundaCompletada) => {
    setPending(true)
    setError('')
    try {
      const { data } = await clientProjectsService.updatePhaseTasks(id, completadas, limpiezaProfundaCompletada)
      setPhaseStatus(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudieron actualizar las tareas.')
    } finally {
      setPending(false)
    }
  }

  if (loading) {
    return <main className="project-page-shell"><p className="admin-loading" role="status">Cargando proyecto asignado…</p></main>
  }

  return (
    <main className="project-page-shell">
      <header className="project-page-header">
        <div>
          <p className="client-eyebrow">Proyecto asignado</p>
          <h1>{phaseStatus?.titulo || 'Detalle del proyecto'}</h1>
          <p>{phaseStatus?.ubicacion || 'Ubicación no disponible'}</p>
        </div>
        <Link className="button button-secondary" to="/">
          <span className="material-symbols-outlined" aria-hidden="true">arrow_back</span>
          Volver al dashboard
        </Link>
      </header>

      {error && <div className="admin-alert admin-alert-error" role="alert">{error}</div>}
      {phaseStatus && (
        <>
          {phaseStatus.descripcion && <section className="project-detail-section"><h2>Descripción del proyecto</h2><p>{phaseStatus.descripcion}</p></section>}
          <PhasePanel canAdvance={false} phaseStatus={phaseStatus} />
          {role === 'ROLE_CONTRACTOR' && (
            <PhaseRequirementActions canManageTasks onUpdateTasks={updateTasks} pending={pending} phaseStatus={phaseStatus} />
          )}
          <ProjectRiskPanel fechaEntrega={phaseStatus.fechaEntrega} indicators={phaseStatus.indicadores} />
          <div className="project-assigned-actions">
            <Link className="button button-secondary" to={role === 'ROLE_CONTRACTOR' ? '/contratistas/mi-equipo' : '/trabajadores/mi-equipo'}>
              <span className="material-symbols-outlined" aria-hidden="true">groups</span>
              Ver equipos y reportar avances
            </Link>
            <button className="button button-secondary" disabled={pending} onClick={() => reload().catch((requestError) => setError(requestError.response?.data?.error || 'No se pudo actualizar el estado.'))} type="button">
              <span className="material-symbols-outlined" aria-hidden="true">refresh</span>
              Actualizar estado
            </button>
          </div>
        </>
      )}
    </main>
  )
}

export function ProyectoAsignado({ id, role }) {
  return <AssignedProject id={id} role={role} />
}
