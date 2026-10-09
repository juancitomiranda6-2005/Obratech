import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('obratech_user') || sessionStorage.getItem('obratech_user')
    return saved ? JSON.parse(saved) : null
  })

  const [token, setToken] = useState(() => localStorage.getItem('obratech_token') || sessionStorage.getItem('obratech_token') || '')
  const [sessionWarningVisible, setSessionWarningVisible] = useState(false)

  useEffect(() => {
    if (!token) return undefined
    const storage = localStorage.getItem('obratech_token') ? localStorage : sessionStorage
    const storedStart = Number(storage.getItem('obratech_session_start'))
    const sessionStart = Number.isFinite(storedStart) && storedStart > 0 ? storedStart : Date.now()
    storage.setItem('obratech_session_start', String(sessionStart))
    const warnAfter = Math.max(0, sessionStart + 2 * 60 * 60 * 1000 - 5 * 60 * 1000 - Date.now())
    const timer = window.setTimeout(() => setSessionWarningVisible(true), warnAfter)
    return () => window.clearTimeout(timer)
  }, [token])

  const login = useCallback((userData, nextToken, rememberMe) => {
    setUser(userData)
    setToken(nextToken)
    localStorage.removeItem('obratech_user')
    localStorage.removeItem('obratech_token')
    localStorage.removeItem('obratech_session_start')
    sessionStorage.removeItem('obratech_user')
    sessionStorage.removeItem('obratech_token')
    sessionStorage.removeItem('obratech_session_start')
    setSessionWarningVisible(false)

    const storage = rememberMe ? localStorage : sessionStorage
    storage.setItem('obratech_user', JSON.stringify(userData))
    storage.setItem('obratech_token', nextToken)
    storage.setItem('obratech_session_start', String(Date.now()))
  }, [])

  const logout = useCallback(() => {
    if (token) {
      fetch('/logout', { method: 'POST', credentials: 'same-origin', keepalive: true }).catch(() => {})
    }
    setUser(null)
    setToken('')
    localStorage.removeItem('obratech_user')
    localStorage.removeItem('obratech_token')
    localStorage.removeItem('obratech_session_start')
    sessionStorage.removeItem('obratech_user')
    sessionStorage.removeItem('obratech_token')
    sessionStorage.removeItem('obratech_session_start')
    setSessionWarningVisible(false)
  }, [token])

  const selectRole = useCallback((role) => {
    if (!user?.roles?.includes(role)) return
    const nextUser = { ...user, role }
    setUser(nextUser)
    const storage = localStorage.getItem('obratech_token') ? localStorage : sessionStorage
    storage.setItem('obratech_user', JSON.stringify(nextUser))
  }, [user])

  const value = useMemo(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token),
      login,
      logout,
      selectRole,
    }),
    [user, token, login, logout, selectRole],
  )

  return (
    <AuthContext.Provider value={value}>
      {children}
      {sessionWarningVisible && <aside aria-live="polite" className="session-warning-toast" role="status">
        <span className="material-symbols-outlined" aria-hidden="true">timer</span>
        <div><strong>Tu sesión está por expirar</strong><p>Guarda tu trabajo. La sesión cierra en 5 minutos.</p></div>
        <button aria-label="Cerrar aviso" onClick={() => setSessionWarningVisible(false)} type="button"><span className="material-symbols-outlined" aria-hidden="true">close</span></button>
      </aside>}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }

  return context
}
