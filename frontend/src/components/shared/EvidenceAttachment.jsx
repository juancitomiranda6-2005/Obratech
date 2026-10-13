import { useEffect, useState } from 'react'
import api from '../../config/api.js'

export function EvidenceAttachment({ evidence }) {
  const [objectUrl, setObjectUrl] = useState('')
  const [failed, setFailed] = useState(false)
  const [loading, setLoading] = useState(false)
  const [previewOpen, setPreviewOpen] = useState(false)

  useEffect(() => {
    return () => { if (objectUrl) URL.revokeObjectURL(objectUrl) }
  }, [objectUrl])

  if (failed) return <span className="evidence-attachment-error">No se pudo cargar {evidence.filename}</span>

  const openPreview = async () => {
    if (objectUrl) {
      setPreviewOpen((current) => !current)
      return
    }
    setLoading(true)
    try {
      const { data } = await api.get(evidence.url, { responseType: 'blob' })
      setObjectUrl(URL.createObjectURL(data))
      setPreviewOpen(true)
    } catch {
      setFailed(true)
    } finally {
      setLoading(false)
    }
  }

  return <div className="evidence-attachment">
    <div className="evidence-attachment-actions">
      <button aria-expanded={previewOpen} className="evidence-file-link" disabled={loading} onClick={openPreview} type="button">
        <span className="material-symbols-outlined" aria-hidden="true">{loading ? 'hourglass_top' : previewOpen ? 'visibility_off' : 'visibility'}</span>
        {loading ? 'Cargando vista previa…' : previewOpen ? 'Cerrar vista previa' : `Ver ${evidence.filename}`}
      </button>
      {objectUrl && <a className="evidence-download-link" download={evidence.filename} href={objectUrl}><span className="material-symbols-outlined" aria-hidden="true">download</span>Descargar</a>}
    </div>
    {previewOpen && objectUrl && <div className="evidence-preview">
      {evidence.contentType?.startsWith('image/')
        ? <img alt={evidence.filename} src={objectUrl} />
        : evidence.contentType === 'application/pdf'
          ? <iframe title={`Vista previa: ${evidence.filename}`} src={objectUrl} />
          : <p>Este formato no tiene vista previa en el navegador. Puedes descargarlo si lo necesitas.</p>}
    </div>}
  </div>
}