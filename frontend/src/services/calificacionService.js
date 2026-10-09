import api from '../config/api.js'

export const calificacionService = {
  list: async () => api.get('/calificaciones'),
  create: async (payload) => api.post('/calificaciones', payload),
  getClientRating: (projectId, contractorId) => api.get(`/client/ratings/${projectId}/${contractorId}`),
  getRatingForProject: (projectId) => api.get(`/client/ratings/project/${projectId}`),
  getClientRatingById: (ratingId) => api.get(`/client/ratings/by-id/${ratingId}`),
  saveClientRating: (projectId, contractorId, payload) => api.put(`/client/ratings/${projectId}/${contractorId}`, payload),
}
