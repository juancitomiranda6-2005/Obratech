export const TEAM_PHASES = [
  { value: 'PLANEACION', label: 'Planeación' },
  { value: 'DESARROLLO', label: 'Desarrollo' },
  { value: 'DETALLE_Y_ENTREGA_FINAL', label: 'Detalles y entrega' },
]

const DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000

export function getDaysSinceActivity(date, now = new Date()) {
  if (!date) return null
  const activityTime = new Date(date).getTime()
  const currentTime = now instanceof Date ? now.getTime() : new Date(now).getTime()
  if (!Number.isFinite(activityTime) || !Number.isFinite(currentTime)) return null
  return Math.max(0, Math.floor((currentTime - activityTime) / DAY_IN_MILLISECONDS))
}

export function getTeamDashboardMetrics(teams = [], project, reports = [], now = new Date()) {
  const reportsByTeam = new Map()
  reports.forEach((report) => {
    if (!report.equipoId || !report.creado) return
    const currentTime = new Date(report.creado).getTime()
    const previousTime = reportsByTeam.get(report.equipoId)
    if (Number.isFinite(currentTime) && (previousTime == null || currentTime > previousTime)) {
      reportsByTeam.set(report.equipoId, currentTime)
    }
  })

  const activityDaysByTeam = Object.fromEntries(teams.map((team) => {
    const lastActivity = reportsByTeam.get(team.id)
    return [team.id, lastActivity == null
      ? null
      : getDaysSinceActivity(new Date(lastActivity), now)]
  }))
  const assignedWorkerIds = new Set(teams.flatMap((team) => (
    (team.integrantes || []).map((member) => member.id).filter(Boolean)
  )))
  const workers = project?.integrantesDisponibles || []
  const phaseCounts = TEAM_PHASES.map((phase) => ({
    fase: phase.label,
    equipos: teams.filter((team) => team.fase === phase.value).length,
  }))
  const missingPhases = phaseCounts.filter((phase) => phase.equipos === 0)
  const staleTeams = teams.filter((team) => activityDaysByTeam[team.id] > 7)
  const averageProgress = teams.length
    ? teams.reduce((total, team) => total + (Number(team.porcentajeAvance) || 0), 0) / teams.length
    : 0

  return {
    teamCount: teams.length,
    firstTeamId: teams[0]?.id,
    workerCount: workers.length,
    assignedWorkerCount: [...assignedWorkerIds].filter((id) => workers.some((worker) => worker.id === id)).length,
    unassignedWorkerCount: workers.filter((worker) => !assignedWorkerIds.has(worker.id)).length,
    staleTeamCount: staleTeams.length,
    staleTeams,
    averageProgress,
    activityDaysByTeam,
    missingPhases,
    phaseCounts,
    progressChartData: teams.map((team) => ({
      equipo: team.nombre || 'Equipo de trabajo',
      fase: TEAM_PHASES.find((phase) => phase.value === team.fase)?.label || 'Fase no definida',
      avance: Math.max(0, Math.min(100, Number(team.porcentajeAvance) || 0)),
    })),
  }
}

export function getTeamDashboardWarnings(metrics) {
  const warnings = []
  if (metrics.missingPhases.length > 0) {
    const canCreateTeam = metrics.unassignedWorkerCount > 0
    const canReviewExistingTeam = Boolean(metrics.firstTeamId)
    warnings.push({
      id: 'missing-phases',
      icon: 'construction',
      title: 'Fases sin equipos',
      description: canCreateTeam
        ? `Aún no hay equipos en ${metrics.missingPhases.map((phase) => phase.fase).join(', ')}.`
        : canReviewExistingTeam
          ? `Aún no hay equipos en ${metrics.missingPhases.map((phase) => phase.fase).join(', ')} y no hay trabajadores disponibles para crear otra cuadrilla.`
          : `Aún no hay equipos en ${metrics.missingPhases.map((phase) => phase.fase).join(', ')}. Asigna trabajadores al proyecto para crear una cuadrilla.`,
      action: canCreateTeam ? 'Crear equipo' : canReviewExistingTeam ? 'Revisar equipo' : 'Asignar trabajadores',
      phase: TEAM_PHASES.find((phase) => phase.label === metrics.missingPhases[0].fase)?.value || 'PLANEACION',
      teamId: canCreateTeam ? undefined : metrics.firstTeamId,
    })
  }
  if (metrics.unassignedWorkerCount > 0) {
    warnings.push({
      id: 'unassigned-workers',
      icon: 'person_add',
      title: 'Trabajadores sin equipo',
      description: `${metrics.unassignedWorkerCount} ${metrics.unassignedWorkerCount === 1 ? 'trabajador no tiene' : 'trabajadores no tienen'} equipo asignado.`,
      action: 'Asignar',
    })
  }
  if (metrics.staleTeamCount > 0) {
    warnings.push({
      id: 'inactive-teams',
      icon: 'schedule',
      title: 'Equipos sin actividad reciente',
      description: `${metrics.staleTeamCount} ${metrics.staleTeamCount === 1 ? 'equipo lleva' : 'equipos llevan'} más de 7 días sin actividad.`,
      action: 'Revisar equipo',
      teamId: metrics.staleTeams[0]?.id,
    })
  }
  return warnings.slice(0, 3)
}
