import { Link, useNavigate } from 'react-router-dom'
import { Button } from '../../components/ui/Button.jsx'
import { useAuth } from '../../hooks/useAuth.js'

export function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()

  const handleSubmit = (event) => {
    event.preventDefault()

    login(
      {
        name: 'Cliente Demo',
        role: 'CLIENTE',
        email: 'demo@obratech.com',
      },
      'demo-token',
    )

    navigate('/dashboard')
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-6 py-12">
      <div className="w-full max-w-md rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-2xl shadow-blue-950/30">
        <p className="text-xs uppercase tracking-[0.25em] text-blue-400">ObraTech</p>
        <h1 className="mt-4 text-3xl font-bold text-white">Iniciar sesión</h1>
        <p className="mt-2 text-sm text-slate-400">Accede a proyectos, perfiles y reportes.</p>

        <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
          <label className="block text-sm text-slate-300">
            Correo
            <input
              type="email"
              defaultValue="demo@obratech.com"
              className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none ring-0 transition focus:border-blue-500"
            />
          </label>

          <label className="block text-sm text-slate-300">
            Contraseña
            <input
              type="password"
              defaultValue="123456"
              className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none ring-0 transition focus:border-blue-500"
            />
          </label>

          <Button type="submit" className="w-full">Entrar</Button>
        </form>

        <div className="mt-6 flex items-center justify-between text-sm text-slate-400">
          <Link to="/registro" className="text-blue-400 hover:text-blue-300">
            Crear cuenta
          </Link>
          <Link to="/" className="hover:text-white">
            Volver al inicio
          </Link>
        </div>
      </div>
    </div>
  )
}
