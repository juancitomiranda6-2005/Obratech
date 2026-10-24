import { useEffect, useState } from 'react'

function getInitialTheme() {
  const savedTheme = window.localStorage.getItem('obratech_theme')
  return savedTheme === 'light' ? 'light' : 'dark'
}

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(getInitialTheme)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    window.localStorage.setItem('obratech_theme', theme)
  }, [theme])

  const toggleTheme = () => {
    setTheme((currentTheme) => (currentTheme === 'dark' ? 'light' : 'dark'))
  }

  return (
    <>
      {children}
      <button
        aria-label={`Cambiar a modo ${theme === 'dark' ? 'claro' : 'oscuro'}`}
        aria-pressed={theme === 'dark'}
        className="theme-toggle"
        onClick={toggleTheme}
        type="button"
      >
        <span className="material-symbols-outlined" aria-hidden="true">
          {theme === 'dark' ? 'light_mode' : 'dark_mode'}
        </span>
        <span>{theme === 'dark' ? 'Modo claro' : 'Modo oscuro'}</span>
      </button>
    </>
  )
}
