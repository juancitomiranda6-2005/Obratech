import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { PhasePanel } from './PhasePanel.jsx'

describe('PhasePanel', () => {
  it('renders all phase weights and payment schedules without advance controls in read-only mode', () => {
    const phaseStatus = {
      faseActual: 'DESARROLLO',
      estadoFaseActual: 'BLOQUEADA',
      avanceObra: 52.5,
      avanceFases: [
        { fase: 'PLANEACION', avance: 100, equipos: 2 },
        { fase: 'DESARROLLO', avance: 45, equipos: 1 },
        { fase: 'DETALLE_Y_ENTREGA_FINAL', avance: 0, equipos: 0 },
      ],
      planPagos: {
        montoFase1: 3_000_000,
        cuotaSemanalFase1: 1_000_000,
        semanasFase1: 3,
        montoFase2: 5_000_000,
        cuotaSemanalFase2: 1_000_000,
        semanasFase2: 5,
        montoFase3: 2_000_000,
      },
      requisitos: {
        tareasCompletadas: false,
        evidenciasDisponibles: false,
        aprobacionCliente: false,
        cuotasPagadas: false,
        limpiezaProfunda: false,
      },
      totalEvidencias: 0,
      cuotasSemanaPagadas: 3,
      cuotasTotalesEsperadas: 8,
      cuotasFase: [],
    }

    const html = renderToStaticMarkup(<PhasePanel phaseStatus={phaseStatus} />)

    expect(html).toContain('Planeación')
    expect(html).toContain('Desarrollo')
    expect(html).toContain('Detalles y entrega')
    expect(html).toContain('por semana durante 3 semanas')
    expect(html).toContain('Pago único')
    expect(html).not.toContain('Avanzar a la siguiente fase')
  })
})
