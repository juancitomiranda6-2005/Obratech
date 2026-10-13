import api from '../config/api.js'

export const participantProjectsService = {
  listAvailable: () => api.get('/participant/projects'),
  apply: (projectId, mensaje = '') => api.post(`/participant/projects/${projectId}/applications`, { mensaje }),
  listMyApplications: () => api.get('/participant/projects/applications'),
  decideOffer: (applicationId, estado) => api.patch(`/contractor/applications/${applicationId}/decision`, { estado }),
}