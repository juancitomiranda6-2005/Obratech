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
import './project-risk-panel.css'

const formatDate = (date) => date
  ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' }).format(new Date(`${date}T00:00:00`))
  : 'Sin datos'

function riskClass(level) {
  return ({ Bajo: 'low', Medio: 'medium', Alto: 'high' })[level] || 'unknown'
}

export function ProjectRiskPanel({ indicators, fechaEntrega, compact = false }) {
  if (!indicators) {
    return <section className="contractor-risk-panel contractor-risk-empty"><p>Los indicadores de riesgo no están disponibles.</p></section>
  }
  const risk = indicators.riesgo
  const forecast = indicators.pronosticoLluvia
  const teams = indicators.avanceEquipos || []
  const factorColors = { Bajo: '#24935b', Medio: '#d9a321', Alto: '#d84a4a' }

  return (
    <section aria-label="Indicadores de riesgo del proyecto" className={`contractor-risk-panel ${compact ? 'is-compact' : ''}`}>
      <header className="contractor-risk-header">
        <div>
          <p className="client-eyebrow">Seguimiento preventivo</p>
          <h3>Riesgo de retraso</h3>
        </div>
        <strong className={`contractor-risk-badge risk-${riskClass(risk?.nivel)}`}>
          {risk?.nivel || 'Sin datos'}{risk?.score != null && ` · ${Math.round(risk.score)}/100`}
        </strong>
      </header>
      <p className="contractor-risk-disclaimer">Estimación basada en cronograma, equipos, clima y pagos. No es una predicción exacta.</p>
      <p className="contractor-risk-coverage">
        Datos disponibles: {risk?.factoresConDatos ?? 0} de {risk?.totalFactores ?? 0} factores.
        Los factores sin datos se excluyen del cálculo.
      </p>

      <div className="contractor-risk-factors">
        {(risk?.factores || []).map((factor) => (
          <div className="contractor-risk-factor" key={factor.nombre}>
            <div className="contractor-risk-factor-heading"><strong>{factor.nombre}</strong><span>{factor.disponible ? `${factor.aporte.toFixed(1)} / ${factor.peso}` : 'Sin datos'}</span></div>
            <div className="contractor-risk-track" aria-hidden="true">
              <span style={{
                width: `${factor.disponible ? Math.min(100, factor.score || 0) : 0}%`,
                backgroundColor: factorColors[risk?.nivel] || '#8b929b',
              }} />
            </div>
            <small>{factor.detalle || 'Sin datos.'}</small>
          </div>
        ))}
      </div>

      <section className="contractor-risk-delivery">
        <h4>Entrega pactada vs. proyectada</h4>
        <div><span>Pactada: <strong>{formatDate(fechaEntrega)}</strong></span><span>Proyectada: <strong>{formatDate(risk?.entregaProyectada)}</strong></span></div>
        <p>{risk?.diasRetrasoProyectado == null
          ? 'No hay datos suficientes para estimar una fecha proyectada.'
          : `${risk.diasRetrasoProyectado > 0 ? `${risk.diasRetrasoProyectado} días de retraso` : `${Math.abs(risk.diasRetrasoProyectado)} días antes de la fecha pactada`}.`}</p>
      </section>

      <section className="contractor-risk-chart">
        <div className="contractor-risk-chart-heading">
          <div><h4>Pronóstico de lluvia · 14 días</h4><p>Fase actual: {indicators.faseActual?.replaceAll('_', ' ') || 'Sin datos'} · exposición {Math.round((indicators.exposicionClimatica || 0) * 100)}%</p></div>
        </div>
        {forecast?.disponible && forecast.dias?.length ? (
          <ResponsiveContainer height={compact ? 150 : 200} width="100%">
            <BarChart data={forecast.dias} margin={{ top: 8, right: 8, bottom: 0, left: -24 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="fecha" tickFormatter={(value) => value.slice(5)} tick={{ fontSize: 10 }} />
              <YAxis domain={[0, 100]} tick={{ fontSize: 10 }} />
              <Tooltip formatter={(value) => [`${value}%`, 'Probabilidad de lluvia']} labelFormatter={formatDate} />
              <Bar dataKey="probabilidadMaxima" name="Probabilidad de lluvia" radius={[4, 4, 0, 0]}>
                {forecast.dias.map((day) => <Cell fill={day.probabilidadMaxima >= 50 ? '#d9a321' : '#5b9bd5'} key={day.fecha} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        ) : <p className="contractor-risk-empty">No hay pronóstico disponible para la ubicación del proyecto.</p>}
      </section>

      <section className="contractor-risk-chart">
        <h4>Avance por equipo y fase</h4>
        {teams.length ? (
          <ResponsiveContainer height={Math.max(130, teams.length * 42)} width="100%">
            <BarChart data={teams.map((team) => ({ ...team, etiqueta: `${team.fase.replaceAll('_', ' ')} · ${team.nombre || team.actividad || 'Equipo'}` }))} layout="vertical" margin={{ top: 4, right: 16, bottom: 0, left: 8 }}>
              <CartesianGrid strokeDasharray="3 3" horizontal={false} />
              <XAxis domain={[0, 100]} tick={{ fontSize: 10 }} type="number" />
              <YAxis dataKey="etiqueta" tick={{ fontSize: 10 }} type="category" width={145} />
              <Tooltip formatter={(value) => [`${value}%`, 'Avance']} />
              <Bar dataKey="porcentajeAvance" name="Avance" radius={[0, 4, 4, 0]}>
                {teams.map((team) => <Cell fill={team.fase === 'PLANEACION' ? '#24935b' : team.fase === 'DESARROLLO' ? '#d9a321' : '#d84a4a'} key={team.id} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        ) : <p className="contractor-risk-empty">Aún no hay equipos con avance registrado.</p>}
      </section>
    </section>
  )
}
