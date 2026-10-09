import api from '../config/api.js'

export const clientApplicationsService = {
  listForClient: () => api.get('/client/applications'),
  listForProject: (projectId) => api.get(`/client/projects/${projectId}/applications`),
  respond: (projectId, applicationId, estado) => api.patch(
    `/client/projects/${projectId}/applications/${applicationId}`,
    { estado },
  ),
  remove: (projectId, applicationId) => api.delete(`/client/projects/${projectId}/applications/${applicationId}`),
}

export const postulacionService = {
  list: async () => api.get('/postulaciones'),
  getById: async (id) => api.get(`/postulaciones/${id}`),
  create: async (payload) => api.post('/postulaciones', payload),
  update: async (id, payload) => api.put(`/postulaciones/${id}`, payload),
}
