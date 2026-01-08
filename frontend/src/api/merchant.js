import api from './index'

export const merchantApi = {
  register(data) {
    return api.post('/merchants/register', data)
  },
  
  login(data) {
    return api.post('/merchants/login', data)
  },
  
  getBalance(merchantId) {
    return api.get(`/merchants/${merchantId}/balance`)
  },
  
  getStats(merchantId) {
    return api.get(`/merchants/${merchantId}/stats`)
  },
  
  getInventory(merchantId, params) {
    return api.get(`/merchants/${merchantId}/inventory`, { params })
  },
  
  addInventory(merchantId, data) {
    return api.post(`/merchants/${merchantId}/inventory`, data)
  },
  
  updatePrice(merchantId, sku, price) {
    return api.put(`/merchants/${merchantId}/inventory/${sku}/price`, { newPrice: price })
  },
  
  getSettlements(merchantId, params) {
    return api.get(`/merchants/${merchantId}/settlements`, { params })
  }
}
