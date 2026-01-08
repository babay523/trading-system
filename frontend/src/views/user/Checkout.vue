<template>
  <div class="checkout-container">
    <el-card>
      <template #header>
        <h2>确认订单</h2>
      </template>
      
      <div v-loading="loading">
        <el-empty v-if="cartStore.items.length === 0" description="购物车是空的">
          <el-button type="primary" @click="$router.push('/products')">
            去购物
          </el-button>
        </el-empty>
        
        <div v-else class="checkout-content">
          <div class="order-items">
            <h3>商品清单</h3>
            <el-table :data="availableItems" style="width: 100%">
              <el-table-column label="商品" min-width="200">
                <template #default="{ row }">
                  <span class="sku">{{ row.sku }}</span>
                </template>
              </el-table-column>
              <el-table-column label="单价" width="120" align="center">
                <template #default="{ row }">
                  ¥{{ formatPrice(row.unitPrice) }}
                </template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="100" align="center" />
              <el-table-column label="小计" width="120" align="center">
                <template #default="{ row }">
                  <span class="subtotal">¥{{ formatPrice(row.subtotal) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
          
          <el-divider />
          
          <div class="order-summary">
            <div class="summary-row">
              <span>商品数量：</span>
              <span>{{ totalQuantity }} 件</span>
            </div>
            <div class="summary-row">
              <span>商品金额：</span>
              <span>¥{{ formatPrice(totalAmount) }}</span>
            </div>
            <div class="summary-row total">
              <span>应付金额：</span>
              <span class="amount">¥{{ formatPrice(totalAmount) }}</span>
            </div>
          </div>
          
          <div class="balance-info">
            <el-icon><Wallet /></el-icon>
            <span>账户余额：¥{{ formatPrice(userStore.balance) }}</span>
            <el-tag v-if="userStore.balance < totalAmount" type="danger" size="small">
              余额不足
            </el-tag>
          </div>
          
          <div class="checkout-actions">
            <el-button @click="$router.back()">返回购物车</el-button>
            <el-button
              type="primary"
              size="large"
              :disabled="submitting || userStore.balance < totalAmount"
              :loading="submitting"
              @click="handleSubmitOrder"
            >
              提交订单
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
import { useOrderStore } from '@/stores/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Wallet } from '@element-plus/icons-vue'

const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()
const orderStore = useOrderStore()

const loading = ref(false)
const submitting = ref(false)

const availableItems = computed(() => {
  return cartStore.items.filter(item => item.available)
})

const totalQuantity = computed(() => {
  return availableItems.value.reduce((sum, item) => sum + item.quantity, 0)
})

const totalAmount = computed(() => {
  return availableItems.value.reduce((sum, item) => sum + Number(item.subtotal), 0)
})

const formatPrice = (price) => {
  if (price === null || price === undefined) return '0.00'
  return Number(price).toFixed(2)
}

const fetchData = async () => {
  loading.value = true
  try {
    await Promise.all([
      cartStore.fetchCart(),
      userStore.fetchBalance()
    ])
  } finally {
    loading.value = false
  }
}

const handleSubmitOrder = async () => {
  if (userStore.balance < totalAmount.value) {
    ElMessage.warning('余额不足，请先充值')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确认支付 ¥${formatPrice(totalAmount.value)} 吗？`,
      '确认订单',
      {
        confirmButtonText: '确认支付',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    submitting.value = true
    
    // Create order from cart
    const order = await orderStore.createFromCart()
    
    // Pay for the order
    await orderStore.pay(order.id)
    
    // Refresh cart and balance
    await Promise.all([
      cartStore.fetchCart(),
      userStore.fetchBalance()
    ])
    
    ElMessage.success('订单支付成功')
    router.push(`/orders/${order.id}`)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '下单失败')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.checkout-container {
  max-width: 800px;
  margin: 0 auto;
}

h2 {
  margin: 0;
  font-size: 20px;
}

h3 {
  font-size: 16px;
  margin: 0 0 16px;
}

.sku {
  font-weight: 500;
}

.subtotal {
  color: #f56c6c;
  font-weight: bold;
}

.order-summary {
  padding: 16px 0;
}

.summary-row {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 8px 0;
  font-size: 14px;
}

.summary-row.total {
  font-size: 18px;
  font-weight: bold;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.summary-row .amount {
  color: #f56c6c;
  font-size: 24px;
}

.balance-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  margin: 16px 0;
}

.checkout-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 24px;
}
</style>
