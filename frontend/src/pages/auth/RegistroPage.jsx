import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authService } from '../../services/authService.js'

const roles = [
  { value: 'contratista', label: 'Contratista', description: 'Gestiono equipos y proyectos', icon: 'engineering' },
  { value: 'cliente', label: 'Cliente', description: 'Publico y contrato proyectos', icon: 'person' },
  { value: 'trabajador', label: 'Trabajador', description: 'Busco oportunidades laborales', icon: 'construction' },
]

function passwordStrength(password) {
  if (!password) return { label: '', level: 0 }
  let level = 1
  if (password.length >= 8) level++
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) level++
  if (/\d|[^A-Za-z0-9]/.test(password)) level++
  return { label: ['Débil', 'Básica', 'Media', 'Buena', 'Fuerte'][level - 1], level }
}

export function RegistroPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [selectedRoles, setSelectedRoles] = useState([])
  const [acceptedTerms, setAcceptedTerms] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const strength = passwordStrength(password)

  const toggleRole = (role) => {
    setSelectedRoles((current) => current.includes(role)
      ? current.filter((selected) => selected !== role)
      : [...current, role])
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    if (!selectedRoles.length) {
      setError('Debes seleccionar al menos un tipo de usuario.')
      return
    }
    if (!acceptedTerms) {
      setError('Debes aceptar los Términos y Condiciones para continuar.')
      return
    }

    setSubmitting(true)
    try {
      await authService.register({ username: email.trim(), password, roles: selectedRoles, acceptedTerms })
      navigate('/login?registered=true')
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo crear la cuenta. Intenta de nuevo.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-shell register-shell">
        <Link className="brand-mark" to="/" aria-label="ObraTech">
          <span className="material-symbols-outlined" aria-hidden="true">construction</span>
        </Link>
        <header className="auth-heading">
          <h1>Crea tu cuenta</h1>
          <p>Únete a ObraTech y conecta proyectos con personas</p>
        </header>

        {error && <div className="form-alert" role="alert">{error}</div>}

        <form className="auth-card register-card" onSubmit={handleSubmit}>
          <label className="field-label" htmlFor="register-email">Correo electrónico</label>
          <div className="input-wrap">
            <span className="material-symbols-outlined" aria-hidden="true">mail</span>
            <input autoComplete="email" id="register-email" onChange={(event) => setEmail(event.target.value)} placeholder="ejemplo@email.com" required type="email" value={email} />
          </div>

          <label className="field-label" htmlFor="register-password">Contraseña</label>
          <div className="input-wrap">
            <span className="material-symbols-outlined" aria-hidden="true">lock</span>
            <input autoComplete="new-password" id="register-password" minLength={8} onChange={(event) => setPassword(event.target.value)} placeholder="Mínimo 8 caracteres" required type={showPassword ? 'text' : 'password'} value={password} />
            <button aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'} className="visibility-button" onClick={() => setShowPassword((visible) => !visible)} type="button">
              <span className="material-symbols-outlined" aria-hidden="true">{showPassword ? 'visibility_off' : 'visibility'}</span>
            </button>
          </div>
          <div aria-label={`Fortaleza de contraseña: ${strength.label || 'sin contraseña'}`} className="password-strength">
            <span className={`strength-level strength-${strength.level}`} />
            <span className="strength-label">{strength.label}</span>
          </div>

          <fieldset className="register-role-fieldset">
            <legend className="field-label">Soy un... (puedes seleccionar varios)</legend>
            <p aria-live="polite" className="role-selection-status">
              {selectedRoles.length ? `${selectedRoles.length} perfil${selectedRoles.length > 1 ? 'es' : ''} seleccionado${selectedRoles.length > 1 ? 's' : ''}` : 'Selecciona al menos un perfil'}
            </p>
            <div className="register-role-list">
              {roles.map((role) => {
                const checked = selectedRoles.includes(role.value)
                return (
                  <label className={`register-role-option ${checked ? 'is-selected' : ''}`} key={role.value}>
                    <input checked={checked} onChange={() => toggleRole(role.value)} type="checkbox" value={role.value} />
                    <span className="material-symbols-outlined role-option-icon" aria-hidden="true">{role.icon}</span>
                    <span className="role-option-copy"><strong>{role.label}</strong><small>{role.description}</small></span>
                    <span className="material-symbols-outlined role-option-check" aria-hidden="true">check</span>
                  </label>
                )
              })}
            </div>
          </fieldset>

          <label className="register-terms">
            <input checked={acceptedTerms} onChange={(event) => setAcceptedTerms(event.target.checked)} type="checkbox" />
            <span>Acepto los <a href="#terminos">Términos y Condiciones</a> y la <a href="#privacidad">Política de Privacidad</a>.</span>
          </label>

          <button className="button button-primary register-submit" disabled={submitting} type="submit">
            {submitting ? 'Creando cuenta…' : 'Crear cuenta'}
          </button>
        </form>

        <p className="register-prompt">¿Ya tienes una cuenta? <Link to="/login">Inicia sesión</Link></p>
        <div className="divider"><span>o continuar con</span></div>
        <a className="google-button" href="/oauth2/authorization/google"><span className="google-mark" aria-hidden="true">G</span>Continuar con Google</a>
      </section>
    </main>
  )
}
