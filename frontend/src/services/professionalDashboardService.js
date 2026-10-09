import api from '../config/api.js'

export const contractorDashboardService = {
  getDashboard: () => api.get('/dashboard/contratista'),
  getHistory: () => api.get('/dashboard/contratista/historial'),
  getReports: () => api.get('/contractor/reports'),
  getWorkerReports: (projectId) => api.get(`/contractor/projects/${projectId}/progress-reports`),
  createReport: (proyectoId, contenido, reportesTrabajadorIds = []) => api.post('/contractor/reports', {
    proyectoId,
    contenido,
    reportesTrabajadorIds,
  }),
  updateReport: (reportId, contenido) => api.put(`/contractor/reports/${reportId}`, { contenido }),
}

export const workerDashboardService = {
  getDashboard: () => api.get('/dashboard/trabajador'),
  respondToInvitation: (id, estado) => api.patch(`/dashboard/trabajador/invitaciones/${id}`, { estado }),
}