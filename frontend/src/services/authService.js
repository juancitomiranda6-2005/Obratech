import api from '../config/api.js'

export const authService = {
  login: async (credentials) => api.post('/auth/login', credentials),
  register: async (payload) => api.post('/auth/register', payload),
  completeRegistration: async (payload) => api.post('/auth/complete-registration', payload),
  exchangeOAuthSession: async () => api.get('/auth/oauth-session'),
  requestPasswordReset: async (email) => api.post('/auth/forgot-password', { email }, { skipAuth: true }),
  resetPassword: async (token, password) => api.post('/auth/reset-password', { token, password }, { skipAuth: true }),
}
