import { useEffect, useState } from 'react'
import { BrowserRouter, Link, useLocation, useNavigate } from 'react-router-dom'
import { authService } from './services/authService.js'
import { clientDashboardService } from './services/clientDashboardService.js'
import { contractorDashboardService, workerDashboardService } from './services/professionalDashboardService.js'
import { AuthProvider, useAuth } from './context/AuthContext.jsx'
import { DashboardAdmin } from './pages/dashboard/DashboardAdmin.jsx'
import { DashboardCliente } from './pages/dashboard/DashboardCliente.jsx'
import { DashboardContratista } from './pages/dashboard/DashboardContratista.jsx'
import { DashboardTrabajador } from './pages/dashboard/DashboardTrabajador.jsx'
import { Usuarios } from './pages/admin/Usuarios.jsx'
import { AdminPerfilDetalle } from './pages/admin/AdminPerfilDetalle.jsx'
import { DetallePostulacion } from './pages/proyectos/DetallePostulacion.jsx'
import { PerfilPublico } from './pages/perfil/PerfilPublico.jsx'
import { DirectorioPerfiles } from './pages/perfil/DirectorioPerfiles.jsx'
import { ReportesPage } from './pages/reportes/ReportesPage.jsx'
import { EquiposPage } from './pages/equipos/EquiposPage.jsx'
import { HistorialContratista } from './pages/proyectos/HistorialContratista.jsx'
import { MisContratistasPage } from './pages/perfil/MisContratistasPage.jsx'
import { CalificarContratista } from './pages/perfil/CalificarContratista.jsx'
import { MercadoTrabajadores } from './pages/perfil/MercadoTrabajadores.jsx'
import { LandingPage } from './pages/landing/LandingPage.jsx'
import { CompletarRegistroPage } from './pages/auth/CompletarRegistroPage.jsx'
import { RegistroPage } from './pages/auth/RegistroPage.jsx'
import { PublicarProyecto } from './pages/proyectos/PublicarProyecto.jsx'
import { MisProyectos } from './pages/proyectos/MisProyectos.jsx'
import { DetalleProyecto, EditarProyecto } from './pages/proyectos/GestionarProyecto.jsx'
import { PostulantesProyecto } from './pages/proyectos/PostulantesProyecto.jsx'
import { ProyectosDisponibles } from './pages/proyectos/ProyectosDisponibles.jsx'
import { MisPostulaciones } from './pages/proyectos/MisPostulaciones.jsx'
import { PostulacionesRecibidas } from './pages/proyectos/PostulacionesRecibidas.jsx'
import { PerfilCliente } from './pages/perfil/PerfilCliente.jsx'
import { PerfilContratista } from './pages/perfil/PerfilContratista.jsx'
import { PerfilTrabajador } from './pages/perfil/PerfilTrabajador.jsx'
import { RecuperarContrasenaPage } from './pages/auth/RecuperarContrasena.jsx'

function rolesFromToken(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const claims = JSON.parse(atob(payload))
    return Array.isArray(claims.roles) ? claims.roles : []
  } catch {
    return []
  }
}

function establishSession(data, username, rememberMe, login, navigate) {
  const roles = Array.isArray(data.roles) ? data.roles : rolesFromToken(data.token)
  const profileRoles = roles.filter((role) => ['ROLE_CLIENT', 'ROLE_CONTRACTOR', 'ROLE_WORKER'].includes(role))
  const role = roles.includes('ROLE_ADMIN')
    ? 'ROLE_ADMIN'
    : profileRoles.length === 1
      ? profileRoles[0]
      : profileRoles.length > 1
        ? null
        : roles[0] || 'ROLE_USER'

  login({ name: username, email: username, roles, role }, data.token, rememberMe)
  navigate(profileRoles.length > 1 && role !== 'ROLE_ADMIN' ? '/seleccionar-perfil' : '/', { replace: true })
}

const roleOptions = {
  ROLE_CLIENT: { label: 'Cliente', description: 'Publicar proyectos y buscar talento', icon: 'person' },
  ROLE_CONTRACTOR: { label: 'Contratista', description: 'Gestionar proyectos y equipos', icon: 'engineering' },
  ROLE_WORKER: { label: 'Trabajador', description: 'Buscar oportunidades laborales', icon: 'construction' },
}

const verificationServices = {
  ROLE_CLIENT: { load: clientDashboardService.getDashboard, profile: '/perfil-cliente/editar', label: 'cliente' },
  ROLE_CONTRACTOR: { load: contractorDashboardService.getDashboard, profile: '/perfil-contratista/editar', label: 'contratista' },
  ROLE_WORKER: { load: workerDashboardService.getDashboard, profile: '/perfil-trabajador/editar', label: 'trabajador' },
}

function VerificationGate({ role, status, onRetry, onLogout }) {
  const account = verificationServices[role]
  const requirements = status?.requisitosFaltantes || []
  const profileIncomplete = Boolean(status?.perfilIncompleto)

  return (
    <main className="auth-page verification-gate-page">
      <section aria-labelledby="verification-gate-title" className="auth-shell verification-gate">
        <span className="material-symbols-outlined verification-gate-icon" aria-hidden="true">{status?.error ? 'cloud_off' : 'gpp_maybe'}</span>
        <p className="eyebrow">Verificación de cuenta</p>
        <h1 id="verification-gate-title">{status?.error ? 'No pudimos confirmar tu estado' : profileIncomplete ? 'Completa tu perfil para continuar' : 'Tu cuenta está en revisión'}</h1>
        <p className="auth-copy">{status?.error
          ? 'Por seguridad, no habilitamos las opciones hasta confirmar tu estado de verificación.'
          : profileIncomplete
            ? 'Completa la información pendiente. Después, el equipo de ObraTech podrá revisar tu cuenta.'
            : `El administrador debe verificar tu perfil de ${account.label} antes de habilitar las demás opciones.`}</p>
        {requirements.length > 0 && <ul className="verification-gate-requirements">{requirements.map((requirement) => <li key={requirement}>{requirement}</li>)}</ul>}
        <div className="verification-gate-actions">
          <Link className="button button-primary" to={account.profile}><span className="material-symbols-outlined" aria-hidden="true">edit</span>{profileIncomplete ? 'Completar perfil' : 'Revisar perfil'}</Link>
          {status?.error && <button className="button button-secondary" onClick={onRetry} type="button"><span className="material-symbols-outlined" aria-hidden="true">refresh</span>Volver a verificar</button>}
          <button className="verification-gate-logout" onClick={onLogout} type="button">Cerrar sesión</button>
        </div>
      </section>
    </main>
  )
}

function RoleSelectionPage({ roles, onSelect, onLogout }) {
  const availableRoles = roles.filter((role) => roleOptions[role])

  return (
    <main className="auth-page">
      <section className="auth-shell role-selection-shell">
        <a className="brand-mark" href="/" aria-label="ObraTech">
          <span className="material-symbols-outlined" aria-hidden="true">construction</span>
        </a>
        <header className="auth-heading">
          <h1>Selecciona tu perfil</h1>
          <p>Elige con qué perfil deseas continuar.</p>
        </header>
        <div className="role-choice-list">
          {availableRoles.map((role) => (
            <button className="role-choice" key={role} onClick={() => onSelect(role)} type="button">
              <span className="material-symbols-outlined" aria-hidden="true">{roleOptions[role].icon}</span>
              <span><strong>{roleOptions[role].label}</strong><small>{roleOptions[role].description}</small></span>
              <span className="material-symbols-outlined role-choice-arrow" aria-hidden="true">chevron_right</span>
            </button>
          ))}
        </div>
        <button className="role-logout" onClick={onLogout} type="button">Cerrar sesión</button>
      </section>
    </main>
  )
}

function LoginApp() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, isAuthenticated, login, logout, selectRole } = useAuth()
  const [verificationStatus, setVerificationStatus] = useState(null)
  const [verificationRetry, setVerificationRetry] = useState(0)
  const accountIdentity = user?.email || user?.name || ''
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [oauthLoading, setOauthLoading] = useState(false)
  const [oauthError, setOauthError] = useState('')
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

  useEffect(() => {
    const role = user?.role
    const verificationService = verificationServices[role]
    if (!isAuthenticated || !verificationService) return undefined
    let active = true
    verificationService.load()
      .then(({ data }) => {
        if (active) setVerificationStatus({
          role,
          identity: accountIdentity,
          verified: Boolean(data.usuarioVerificado),
          perfilIncompleto: Boolean(data.perfilIncompleto),
          requisitosFaltantes: data.requisitosFaltantes || [],
        })
      })
      .catch(() => {
        if (active) setVerificationStatus({ role, identity: accountIdentity, error: true, verified: false })
      })
    return () => { active = false }
  }, [accountIdentity, isAuthenticated, user?.role, verificationRetry])

  useEffect(() => {
    if (isAuthenticated) return
    const titles = {
      '/': 'ObraTech | Construcción conectada',
      '/index': 'ObraTech | Construcción conectada',
      '/login': 'Inicio de sesión | ObraTech',
      '/registro': 'Crear cuenta | ObraTech',
      '/completar-registro-oauth2': 'Completar registro | ObraTech',
      '/oauth/callback': 'Conectando con Google | ObraTech',
    }
    document.title = titles[location.pathname] || 'ObraTech'
  }, [isAuthenticated, location.pathname])

  useEffect(() => {
    if (location.pathname !== '/oauth/callback' || isAuthenticated) return undefined
    let active = true
    setOauthLoading(true)
    authService.exchangeOAuthSession()
      .then(({ data }) => {
        if (!active) return
        establishSession(data, data.username, true, login, navigate)
      })
      .catch((requestError) => {
        if (active) setOauthError(requestError.response?.data?.error || 'No se pudo recuperar la sesión de Google.')
      })
      .finally(() => { if (active) setOauthLoading(false) })
    return () => { active = false }
  }, [isAuthenticated, location.pathname, login, navigate])

  if (location.pathname === '/recuperar-contrasena') {
    return <RecuperarContrasenaPage onComplete={logout} />
  }
  if (location.pathname === '/restablecer-contrasena') {
    return <RecuperarContrasenaPage onComplete={logout} resetting />
  }
  if (location.pathname === '/error') {
    return <main className="auth-page"><section className="auth-shell auth-result"><span className="material-symbols-outlined error-page-icon" aria-hidden="true">error</span><p className="eyebrow">ObraTech</p><h1>No se pudo completar la solicitud</h1><p className="auth-copy">El recurso puede haber cambiado o no estar disponible. Vuelve al inicio y continúa desde allí.</p><Link className="button button-primary" to={isAuthenticated ? '/' : '/login'}>Volver</Link></section></main>
  }

  const completeOAuthRegistration = (data) => {
    establishSession(data, data.username, true, login, navigate)
  }

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

  if (isAuthenticated) {
    if (location.pathname === '/seleccionar-perfil' || !user?.role) {
      return <RoleSelectionPage roles={user?.roles || []} onSelect={(role) => { selectRole(role); navigate('/', { replace: true }) }} onLogout={logout} />
    }
    const accountVerification = verificationServices[user?.role]
    const profileRoutes = user?.role === 'ROLE_CLIENT'
      ? ['/perfil-cliente', '/perfil-cliente/editar']
      : user?.role === 'ROLE_CONTRACTOR'
        ? ['/perfil-contratista', '/perfil-contratista/editar', '/contratistas/editar']
        : user?.role === 'ROLE_WORKER'
          ? ['/perfil-trabajador', '/perfil-trabajador/editar', '/perfil-laboral', '/trabajadores/perfil']
          : []
    if (accountVerification && !profileRoutes.includes(location.pathname)) {
      if (verificationStatus?.role !== user.role || verificationStatus?.identity !== accountIdentity) {
        return <VerificationGate role={user.role} status={null} onRetry={() => setVerificationRetry((current) => current + 1)} onLogout={logout} />
      }
      if (!verificationStatus.verified) {
        return <VerificationGate role={user.role} status={verificationStatus} onRetry={() => setVerificationRetry((current) => current + 1)} onLogout={logout} />
      }
    }
    if (user?.role === 'ROLE_ADMIN') {
      if (location.pathname === '/admin/usuarios') return <Usuarios onLogout={logout} />
      if (location.pathname === '/trabajadores/crear') return <Usuarios createWorkerInitially onLogout={logout} />
      if (location.pathname === '/trabajadores') return <Usuarios onLogout={logout} />
      const adminWorkerRoute = location.pathname.match(/^\/trabajadores\/([^/]+)$/)
      if (adminWorkerRoute) return <AdminPerfilDetalle id={decodeURIComponent(adminWorkerRoute[1])} onLogout={logout} />
      if (location.pathname === '/clientes' || location.pathname === '/contratistas') {
        return <DirectorioPerfiles kind={location.pathname === '/clientes' ? 'clients' : 'contractors'} onLogout={logout} />
      }
      const adminApplicantsRoute = location.pathname.match(/^\/(?:proyectos|postulaciones)\/([^/]+)\/postulantes$/)
      if (adminApplicantsRoute) return <PostulantesProyecto projectId={decodeURIComponent(adminApplicantsRoute[1])} />
      const adminProjectRoute = location.pathname.match(/^\/proyectos\/([^/]+)$/)
      if (adminProjectRoute) return <DetalleProyecto id={decodeURIComponent(adminProjectRoute[1])} />
      const adminProfileRoute = location.pathname.match(/^\/admin\/(?:perfiles|clientes|contratistas)\/([^/]+)$/)
      if (adminProfileRoute) return <AdminPerfilDetalle id={decodeURIComponent(adminProfileRoute[1])} onLogout={logout} />
      return <DashboardAdmin onLogout={logout} />
    }
    if (user?.role === 'ROLE_CLIENT') {
      if (location.pathname === '/trabajadores' || location.pathname === '/trabajadores/disponibles') {
        return <DirectorioPerfiles availableOnly={location.pathname.endsWith('/disponibles')} kind="workers" onLogout={logout} />
      }
      if (location.pathname === '/contratistas' || location.pathname.startsWith('/contratistas/buscar/')) {
        const specialty = location.pathname.startsWith('/contratistas/buscar/')
          ? decodeURIComponent(location.pathname.slice('/contratistas/buscar/'.length))
          : ''
        return <DirectorioPerfiles kind="contractors" specialty={specialty} onLogout={logout} />
      }
      if (location.pathname === '/perfil-cliente/editar') {
        return <PerfilCliente editing />
      }
      if (location.pathname === '/perfil-cliente') {
        return <PerfilCliente />
      }
      if (location.pathname === '/perfil-cliente/editar') return <PerfilCliente editing />
      if (location.pathname === '/proyectos/publicar') {
        return <PublicarProyecto />
      }
      if (location.pathname === '/clientes/proyectos-en-proceso') return <MisProyectos onLogout={logout} inProgressOnly />
      if (location.pathname === '/mis-proyectos' || location.pathname === '/proyectos/mis-proyectos') {
        return <MisProyectos onLogout={logout} />
      }
      if (location.pathname === '/clientes/reportes') {
        return <ReportesPage onLogout={logout} />
      }
      if (location.pathname === '/clientes/mis-contratistas') {
        return <MisContratistasPage onLogout={logout} />
      }
      if (location.pathname === '/clientes/mis-postulaciones') {
        return <PostulacionesRecibidas onLogout={logout} />
      }
      if (location.pathname === '/postulaciones/mis-postulaciones') return <PostulacionesRecibidas onLogout={logout} />
      if (location.pathname === '/calificaciones/contratistas'
          || location.pathname.startsWith('/contratistas/para-calificar/')
          || location.pathname.startsWith('/calificaciones/calificar-por-contratista/')) {
        const projectId = location.pathname.match(/^\/contratistas\/para-calificar\/([^/]+)$/)?.[1]
        return projectId
          ? <CalificarContratista onLogout={logout} projectId={decodeURIComponent(projectId)} />
          : <MisContratistasPage onLogout={logout} />
      }
      const publicWorkerRoute = location.pathname.match(/^\/trabajadores\/([^/]+)$/)
      if (publicWorkerRoute) return <PerfilPublico id={decodeURIComponent(publicWorkerRoute[1])} onLogout={logout} />
      const contractorUsernameRoute = location.pathname.match(/^\/contratistas\/por-username\/([^/]+)$/)
      if (contractorUsernameRoute) return <PerfilPublico id={decodeURIComponent(contractorUsernameRoute[1])} onLogout={logout} />
      const publicContractorRoute = location.pathname.match(/^\/contratistas\/([^/]+)$/)
      if (publicContractorRoute) return <PerfilPublico id={decodeURIComponent(publicContractorRoute[1])} onLogout={logout} />
      const ratingRoute = location.pathname.match(/^\/calificaciones\/(?:crear|calificar)\/([^/]+)\/([^/]+)$/)
      if (ratingRoute) {
        return <CalificarContratista onLogout={logout} projectId={decodeURIComponent(ratingRoute[1])} contractorId={decodeURIComponent(ratingRoute[2])} />
      }
      const projectRatingRoute = location.pathname.match(/^\/calificaciones\/calificar\/([^/]+)$/)
      if (projectRatingRoute) return <CalificarContratista onLogout={logout} projectId={decodeURIComponent(projectRatingRoute[1])} />
      const ratingEditRoute = location.pathname.match(/^\/calificaciones\/([^/]+)\/editar$/)
      if (ratingEditRoute) return <CalificarContratista onLogout={logout} ratingId={decodeURIComponent(ratingEditRoute[1])} />
      const applicationsRoute = location.pathname.match(/^\/proyectos\/([^/]+)\/postulantes$/)
      if (applicationsRoute) {
        return <PostulantesProyecto projectId={decodeURIComponent(applicationsRoute[1])} />
      }
      const legacyApplicationsRoute = location.pathname.match(/^\/postulaciones\/([^/]+)\/postulantes$/)
      if (legacyApplicationsRoute) return <PostulantesProyecto projectId={decodeURIComponent(legacyApplicationsRoute[1])} />
      const projectRoute = location.pathname.match(/^\/proyectos\/([^/]+)(?:\/(editar))?$/)
      if (projectRoute) {
        const projectId = decodeURIComponent(projectRoute[1])
        return projectRoute[2]
          ? <EditarProyecto id={projectId} />
          : <DetalleProyecto id={projectId} />
      }
      return <DashboardCliente onLogout={logout} />
    }
    if (user?.role === 'ROLE_CONTRACTOR') {
      if (location.pathname === '/clientes') return <DirectorioPerfiles kind="clients" onLogout={logout} />
      if (location.pathname === '/contratistas' || location.pathname.startsWith('/contratistas/buscar/')) {
        const specialty = location.pathname.startsWith('/contratistas/buscar/')
          ? decodeURIComponent(location.pathname.slice('/contratistas/buscar/'.length))
          : ''
        return <DirectorioPerfiles kind="contractors" specialty={specialty} onLogout={logout} />
      }
      const hireWorkerRoute = location.pathname.match(/^\/contratistas\/contratar\/([^/]+)$/)
      if (hireWorkerRoute) return <MercadoTrabajadores onLogout={logout} workerId={decodeURIComponent(hireWorkerRoute[1])} />
      const teamProjectRoute = location.pathname.match(/^\/contratistas\/proyectos\/([^/]+)\/equipos\/crear$/)
      if (teamProjectRoute) return <EquiposPage onLogout={logout} role={user.role} projectId={decodeURIComponent(teamProjectRoute[1])} />
      if (location.pathname === '/trabajadores' || location.pathname === '/trabajadores/disponibles') return <MercadoTrabajadores onLogout={logout} />
      if (location.pathname === '/proyectos/postular') return <ProyectosDisponibles onLogout={logout} role={user.role} />
      const legacyAvailableProject = location.pathname.match(/^\/proyectos\/postular\/([^/]+)$/)
      if (legacyAvailableProject) return <ProyectosDisponibles onLogout={logout} role={user.role} projectId={decodeURIComponent(legacyAvailableProject[1])} />
      const publicWorkerRoute = location.pathname.match(/^\/trabajadores\/([^/]+)$/)
      if (publicWorkerRoute) return <PerfilPublico id={decodeURIComponent(publicWorkerRoute[1])} onLogout={logout} />
      if (location.pathname === '/contratistas/mi-equipo') return <EquiposPage onLogout={logout} role={user.role} />
      if (location.pathname === '/contratistas/historial') return <HistorialContratista onLogout={logout} />
      if (location.pathname === '/contratistas/proyectos-asignados') return <DashboardContratista onLogout={logout} />
      if (location.pathname === '/perfil-contratista/editar' || location.pathname === '/contratistas/editar') return <PerfilContratista editing />
      if (location.pathname === '/perfil-contratista') return <PerfilContratista />
      if (location.pathname === '/postulaciones') return <ProyectosDisponibles onLogout={logout} role={user.role} />
      const availableProjectRoute = location.pathname.match(/^\/postulaciones\/(?:ver\/)?([^/]+)$/)
      if (availableProjectRoute && !['mis-postulaciones', 'mis-postulaciones-contratista'].includes(availableProjectRoute[1])) {
        return <ProyectosDisponibles onLogout={logout} role={user.role} projectId={decodeURIComponent(availableProjectRoute[1])} />
      }
      if (location.pathname === '/mis-postulaciones' || location.pathname === '/postulaciones/mis-postulaciones' || location.pathname === '/postulaciones/mis-postulaciones-contratista') {
        return <MisPostulaciones onLogout={logout} role={user.role} />
      }
      const contractorApplicationRoute = location.pathname.match(/^\/mis-postulaciones\/([^/]+)$/)
      if (contractorApplicationRoute) return <DetallePostulacion id={decodeURIComponent(contractorApplicationRoute[1])} onLogout={logout} role={user.role} />
      return <DashboardContratista onLogout={logout} />
    }
    if (user?.role === 'ROLE_WORKER') {
      if (location.pathname === '/trabajadores/perfil' || location.pathname === '/perfil-laboral') return <PerfilTrabajador editing />
      if (location.pathname === '/trabajadores/mi-equipo') return <EquiposPage onLogout={logout} role={user.role} />
      if (location.pathname === '/trabajadores/disponibles') return <ProyectosDisponibles onLogout={logout} role={user.role} />
      if (location.pathname === '/proyectos/postular') return <ProyectosDisponibles onLogout={logout} role={user.role} />
      const legacyAvailableProject = location.pathname.match(/^\/proyectos\/postular\/([^/]+)$/)
      if (legacyAvailableProject) return <ProyectosDisponibles onLogout={logout} role={user.role} projectId={decodeURIComponent(legacyAvailableProject[1])} />
      if (location.pathname === '/contratistas' || location.pathname.startsWith('/contratistas/buscar/')) {
        const specialty = location.pathname.startsWith('/contratistas/buscar/')
          ? decodeURIComponent(location.pathname.slice('/contratistas/buscar/'.length))
          : ''
        return <DirectorioPerfiles kind="contractors" specialty={specialty} onLogout={logout} />
      }
      const contractorUsernameRoute = location.pathname.match(/^\/contratistas\/por-username\/([^/]+)$/)
      if (contractorUsernameRoute) return <PerfilPublico id={decodeURIComponent(contractorUsernameRoute[1])} onLogout={logout} />
      const publicContractorRoute = location.pathname.match(/^\/contratistas\/([^/]+)$/)
      if (publicContractorRoute) return <PerfilPublico id={decodeURIComponent(publicContractorRoute[1])} onLogout={logout} />
      if (location.pathname === '/trabajadores/mi-equipo') return <EquiposPage onLogout={logout} role={user.role} />
      if (location.pathname === '/perfil-trabajador/editar') return <PerfilTrabajador editing />
      if (location.pathname === '/perfil-trabajador') return <PerfilTrabajador />
      if (location.pathname === '/postulaciones') return <ProyectosDisponibles onLogout={logout} role={user.role} />
      const availableProjectRoute = location.pathname.match(/^\/postulaciones\/(?:ver\/)?([^/]+)$/)
      if (availableProjectRoute && !['mis-postulaciones', 'mis-postulaciones-contratista'].includes(availableProjectRoute[1])) {
        return <ProyectosDisponibles onLogout={logout} role={user.role} projectId={decodeURIComponent(availableProjectRoute[1])} />
      }
      if (location.pathname === '/mis-postulaciones' || location.pathname === '/trabajadores/mis-postulaciones' || location.pathname === '/postulaciones/mis-postulaciones-contratista') {
        return <MisPostulaciones onLogout={logout} role={user.role} />
      }
      const workerApplicationRoute = location.pathname.match(/^\/mis-postulaciones\/([^/]+)$/)
      if (workerApplicationRoute) return <DetallePostulacion id={decodeURIComponent(workerApplicationRoute[1])} onLogout={logout} role={user.role} />
      return <DashboardTrabajador onLogout={logout} />
    }

    return (
      <main className="auth-page">
        <section className="auth-shell auth-result">
          <a className="brand-mark" href="/" aria-label="ObraTech">
            <span className="material-symbols-outlined" aria-hidden="true">construction</span>
          </a>
          <p className="eyebrow">Acceso confirmado</p>
          <h1>Sesión iniciada</h1>
          <p className="auth-copy">La API aceptó las credenciales y emitió un token válido para:</p>
          <div className="identity-row">
            <span>{user?.email}</span>
            <strong>{user?.role?.replace('ROLE_', '')}</strong>
          </div>
          <p className="migration-note">Tu cuenta no tiene asignado un perfil compatible con esta plataforma. Contacta al administrador para revisar el acceso.</p>
          <button className="button button-primary" onClick={logout}>Cerrar sesión</button>
        </section>
      </main>
    )
  }

  if (location.pathname === '/registro') {
    return <RegistroPage />
  }

  if (location.pathname === '/completar-registro-oauth2') {
    return <CompletarRegistroPage onComplete={completeOAuthRegistration} />
  }

  if (location.pathname === '/oauth/callback') {
    return <main className="auth-page"><section className="auth-shell"><p className="eyebrow">Google</p><h1>{oauthError ? 'No se pudo iniciar sesión' : 'Conectando tu cuenta…'}</h1>{oauthError ? <div className="form-alert" role="alert">{oauthError}</div> : <p role="status">{oauthLoading ? 'Validando sesión segura.' : 'Redirigiendo…'}</p>}{oauthError && <Link className="button button-primary" to="/login">Volver al inicio de sesión</Link>}</section></main>
  }

  if (location.pathname === '/' || location.pathname === '/index') {
    return <LandingPage />
  }

  return (
    <main className="auth-page">
      <section className="auth-shell">
        <a className="brand-mark" href="/" aria-label="ObraTech">
          <span className="material-symbols-outlined" aria-hidden="true">construction</span>
        </a>

        <header className="auth-heading">
          <h1>Bienvenido a ObraTech</h1>
          <p>Ingresa tus credenciales para continuar</p>
        </header>

        {error && <div className="form-alert" role="alert">{error}</div>}
        {!error && authStatus && <div className={`form-alert ${authStatus.tone === 'success' ? 'form-alert-success' : ''}`} role={authStatus.tone === 'success' ? 'status' : 'alert'}>{authStatus.message}</div>}
        {registered && <div className="form-alert form-alert-success" role="status">Cuenta creada exitosamente. Ya puedes iniciar sesión.</div>}

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

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <LoginApp />
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
