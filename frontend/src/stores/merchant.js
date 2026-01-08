import { defineStore } from 'pinia'
import { merchantApi } from '@/api/merchant'

export const useMerchantStore = defineStore('merchant', {
  state: () => ({
    merchant: JSON.parse(localStorage.getItem('merchant')) || null,
    balance: 0,
    isLoggedIn: !!localStorage.getItem('merchant')
  }),
  
  actions: {
    async login(credentials) {
      const response = await merchantApi.login(credentials)
      // 保存JWT令牌
      localStorage.setItem('token', response.accessToken)
      // 保存商家信息
      this.merchant = response.merchant
      this.isLoggedIn = true
      localStorage.setItem('merchant', JSON.stringify(response.merchant))
      await this.fetchBalance()
      return response.merchant
    },
    
    async register(data) {
      const merchant = await merchantApi.register(data)
      return merchant
    },
    
    async fetchBalance() {
      if (!this.merchant?.id) return
      const data = await merchantApi.getBalance(this.merchant.id)
      this.balance = data.balance || data || 0
    },
    
    logout() {
      this.merchant = null
      this.balance = 0
      this.isLoggedIn = false
      localStorage.removeItem('merchant')
      localStorage.removeItem('token')
    }
  }
})
