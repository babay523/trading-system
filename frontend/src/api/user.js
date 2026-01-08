import api from './index'

export const userApi = {
  register(data) {
    return api.post('/users/register', data)
  },
  
  login(data) {
    return api.post('/users/login', data)
  },
  
  getBalance(userId) {
    return api.get(`/users/${userId}/balance`)
  },
  
  deposit(userId, amount) {
    return api.post(`/users/${userId}/deposit`, { amount })
  },
  
  getTransactions(userId, params) {
    return api.get(`/users/${userId}/transactions`, { params })
  }
}
