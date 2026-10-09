import { useState } from 'react'
import { Link } from 'react-router-dom'
import { authService } from '../../services/authService.js'

const availableRoles = [
  { value: 'cliente', label: 'Cliente', icon: 'person', description: 'Publico y contrato proyectos' },
  { value: 'contratista', label: 'Contratista', icon: 'engineering', description: 'Gestiono proyectos y equipos' },
  { value: 'trabajador', label: 'Trabajador', icon: 'construction', description: 'Busco oportunidades laborales' },
]

export function CompletarRegistroPage({ onComplete }) {
  const [roles, setRoles] = useState([])
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [acceptedTerms, setAcceptedTerms] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const toggleRole = (role) => setRoles((current) => current.includes(role)
    ? current.filter((item) => item !== role)
    : [...current, role])

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    if (!roles.length) return setError('Selecciona al menos un perfil.')
    if (password.length < 8) return setError('La contraseña debe tener al menos 8 caracteres.')
    if (password !== confirmPassword) return setError('Las contraseñas no coinciden.')
    if (!acceptedTerms) return setError('Debes aceptar los términos y condiciones.')

    setSaving(true)
    try {
      const { data } = await authService.completeRegistration({ roles, password, acceptedTerms })
      onComplete(data)
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo completar el registro.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-shell register-shell">
        <Link className="brand-mark" to="/" aria-label="ObraTech"><span className="material-symbols-outlined" aria-hidden="true">construction</span></Link>
        <header className="auth-heading"><p className="client-eyebrow">Finalizar registro</p><h1>Completa tu cuenta</h1><p>Elige cómo usarás ObraTech y crea una contraseña.</p></header>
        {error && <div className="form-alert" role="alert">{error}</div>}
        <form className="auth-card register-card" onSubmit={submit}>
          <fieldset className="register-role-fieldset"><legend className="field-label">Selecciona tus perfiles</legend><div className="register-role-list">{availableRoles.map((role) => {
            const selected = roles.includes(role.value)
            return <label className={`register-role-option ${selected ? 'is-selected' : ''}`} key={role.value}><input checked={selected} onChange={() => toggleRole(role.value)} type="checkbox" /><span className="material-symbols-outlined role-option-icon" aria-hidden="true">{role.icon}</span><span className="role-option-copy"><strong>{role.label}</strong><small>{role.description}</small></span><span className="material-symbols-outlined role-option-check" aria-hidden="true">check</span></label>
          })}</div></fieldset>
          <label className="field-label" htmlFor="oauth-password">Contraseña</label>
          <div className="input-wrap"><span className="material-symbols-outlined" aria-hidden="true">lock</span><input autoComplete="new-password" id="oauth-password" minLength={8} onChange={(event) => setPassword(event.target.value)} required type="password" value={password} /></div>
          <label className="field-label" htmlFor="oauth-confirm-password">Confirmar contraseña</label>
          <div className="input-wrap"><span className="material-symbols-outlined" aria-hidden="true">lock_reset</span><input autoComplete="new-password" id="oauth-confirm-password" onChange={(event) => setConfirmPassword(event.target.value)} required type="password" value={confirmPassword} /></div>
          <label className="register-terms"><input checked={acceptedTerms} onChange={(event) => setAcceptedTerms(event.target.checked)} type="checkbox" /><span>Acepto los términos y condiciones y la política de privacidad.</span></label>
          <button className="button button-primary register-submit" disabled={saving} type="submit">{saving ? 'Guardando…' : 'Completar registro'}</button>
        </form>
      </section>
    </main>
  )
}
