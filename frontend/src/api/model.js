import request from '@/utils/request'

export const modelApi = {
  getModelList(params) {
    return request.get('/model/list', { params })
  },

  getModelConfig() {
    return request.get('/model/config', { customHandleBusinessError: true })
  },

  saveModelConfig(config) {
    return request.post('/model/config', config)
  },

  getModelProviders(params) {
    return request.get('/model/providers', { params })
  },

  getModelGroups(params) {
    return request.get('/model/groups', { params })
  }
}
