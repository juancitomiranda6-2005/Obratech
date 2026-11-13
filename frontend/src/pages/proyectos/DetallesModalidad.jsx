const modalityFields = {
  'Construcción nueva': [
    { name: 'terrenoDisponible', label: '¿Tienes el terreno disponible?', type: 'select', options: ['Sí', 'No'] },
    { name: 'areaConstruir', label: 'Área aproximada a construir (m²)', type: 'number' },
    { name: 'numeroPisos', label: 'Número de pisos', type: 'number' },
  ],
  Remodelación: [
    { name: 'espaciosRemodelar', label: '¿Qué espacios deseas remodelar?', type: 'text' },
    { name: 'estadoActual', label: 'Estado actual del inmueble', type: 'select', options: ['Bueno', 'Regular', 'Malo'] },
    { name: 'viviendaOcupada', label: '¿Estará ocupada durante la obra?', type: 'select', options: ['Sí', 'No'] },
  ],
  Ampliación: [
    { name: 'areaAmpliar', label: 'Área aproximada a ampliar (m²)', type: 'number' },
    { name: 'tipoAmpliacion', label: 'Describa qué desea ampliar', type: 'text' },
    { name: 'viviendaOcupada', label: '¿Estará ocupada durante la obra?', type: 'select', options: ['Sí', 'No'] },
  ],
  Mantenimiento: [
    { name: 'trabajosMantenimiento', label: '¿Qué trabajos de mantenimiento necesita?', type: 'textarea' },
    { name: 'frecuencia', label: 'Frecuencia', type: 'select', options: ['Semanal', 'Mensual', 'Trimestral', 'Anual'] },
    { name: 'urgencia', label: '¿Es urgente?', type: 'select', options: ['Baja', 'Media', 'Alta'] },
  ],
  Reparación: [
    { name: 'elementoAfectado', label: '¿Qué elemento necesita reparación?', type: 'text' },
    { name: 'descripcionDano', label: 'Describa el daño', type: 'textarea' },
    { name: 'urgencia', label: '¿Es urgente?', type: 'select', options: ['Baja', 'Media', 'Alta'] },
  ],
  Demolición: [
    { name: 'tipoDemolicion', label: 'Tipo de demolición', type: 'select', options: ['Total', 'Parcial'] },
    { name: 'tipoEstructura', label: 'Tipo de estructura', type: 'text' },
    { name: 'areaDemoler', label: 'Área aproximada a demoler (m²)', type: 'number' },
    { name: 'retiroEscombros', label: '¿Necesita retiro de escombros?', type: 'select', options: ['Sí', 'No'] },
  ],
}

export function DetallesModalidad({ modalidadTrabajo, detallesModalidad, onChange }) {
  if (!modalidadTrabajo) return null

  const fields = modalityFields[modalidadTrabajo] || []

  return (
    <fieldset className="publish-field-full modality-details-fields">
      <legend>Detalles de {modalidadTrabajo.toLowerCase()}</legend>

      {fields.map((field) => (
        <label className="publish-field" key={field.name}>
          <span>{field.label}</span>

          {field.type === 'select' ? (
            <select
              name={field.name}
              onChange={onChange}
              required
              value={detallesModalidad[field.name] || ''}
            >
              <option value="">Selecciona una opción…</option>
              {field.options.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          ) : field.type === 'textarea' ? (
            <textarea
              name={field.name}
              onChange={onChange}
              required
              rows={3}
              value={detallesModalidad[field.name] || ''}
            />
          ) : (
            <input
              min={field.type === 'number' ? '0' : undefined}
              name={field.name}
              onChange={onChange}
              required
              type={field.type}
              value={detallesModalidad[field.name] || ''}
            />
          )}
        </label>
      ))}
    </fieldset>
  )
}
