import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './styles/landing.css'
import './styles/auth.css'
import './styles/dashboard.css'
import './styles/projects.css'
import './styles/profiles.css'
import './styles/admin.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
