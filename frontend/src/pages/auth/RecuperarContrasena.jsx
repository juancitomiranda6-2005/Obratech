import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { authService } from '../../services/authService.js'

export function RecuperarContrasenaPage({ onComplete, resetting = false }) {
  const location = useLocation()
  const token = new URLSearchParams(location.search).get('token') || ''
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [complete, setComplete] = useState(false)

  const submitRequest = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await authService.requestPasswordReset(email.trim())
      setComplete(true)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo procesar la solicitud. Intenta de nuevo.')
    } finally {
      setLoading(false)
    }
  }

  const submitPassword = async (event) => {
    event.preventDefault()
    setError('')
    if (!token) {
      setError('El enlace no contiene un token válido. Solicita uno nuevo.')
      return
    }
    if (password !== confirmation) {
      setError('Las contraseñas no coinciden.')
      return
    }
    setLoading(true)
    try {
      await authService.resetPassword(token, password)
      onComplete?.()
      setComplete(true)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo cambiar la contraseña. Solicita un enlace nuevo.')
    } finally {
      setLoading(false)
    }
  }

  const title = resetting ? 'Crea una contraseña nueva' : 'Recupera tu contraseña'

  return (
    <main className="auth-page">
      <section className="auth-shell auth-result">
        <Link className="brand-mark" to="/" aria-label="ObraTech">
          <span className="material-symbols-outlined" aria-hidden="true">construction</span>
        </Link>
        <p className="eyebrow">ObraTech</p>
        <h1>{complete ? 'Solicitud completada' : title}</h1>

        {error && <div className="form-alert" role="alert">{error}</div>}

        {complete ? (
          <>
            <p className="auth-copy">
              {resetting
                ? 'Tu contraseña fue actualizada. Ya puedes iniciar sesión.'
                : 'Si la cuenta existe y el correo está configurado, recibirás instrucciones para continuar.'}
            </p>
            <Link className="button button-primary" to="/login">Volver al inicio de sesión</Link>
          </>
        ) : resetting ? (
          <form className="auth-card" onSubmit={submitPassword}>
            <label className="field-label" htmlFor="new-password">Nueva contraseña</label>
            <div className="input-wrap">
              <span className="material-symbols-outlined" aria-hidden="true">lock</span>
              <input autoComplete="new-password" id="new-password" minLength={8} onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
            </div>
            <label className="field-label" htmlFor="confirm-password">Confirmar contraseña</label>
            <div className="input-wrap">
              <span className="material-symbols-outlined" aria-hidden="true">lock_reset</span>
              <input autoComplete="new-password" id="confirm-password" minLength={8} onChange={(event) => setConfirmation(event.target.value)} required type="password" value={confirmation} />
            </div>
            <button className="button button-primary" disabled={loading} type="submit">{loading ? 'Actualizando…' : 'Guardar contraseña'}</button>
          </form>
        ) : (
          <form className="auth-card" onSubmit={submitRequest}>
            <p className="auth-copy">Escribe el correo asociado a tu cuenta. Si está registrada, te enviaremos un enlace temporal.</p>
            <label className="field-label" htmlFor="recovery-email">Correo electrónico</label>
            <div className="input-wrap">
              <span className="material-symbols-outlined" aria-hidden="true">mail</span>
              <input autoComplete="email" id="recovery-email" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
            </div>
            <button className="button button-primary" disabled={loading} type="submit">{loading ? 'Enviando…' : 'Enviar instrucciones'}</button>
          </form>
        )}

        {!complete && <Link className="register-prompt" to="/login">Volver al inicio de sesión</Link>}
      </section>
    </main>
  )
}