import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { authService, establishSession } from '../../services/authService.js'
import { useAuth } from '../../context/AuthContext.jsx'

export function LoginPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated, login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true })
    }
  }, [isAuthenticated, navigate])

  const query = new URLSearchParams(location.search)
  const registered = query.get('registered') === 'true'
  const authStatus = query.has('blocked')
    ? { message: 'Tu cuenta está bloqueada temporalmente. Intenta de nuevo en 15 minutos.', tone: 'error' }
    : query.get('error') === 'google'
      ? { message: 'No se pudo iniciar sesión con Google. Verifica tu cuenta e inténtalo de nuevo.', tone: 'error' }
      : query.has('error')
        ? { message: 'Usuario o contraseña incorrectos. Verifica tus datos.', tone: 'error' }
        : query.has('expired')
          ? { message: 'Tu sesión expiró. Inicia sesión para continuar.', tone: 'error' }
          : query.has('unauthorized')
            ? { message: 'Debes iniciar sesión para acceder a esa sección.', tone: 'error' }
            : query.has('logout')
              ? { message: 'Sesión cerrada correctamente.', tone: 'success' }
              : null

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)

    try {
      const { data } = await authService.login({ username: email.trim(), password })
      establishSession(data, data.username, rememberMe, login, navigate)
      setPassword('')
    } catch (requestError) {
      setError(requestError.response?.data?.error || 'No se pudo conectar con el servidor. Intenta de nuevo.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-page auth-page-login">
      <aside className="auth-visual auth-visual-login">
        <img alt="Equipo de construcción trabajando en una obra" className="auth-visual-image" src="/images/obra-equipo.jpg" />
        <div className="auth-visual-content">
          <span className="auth-visual-kicker">Construimos en equipo</span>
          <h2>Tu próximo proyecto empieza con una buena conexión.</h2>
          <p>Encuentra personas, oportunidades y herramientas para llevar cada obra adelante.</p>
          <span className="auth-visual-note"><span className="material-symbols-outlined" aria-hidden="true">verified</span> Personas y proyectos en un solo lugar</span>
        </div>
      </aside>
      <section className="auth-shell">
        <Link className="brand-mark" to="/" aria-label="ObraTech">
          <img alt="ObraTech" src="/images/obratech-logo.png" />
        </Link>

        <header className="auth-heading">
          <h1>Bienvenido a ObraTech</h1>
          <p>Ingresa tus credenciales para continuar</p>
        </header>

        {error && <div className="form-alert" role="alert">{error}</div>}
        {!error && authStatus && (
          <div
            className={`form-alert ${authStatus.tone === 'success' ? 'form-alert-success' : ''}`}
            role={authStatus.tone === 'success' ? 'status' : 'alert'}
          >
            {authStatus.message}
          </div>
        )}
        {registered && (
          <div className="form-alert form-alert-success" role="status">
            Cuenta creada exitosamente. Ya puedes iniciar sesión.
          </div>
        )}

        <form className="auth-card" onSubmit={handleSubmit}>
          <label className="field-label" htmlFor="username">Correo electrónico</label>
          <div className="input-wrap">
            <span className="material-symbols-outlined" aria-hidden="true">mail</span>
            <input
              autoComplete="email"
              id="username"
              name="username"
              onChange={(event) => setEmail(event.target.value)}
              placeholder="ejemplo@email.com"
              required
              type="email"
              value={email}
            />
          </div>

          <div className="password-label-row">
            <label className="field-label" htmlFor="password">Contraseña</label>
            <Link className="forgot-link" to="/recuperar-contrasena">¿Olvidaste tu contraseña?</Link>
          </div>
          <div className="input-wrap">
            <span className="material-symbols-outlined" aria-hidden="true">lock</span>
            <input
              autoComplete="current-password"
              id="password"
              name="password"
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Tu contraseña"
              required
              type={showPassword ? 'text' : 'password'}
              value={password}
            />
            <button
              aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              className="visibility-button"
              onClick={() => setShowPassword((visible) => !visible)}
              type="button"
            >
              <span className="material-symbols-outlined" aria-hidden="true">
                {showPassword ? 'visibility_off' : 'visibility'}
              </span>
            </button>
          </div>

          <label className="remember-row">
            <input checked={rememberMe} onChange={(event) => setRememberMe(event.target.checked)} type="checkbox" />
            <span>Recordarme en este dispositivo</span>
          </label>

          <div className="form-actions">
            <button className="button button-primary" disabled={isSubmitting} type="submit">
              {isSubmitting ? 'Verificando…' : 'Iniciar sesión'}
            </button>
            <Link className="button button-secondary" to="/">Cancelar</Link>
          </div>
        </form>

        <p className="register-prompt">
          ¿No tienes una cuenta? <Link to="/registro">Crea una aquí</Link>
        </p>

        <div className="divider"><span>o continuar con</span></div>
        <a className="google-button" href="/oauth2/authorization/google">
          <span className="google-mark" aria-hidden="true">G</span>
          Continuar con Google
        </a>

        <p className="legal-links">
          <a href="#terminos">Términos y Condiciones</a>
          <span aria-hidden="true">·</span>
          <a href="#privacidad">Política de Privacidad</a>
        </p>
      </section>
    </main>
  )
}
