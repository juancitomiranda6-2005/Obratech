import api from '../config/api.js'

export const teamService = {
  getContractorTeams: () => api.get('/contractor/teams'),
  getContractorOptions: () => api.get('/contractor/teams/options'),
  createContractorTeam: (projectId, payload) => api.post(`/contractor/projects/${projectId}/teams`, payload),
  deleteContractorTeam: (projectId, teamId) => api.delete(`/contractor/projects/${projectId}/teams/${teamId}`),
  addMemberToTeam: (projectId, teamId, memberId) => api.post(`/contractor/projects/${projectId}/teams/${teamId}/members`, { memberId }),
  removeMemberFromTeam: (projectId, teamId, memberId) => api.delete(`/contractor/projects/${projectId}/teams/${teamId}/members/${memberId}`),
  getWorkerTeams: () => api.get('/worker/teams'),
  updateWorkerProgress: (teamId, porcentajeAvance) => api.put(`/worker/teams/${teamId}/progress`, { porcentajeAvance }),
  createWorkerReport: (teamId, contenido, files) => {
    const formData = new FormData()
    formData.append('contenido', contenido)
    files.forEach((file) => formData.append('evidencias', file))
    return api.post(`/worker/teams/${teamId}/progress-reports`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  getWorkerReports: (teamId) => api.get(`/worker/teams/${teamId}/progress-reports`),
  updateWorkerReport: (teamId, reportId, contenido) => api.put(
    `/worker/teams/${teamId}/progress-reports/${reportId}`,
    { contenido },
  ),
  getProjectProgressReports: (projectId) => api.get(`/contractor/projects/${projectId}/progress-reports`),
}