export const RISK_THRESHOLDS = Object.freeze({
  low: 33,
  medium: 66,
})

const formatPercent = (value) => `${new Intl.NumberFormat('es-CO', {
  maximumFractionDigits: 1,
}).format(value)}%`

function parseNumber(value) {
  const parsed = Number(value.replace(',', '.'))
  return Number.isFinite(parsed) ? parsed : null
}

function parseCountPair(detail, expression) {
  const match = detail?.match(expression)
  return match ? { current: Number(match[1]), total: Number(match[2]) } : null
}

function plural(count, singular, pluralForm = /[aeiouáéíóú]$/i.test(singular) ? `${singular}s` : `${singular}es`) {
  return count === 1 ? singular : pluralForm
}

export function getRiskLevel(score) {
  if (score == null || !Number.isFinite(Number(score))) return 'Sin datos'
  if (score <= RISK_THRESHOLDS.low) return 'Bajo'
  if (score <= RISK_THRESHOLDS.medium) return 'Medio'
  return 'Alto'
}

export function getRiskLevelClass(score) {
  const level = getRiskLevel(score)
  return ({
    Bajo: 'low',
    Medio: 'medium',
    Alto: 'high',
  })[level] || 'unknown'
}

export function getRiskPhaseLabel(phase) {
  return ({
    PLANEACION: 'Planeación',
    DESARROLLO: 'Desarrollo',
    DETALLE_Y_ENTREGA_FINAL: 'Detalles y entrega',
  })[phase] || 'Sin definir'
}

export function getRiskFactorSentence(factor) {
  const detail = factor?.detalle || ''

  if (factor?.nombre === 'Cronograma') {
    const match = detail.match(/Avance esperado ([\d.,]+)% frente a ([\d.,]+)% real/i)
    if (match) {
      const expected = parseNumber(match[1])
      const actual = parseNumber(match[2])
      if (expected != null && actual != null) {
        return actual < expected
          ? `Vas atrasado: llevas ${formatPercent(actual)} de avance y deberías ir en ${formatPercent(expected)}.`
          : `Vas al día: llevas ${formatPercent(actual)} de avance y deberías ir en ${formatPercent(expected)}.`
      }
    }
  }

  if (factor?.nombre === 'Equipos') {
    const inactive = parseCountPair(detail, /Equipos sin actividad conocida en 7 días:\s*(\d+)\s*\/\s*(\d+)/i)
    const withoutTeam = parseCountPair(detail, /trabajadores sin equipo:\s*(\d+)\s*\/\s*(\d+)/i)
    const unassignedActivity = detail.match(/actividades sin asignar:\s*(\d+)\s*\/\s*(\d+)/i)
    const noCrews = detail.match(/No hay cuadrillas registradas;\s*(\d+)\s*trabajadores no tienen equipo/i)
    const sentences = []

    if (inactive?.current > 0) {
      sentences.push(inactive.current === 1 && inactive.total === 1
        ? 'Tu único equipo lleva más de 7 días sin actividad.'
        : `${inactive.current} de ${inactive.total} equipos llevan más de 7 días sin actividad.`)
    } else if (inactive?.total > 0) {
      sentences.push('Tus equipos han tenido actividad en los últimos 7 días.')
    }

    const unassignedCount = withoutTeam?.current ?? (noCrews ? Number(noCrews[1]) : 0)
    const workerTotal = withoutTeam?.total
    if (unassignedCount > 0) {
      sentences.push(workerTotal == null
        ? `${unassignedCount} ${plural(unassignedCount, 'trabajador')} ${unassignedCount === 1 ? 'no tiene' : 'no tienen'} equipo asignado.`
        : `${unassignedCount} de ${workerTotal} ${plural(workerTotal, 'trabajador')} ${unassignedCount === 1 ? 'no tiene' : 'no tienen'} equipo asignado.`)
    }
    const missingActivities = unassignedActivity ? Number(unassignedActivity[1]) : 0
    if (missingActivities > 0) {
      sentences.push(`Falta asignar actividad a ${missingActivities} ${plural(missingActivities, 'equipo')}.`)
    }

    if (!sentences.length && /sin datos/i.test(detail)) {
      return 'Aún no hay registros suficientes para revisar la actividad de los equipos.'
    }
    return sentences.join(' ') || 'No se detectan problemas de actividad en los equipos.'
  }

  if (factor?.nombre === 'Clima') {
    const match = detail.match(/(\d+)\s+de\s+(\d+)\s+días?\s+con probabilidad de lluvia\s*≥\s*([\d.,]+)%/i)
    if (match) {
      const rainyDays = Number(match[1])
      const days = Number(match[2])
      const threshold = parseNumber(match[3])
      if (threshold != null) {
        return `Se espera lluvia (≥${formatPercent(threshold)} de probabilidad) en ${rainyDays} de los próximos ${days} días.`
      }
    }
  }

  if (factor?.nombre === 'Pagos y aprobaciones') {
    const overdue = detail.match(/Cuotas vencidas sin registrar:\s*(\d+)/i)
    const approvalPending = /aprobación pendiente/i.test(detail)
    const evidencePending = /evidencias pendientes/i.test(detail)
    const count = overdue ? Number(overdue[1]) : 0
    const sentences = []

    if (count > 0) {
      sentences.push(`Tienes ${count} ${plural(count, 'cuota')} vencida${count === 1 ? '' : 's'} sin registrar.`)
    }
    if (approvalPending) sentences.push('Hay una aprobación pendiente.')
    if (evidencePending) sentences.push('Faltan evidencias de la fase actual.')
    return sentences.join(' ') || 'No tienes pagos ni aprobaciones pendientes.'
  }

  if (factor?.nombre === 'Cronograma') return 'No hay información suficiente para comparar el avance con el plan.'
  if (factor?.nombre === 'Equipos') return 'No hay información suficiente para revisar la actividad de los equipos.'
  if (factor?.nombre === 'Clima') return 'No hay un pronóstico disponible para revisar las lluvias.'
  if (factor?.nombre === 'Pagos y aprobaciones') return 'No hay información suficiente sobre pagos y aprobaciones.'

  return detail || 'No hay información adicional disponible.'
}

export function getRiskSummary(risk) {
  const level = getRiskLevel(risk?.score)
  const availableFactors = (risk?.factores || [])
    .filter((factor) => factor.disponible && factor.score != null)
    .sort((first, second) => second.score - first.score)
  const mainFactor = availableFactors[0]

  if (level === 'Sin datos') return 'Aún no hay datos suficientes para estimar el riesgo de retraso.'
  if (!mainFactor) return `Tu proyecto tiene un riesgo ${level.toUpperCase()} de retrasarse.`

  return `Tu proyecto tiene un riesgo ${level.toUpperCase()} de retrasarse. Razón principal: ${getRiskFactorSentence(mainFactor)}`
}

export function getRiskActions(factors = []) {
  const availableFactors = factors.filter((factor) => (
    factor.disponible && factor.score != null && getRiskLevel(factor.score) !== 'Bajo'
  ))
  const actions = []

  availableFactors.forEach((factor, index) => {
    const detail = factor.detalle || ''
    const impact = Number(factor.aporte) || 0
    const priority = availableFactors.length - index
    const recommendations = []

    if (factor.nombre === 'Pagos y aprobaciones') {
      const overdueMatch = detail.match(/Cuotas vencidas sin registrar:\s*(\d+)/i)
      const overdue = overdueMatch ? Number(overdueMatch[1]) : 0
      if (overdue > 0) {
        recommendations.push(`registrar ${overdue} ${plural(overdue, 'cuota')} vencida${overdue === 1 ? '' : 's'}`)
      }
      if (/aprobación pendiente/i.test(detail)) {
        recommendations.push('resolver la aprobación pendiente')
      }
      if (/evidencias pendientes/i.test(detail)) {
        recommendations.push('adjuntar evidencias de la fase actual')
      }
      if (recommendations.length) {
        actions.push({ text: `${recommendations[0][0].toUpperCase()}${recommendations[0].slice(1)}${recommendations.length > 1 ? ` y ${recommendations.slice(1).join(' y ')}` : ''}`, impact, priority })
      }
    }

    if (factor.nombre === 'Equipos') {
      const match = detail.match(/trabajadores sin equipo:\s*(\d+)\s*\/\s*(\d+)/i)
      const unassignedActivity = detail.match(/actividades sin asignar:\s*(\d+)\s*\/\s*(\d+)/i)
      const noCrews = detail.match(/No hay cuadrillas registradas;\s*(\d+)\s*trabajadores no tienen equipo/i)
      const count = match ? Number(match[1]) : noCrews ? Number(noCrews[1]) : 0
      if (count > 0) {
        recommendations.push(`asignar equipo a ${count} ${plural(count, 'trabajador')} sin equipo`)
      }
      const missingActivities = unassignedActivity ? Number(unassignedActivity[1]) : 0
      if (missingActivities > 0) {
        recommendations.push(`definir la actividad de ${missingActivities} ${plural(missingActivities, 'equipo')}`)
      }
      const inactive = parseCountPair(detail, /Equipos sin actividad conocida en 7 días:\s*(\d+)\s*\/\s*(\d+)/i)
      if (inactive?.current > 0) {
        recommendations.push(`solicitar actualización de avance a ${inactive.current} ${plural(inactive.current, 'equipo')}`)
      }
      if (recommendations.length) {
        actions.push({ text: `${recommendations[0][0].toUpperCase()}${recommendations[0].slice(1)}${recommendations.length > 1 ? ` y ${recommendations.slice(1).join(' y ')}` : ''}`, impact, priority })
      }
    }

    if (factor.nombre === 'Cronograma' && getRiskLevel(factor.score) === 'Alto') {
      actions.push({ text: 'Replanificar el cronograma', impact, priority })
    }

    if (factor.nombre === 'Clima') {
      actions.push({ text: 'Considerar la lluvia al programar las próximas tareas', impact, priority })
    }
  })

  return actions
    .sort((first, second) => second.impact - first.impact || second.priority - first.priority)
    .slice(0, 4)
    .map(({ text }) => text)
}

export function getRiskWeightExplanation(factors = []) {
  const weights = factors.map((factor) => `${factor.nombre} ${factor.peso}%`)
  if (!weights.length) return 'El puntaje combina solo los factores que cuentan con datos.'
  return `El puntaje combina solo los factores con datos. Cada uno aporta según su peso: ${weights.join(', ')}.`
}
