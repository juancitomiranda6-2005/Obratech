import api from '../config/api.js'

export const adminService = {
  getDashboard: () => api.get('/admin/dashboard'),
  getProfile: (id) => api.get(`/admin/perfiles/${id}`),
  updateProjectValidation: (id, estado) => api.patch(`/admin/proyectos/${id}/validacion`, { estado }),
  updateProfileActive: (id, activo) => api.patch(`/admin/perfiles/${id}/activo`, { activo }),
  updateProfileVerified: (id, verificado) => api.patch(`/admin/perfiles/${id}/verificado`, { verificado }),
  deleteUser: (id) => api.delete(`/admin/usuarios/${id}`),
  createWorker: (payload) => api.post('/admin/trabajadores', payload),
}