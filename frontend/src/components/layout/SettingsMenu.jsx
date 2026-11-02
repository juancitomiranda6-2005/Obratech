import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth.js'
import { useTheme } from '../../context/themeContext.js'

const profilePaths = {
  ROLE_CLIENT: '/perfil-cliente',
  ROLE_CONTRACTOR: '/perfil-contratista',
  ROLE_WORKER: '/perfil-trabajador',
}

export function SettingsMenu({ inline = false, triggerContent, triggerLabel = 'Configuración' }) {
  const { user, isAuthenticated, logout } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const [isOpen, setIsOpen] = useState(false)
  const menuRef = useRef(null)
  const profilePath = profilePaths[user?.role]
  const panelId = inline ? 'settings-panel-inline' : 'settings-panel-floating'

  useEffect(() => {
    if (!isOpen) return undefined

    const closeOnOutsideClick = (event) => {
      if (!menuRef.current?.contains(event.target)) setIsOpen(false)
    }
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') setIsOpen(false)
    }

    document.addEventListener('pointerdown', closeOnOutsideClick)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsideClick)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [isOpen])

  if (!isAuthenticated) return null

  return (
    <div className={`settings-menu${inline ? ' settings-menu-inline' : ''}`} ref={menuRef}>
      <button
        aria-controls={panelId}
        aria-expanded={isOpen}
        aria-label={triggerLabel}
        className={`settings-trigger${triggerContent ? ' settings-account-trigger' : ''}`}
        onClick={() => setIsOpen((open) => !open)}
        type="button"
      >
        {triggerContent || (
          <>
            <span className="material-symbols-outlined" aria-hidden="true">settings</span>
            {inline && <span>Configuración</span>}
          </>
        )}
        {triggerContent && <span className="material-symbols-outlined settings-account-chevron" aria-hidden="true">expand_less</span>}
      </button>

      {isOpen && (
        <section aria-label="Configuración de cuenta" className="settings-panel" id={panelId}>
          <p className="settings-heading">Configuración</p>
          {profilePath && (
            <Link
              className="settings-item"
              onClick={() => setIsOpen(false)}
              to={profilePath}
            >
              <span className="material-symbols-outlined" aria-hidden="true">person</span>
              Mi perfil
            </Link>
          )}
          <button
            aria-pressed={theme === 'dark'}
            className="settings-item"
            onClick={toggleTheme}
            type="button"
          >
            <span className="material-symbols-outlined" aria-hidden="true">
              {theme === 'dark' ? 'light_mode' : 'dark_mode'}
            </span>
            {theme === 'dark' ? 'Modo claro' : 'Modo oscuro'}
          </button>
          <button
            className="settings-item settings-logout"
            onClick={() => {
              setIsOpen(false)
              logout()
            }}
            type="button"
          >
            <span className="material-symbols-outlined" aria-hidden="true">logout</span>
            Cerrar sesión
          </button>
        </section>
      )}
    </div>
  )
}
