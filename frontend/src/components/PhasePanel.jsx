import './phase-panel.css'

const phaseDefinitions = [
  { id: 'PLANEACION', label: 'Fase 1 · Planeación', weight: 30 },
  { id: 'DESARROLLO', label: 'Fase 2 · Desarrollo', weight: 50 },
  { id: 'DETALLE_Y_ENTREGA_FINAL', label: 'Fase 3 · Detalles y entrega', weight: 20 },
]

const currency = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
})

const phaseLabel = (phase) => phaseDefinitions.find((item) => item.id === phase)?.label || phase

function Requirement({ complete, label }) {
  return (
    <li className={complete ? 'phase-requirement is-complete' : 'phase-requirement'}>
      <span className="material-symbols-outlined" aria-hidden="true">{complete ? 'check_circle' : 'radio_button_unchecked'}</span>
      <span>{label}</span>
    </li>
  )
}

export function PhasePanel({ phaseStatus, canAdvance = false, onAdvance, advancing = false }) {
  if (!phaseStatus) return null

  const phaseRows = phaseDefinitions.map((phase) => {
    const progress = phaseStatus.avanceFases?.find((item) => item.fase === phase.id)
    const phaseComplete = phaseDefinitions.findIndex((item) => item.id === phase.id)
      < phaseDefinitions.findIndex((item) => item.id === phaseStatus.faseActual)
    const percentage = phaseComplete ? 100 : Math.max(0, Math.min(100, progress?.avance || 0))
    const plan = phase.id === 'PLANEACION'
      ? {
          amount: phaseStatus.planPagos?.montoFase1,
          weekly: phaseStatus.planPagos?.cuotaSemanalFase1,
          weeks: phaseStatus.planPagos?.semanasFase1,
        }
      : phase.id === 'DESARROLLO'
        ? {
            amount: phaseStatus.planPagos?.montoFase2,
            weekly: phaseStatus.planPagos?.cuotaSemanalFase2,
            weeks: phaseStatus.planPagos?.semanasFase2,
          }
        : { amount: phaseStatus.planPagos?.montoFase3 }
    return { ...phase, phaseComplete, percentage, teams: progress?.equipos || 0, plan }
  })

  const requirements = phaseStatus.requisitos || {
    tareasCompletadas: phaseStatus.tareasFaseCompletadas,
    evidenciasDisponibles: phaseStatus.totalEvidencias > 0,
    aprobacionCliente: phaseStatus.aprobacionClienteFase,
    cuotasPagadas: phaseStatus.cuotasSemanaPagadas >= phaseStatus.cuotasTotalesEsperadas,
    limpiezaProfunda: phaseStatus.limpiezaProfundaCompletada,
  }

  return (
    <section aria-labelledby="phase-panel-heading" className="phase-panel">
      <header className="phase-panel-header">
        <div>
          <p className="client-eyebrow">Seguimiento de obra</p>
          <h2 id="phase-panel-heading">{phaseLabel(phaseStatus.faseActual)}</h2>
          <p>Avance real calculado con el porcentaje de los equipos de cada fase.</p>
        </div>
        <strong className="phase-panel-overall">{Math.round(phaseStatus.avanceObra || 0)}%</strong>
      </header>

      <div aria-label="Avance segmentado: planeación 30%, desarrollo 50%, detalles y entrega 20%" className="phase-segment-list">
        {phaseRows.map((phase) => (
          <article className="phase-segment" key={phase.id}>
            <div className="phase-segment-heading">
              <strong>{phase.label}</strong>
              <span>{phase.weight}% · {Math.round(phase.percentage)}%</span>
            </div>
            <div className="phase-segment-track" role="progressbar" aria-valuemax="100" aria-valuemin="0" aria-valuenow={Math.round(phase.percentage)}>
              <span style={{ width: `${phase.percentage}%` }} />
            </div>
            <small>{phase.teams} equipos vinculados</small>
          </article>
        ))}
      </div>

      <div className="phase-panel-grid">
        <section className="phase-panel-card">
          <h3>Plan de pagos</h3>
          <ul className="phase-payment-list">
            {phaseRows.map((phase) => (
              <li key={phase.id}>
                <span>{phase.label}</span>
                <strong>{currency.format(phase.plan.amount || 0)}</strong>
                <small>{phase.id === 'DETALLE_Y_ENTREGA_FINAL'
                  ? 'Pago único'
                  : `${currency.format(phase.plan.weekly || 0)} por semana durante ${phase.plan.weeks || 0} semanas`}</small>
              </li>
            ))}
          </ul>
        </section>

        <section className="phase-panel-card">
          <h3>Requisitos para cerrar esta fase</h3>
          <ul className="phase-requirement-list">
            <Requirement complete={requirements.tareasCompletadas} label="Tareas de la fase completadas" />
            <Requirement complete={requirements.evidenciasDisponibles} label={`Evidencias registradas (${phaseStatus.totalEvidencias || 0})`} />
            <Requirement complete={requirements.aprobacionCliente} label="Aprobación del cliente" />
            <Requirement complete={requirements.cuotasPagadas} label={`Cuotas pagadas (${phaseStatus.cuotasSemanaPagadas || 0}/${phaseStatus.cuotasTotalesEsperadas || 0})`} />
            {phaseStatus.faseActual === 'DETALLE_Y_ENTREGA_FINAL' && (
              <Requirement complete={requirements.limpiezaProfunda} label="Limpieza profunda completada" />
            )}
          </ul>
          <p className="phase-evidence-hint">Las evidencias se adjuntan a los reportes de avance del equipo.</p>
        </section>
      </div>

      {phaseStatus.cuotasFase?.length > 0 && (
        <section className="phase-panel-card phase-installments">
          <h3>Cuotas registradas en esta fase</h3>
          <ul>
            {phaseStatus.cuotasFase.map((installment) => (
              <li key={installment.numero}>
                Cuota {installment.numero}: {currency.format(installment.monto)}
                <small>{installment.registradoEn ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' }).format(new Date(installment.registradoEn)) : 'Fecha no disponible'}</small>
              </li>
            ))}
          </ul>
        </section>
      )}

      {canAdvance && (
        <footer className="phase-panel-actions">
          {phaseStatus.estadoFaseActual === 'APROBADA'
            ? <p role="status">El proyecto completó sus fases.</p>
            : <>
              {!phaseStatus.puedeDesbloquear && <p role="status">Completa los requisitos pendientes antes de avanzar.</p>}
              <button className="button button-primary" disabled={advancing || !phaseStatus.puedeDesbloquear} onClick={onAdvance} type="button">
                <span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
                {advancing ? 'Avanzando…' : 'Avanzar a la siguiente fase'}
              </button>
            </>}
        </footer>
      )}
    </section>
  )
}

export function PhaseRequirementActions({
  phaseStatus,
  canManageTasks = false,
  canApprove = false,
  onUpdateTasks,
  onApprove,
  onRegisterInstallment,
  pending = false,
}) {
  if (!phaseStatus) return null
  return (
    <section aria-label="Actualizar requisitos de la fase" className="phase-requirement-actions">
      <h3>Gestión de requisitos</h3>
      {canManageTasks && (
        <div className="phase-action-group">
          <button className="button button-secondary" disabled={pending} onClick={() => onUpdateTasks(!phaseStatus.tareasFaseCompletadas, phaseStatus.limpiezaProfundaCompletada)} type="button">
            {phaseStatus.tareasFaseCompletadas ? 'Marcar tareas pendientes' : 'Marcar tareas completadas'}
          </button>
          {phaseStatus.faseActual === 'DETALLE_Y_ENTREGA_FINAL' && (
            <button className="button button-secondary" disabled={pending} onClick={() => onUpdateTasks(phaseStatus.tareasFaseCompletadas, !phaseStatus.limpiezaProfundaCompletada)} type="button">
              {phaseStatus.limpiezaProfundaCompletada ? 'Marcar limpieza pendiente' : 'Confirmar limpieza profunda'}
            </button>
          )}
        </div>
      )}
      {canApprove && (
        <div className="phase-action-group">
          <p>Registro manual de pagos: ObraTech no procesa ni verifica transacciones.</p>
          <button className="button button-secondary" disabled={pending} onClick={() => onApprove(!phaseStatus.aprobacionClienteFase)} type="button">
            {phaseStatus.aprobacionClienteFase ? 'Retirar aprobación' : 'Aprobar esta fase'}
          </button>
          {phaseStatus.puedeRegistrarCuota && (
            <button className="button button-secondary" disabled={pending} onClick={onRegisterInstallment} type="button">
              Confirmar cuota pagada ({phaseStatus.cuotasPagadasEnFase}/{phaseStatus.cuotasEsperadasEnFase})
            </button>
          )}
        </div>
      )}
      {!canManageTasks && !canApprove && (
        <p>El contratista registra tareas y evidencias; el cliente registra las cuotas y la aprobación.</p>
      )}
    </section>
  )
}
