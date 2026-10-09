import api from '../config/api.js'

export const clientProfileService = {
  getMine: () => api.get('/client/projects/profile'),
  updateMine: (payload) => api.put('/client/projects/profile', payload),
}

export const contractorProfileService = {
  getMine: () => api.get('/contractor/profile'),
  updateMine: (profile, files) => {
    const formData = new FormData()
    formData.append('profile', new Blob([JSON.stringify(profile)], { type: 'application/json' }))
    for (const [field, file] of Object.entries(files)) {
      if (file) formData.append(field, file)
    }
    return api.put('/contractor/profile', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  getFile: (id) => api.get(`/contractor/profile/files/${id}`, { responseType: 'blob' }),
}

export const workerProfileService = {
  getMine: () => api.get('/worker/profile'),
  updateMine: (payload, cvFile) => {
    const formData = new FormData()
    formData.append('profile', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
    if (cvFile) formData.append('cvFile', cvFile)
    return api.put('/worker/profile', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  getCv: (id) => api.get(`/worker/profile/files/${id}`, { responseType: 'blob' }),
}

export const perfilService = {
  list: async () => api.get('/perfiles'),
  getById: async (id) => api.get(`/perfiles/${id}`),
  create: async (payload) => api.post('/perfiles', payload),
  update: async (id, payload) => api.put(`/perfiles/${id}`, payload),
  remove: async (id) => api.delete(`/perfiles/${id}`),
}

export const publicProfileService = {
  getById: (id) => api.get(`/public/profiles/${id}`),
}

export const profileDirectoryService = {
  listContractors: (especialidad = '') => api.get('/directory/contractors', { params: especialidad ? { especialidad } : {} }),
  listClients: () => api.get('/directory/clients'),
  listWorkers: (available = false) => api.get('/directory/workers', { params: { available } }),
}
