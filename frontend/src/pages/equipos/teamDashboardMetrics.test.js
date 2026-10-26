import { describe, expect, it } from 'vitest'
import {
  getDaysSinceActivity,
  getTeamDashboardMetrics,
  getTeamDashboardWarnings,
} from './teamDashboardMetrics.js'

describe('team dashboard metrics', () => {
  const now = new Date('2026-10-25T12:00:00')

  it('combines project members, team progress, phase counts, and recent reports', () => {
    const metrics = getTeamDashboardMetrics([
      {
        id: 'team-1',
        nombre: 'Cimientos',
        fase: 'PLANEACION',
        porcentajeAvance: 40,
        integrantes: [{ id: 'worker-1' }, { id: 'worker-2' }],
      },
      {
        id: 'team-2',
        nombre: 'Acabados',
        fase: 'PLANEACION',
        porcentajeAvance: 80,
        integrantes: [{ id: 'worker-2' }, { id: 'worker-3' }],
      },
    ], {
      integrantesDisponibles: [
        { id: 'worker-1' },
        { id: 'worker-2' },
        { id: 'worker-3' },
        { id: 'worker-4' },
      ],
    }, [
      { equipoId: 'team-1', creado: '2026-10-16T12:00:00' },
      { equipoId: 'team-1', creado: '2026-10-24T12:00:00' },
      { equipoId: 'team-2', creado: '2026-10-17T12:00:00' },
    ], now)

    expect(metrics).toMatchObject({
      teamCount: 2,
      workerCount: 4,
      assignedWorkerCount: 3,
      unassignedWorkerCount: 1,
      staleTeamCount: 1,
      averageProgress: 60,
      activityDaysByTeam: { 'team-1': 1, 'team-2': 8 },
      missingPhases: [
        { fase: 'Desarrollo', equipos: 0 },
        { fase: 'Detalles y entrega', equipos: 0 },
      ],
    })
    expect(metrics.progressChartData[0]).toEqual({
      equipo: 'Cimientos',
      fase: 'Planeación',
      avance: 40,
    })
  })

  it('handles empty projects, missing activity dates, and invalid dates', () => {
    const metrics = getTeamDashboardMetrics([], { integrantesDisponibles: [] }, [], now)

    expect(metrics.teamCount).toBe(0)
    expect(metrics.assignedWorkerCount).toBe(0)
    expect(metrics.unassignedWorkerCount).toBe(0)
    expect(metrics.averageProgress).toBe(0)
    expect(metrics.staleTeamCount).toBe(0)
    expect(metrics.activityDaysByTeam).toEqual({})
    expect(getDaysSinceActivity(null, now)).toBeNull()
    expect(getDaysSinceActivity('invalid-date', now)).toBeNull()
  })

  it('counts known assigned workers once even if a member appears in multiple teams', () => {
    const metrics = getTeamDashboardMetrics([
      { id: 'team-1', porcentajeAvance: 0, integrantes: [{ id: 'worker-1' }] },
      { id: 'team-2', porcentajeAvance: 0, integrantes: [{ id: 'worker-1' }, { id: 'unknown' }] },
    ], { integrantesDisponibles: [{ id: 'worker-1' }, { id: 'worker-2' }] })

    expect(metrics.assignedWorkerCount).toBe(1)
    expect(metrics.unassignedWorkerCount).toBe(1)
  })

  it('returns only applicable project warnings and caps the list at three', () => {
    const metrics = getTeamDashboardMetrics([
      {
        id: 'stale-team',
        nombre: 'Equipo antiguo',
        fase: 'PLANEACION',
        integrantes: [{ id: 'worker-1' }],
      },
    ], {
      integrantesDisponibles: [{ id: 'worker-1' }, { id: 'worker-2' }],
    }, [{ equipoId: 'stale-team', creado: '2026-10-17T12:00:00' }], now)

    const warnings = getTeamDashboardWarnings(metrics)

    expect(warnings).toHaveLength(3)
    expect(warnings.map((warning) => warning.id)).toEqual([
      'missing-phases',
      'unassigned-workers',
      'inactive-teams',
    ])
    expect(warnings[0].phase).toBe('DESARROLLO')
    expect(warnings[1].action).toBe('Asignar')
    expect(warnings[2].teamId).toBe('stale-team')

    const noAvailableWorkersMetrics = getTeamDashboardMetrics([
      { id: 'existing-team', fase: 'PLANEACION', integrantes: [{ id: 'worker-1' }] },
    ], { integrantesDisponibles: [{ id: 'worker-1' }] }, [], now)
    const missingPhaseWarning = getTeamDashboardWarnings(noAvailableWorkersMetrics)[0]
    expect(missingPhaseWarning.action).toBe('Revisar equipo')
    expect(missingPhaseWarning.teamId).toBe('existing-team')
    expect(missingPhaseWarning.description).toContain('no hay trabajadores disponibles')

    const healthyMetrics = getTeamDashboardMetrics([
      { id: 'team-1', fase: 'PLANEACION', porcentajeAvance: 20, integrantes: [{ id: 'worker-1' }] },
      { id: 'team-2', fase: 'DESARROLLO', porcentajeAvance: 20, integrantes: [] },
      { id: 'team-3', fase: 'DETALLE_Y_ENTREGA_FINAL', porcentajeAvance: 20, integrantes: [] },
    ], { integrantesDisponibles: [{ id: 'worker-1' }] }, [
      { equipoId: 'team-1', creado: '2026-10-24T12:00:00' },
      { equipoId: 'team-2', creado: '2026-10-24T12:00:00' },
      { equipoId: 'team-3', creado: '2026-10-24T12:00:00' },
    ], now)
    expect(getTeamDashboardWarnings(healthyMetrics)).toEqual([])
  })
})
