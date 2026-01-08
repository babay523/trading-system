import { defineStore } from 'pinia'
import { cartApi } from '@/api/cart'
import { useUserStore } from './user'

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [],
    total: 0
  }),
  
  getters: {
    itemCount: (state) => state.items.reduce((sum, item) => sum + item.quantity, 0)
  },
  
  actions: {
    async fetchCart() {
      const userStore = useUserStore()
      if (!userStore.user?.id) return
      
      try {
        const data = await cartApi.getCart(userStore.user.id)
        this.items = data.items || []
        this.total = data.totalAmount || 0
      } catch (e) {
        this.items = []
        this.total = 0
      }
    },
    
    async addItem(sku, quantity) {
      const userStore = useUserStore()
      if (!userStore.user?.id) return
      
      await cartApi.addItem(userStore.user.id, { sku, quantity })
      await this.fetchCart()
    },
    
    async updateQuantity(sku, quantity) {
      const userStore = useUserStore()
      if (!userStore.user?.id) return
      
      await cartApi.updateQuantity(userStore.user.id, sku, quantity)
      await this.fetchCart()
    },
    
    async removeItem(sku) {
      const userStore = useUserStore()
      if (!userStore.user?.id) return
      
      await cartApi.removeItem(userStore.user.id, sku)
      await this.fetchCart()
    },
    
    async clearCart() {
      const userStore = useUserStore()
      if (!userStore.user?.id) return
      
      await cartApi.clearCart(userStore.user.id)
      this.items = []
      this.total = 0
    }
  }
})
