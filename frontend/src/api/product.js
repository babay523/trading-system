import api from './index'

export const productApi = {
  create(data) {
    return api.post('/products', data)
  },
  
  getList(params) {
    return api.get('/products', { params })
  },
  
  getById(id) {
    return api.get(`/products/${id}`)
  },
  
  getInventory(productId) {
    return api.get(`/products/${productId}/inventory`)
  }
}
