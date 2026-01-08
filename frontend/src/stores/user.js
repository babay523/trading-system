import { defineStore } from 'pinia'
import { userApi } from '@/api/user'
import { useCartStore } from './cart'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null,
    balance: 0,
    isLoggedIn: !!localStorage.getItem('user')
  }),
  
  actions: {
    async login(credentials) {
      const user = await userApi.login(credentials)
      // 保存JWT令牌
      if (user.token) {
        localStorage.setItem('token', user.token)
      }
      this.user = user
      this.isLoggedIn = true
      localStorage.setItem('user', JSON.stringify(user))
      await this.fetchBalance()
      // Fetch cart after login
      const cartStore = useCartStore()
      await cartStore.fetchCart()
      return user
    },
    
    async register(data) {
      const user = await userApi.register(data)
      return user
    },
    
    async fetchBalance() {
      if (!this.user?.id) return
      try {
        const data = await userApi.getBalance(this.user.id)
        this.balance = data.balance || data || 0
      } catch (error) {
        console.error('获取余额失败:', error)
        // 不抛出错误，避免影响页面加载
        this.balance = 0
      }
    },
    
    async deposit(amount) {
      if (!this.user?.id) return
      await userApi.deposit(this.user.id, amount)
      await this.fetchBalance()
    },
    
    logout() {
      this.user = null
      this.balance = 0
      this.isLoggedIn = false
      localStorage.removeItem('user')
      localStorage.removeItem('token') // 清除JWT令牌
      // Clear cart on logout
      const cartStore = useCartStore()
      cartStore.items = []
      cartStore.total = 0
    },
    
    // Initialize user state on app load
    async initializeUser() {
      if (this.isLoggedIn && this.user?.id) {
        try {
          await this.fetchBalance()
          const cartStore = useCartStore()
          await cartStore.fetchCart()
        } catch (error) {
          console.error('初始化用户信息失败:', error)
          // 如果初始化失败，清除登录状态
          this.logout()
        }
      }
    }
  }
})
