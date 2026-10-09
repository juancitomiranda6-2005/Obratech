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

export default api
