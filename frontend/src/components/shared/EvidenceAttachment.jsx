import { useEffect, useState } from 'react'
import api from '../../config/api.js'

export function EvidenceAttachment({ evidence }) {
  const [objectUrl, setObjectUrl] = useState('')
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let active = true
    let createdUrl = ''
    api.get(evidence.url, { responseType: 'blob' })
      .then(({ data }) => {
        createdUrl = URL.createObjectURL(data)
        if (active) setObjectUrl(createdUrl)
        else URL.revokeObjectURL(createdUrl)
      })
      .catch(() => { if (active) setFailed(true) })
    return () => {
      active = false
      if (createdUrl) URL.revokeObjectURL(createdUrl)
    }
  }, [evidence.url])

  if (failed) return <span className="evidence-attachment-error">No se pudo cargar {evidence.filename}</span>
  if (!objectUrl) return <span className="evidence-attachment-loading">Cargando {evidence.filename}…</span>

  if (evidence.contentType?.startsWith('image/')) {
    return <a className="evidence-image-link" href={objectUrl} rel="noreferrer" target="_blank" title={`Abrir ${evidence.filename}`}>
      <img alt={evidence.filename} loading="lazy" src={objectUrl} />
      <span>{evidence.filename}</span>
    </a>
  }

  return <a className="evidence-file-link" download={evidence.filename} href={objectUrl} rel="noreferrer" target="_blank">
    <span className="material-symbols-outlined" aria-hidden="true">picture_as_pdf</span>{evidence.filename}
  </a>
}