import { useEffect, useState } from 'react'
import { BrowserRouter, Link, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom'
import { authService, establishSession } from './services/authService.js'
import { AuthProvider, useAuth } from './context/AuthContext.jsx'
import { LoginPage } from './pages/auth/LoginPage.jsx'
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

const roleOptions = {
  ROLE_CLIENT: { label: 'Cliente', description: 'Publicar proyectos y buscar talento', icon: 'person' },
  ROLE_CONTRACTOR: { label: 'Contratista', description: 'Gestionar proyectos y equipos', icon: 'engineering' },
  ROLE_WORKER: { label: 'Trabajador', description: 'Buscar oportunidades laborales', icon: 'construction' },
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

function AdminPerfilDetalleRoute({ onLogout }) {
  const { id } = useParams()
  return <AdminPerfilDetalle id={id} onLogout={onLogout} />
}

function PostulantesProyectoRoute() {
  const { projectId } = useParams()
  return <PostulantesProyecto projectId={projectId} />
}

function DetalleProyectoRoute() {
  const { id } = useParams()
  return <DetalleProyecto id={id} />
}

function EditarProyectoRoute() {
  const { id } = useParams()
  return <EditarProyecto id={id} />
}

function PerfilPublicoRoute({ onLogout, viewerRole }) {
  const { id } = useParams()
  return <PerfilPublico id={id} onLogout={onLogout} viewerRole={viewerRole} />
}

function DirectorioContratistasEspecialidadRoute({ onLogout }) {
  const { specialty } = useParams()
  return <DirectorioPerfiles kind="contractors" specialty={specialty ? decodeURIComponent(specialty) : ''} onLogout={onLogout} />
}

function CalificarContratistaRoute({ onLogout }) {
  const { projectId, contractorId, ratingId } = useParams()
  return <CalificarContratista onLogout={onLogout} projectId={projectId} contractorId={contractorId} ratingId={ratingId} />
}

function MercadoTrabajadoresRoute({ onLogout }) {
  const { workerId } = useParams()
  return <MercadoTrabajadores onLogout={onLogout} workerId={workerId} />
}

function EquiposPageRoute({ onLogout, role }) {
  const { projectId } = useParams()
  return <EquiposPage onLogout={onLogout} role={role} projectId={projectId} />
}

function ProyectosDisponiblesRoute({ onLogout, role }) {
  const { projectId } = useParams()
  return <ProyectosDisponibles onLogout={onLogout} role={role} projectId={projectId} />
}

function DetallePostulacionRoute({ onLogout, role }) {
  const { id } = useParams()
  return <DetallePostulacion id={id} onLogout={onLogout} role={role} />
}

function OAuthCallbackRoute({ onComplete }) {
  const [oauthLoading, setOauthLoading] = useState(true)
  const [oauthError, setOauthError] = useState('')

  useEffect(() => {
    let active = true
    authService.exchangeOAuthSession()
      .then(({ data }) => {
        if (!active) return
        onComplete(data)
      })
      .catch((requestError) => {
        if (active) setOauthError(requestError.response?.data?.error || 'No se pudo recuperar la sesión de Google.')
      })
      .finally(() => { if (active) setOauthLoading(false) })
    return () => { active = false }
  }, [onComplete])

  return (
    <main className="auth-page">
      <section className="auth-shell">
        <p className="eyebrow">Google</p>
        <h1>{oauthError ? 'No se pudo iniciar sesión' : 'Conectando tu cuenta…'}</h1>
        {oauthError ? (
          <div className="form-alert" role="alert">{oauthError}</div>
        ) : (
          <p role="status">{oauthLoading ? 'Validando sesión segura.' : 'Redirigiendo…'}</p>
        )}
        {oauthError && <Link className="button button-primary" to="/login">Volver al inicio de sesión</Link>}
      </section>
    </main>
  )
}

function ErrorRoute({ isAuthenticated }) {
  return (
    <main className="auth-page">
      <section className="auth-shell auth-result">
        <span className="material-symbols-outlined error-page-icon" aria-hidden="true">error</span>
        <p className="eyebrow">ObraTech</p>
        <h1>No se pudo completar la solicitud</h1>
        <p className="auth-copy">El recurso puede haber cambiado o no estar disponible. Vuelve al inicio y continúa desde allí.</p>
        <Link className="button button-primary" to={isAuthenticated ? '/' : '/login'}>Volver</Link>
      </section>
    </main>
  )
}

function NotFoundRoute({ isAuthenticated }) {
  return (
    <main className="auth-page">
      <section className="auth-shell auth-result">
        <span className="material-symbols-outlined error-page-icon" aria-hidden="true">search_off</span>
        <p className="eyebrow">404</p>
        <h1>Página no encontrada</h1>
        <p className="auth-copy">La página que buscas no existe o ha sido movida.</p>
        <Link className="button button-primary" to={isAuthenticated ? '/' : '/login'}>Ir al inicio</Link>
      </section>
    </main>
  )
}

function AppRoutes() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, isAuthenticated, login, logout, selectRole } = useAuth()

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

  const completeOAuthRegistration = (data) => {
    establishSession(data, data.username, true, login, navigate)
  }

  // Si está autenticado pero necesita seleccionar rol o está en /seleccionar-perfil
  if (isAuthenticated && (location.pathname === '/seleccionar-perfil' || !user?.role)) {
    return (
      <RoleSelectionPage
        roles={user?.roles || []}
        onSelect={(role) => {
          selectRole(role)
          navigate('/', { replace: true })
        }}
        onLogout={logout}
      />
    )
  }

  // Rutas para usuarios autenticados según rol
  if (isAuthenticated) {
    if (user?.role === 'ROLE_ADMIN') {
      return (
        <Routes>
          <Route path="/admin/usuarios" element={<Usuarios onLogout={logout} />} />
          <Route path="/trabajadores/crear" element={<Usuarios createWorkerInitially onLogout={logout} />} />
          <Route path="/trabajadores" element={<Usuarios onLogout={logout} />} />
          <Route path="/trabajadores/:id" element={<AdminPerfilDetalleRoute onLogout={logout} />} />
          <Route path="/clientes" element={<DirectorioPerfiles kind="clients" onLogout={logout} />} />
          <Route path="/contratistas" element={<DirectorioPerfiles kind="contractors" onLogout={logout} />} />
          <Route path="/proyectos/:projectId/postulantes" element={<PostulantesProyectoRoute />} />
          <Route path="/postulaciones/:projectId/postulantes" element={<PostulantesProyectoRoute />} />
          <Route path="/proyectos/:id" element={<DetalleProyectoRoute />} />
          <Route path="/admin/perfiles/:id" element={<AdminPerfilDetalleRoute onLogout={logout} />} />
          <Route path="/admin/clientes/:id" element={<AdminPerfilDetalleRoute onLogout={logout} />} />
          <Route path="/admin/contratistas/:id" element={<AdminPerfilDetalleRoute onLogout={logout} />} />
          <Route path="/error" element={<ErrorRoute isAuthenticated={true} />} />
          <Route path="/" element={<DashboardAdmin onLogout={logout} />} />
          <Route path="/dashboard" element={<DashboardAdmin onLogout={logout} />} />
          <Route path="*" element={<DashboardAdmin onLogout={logout} />} />
        </Routes>
      )
    }

    if (user?.role === 'ROLE_CLIENT') {
      return (
        <Routes>
          <Route path="/trabajadores" element={<DirectorioPerfiles kind="workers" onLogout={logout} />} />
          <Route path="/trabajadores/disponibles" element={<DirectorioPerfiles availableOnly kind="workers" onLogout={logout} />} />
          <Route path="/contratistas" element={<DirectorioPerfiles kind="contractors" onLogout={logout} />} />
          <Route path="/contratistas/buscar/:specialty" element={<DirectorioContratistasEspecialidadRoute onLogout={logout} />} />
          <Route path="/perfil-cliente/editar" element={<PerfilCliente editing />} />
          <Route path="/perfil-cliente" element={<PerfilCliente />} />
          <Route path="/proyectos/publicar" element={<PublicarProyecto />} />
          <Route path="/clientes/proyectos-en-proceso" element={<MisProyectos onLogout={logout} inProgressOnly />} />
          <Route path="/mis-proyectos" element={<MisProyectos onLogout={logout} />} />
          <Route path="/proyectos/mis-proyectos" element={<MisProyectos onLogout={logout} />} />
          <Route path="/clientes/reportes" element={<ReportesPage onLogout={logout} />} />
          <Route path="/clientes/mis-contratistas" element={<MisContratistasPage onLogout={logout} />} />
          <Route path="/clientes/mis-postulaciones" element={<PostulacionesRecibidas onLogout={logout} />} />
          <Route path="/postulaciones/mis-postulaciones" element={<PostulacionesRecibidas onLogout={logout} />} />
          <Route path="/calificaciones/contratistas" element={<MisContratistasPage onLogout={logout} />} />
          <Route path="/contratistas/para-calificar/:projectId" element={<CalificarContratistaRoute onLogout={logout} />} />
          <Route path="/trabajadores/:id" element={<PerfilPublicoRoute onLogout={logout} viewerRole={user.role} />} />
          <Route path="/contratistas/por-username/:id" element={<PerfilPublicoRoute onLogout={logout} viewerRole={user.role} />} />
          <Route path="/contratistas/:id" element={<PerfilPublicoRoute onLogout={logout} viewerRole={user.role} />} />
          <Route path="/calificaciones/crear/:projectId/:contractorId" element={<CalificarContratistaRoute onLogout={logout} />} />
          <Route path="/calificaciones/calificar/:projectId/:contractorId" element={<CalificarContratistaRoute onLogout={logout} />} />
          <Route path="/calificaciones/calificar/:projectId" element={<CalificarContratistaRoute onLogout={logout} />} />
          <Route path="/calificaciones/:ratingId/editar" element={<CalificarContratistaRoute onLogout={logout} />} />
          <Route path="/proyectos/:projectId/postulantes" element={<PostulantesProyectoRoute />} />
          <Route path="/postulaciones/:projectId/postulantes" element={<PostulantesProyectoRoute />} />
          <Route path="/proyectos/:id/editar" element={<EditarProyectoRoute />} />
          <Route path="/proyectos/:id" element={<DetalleProyectoRoute />} />
          <Route path="/error" element={<ErrorRoute isAuthenticated={true} />} />
          <Route path="/" element={<DashboardCliente onLogout={logout} />} />
          <Route path="/dashboard" element={<DashboardCliente onLogout={logout} />} />
          <Route path="*" element={<DashboardCliente onLogout={logout} />} />
        </Routes>
      )
    }

    if (user?.role === 'ROLE_CONTRACTOR') {
      return (
        <Routes>
          <Route path="/clientes/perfil/:id" element={<PerfilPublicoRoute onLogout={logout} viewerRole={user.role} />} />
          <Route path="/clientes" element={<DirectorioPerfiles kind="clients" onLogout={logout} />} />
          <Route path="/contratistas" element={<DirectorioPerfiles kind="contractors" onLogout={logout} />} />
          <Route path="/contratistas/buscar/:specialty" element={<DirectorioContratistasEspecialidadRoute onLogout={logout} />} />
          <Route path="/contratistas/contratar/:workerId" element={<MercadoTrabajadoresRoute onLogout={logout} />} />
          <Route path="/contratistas/proyectos/:projectId/equipos/crear" element={<EquiposPageRoute onLogout={logout} role={user.role} />} />
          <Route path="/trabajadores" element={<MercadoTrabajadores onLogout={logout} />} />
          <Route path="/trabajadores/disponibles" element={<MercadoTrabajadores onLogout={logout} />} />
          <Route path="/proyectos/postular" element={<ProyectosDisponibles onLogout={logout} role={user.role} />} />
          <Route path="/proyectos/postular/:projectId" element={<ProyectosDisponiblesRoute onLogout={logout} role={user.role} />} />
          <Route path="/trabajadores/:id" element={<PerfilPublicoRoute onLogout={logout} viewerRole={user.role} />} />
          <Route path="/contratistas/mi-equipo" element={<EquiposPage onLogout={logout} role={user.role} />} />
          <Route path="/contratistas/historial" element={<HistorialContratista onLogout={logout} />} />
          <Route path="/contratistas/proyectos-asignados" element={<DashboardContratista onLogout={logout} />} />
          <Route path="/perfil-contratista/editar" element={<PerfilContratista editing />} />
          <Route path="/contratistas/editar" element={<PerfilContratista editing />} />
          <Route path="/perfil-contratista" element={<PerfilContratista />} />
          <Route path="/postulaciones" element={<ProyectosDisponibles onLogout={logout} role={user.role} />} />
          <Route path="/postulaciones/ver/:projectId" element={<ProyectosDisponiblesRoute onLogout={logout} role={user.role} />} />
          <Route path="/postulaciones/:projectId" element={<ProyectosDisponiblesRoute onLogout={logout} role={user.role} />} />
          <Route path="/mis-postulaciones" element={<MisPostulaciones onLogout={logout} role={user.role} />} />
          <Route path="/postulaciones/mis-postulaciones" element={<MisPostulaciones onLogout={logout} role={user.role} />} />
          <Route path="/postulaciones/mis-postulaciones-contratista" element={<MisPostulaciones onLogout={logout} role={user.role} />} />
          <Route path="/mis-postulaciones/:id" element={<DetallePostulacionRoute onLogout={logout} role={user.role} />} />
          <Route path="/error" element={<ErrorRoute isAuthenticated={true} />} />
          <Route path="/" element={<DashboardContratista onLogout={logout} />} />
          <Route path="/dashboard" element={<DashboardContratista onLogout={logout} />} />
          <Route path="*" element={<DashboardContratista onLogout={logout} />} />
        </Routes>
      )
    }

    if (user?.role === 'ROLE_WORKER') {
      return (
        <Routes>
          <Route path="/clientes/perfil/:id" element={<PerfilPublicoRoute onLogout={logout} viewerRole={user.role} />} />
          <Route path="/trabajadores/perfil" element={<PerfilTrabajador editing />} />
          <Route path="/perfil-laboral" element={<PerfilTrabajador editing />} />
          <Route path="/trabajadores/mi-equipo" element={<EquiposPage onLogout={logout} role={user.role} />} />
          <Route path="/trabajadores/disponibles" element={<ProyectosDisponibles onLogout={logout} role={user.role} />} />
          <Route path="/proyectos/postular" element={<ProyectosDisponibles onLogout={logout} role={user.role} />} />
          <Route path="/proyectos/postular/:projectId" element={<ProyectosDisponiblesRoute onLogout={logout} role={user.role} />} />
          <Route path="/contratistas" element={<DirectorioPerfiles kind="contractors" onLogout={logout} />} />
          <Route path="/contratistas/buscar/:specialty" element={<DirectorioContratistasEspecialidadRoute onLogout={logout} />} />
          <Route path="/contratistas/por-username/:id" element={<PerfilPublicoRoute onLogout={logout} viewerRole={user.role} />} />
          <Route path="/contratistas/:id" element={<PerfilPublicoRoute onLogout={logout} viewerRole={user.role} />} />
          <Route path="/perfil-trabajador/editar" element={<PerfilTrabajador editing />} />
          <Route path="/perfil-trabajador" element={<PerfilTrabajador />} />
          <Route path="/postulaciones" element={<ProyectosDisponibles onLogout={logout} role={user.role} />} />
          <Route path="/postulaciones/ver/:projectId" element={<ProyectosDisponiblesRoute onLogout={logout} role={user.role} />} />
          <Route path="/postulaciones/:projectId" element={<ProyectosDisponiblesRoute onLogout={logout} role={user.role} />} />
          <Route path="/mis-postulaciones" element={<MisPostulaciones onLogout={logout} role={user.role} />} />
          <Route path="/trabajadores/mis-postulaciones" element={<MisPostulaciones onLogout={logout} role={user.role} />} />
          <Route path="/postulaciones/mis-postulaciones-contratista" element={<MisPostulaciones onLogout={logout} role={user.role} />} />
          <Route path="/mis-postulaciones/:id" element={<DetallePostulacionRoute onLogout={logout} role={user.role} />} />
          <Route path="/error" element={<ErrorRoute isAuthenticated={true} />} />
          <Route path="/" element={<DashboardTrabajador onLogout={logout} />} />
          <Route path="/dashboard" element={<DashboardTrabajador onLogout={logout} />} />
          <Route path="*" element={<DashboardTrabajador onLogout={logout} />} />
        </Routes>
      )
    }

    // Usuario autenticado pero sin rol de negocio válido
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

  // Rutas públicas (no autenticado)
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/index" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegistroPage />} />
      <Route path="/recuperar-contrasena" element={<RecuperarContrasenaPage onComplete={logout} />} />
      <Route path="/restablecer-contrasena" element={<RecuperarContrasenaPage onComplete={logout} resetting />} />
      <Route path="/completar-registro-oauth2" element={<CompletarRegistroPage onComplete={completeOAuthRegistration} />} />
      <Route path="/oauth/callback" element={<OAuthCallbackRoute onComplete={completeOAuthRegistration} />} />
      <Route path="/error" element={<ErrorRoute isAuthenticated={false} />} />
      <Route path="*" element={<NotFoundRoute isAuthenticated={false} />} />
    </Routes>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
