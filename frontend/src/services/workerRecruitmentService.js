import api from '../config/api.js'

export const workerRecruitmentService = {
  listAvailable: (projectId = '') => api.get('/contractor/workers/available', { params: projectId ? { projectId } : {} }),
  invite: (workerId, proyectoId) => api.post(`/contractor/workers/${workerId}/invitations`, { proyectoId }),
}