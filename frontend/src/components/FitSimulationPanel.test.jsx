import { describe, expect, it } from 'vitest'
import { formatDefaultFields, modelPercentage, modelUnavailableReason } from './FitSimulationPanel.jsx'

describe('FitSimulationPanel model percentage', () => {
  it('renders 48 as 48 percent without scaling it again', () => {
    expect(`${modelPercentage({ porcentaje: 48 })}%`).toBe('48%')
  })

  it('uses friendly names for missing model fields', () => {
    expect(formatDefaultFields(['distancia_km', 'pct_obras_a_tiempo']))
      .toBe('distancia a la obra y obras entregadas a tiempo')
  })

  it('identifies unavailable model responses', () => {
    expect(modelUnavailableReason({ mock: true })).toBe('mock: true')
    expect(modelUnavailableReason({ mock: false })).toBe('porcentaje ausente o inválido')
    expect(modelUnavailableReason({ mock: false, porcentaje: 48 })).toBe('')
  })
})
