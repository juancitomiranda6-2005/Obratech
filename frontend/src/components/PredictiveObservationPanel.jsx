import { useEffect, useMemo, useState } from 'react'
import { clientProjectsService } from '../services/proyectoService.js'
import './predictive-observation-panel.css'

const emptyObservation = {
  avanceActual: '',
  diasSinActividad: 0,
  equiposActivos: 0,
  equiposTotales: 0,
  pagosPendientes: 0,
  aprobacionesPendientes: 0,
  probabilidadLluvia: 0,
  faseActual: 'PLANEACION',
}

export function PredictiveObservationPanel({ projectId, project }) {
  const [observations, setObservations] = useState([])
  const [form, setForm] = useState(emptyObservation)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const weatherProbability = useMemo(() => {
    const days = project?.indicadores?.pronosticoLluvia?.dias || []
    return days.length ? Math.max(...days.map((day) => Number(day.probabilidadMaxima) || 0)) : 0
  }, [project])

  useEffect(() => {
    let active = true
    clientProjectsService.listPredictionObservations(projectId)
      .then(({ data }) => { if (active) setObservations(data) })
      .catch(() => { if (active) setError('No se pudo cargar el historial predictivo.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [projectId])

  useEffect(() => {
    setForm((current) => ({
      ...current,
      avanceActual: project?.progresoProyecto ?? '',
      equiposTotales: project?.equipos?.length ?? 0,
      probabilidadLluvia: weatherProbability,
      faseActual: project?.faseActual || 'PLANEACION',
    }))
  }, [project, weatherProbability])

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      const { data } = await clientProjectsService.createPredictionObservation(projectId, {
        ...form,
        presupuesto: Number(project?.presupuesto || 0),
        avanceActual: Number(form.avanceActual),
        diasSinActividad: Number(form.diasSinActividad),
        equiposActivos: Number(form.equiposActivos),
        equiposTotales: Number(form.equiposTotales),
        pagosPendientes: Number(form.pagosPendientes),
        aprobacionesPendientes: Number(form.aprobacionesPendientes),
        probabilidadLluvia: Number(form.probabilidadLluvia),
      })
      setObservations((current) => [...current, data])
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo guardar la observación.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section aria-labelledby="predictive-observation-title" className="predictive-observation-panel project-detail-section">
      <div className="predictive-observation-heading">
        <div>
          <p className="client-eyebrow">Datos para el futuro modelo</p>
          <h3 id="predictive-observation-title">Observación predictiva</h3>
        </div>
        <strong>{observations.length} registros</strong>
      </div>
      <p className="predictive-observation-help">
        Guarda una fotografía semanal del proyecto. El resultado final se etiquetará cuando la obra termine.
      </p>
      {error && <p className="predictive-observation-error" role="alert">{error}</p>}
      <form className="predictive-observation-form" onSubmit={submit}>
        <label><span>Avance actual (%)</span><input min="0" max="100" name="avanceActual" onChange={updateField} required type="number" step="0.1" value={form.avanceActual} /></label>
        <label><span>Días sin actividad</span><input min="0" name="diasSinActividad" onChange={updateField} required type="number" value={form.diasSinActividad} /></label>
        <label><span>Equipos activos</span><input min="0" name="equiposActivos" onChange={updateField} required type="number" value={form.equiposActivos} /></label>
        <label><span>Equipos totales</span><input min="0" name="equiposTotales" onChange={updateField} required type="number" value={form.equiposTotales} /></label>
        <label><span>Pagos pendientes</span><input min="0" name="pagosPendientes" onChange={updateField} required type="number" value={form.pagosPendientes} /></label>
        <label><span>Aprobaciones pendientes</span><input min="0" name="aprobacionesPendientes" onChange={updateField} required type="number" value={form.aprobacionesPendientes} /></label>
        <label><span>Probabilidad de lluvia (%)</span><input min="0" max="100" name="probabilidadLluvia" onChange={updateField} required type="number" value={form.probabilidadLluvia} /></label>
        <label><span>Fase actual</span><select name="faseActual" onChange={updateField} value={form.faseActual}><option value="PLANEACION">Planeación</option><option value="DESARROLLO">Desarrollo</option><option value="DETALLE_Y_ENTREGA_FINAL">Entrega final</option></select></label>
        <button className="button button-primary predictive-observation-submit" disabled={saving} type="submit">{saving ? 'Guardando…' : 'Guardar observación'}</button>
      </form>
      <div className="predictive-observation-history">
        <h4>Historial capturado</h4>
        {loading ? <p>Cargando registros…</p> : observations.length === 0 ? <p>Aún no hay observaciones. Guarda la primera desde este formulario.</p> : (
          <ul>
            {observations.slice(-6).reverse().map((observation) => (
              <li key={observation.id || observation.observadoEn}>
                <strong>{new Date(observation.observadoEn).toLocaleDateString('es-CO')}</strong>
                <span>Avance {observation.avanceActual}% · {observation.diasSinActividad} días inactivo · lluvia {observation.probabilidadLluvia}%</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}
