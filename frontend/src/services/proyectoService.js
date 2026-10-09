import api from '../config/api.js'

export const clientProjectsService = {
  listMine: () => api.get('/client/projects'),
  getMineById: (id) => api.get(`/client/projects/${id}`),
  getWorkspace: (id) => api.get(`/projects/${id}/workspace`),
  getLegalDocument: (id, download = false) => api.get(`/projects/${id}/document`, {
    params: { download },
    responseType: 'blob',
  }),
  updateMine: (id, payload) => api.put(`/client/projects/${id}`, payload),
  deleteMine: (id) => api.delete(`/client/projects/${id}`),
  createForCurrentClient: (payload, documentFile) => {
    if (!documentFile) return api.post('/client/projects', payload)

    const formData = new FormData()
    formData.append('project', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
    formData.append('documentoLegal', documentFile)
    return api.post('/client/projects/with-document', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

export const proyectoService = {
  list: async () => api.get('/proyectos'),
  getById: async (id) => api.get(`/proyectos/${id}`),
  create: async (payload) => api.post('/proyectos', payload),
  update: async (id, payload) => api.put(`/proyectos/${id}`, payload),
  remove: async (id) => api.delete(`/proyectos/${id}`),
  assign: async (proyectoId, perfilId) => api.post(`/proyectos/${proyectoId}/asignar/${perfilId}`),
}
