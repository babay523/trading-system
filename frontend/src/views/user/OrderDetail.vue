<template>
  <div class="order-detail-container">
    <el-card v-loading="orderStore.loading">
      <template #header>
        <div class="header">
          <el-button text @click="$router.back()">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <h2>订单详情</h2>
        </div>
      </template>
      
      <div v-if="order" class="order-content">
        <div class="order-status-section">
          <el-tag :type="getStatusType(order.status)" size="large">
            {{ getStatusText(order.status) }}
          </el-tag>
          <span class="order-number">订单号：{{ order.orderNumber }}</span>
        </div>
        
        <el-divider />
        
        <div class="order-info-section">
          <h3>订单信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="创建时间">
              {{ formatTime(order.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ formatTime(order.updatedAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="商家ID">
              {{ order.merchantId }}
            </el-descriptions-item>
            <el-descriptions-item label="订单状态">
              {{ getStatusText(order.status) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
        
        <el-divider />
        
        <div class="order-items-section">
          <h3>商品清单</h3>
          <el-table :data="order.items" style="width: 100%">
            <el-table-column label="商品" min-width="200">
              <template #default="{ row }">
                <div class="item-info">
                  <div class="item-icon">
                    <el-icon size="32" color="#909399"><Box /></el-icon>
                  </div>
                  <div class="item-detail">
                    <span class="item-name">{{ row.productName || row.sku }}</span>
                    <span class="item-sku">SKU: {{ row.sku }}</span>
                  </div>
                </div>
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
        
        <div class="order-total">
          <span>订单总额：</span>
          <span class="amount">¥{{ formatPrice(order.totalAmount) }}</span>
        </div>
        
        <el-divider />
        
        <div class="order-actions">
          <el-button
            v-if="order.status === 'PENDING'"
            type="primary"
            @click="handlePay"
            :loading="actionLoading"
          >
            立即支付
          </el-button>
          <el-button
            v-if="order.status === 'PENDING'"
            @click="handleCancel"
            :loading="actionLoading"
          >
            取消订单
          </el-button>
          <el-button
            v-if="order.status === 'SHIPPED'"
            type="primary"
            @click="handleComplete"
            :loading="actionLoading"
          >
            确认收货
          </el-button>
          <el-button
            v-if="order.status === 'PAID' || order.status === 'SHIPPED'"
            type="danger"
            @click="handleRefund"
            :loading="actionLoading"
          >
            申请退款
          </el-button>
        </div>
      </div>
      
      <el-empty v-else-if="!orderStore.loading" description="订单不存在" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Box } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()
const userStore = useUserStore()

const actionLoading = ref(false)

const order = computed(() => orderStore.currentOrder)

const statusMap = {
  PENDING: { text: '待支付', type: 'warning' },
  PAID: { text: '已支付', type: 'primary' },
  SHIPPED: { text: '已发货', type: '' },
  COMPLETED: { text: '已完成', type: 'success' },
  CANCELLED: { text: '已取消', type: 'info' },
  REFUNDED: { text: '已退款', type: 'danger' }
}

const getStatusText = (status) => statusMap[status]?.text || status
const getStatusType = (status) => statusMap[status]?.type || ''

const formatPrice = (price) => {
  if (price === null || price === undefined) return '0.00'
  return Number(price).toFixed(2)
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const fetchOrder = async () => {
  const orderId = route.params.id
  await orderStore.fetchOrderById(orderId)
}

const handlePay = async () => {
  try {
    await ElMessageBox.confirm(
      `确认支付 ¥${formatPrice(order.value.totalAmount)} 吗？`,
      '确认支付',
      {
        confirmButtonText: '确认支付',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    actionLoading.value = true
    await orderStore.pay(order.value.id)
    await userStore.fetchBalance()
    ElMessage.success('支付成功')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '支付失败')
    }
  } finally {
    actionLoading.value = false
  }
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定要取消订单吗？', '取消订单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    actionLoading.value = true
    await orderStore.cancel(order.value.id)
    ElMessage.success('订单已取消')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '取消失败')
    }
  } finally {
    actionLoading.value = false
  }
}

const handleComplete = async () => {
  try {
    await ElMessageBox.confirm('确认已收到商品吗？', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '取消',
      type: 'info'
    })
    
    actionLoading.value = true
    await orderStore.complete(order.value.id)
    ElMessage.success('已确认收货')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  } finally {
    actionLoading.value = false
  }
}

const handleRefund = async () => {
  try {
    await ElMessageBox.confirm('确定要申请退款吗？', '申请退款', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    actionLoading.value = true
    await orderStore.refund(order.value.id)
    await userStore.fetchBalance()
    ElMessage.success('退款成功')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '退款失败')
    }
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => {
  fetchOrder()
})
</script>

<style scoped>
.order-detail-container {
  max-width: 900px;
  margin: 0 auto;
}

.header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header h2 {
  margin: 0;
  font-size: 20px;
}

h3 {
  font-size: 16px;
  margin: 0 0 16px;
}

.order-status-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.order-number {
  color: #909399;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-icon {
  width: 48px;
  height: 48px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-name {
  font-weight: 500;
}

.item-sku {
  font-size: 12px;
  color: #909399;
}

.subtotal {
  color: #f56c6c;
  font-weight: bold;
}

.order-total {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  font-size: 16px;
}

.order-total .amount {
  font-size: 24px;
  font-weight: bold;
  color: #f56c6c;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
