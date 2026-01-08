<template>
  <div class="orders-container">
    <el-card>
      <template #header>
        <h2>我的订单</h2>
      </template>
      
      <div v-loading="orderStore.loading">
        <el-empty v-if="orderStore.orders.length === 0" description="暂无订单">
          <el-button type="primary" @click="$router.push('/products')">
            去购物
          </el-button>
        </el-empty>
        
        <div v-else class="orders-list">
          <el-card
            v-for="order in orderStore.orders"
            :key="order.id"
            class="order-card"
            shadow="hover"
            @click="goToDetail(order.id)"
          >
            <div class="order-header">
              <div class="order-info">
                <span class="order-number">订单号：{{ order.orderNumber }}</span>
                <span class="order-time">{{ formatTime(order.createdAt) }}</span>
              </div>
              <el-tag :type="getStatusType(order.status)">
                {{ getStatusText(order.status) }}
              </el-tag>
            </div>
            
            <el-divider />
            
            <div class="order-items">
              <div
                v-for="item in order.items?.slice(0, 3)"
                :key="item.id"
                class="order-item"
              >
                <div class="item-icon">
                  <el-icon size="32" color="#909399"><Box /></el-icon>
                </div>
                <div class="item-info">
                  <span class="item-name">{{ item.productName || item.sku }}</span>
                  <span class="item-sku">{{ item.sku }}</span>
                </div>
                <div class="item-price">
                  <span>¥{{ formatPrice(item.unitPrice) }} × {{ item.quantity }}</span>
                </div>
              </div>
              <div v-if="order.items?.length > 3" class="more-items">
                还有 {{ order.items.length - 3 }} 件商品...
              </div>
            </div>
            
            <div class="order-footer">
              <span class="total-label">共 {{ getTotalQuantity(order) }} 件商品</span>
              <span class="total-amount">
                合计：<em>¥{{ formatPrice(order.totalAmount) }}</em>
              </span>
            </div>
          </el-card>
          
          <el-pagination
            v-if="orderStore.total > pageSize"
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="orderStore.total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'
import { Box } from '@element-plus/icons-vue'

const router = useRouter()
const orderStore = useOrderStore()

const currentPage = ref(1)
const pageSize = 10

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

const getTotalQuantity = (order) => {
  return order.items?.reduce((sum, item) => sum + item.quantity, 0) || 0
}

const fetchOrders = async () => {
  await orderStore.fetchOrders({
    page: currentPage.value - 1,
    size: pageSize
  })
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchOrders()
}

const goToDetail = (orderId) => {
  router.push(`/orders/${orderId}`)
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.orders-container {
  max-width: 900px;
  margin: 0 auto;
}

h2 {
  margin: 0;
  font-size: 20px;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.order-card:hover {
  transform: translateY(-2px);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-number {
  font-weight: 500;
}

.order-time {
  font-size: 12px;
  color: #909399;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-item {
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

.item-info {
  flex: 1;
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

.item-price {
  color: #606266;
}

.more-items {
  font-size: 12px;
  color: #909399;
  padding-left: 60px;
}

.order-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.total-label {
  color: #909399;
}

.total-amount em {
  font-style: normal;
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

.el-pagination {
  justify-content: center;
  margin-top: 24px;
}
</style>
