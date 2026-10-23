import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  if (config.skipAuth) {
    delete config.headers.Authorization
    return config
  }

  const token = localStorage.getItem('obratech_token') || sessionStorage.getItem('obratech_token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      const url = error.config?.url || ''
      const isAuthEndpoint =
        url.includes('/auth/login') ||
        url.includes('/auth/register') ||
        url.includes('/auth/oauth-session')

      if (!isAuthEndpoint) {
        localStorage.removeItem('obratech_token')
        localStorage.removeItem('obratech_user')
        localStorage.removeItem('obratech_session_start')
        sessionStorage.removeItem('obratech_token')
        sessionStorage.removeItem('obratech_user')
        sessionStorage.removeItem('obratech_session_start')

        if (typeof window !== 'undefined' && window.location.pathname !== '/login' && window.location.pathname !== '/') {
          window.location.href = '/login?expired=true'
        }
      }
    }
    return Promise.reject(error)
  },
)

export default api
