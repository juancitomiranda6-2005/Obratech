import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import {
  getRiskActions,
  getRiskFactorSentence,
  getRiskLevel,
  getRiskLevelClass,
  getRiskPhaseLabel,
  getRiskSummary,
  getRiskWeightExplanation,
} from './projectRiskText.js'
import './project-risk-panel.css'

const formatDate = (date) => date
  ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' }).format(new Date(`${date}T00:00:00`))
  : 'Sin datos'

export function ProjectRiskPanel({
  indicators,
  fechaEntrega,
  compact = false,
  sections = ['summary', 'factors', 'delivery', 'weather', 'teams'],
}) {
  if (!indicators) {
    return <section className="contractor-risk-panel contractor-risk-empty"><p>Los indicadores de riesgo no están disponibles.</p></section>
  }
  const showSummary = sections.includes('summary')
  const showFactors = sections.includes('factors')
  const showDelivery = sections.includes('delivery')
  const showWeather = sections.includes('weather')
  const showTeams = sections.includes('teams')
  const risk = indicators.riesgo
  const forecast = indicators.pronosticoLluvia
  const teams = indicators.avanceEquipos || []
  const riskFactors = (risk?.factores || []).filter((factor) => factor.disponible && factor.score != null)
    .sort((first, second) => second.score - first.score)
  const summaryLevel = getRiskLevel(risk?.score)
  const riskActions = getRiskActions(risk?.factores || [])

  return (
    <section aria-label="Indicadores de riesgo del proyecto" className={`contractor-risk-panel ${compact ? 'is-compact' : ''}`}>
      {showSummary && (
        <>
          <header className="contractor-risk-header">
            <div>
              <p className="client-eyebrow">Seguimiento preventivo</p>
              <h3>Riesgo de retraso</h3>
            </div>
            <strong className={`contractor-risk-badge risk-${getRiskLevelClass(risk?.score)}`}>
              {summaryLevel}{risk?.score != null && ` · ${Math.round(risk.score)}/100`}
            </strong>
          </header>
          <p className="contractor-risk-summary">{getRiskSummary(risk)}</p>
          <p className="contractor-risk-method">{getRiskWeightExplanation(risk?.factores || [])}</p>
        </>
      )}

      {showFactors && <section aria-label="Riesgo por factor" className="contractor-risk-chart contractor-risk-factor-chart">
        <div className="contractor-risk-chart-heading">
          <div>
            <h4>Riesgo por factor</h4>
            <p>Los factores con mayor riesgo aparecen primero.</p>
          </div>
        </div>
        {riskFactors.length ? (
          <div className="contractor-risk-factors">
            {riskFactors.map((factor) => {
              const level = getRiskLevel(factor.score)
              return (
                <article className="contractor-risk-factor" key={factor.nombre}>
                  <div className="contractor-risk-factor-heading">
                    <h5>{factor.nombre}</h5>
                    <div className="contractor-risk-factor-meta">
                      <span className={`contractor-risk-factor-level risk-${getRiskLevelClass(factor.score)}`}>{level}</span>
                      <strong>{Math.round(factor.score)}%</strong>
                    </div>
                  </div>
                  <div
                    aria-label={`${factor.nombre}: riesgo ${level}, ${Math.round(factor.score)} por ciento`}
                    aria-valuemax="100"
                    aria-valuemin="0"
                    aria-valuenow={Math.round(factor.score)}
                    className="contractor-risk-track"
                    role="progressbar"
                  >
                    <span className={`risk-fill-${getRiskLevelClass(factor.score)}`} style={{ width: `${Math.max(0, Math.min(100, factor.score))}%` }} />
                  </div>
                  <p>{getRiskFactorSentence(factor)}</p>
                </article>
              )
            })}
          </div>
        ) : <p className="contractor-risk-empty">No hay factores con datos para comparar.</p>}
        <p className="contractor-risk-chart-legend">
          <span><i className="risk-legend-low" /> Bajo (0–33%)</span>
          <span><i className="risk-legend-medium" /> Medio (34–66%)</span>
          <span><i className="risk-legend-high" /> Alto (67–100%)</span>
        </p>
      </section>}

      {showFactors && <section aria-labelledby="contractor-risk-actions-heading" className="contractor-risk-actions">
        <h4 id="contractor-risk-actions-heading">Qué puedes hacer</h4>
        {riskActions.length ? (
          <ul>{riskActions.map((action) => <li key={action}><span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span>{action}</li>)}</ul>
        ) : <p>No hay acciones urgentes.</p>}
      </section>}

      {showDelivery && <section className="contractor-risk-delivery">
        <h4>Entrega pactada vs. proyectada</h4>
        <div><span>Pactada: <strong>{formatDate(fechaEntrega)}</strong></span><span>Proyectada: <strong>{formatDate(risk?.entregaProyectada)}</strong></span></div>
        <p>{risk?.diasRetrasoProyectado == null
          ? 'No hay datos suficientes para estimar una fecha proyectada.'
          : `${risk.diasRetrasoProyectado > 0 ? `${risk.diasRetrasoProyectado} días de retraso` : `${Math.abs(risk.diasRetrasoProyectado)} días antes de la fecha pactada`}.`}</p>
      </section>}

      {showWeather && <section aria-label="Pronóstico de lluvia" className="contractor-risk-chart">
        <div className="contractor-risk-chart-heading">
          <div><h4>Pronóstico de lluvia · 14 días</h4><p>Fase actual: {getRiskPhaseLabel(indicators.faseActual)}. La lluvia puede afectar las próximas tareas.</p></div>
        </div>
        {forecast?.disponible && forecast.dias?.length ? (
          <ResponsiveContainer height={compact ? 150 : 200} width="100%">
            <BarChart data={forecast.dias} margin={{ top: 8, right: 8, bottom: 0, left: -24 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis axisLine={false} dataKey="fecha" tick={{ fill: 'var(--ot-muted)', fontSize: 10 }} tickFormatter={(value) => value.slice(5)} tickLine={false} />
              <YAxis axisLine={false} domain={[0, 100]} tick={{ fill: 'var(--ot-muted)', fontSize: 10 }} tickLine={false} />
              <Tooltip contentStyle={{ border: '1px solid var(--ot-line)', borderRadius: 10, background: 'var(--ot-panel)' }} formatter={(value) => [`${value}%`, 'Probabilidad de lluvia']} itemStyle={{ color: 'var(--ot-text)' }} labelFormatter={formatDate} labelStyle={{ color: 'var(--ot-text)' }} />
              <Bar dataKey="probabilidadMaxima" name="Probabilidad de lluvia" radius={[4, 4, 0, 0]}>
                {forecast.dias.map((day) => <Cell fill={day.probabilidadMaxima >= 50 ? '#d9a321' : '#5b9bd5'} key={day.fecha} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        ) : <p className="contractor-risk-empty">No hay pronóstico disponible para la ubicación del proyecto.</p>}
      </section>}

      {showTeams && <section aria-label="Avance por equipo y fase" className="contractor-risk-chart">
        <h4>Avance por equipo y fase</h4>
        {teams.length ? (
          <ResponsiveContainer height={Math.max(130, teams.length * 42)} width="100%">
            <BarChart data={teams.map((team) => ({ ...team, etiqueta: `${team.fase.replaceAll('_', ' ')} · ${team.nombre || team.actividad || 'Equipo'}` }))} layout="vertical" margin={{ top: 4, right: 16, bottom: 0, left: 8 }}>
              <CartesianGrid strokeDasharray="3 3" horizontal={false} />
              <XAxis axisLine={false} domain={[0, 100]} tick={{ fill: 'var(--ot-muted)', fontSize: 10 }} tickLine={false} type="number" />
              <YAxis axisLine={false} dataKey="etiqueta" tick={{ fill: 'var(--ot-text)', fontSize: 10 }} tickLine={false} type="category" width={145} />
              <Tooltip contentStyle={{ border: '1px solid var(--ot-line)', borderRadius: 10, background: 'var(--ot-panel)' }} formatter={(value) => [`${value}%`, 'Avance']} itemStyle={{ color: 'var(--ot-text)' }} labelStyle={{ color: 'var(--ot-text)' }} />
              <Bar dataKey="porcentajeAvance" name="Avance" radius={[0, 4, 4, 0]}>
                {teams.map((team) => <Cell fill={team.fase === 'PLANEACION' ? '#24935b' : team.fase === 'DESARROLLO' ? '#d9a321' : '#d84a4a'} key={team.id} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        ) : <p className="contractor-risk-empty">Aún no hay equipos con avance registrado.</p>}
      </section>}
      {showDelivery && <footer className="contractor-risk-footnote">
          <p>Estimación basada en cronograma, equipos, clima y pagos. No es una predicción exacta.</p>
          <p>Datos disponibles: {risk?.factoresConDatos ?? riskFactors.length} de {risk?.totalFactores ?? 4} factores.</p>
        </footer>}
    </section>
  )
}
