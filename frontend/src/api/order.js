import api from './index'

export const orderApi = {
  createFromCart(userId) {
    return api.post(`/users/${userId}/orders/from-cart`)
  },
  
  createDirect(userId, data) {
    return api.post(`/users/${userId}/orders/direct`, data)
  },
  
  pay(orderId) {
    return api.post(`/orders/${orderId}/pay`)
  },
  
  ship(orderId) {
    return api.post(`/orders/${orderId}/ship`)
  },
  
  complete(orderId) {
    return api.post(`/orders/${orderId}/complete`)
  },
  
  cancel(orderId) {
    return api.post(`/orders/${orderId}/cancel`)
  },
  
  refund(orderId) {
    return api.post(`/orders/${orderId}/refund`)
  },
  
  getById(orderId) {
    return api.get(`/orders/${orderId}`)
  },
  
  getByUser(userId, params = {}) {
    return api.get(`/users/${userId}/orders`, { params })
  },
  
  getByMerchant(merchantId, params = {}) {
    return api.get(`/merchants/${merchantId}/orders`, { params })
  }
}
