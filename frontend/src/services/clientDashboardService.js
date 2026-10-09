import api from '../config/api.js'

export const clientDashboardService = {
  getDashboard: () => api.get('/dashboard/cliente'),
  getReports: () => api.get('/dashboard/cliente/reportes'),
  getContractors: () => api.get('/dashboard/cliente/contratistas'),
}