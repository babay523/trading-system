import api from './index'

export const cartApi = {
  getCart(userId) {
    return api.get(`/users/${userId}/cart`)
  },
  
  addItem(userId, data) {
    return api.post(`/users/${userId}/cart/items`, data)
  },
  
  updateQuantity(userId, sku, quantity) {
    return api.put(`/users/${userId}/cart/items/${sku}`, { quantity })
  },
  
  removeItem(userId, sku) {
    return api.delete(`/users/${userId}/cart/items/${sku}`)
  },
  
  clearCart(userId) {
    return api.delete(`/users/${userId}/cart`)
  }
}
