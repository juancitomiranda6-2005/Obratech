import { useEffect, useState } from 'react'
import { participantProjectsService } from '../services/participantProjectsService.js'
import './fit-simulation-panel.css'

const labels = [
  ['fitTecnico', 'Fit técnico'],
  ['fitPresupuesto', 'Fit presupuesto'],
  ['fitUbicacion', 'Fit ubicación'],
]

const modelExplanation = 'Se calcula con el historial de postulaciones anteriores, comparando tu compatibilidad, calificación, experiencia, cumplimiento de plazos, distancia y cuántos postulantes hay. La simulación de arriba solo compara tu compatibilidad con la de los demás postulantes.'

function percentage(value) {
  return Math.round(Math.max(0, Math.min(1, Number(value) || 0)) * 100)
}

function level(value) {
  const score = percentage(value)
  return score >= 70 ? 'high' : score >= 40 ? 'medium' : 'low'
}

export function FitSimulationPanel({ projectId, contractorId = '' }) {
  const [simulation, setSimulation] = useState(null)
  const [modelEstimate, setModelEstimate] = useState(null)
  const [modelUnavailable, setModelUnavailable] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    participantProjectsService.simulateFit(projectId, contractorId)
      .then(({ data }) => { if (active) setSimulation(data) })
      .catch((requestError) => {
        if (active) setError(requestError.response?.data?.error || 'No se pudo calcular tu compatibilidad.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [projectId, contractorId])

  useEffect(() => {
    let active = true
    setModelEstimate(null)
    setModelUnavailable(false)
    participantProjectsService.predictSelection(projectId)
      .then(({ data }) => {
        const reason = modelUnavailableReason(data)
        if (reason) {
          console.warn(`Estimación del modelo no disponible: ${reason}`)
        }
        if (active) {
          setModelEstimate(reason ? null : data)
          setModelUnavailable(Boolean(reason))
        }
      })
      .catch((requestError) => {
        const status = requestError.response?.status || 'sin respuesta'
        console.warn(`Estimación del modelo no disponible: estado HTTP ${status}`)
        if (active) {
          setModelEstimate(null)
          setModelUnavailable(true)
        }
      })
    return () => { active = false }
  }, [projectId])

  if (loading) {
    return <section aria-live="polite" className="fit-simulation-panel"><p>Calculando tu compatibilidad…</p></section>
  }
  if (error) {
    return <section aria-live="polite" className="fit-simulation-panel fit-simulation-error"><p>{error}</p></section>
  }
  if (!simulation) return null

  const compatibility = percentage(simulation.compatibilidadGeneral)
  const probability = percentage(simulation.probabilidadGanar)
  return (
    <section aria-labelledby="fit-simulation-title" className="fit-simulation-panel">
      <div className="fit-simulation-heading">
        <div>
          <p className="client-eyebrow">Simulación antes de postularte</p>
          <h3 id="fit-simulation-title">Tu compatibilidad con este proyecto</h3>
        </div>
        <strong className={`fit-simulation-score fit-${level(simulation.compatibilidadGeneral)}`}>{compatibility}%</strong>
      </div>
      <div className="fit-simulation-bars">
        {labels.map(([key, label]) => {
          const value = percentage(simulation[key])
          return (
            <div className="fit-simulation-metric" key={key}>
              <div><span>{label}</span><strong>{value}%</strong></div>
              <div aria-label={`${label}: ${value}%`} aria-valuemax="100" aria-valuemin="0" aria-valuenow={value} className="fit-simulation-track" role="progressbar">
                <span className={`fit-fill-${level(simulation[key])}`} style={{ width: `${value}%` }} />
              </div>
            </div>
          )
        })}
      </div>
      <p className={`fit-simulation-probability fit-${level(simulation.probabilidadGanar)}`}>
        Probabilidad simulada de ganar: <strong>{probability}%</strong>
      </p>
      {modelEstimate && !modelEstimate.mock && modelPercentage(modelEstimate) !== null && (
        <div className="fit-model-estimate">
          <p className={`fit-simulation-probability fit-${modelLevel(modelEstimate.nivel, modelEstimate.porcentaje)}`}>
            <span className="fit-model-title">
              Probabilidad estimada por el modelo
              <button
                aria-describedby="fit-model-explanation"
                aria-label="Cómo se calcula la probabilidad del modelo"
                className="fit-model-info"
                title={modelExplanation}
                type="button"
              >
                i
              </button>
              <span className="fit-model-tooltip" id="fit-model-explanation" role="tooltip">
                {modelExplanation}
              </span>
            </span>
            <strong>{modelLevelLabel(modelEstimate.nivel, modelEstimate.porcentaje)} · {modelPercentage(modelEstimate)}%</strong>
          </p>
          <h4>Qué influye más en tu resultado</h4>
          {Array.isArray(modelEstimate.factores) && modelEstimate.factores.length > 0 && (
            <ul className="fit-model-factors">
              {modelEstimate.factores.slice(0, 3).map((factor) => (
                <li key={factor.factor || factor.mensaje}>
                  <span className={`fit-factor-${factor.impacto || 'neutral'}`}>
                    {translateImpact(factor.impacto)}
                  </span>{' '}
                  {factor.mensaje}
                </li>
              ))}
            </ul>
          )}
          {modelEstimate.camposPorDefecto?.length > 0 && (
            <p className="fit-model-defaults">
              Estimación aproximada: no tenemos datos de {formatDefaultFields(modelEstimate.camposPorDefecto)}.
            </p>
          )}
        </div>
      )}
      {modelUnavailable && (
        <small className="fit-model-defaults">Estimación del modelo no disponible por ahora</small>
      )}
      <p className="fit-simulation-context">
        {simulation.postulantesActivos} postulantes activos · {simulation.competidoresMasFuertes} con mayor compatibilidad.
      </p>
      <small>Modelo en fase de prueba con datos de demostración. Es una estimación informativa; no garantiza la adjudicación.</small>
    </section>
  )
}

export function modelPercentage(estimate) {
  const value = Number(estimate?.porcentaje)
  if (!Number.isFinite(value)) return null
  return Math.round(Math.max(0, Math.min(100, value)))
}

export function modelUnavailableReason(estimate) {
  if (!estimate || estimate.mock === true) return estimate?.mock === true ? 'mock: true' : 'respuesta ausente'
  if (modelPercentage(estimate) === null) return 'porcentaje ausente o inválido'
  return ''
}

function modelLevel(value, percentageValue) {
  if (value === 'alto' || value === 'high') return 'high'
  if (value === 'medio' || value === 'medium') return 'medium'
  if (value === 'bajo' || value === 'low') return 'low'
  const score = Number(percentageValue) || 0
  return score >= 70 ? 'high' : score >= 40 ? 'medium' : 'low'
}

function modelLevelLabel(value, percentageValue) {
  return `Probabilidad ${
    { high: 'alta', medium: 'media', low: 'baja' }[modelLevel(value, percentageValue)]
  }`
}

function translateLevel(value) {
  return { high: 'alto', medium: 'medio', low: 'bajo', alto: 'alto', medio: 'medio', bajo: 'bajo' }[value] || value
}

function translateImpact(value) {
  return { favorable: 'A favor', desfavorable: 'En contra', neutral: 'Neutral' }[value] || 'Neutral'
}

export function formatDefaultFields(fields) {
  const labelsByField = {
    distancia_km: 'distancia a la obra',
    pct_obras_a_tiempo: 'obras entregadas a tiempo',
    experiencia_anios: 'experiencia',
    obras_completadas: 'obras completadas',
    num_postulantes: 'cantidad de postulantes',
    ratio_precio_ofertado: 'precio ofertado',
    calificacion: 'calificación',
    compatibilidad: 'compatibilidad',
  }
  const labels = fields.map((field) => labelsByField[field] || field)
  if (labels.length < 2) return labels[0] || 'algunos datos'
  if (labels.length === 2) return `${labels[0]} y ${labels[1]}`
  return `${labels.slice(0, -1).join(', ')} y ${labels.at(-1)}`
}
