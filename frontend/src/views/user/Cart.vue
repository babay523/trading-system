<template>
  <div class="cart-container">
    <el-card>
      <template #header>
        <div class="cart-header">
          <h2>购物车</h2>
          <el-button 
            v-if="cartStore.items.length > 0"
            type="danger" 
            text 
            @click="handleClearCart"
          >
            清空购物车
          </el-button>
        </div>
      </template>
      
      <div v-loading="loading">
        <el-empty v-if="cartStore.items.length === 0" description="购物车是空的">
          <el-button type="primary" @click="$router.push('/products')">
            去购物
          </el-button>
        </el-empty>
        
        <div v-else class="cart-content">
          <el-table :data="cartStore.items" style="width: 100%">
            <el-table-column label="商品" min-width="200">
              <template #default="{ row }">
                <div class="product-info">
                  <div class="product-icon">
                    <el-icon size="40" color="#909399"><Box /></el-icon>
                  </div>
                  <div class="product-detail">
                    <span class="sku">{{ row.sku }}</span>
                    <el-tag v-if="!row.available" type="danger" size="small">
                      已下架
                    </el-tag>
                  </div>
                </div>
              </template>
            </el-table-column>
            
            <el-table-column label="单价" width="120" align="center">
              <template #default="{ row }">
                <span class="price">¥{{ formatPrice(row.unitPrice) }}</span>
              </template>
            </el-table-column>
            
            <el-table-column label="数量" width="180" align="center">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.quantity"
                  :min="1"
                  :max="row.stockQuantity || 99"
                  :disabled="!row.available"
                  size="small"
                  @change="(val) => handleQuantityChange(row.sku, val)"
                />
                <div class="stock-info" v-if="row.stockQuantity">
                  库存: {{ row.stockQuantity }}
                </div>
              </template>
            </el-table-column>
            
            <el-table-column label="小计" width="120" align="center">
              <template #default="{ row }">
                <span class="subtotal">¥{{ formatPrice(row.subtotal) }}</span>
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button
                  type="danger"
                  text
                  @click="handleRemoveItem(row.sku)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <div class="cart-footer">
            <div class="cart-summary">
              <span class="item-count">共 {{ cartStore.itemCount }} 件商品</span>
              <span class="total-label">合计：</span>
              <span class="total-amount">¥{{ formatPrice(cartStore.total) }}</span>
            </div>
            <el-button
              type="primary"
              size="large"
              :disabled="!hasAvailableItems"
              @click="handleCheckout"
            >
              去结算
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Box } from '@element-plus/icons-vue'

const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const loading = ref(false)

const hasAvailableItems = computed(() => {
  return cartStore.items.some(item => item.available)
})

const formatPrice = (price) => {
  if (price === null || price === undefined) return '0.00'
  return Number(price).toFixed(2)
}

const fetchCart = async () => {
  if (!userStore.isLoggedIn) return
  
  loading.value = true
  try {
    await cartStore.fetchCart()
  } finally {
    loading.value = false
  }
}

const handleQuantityChange = async (sku, quantity) => {
  try {
    await cartStore.updateQuantity(sku, quantity)
  } catch (e) {
    ElMessage.error(e.message || '更新数量失败')
    await fetchCart()
  }
}

const handleRemoveItem = async (sku) => {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await cartStore.removeItem(sku)
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

const handleClearCart = async () => {
  try {
    await ElMessageBox.confirm('确定要清空购物车吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await cartStore.clearCart()
    ElMessage.success('购物车已清空')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '清空失败')
    }
  }
}

const handleCheckout = () => {
  router.push('/checkout')
}

onMounted(() => {
  fetchCart()
})
</script>

<style scoped>
.cart-container {
  max-width: 1000px;
  margin: 0 auto;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cart-header h2 {
  margin: 0;
  font-size: 20px;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-icon {
  width: 60px;
  height: 60px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sku {
  font-weight: 500;
}

.price {
  color: #606266;
}

.stock-info {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.subtotal {
  color: #f56c6c;
  font-weight: bold;
}

.cart-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 24px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #ebeef5;
}

.cart-summary {
  display: flex;
  align-items: center;
  gap: 16px;
}

.item-count {
  color: #909399;
}

.total-label {
  font-size: 16px;
}

.total-amount {
  font-size: 24px;
  font-weight: bold;
  color: #f56c6c;
}
</style>
