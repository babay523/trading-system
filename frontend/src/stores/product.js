import { defineStore } from 'pinia'
import { productApi } from '@/api/product'

export const useProductStore = defineStore('product', {
  state: () => ({
    products: [],
    currentProduct: null,
    currentInventory: [],
    total: 0,
    loading: false
  }),
  
  actions: {
    async fetchProducts(params = {}) {
      this.loading = true
      try {
        const data = await productApi.getList(params)
        this.products = data.content || data || []
        this.total = data.totalElements || this.products.length
      } finally {
        this.loading = false
      }
    },
    
    async fetchProductById(id) {
      this.loading = true
      try {
        this.currentProduct = await productApi.getById(id)
      } finally {
        this.loading = false
      }
    },
    
    async fetchProductInventory(productId) {
      try {
        this.currentInventory = await productApi.getInventory(productId)
      } catch (e) {
        this.currentInventory = []
      }
    }
  }
})
