import { describe, expect, it } from 'vitest'
import {
  getRiskActions,
  getRiskFactorSentence,
  getRiskLevel,
  getRiskLevelClass,
  getRiskPhaseLabel,
  getRiskSummary,
  getRiskWeightExplanation,
} from './projectRiskText.js'

describe('project risk text helpers', () => {
  it('uses the displayed risk thresholds consistently', () => {
    expect(getRiskLevel(33)).toBe('Bajo')
    expect(getRiskLevel(34)).toBe('Medio')
    expect(getRiskLevel(66)).toBe('Medio')
    expect(getRiskLevel(67)).toBe('Alto')
    expect(getRiskLevel(null)).toBe('Sin datos')
    expect(getRiskLevelClass(20)).toBe('low')
    expect(getRiskLevelClass(50)).toBe('medium')
    expect(getRiskLevelClass(80)).toBe('high')
    expect(getRiskLevelClass(null)).toBe('unknown')
  })

  it('translates phase codes into labels for the weather summary', () => {
    expect(getRiskPhaseLabel('PLANEACION')).toBe('Planeación')
    expect(getRiskPhaseLabel('DETALLE_Y_ENTREGA_FINAL')).toBe('Detalles y entrega')
  })

  it('summarizes the overall risk and names the highest available factor', () => {
    const summary = getRiskSummary({
      score: 72,
      factores: [
        { nombre: 'Cronograma', score: 40, disponible: true, detalle: 'Avance esperado 58,6% frente a 4,6% real.' },
        { nombre: 'Clima', score: 100, disponible: false, detalle: 'Sin datos suficientes.' },
        { nombre: 'Equipos', score: 82, disponible: true, detalle: 'Equipos sin actividad conocida en 7 días: 1/1; actividades sin asignar: 0/2; trabajadores sin equipo: 0/3' },
      ],
    })

    expect(summary).toContain('riesgo ALTO')
    expect(summary).toContain('Razón principal:')
    expect(summary).toContain('Tu único equipo')
  })

  it('describes schedule progress in plain language with localized values', () => {
    expect(getRiskFactorSentence({
      nombre: 'Cronograma',
      detalle: 'Avance esperado 58.6% frente a 4.6% real.',
    })).toBe('Vas atrasado: llevas 4,6% de avance y deberías ir en 58,6%.')
  })

  it('describes team inactivity and unassigned workers without technical counters', () => {
    expect(getRiskFactorSentence({
      nombre: 'Equipos',
      detalle: 'Equipos sin actividad conocida en 7 días: 1/1; actividades sin asignar: 0/2; trabajadores sin equipo: 2/3',
    })).toBe('Tu único equipo lleva más de 7 días sin actividad. 2 de 3 trabajadores no tienen equipo asignado.')
  })

  it('mentions missing team activities only when there are any', () => {
    expect(getRiskFactorSentence({
      nombre: 'Equipos',
      detalle: 'Equipos sin actividad conocida en 7 días: sin datos; actividades sin asignar: 1/2; trabajadores sin equipo: sin datos',
    })).toContain('Falta asignar actividad a 1 equipo.')
  })

  it('describes rain and payment issues with natural singular and plural forms', () => {
    expect(getRiskFactorSentence({
      nombre: 'Clima',
      detalle: '13 de 14 días con probabilidad de lluvia ≥ 50.0%; exposición de fase 0.5.',
    })).toBe('Se espera lluvia (≥50% de probabilidad) en 13 de los próximos 14 días.')
    expect(getRiskFactorSentence({
      nombre: 'Pagos y aprobaciones',
      detalle: 'Cuotas vencidas sin registrar: 1; aprobación pendiente; evidencias disponibles.',
    })).toBe('Tienes 1 cuota vencida sin registrar. Hay una aprobación pendiente.')
    expect(getRiskFactorSentence({
      nombre: 'Pagos y aprobaciones',
      detalle: 'Cuotas vencidas sin registrar: 3; aprobación pendiente; evidencias disponibles.',
    })).toBe('Tienes 3 cuotas vencidas sin registrar. Hay una aprobación pendiente.')
  })

  it('shows a no-data summary and no urgent actions when all available factors are low', () => {
    expect(getRiskSummary({ score: null, factores: [] })).toContain('no hay datos suficientes')
    expect(getRiskSummary({
      score: 22,
      factores: [{ nombre: 'Clima', score: 20, disponible: true, detalle: '2 de 14 días con probabilidad de lluvia ≥ 50%; exposición de fase 0.5.' }],
    })).toContain('riesgo BAJO')
    expect(getRiskActions([
      { nombre: 'Clima', score: 20, disponible: true, detalle: '10 de 14 días con probabilidad de lluvia ≥ 50%; exposición de fase 0.5.' },
      { nombre: 'Equipos', score: null, disponible: false, detalle: 'Sin datos suficientes.' },
    ])).toEqual([])
  })

  it('recommends high-impact actions first and returns at most four', () => {
    const actions = getRiskActions([
      { nombre: 'Cronograma', score: 90, aporte: 36, disponible: true, detalle: 'Avance esperado 58.6% frente a 4.6% real.' },
      { nombre: 'Equipos', score: 80, aporte: 20, disponible: true, detalle: 'Equipos sin actividad conocida en 7 días: 1/1; actividades sin asignar: 0/2; trabajadores sin equipo: 2/3' },
      { nombre: 'Clima', score: 70, aporte: 14, disponible: true, detalle: '13 de 14 días con probabilidad de lluvia ≥ 50%; exposición de fase 0.5.' },
      { nombre: 'Pagos y aprobaciones', score: 70, aporte: 10, disponible: true, detalle: 'Cuotas vencidas sin registrar: 3; aprobación pendiente; evidencias pendientes.' },
    ])

    expect(actions).toHaveLength(4)
    expect(actions[0]).toBe('Replanificar el cronograma')
    expect(actions.some((action) => action.startsWith('Asignar equipo a 2 trabajadores sin equipo'))).toBe(true)
    expect(actions).toContain('Considerar la lluvia al programar las próximas tareas')
    expect(actions).toContain('Registrar 3 cuotas vencidas y resolver la aprobación pendiente y adjuntar evidencias de la fase actual')
  })

  it('explains weights using the actual values provided by the API', () => {
    expect(getRiskWeightExplanation([
      { nombre: 'Cronograma', peso: 40 },
      { nombre: 'Pagos y aprobaciones', peso: 15 },
    ])).toBe('El puntaje combina solo los factores con datos. Cada uno aporta según su peso: Cronograma 40%, Pagos y aprobaciones 15%.')
  })
})
