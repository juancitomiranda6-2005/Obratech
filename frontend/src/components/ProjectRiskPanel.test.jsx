import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { ProjectRiskPanel } from './ProjectRiskPanel.jsx'

const indicators = {
  riesgo: {
    score: 69,
    factores: [],
    factoresConDatos: 0,
    totalFactores: 4,
    entregaProyectada: '2027-04-11',
    diasRetrasoProyectado: 10,
  },
}

describe('ProjectRiskPanel sections', () => {
  it('keeps delivery details out of the compact risk summary', () => {
    const html = renderToStaticMarkup(
      <ProjectRiskPanel indicators={indicators} sections={['summary']} />,
    )

    expect(html).toContain('Riesgo de retraso')
    expect(html).not.toContain('Entrega pactada vs. proyectada')
    expect(html).not.toContain('Datos disponibles:')
  })

  it('renders delivery details and notes in their separate section', () => {
    const html = renderToStaticMarkup(
      <ProjectRiskPanel
        fechaEntrega="2027-04-01"
        indicators={indicators}
        sections={['delivery']}
      />,
    )

    expect(html).toContain('Entrega pactada vs. proyectada')
    expect(html).toContain('Datos disponibles: 0 de 4 factores.')
    expect(html).not.toContain('Riesgo de retraso')
  })
})
