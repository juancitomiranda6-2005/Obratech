import { NavLink } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth.js'

const roles = {
  CLIENTE: [
    { label: 'Dashboard', to: '/dashboard' },
    { label: 'Mis proyectos', to: '/mis-proyectos' },
    { label: 'Perfil', to: '/perfil-cliente' },
  ],
  CONTRATISTA: [
    { label: 'Dashboard', to: '/dashboard' },
    { label: 'Perfil', to: '/perfil-contratista' },
    { label: 'Proyectos', to: '/dashboard' },
  ],
  TRABAJADOR: [
    { label: 'Dashboard', to: '/dashboard' },
    { label: 'Perfil', to: '/dashboard' },
    { label: 'Postulaciones', to: '/dashboard' },
  ],
  ADMIN: [
    { label: 'Dashboard', to: '/dashboard' },
    { label: 'Usuarios', to: '/dashboard' },
    { label: 'Reportes', to: '/dashboard' },
  ],
}

export function Sidebar() {
  const { user, logout } = useAuth()
  const links = roles[user?.role] || roles.CLIENTE

  return (
    <aside className="w-72 border-r border-slate-800 bg-slate-950/80 p-6">
      <div className="mb-8">
        <p className="text-xs uppercase tracking-[0.2em] text-blue-400">ObraTech</p>
        <h2 className="mt-2 text-2xl font-bold text-white">Panel</h2>
      </div>

      <nav className="space-y-2">
        {links.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `block rounded-xl px-4 py-3 text-sm font-medium transition ${
                isActive ? 'bg-blue-600 text-white' : 'text-slate-300 hover:bg-slate-800 hover:text-white'
              }`
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="mt-8 rounded-2xl border border-slate-800 bg-slate-900 p-4">
        <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Usuario</p>
        <p className="mt-2 font-semibold text-white">{user?.name || 'Demo User'}</p>
        <p className="text-sm text-slate-400">{user?.role || 'CLIENTE'}</p>
        <button
          type="button"
          onClick={logout}
          className="mt-4 w-full rounded-xl border border-slate-700 px-3 py-2 text-sm text-slate-200 transition hover:border-slate-500 hover:bg-slate-800"
        >
          Cerrar sesión
        </button>
      </div>
    </aside>
  )
}
