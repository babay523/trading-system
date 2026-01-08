<template>
  <div class="product-detail-container">
    <el-card v-loading="productStore.loading">
      <el-row :gutter="40" v-if="product">
        <el-col :span="10">
          <div class="product-image">
            <el-icon size="200" color="#909399"><Box /></el-icon>
          </div>
        </el-col>
        
        <el-col :span="14">
          <div class="product-info">
            <h1 class="product-title">{{ product.name }}</h1>
            <div class="product-meta">
              <el-tag>{{ product.category || '其他' }}</el-tag>
              <span class="merchant-info">
                <el-icon><Shop /></el-icon>
                商家ID: {{ product.merchantId }}
              </span>
            </div>
            
            <el-divider />
            
            <div class="product-description">
              <h3>商品描述</h3>
              <p>{{ product.description || '暂无描述' }}</p>
            </div>
            
            <el-divider />
            
            <div class="sku-section">
              <h3>选择规格</h3>
              <el-radio-group v-model="selectedSku" v-if="skus.length > 0">
                <el-radio
                  v-for="sku in skus"
                  :key="sku.sku"
                  :label="sku.sku"
                  :disabled="!sku.inStock"
                  border
                  class="sku-radio"
                >
                  <div class="sku-option">
                    <span class="sku-name">{{ sku.sku }}</span>
                    <span class="sku-price">¥{{ formatPrice(sku.price) }}</span>
                    <span class="sku-stock" :class="{ 'out-of-stock': !sku.inStock }">
                      {{ sku.inStock ? `库存: ${sku.quantity}` : '缺货' }}
                    </span>
                  </div>
                </el-radio>
              </el-radio-group>
              <el-empty v-else description="暂无可用规格" :image-size="100" />
            </div>
            
            <div class="quantity-section" v-if="selectedSku">
              <span class="label">数量：</span>
              <el-input-number
                v-model="quantity"
                :min="1"
                :max="currentSku?.quantity || 1"
              />
              <span class="stock-tip">库存: {{ currentSku?.quantity || 0 }}</span>
            </div>
            
            <div class="price-section" v-if="selectedSku">
              <span class="label">总价：</span>
              <span class="total-price">¥{{ formatPrice(totalPrice) }}</span>
            </div>
            
            <div class="action-buttons">
              <el-button
                type="primary"
                size="large"
                :disabled="!selectedSku || addingToCart"
                :loading="addingToCart"
                @click="handleAddToCart"
              >
                <el-icon v-if="!addingToCart"><ShoppingCart /></el-icon>
                加入购物车
              </el-button>
              <el-button
                type="danger"
                size="large"
                :disabled="!selectedSku"
                @click="handleBuyNow"
              >
                立即购买
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>
      
      <el-empty v-else-if="!productStore.loading" description="商品不存在" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProductStore } from '@/stores/product'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Box, Shop, ShoppingCart } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const productStore = useProductStore()
const cartStore = useCartStore()
const userStore = useUserStore()

const selectedSku = ref('')
const quantity = ref(1)
const addingToCart = ref(false)

const product = computed(() => productStore.currentProduct)
const skus = computed(() => productStore.currentInventory || [])

const currentSku = computed(() => {
  return skus.value.find(s => s.sku === selectedSku.value)
})

const totalPrice = computed(() => {
  if (!currentSku.value) return 0
  return currentSku.value.price * quantity.value
})

const formatPrice = (price) => {
  if (price === null || price === undefined) return '0.00'
  return Number(price).toFixed(2)
}

const fetchProductDetail = async () => {
  const productId = route.params.id
  await productStore.fetchProductById(productId)
  await productStore.fetchProductInventory(productId)
  
  // Auto-select first available SKU
  if (skus.value.length > 0) {
    const availableSku = skus.value.find(s => s.inStock)
    if (availableSku) {
      selectedSku.value = availableSku.sku
    }
  }
}

// Reset quantity when SKU changes
watch(selectedSku, () => {
  quantity.value = 1
})

const handleAddToCart = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  if (!selectedSku.value) {
    ElMessage.warning('请选择商品规格')
    return
  }
  
  addingToCart.value = true
  try {
    await cartStore.addItem(selectedSku.value, quantity.value)
    ElMessage.success('已加入购物车')
  } catch (e) {
    ElMessage.error(e.message || '加入购物车失败')
  } finally {
    addingToCart.value = false
  }
}

const handleBuyNow = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  if (!selectedSku.value) {
    ElMessage.warning('请选择商品规格')
    return
  }
  
  addingToCart.value = true
  try {
    await cartStore.addItem(selectedSku.value, quantity.value)
    router.push('/cart')
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    addingToCart.value = false
  }
}

onMounted(() => {
  fetchProductDetail()
})
</script>

<style scoped>
.product-detail-container {
  max-width: 1200px;
  margin: 0 auto;
}

.product-image {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 8px;
}

.product-info {
  padding: 20px 0;
}

.product-title {
  font-size: 24px;
  margin: 0 0 16px;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #909399;
}

.merchant-info {
  display: flex;
  align-items: center;
  gap: 4px;
}

.product-description h3 {
  font-size: 16px;
  margin: 0 0 12px;
}

.product-description p {
  color: #606266;
  line-height: 1.6;
}

.sku-section h3 {
  font-size: 16px;
  margin: 0 0 16px;
}

.sku-radio {
  margin: 8px 12px 8px 0;
  height: auto;
  padding: 12px 16px;
}

.sku-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sku-name {
  font-weight: 500;
}

.sku-price {
  color: #f56c6c;
  font-weight: bold;
}

.sku-stock {
  font-size: 12px;
  color: #67c23a;
}

.sku-stock.out-of-stock {
  color: #909399;
}

.quantity-section,
.price-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 20px 0;
}

.label {
  font-size: 16px;
  color: #606266;
}

.stock-tip {
  font-size: 14px;
  color: #909399;
}

.total-price {
  font-size: 28px;
  font-weight: bold;
  color: #f56c6c;
}

.action-buttons {
  display: flex;
  gap: 16px;
  margin-top: 32px;
}

.action-buttons .el-button {
  flex: 1;
}
</style>
