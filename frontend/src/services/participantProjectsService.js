import api from '../config/api.js'

export const participantProjectsService = {
  listAvailable: () => api.get('/participant/projects'),
  getPhaseStatus: (projectId) => api.get(`/participant/projects/${projectId}/phase-status`),
  simulateFit: (projectId, contractorId) => api.get(`/projects/${projectId}/simulate-fit`, {
    params: contractorId ? { contratistaId: contractorId } : undefined,
  }),
  predictSelection: (projectId) => api.get(`/predicciones/proyectos/${projectId}/probabilidad-seleccion`),
  apply: (projectId, mensaje = '') => api.post(`/participant/projects/${projectId}/applications`, { mensaje }),
  listMyApplications: () => api.get('/participant/projects/applications'),
  decideOffer: (applicationId, estado) => api.patch(`/contractor/applications/${applicationId}/decision`, { estado }),
}