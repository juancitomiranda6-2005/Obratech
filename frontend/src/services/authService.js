import api from '../config/api.js'

export function rolesFromToken(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const claims = JSON.parse(atob(payload))
    return Array.isArray(claims.roles) ? claims.roles : []
  } catch {
    return []
  }
}

export function establishSession(data, username, rememberMe, login, navigate) {
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

export const authService = {
  login: async (credentials) => api.post('/auth/login', credentials),
  register: async (payload) => api.post('/auth/register', payload),
  completeRegistration: async (payload) => api.post('/auth/complete-registration', payload),
  exchangeOAuthSession: async () => api.get('/auth/oauth-session'),
  requestPasswordReset: async (email) => api.post('/auth/forgot-password', { email }, { skipAuth: true }),
  resetPassword: async (token, password) => api.post('/auth/reset-password', { token, password }, { skipAuth: true }),
}
