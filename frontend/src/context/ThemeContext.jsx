import { useEffect, useState } from 'react'
import { ThemeContext } from './themeContext.js'

function getInitialTheme() {
  const savedTheme = window.localStorage.getItem('obratech_theme')
  return savedTheme === 'light' ? 'light' : 'dark'
}

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(getInitialTheme)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    window.localStorage.setItem('obratech_theme', theme)
    document.querySelector('meta[name="theme-color"]')?.setAttribute(
      'content',
      theme === 'dark' ? '#08111d' : '#f5f7fa',
    )
  }, [theme])

  const toggleTheme = () => {
    setTheme((currentTheme) => (currentTheme === 'dark' ? 'light' : 'dark'))
  }

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}
