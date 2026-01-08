import { defineStore } from 'pinia'
import { orderApi } from '@/api/order'
import { useUserStore } from './user'

export const useOrderStore = defineStore('order', {
  state: () => ({
    orders: [],
    currentOrder: null,
    total: 0,
    loading: false
  }),
  
  actions: {
    async fetchOrders(params = {}) {
      const userStore = useUserStore()
      if (!userStore.user?.id) return
      
      this.loading = true
      try {
        const data = await orderApi.getByUser(userStore.user.id, params)
        this.orders = data.content || data || []
        this.total = data.totalElements || this.orders.length
      } finally {
        this.loading = false
      }
    },
    
    async fetchOrderById(id) {
      this.loading = true
      try {
        this.currentOrder = await orderApi.getById(id)
      } finally {
        this.loading = false
      }
    },
    
    async createFromCart() {
      const userStore = useUserStore()
      if (!userStore.user?.id) throw new Error('请先登录')
      return await orderApi.createFromCart(userStore.user.id)
    },
    
    async createDirect(data) {
      const userStore = useUserStore()
      if (!userStore.user?.id) throw new Error('请先登录')
      return await orderApi.createDirect(userStore.user.id, data)
    },
    
    async pay(orderId) {
      const result = await orderApi.pay(orderId)
      await this.fetchOrderById(orderId)
      return result
    },
    
    async ship(orderId) {
      const result = await orderApi.ship(orderId)
      await this.fetchOrderById(orderId)
      return result
    },
    
    async complete(orderId) {
      const result = await orderApi.complete(orderId)
      await this.fetchOrderById(orderId)
      return result
    },
    
    async cancel(orderId) {
      const result = await orderApi.cancel(orderId)
      await this.fetchOrderById(orderId)
      return result
    },
    
    async refund(orderId) {
      const result = await orderApi.refund(orderId)
      await this.fetchOrderById(orderId)
      return result
    }
  }
})
