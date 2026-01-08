<template>
  <div class="dashboard-container">
    <h2>商家仪表盘</h2>
    
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon size="48" color="#409eff"><Wallet /></el-icon>
            <div class="stat-info">
              <span class="stat-label">账户余额</span>
              <span class="stat-value">¥{{ formatPrice(merchantStore.balance) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon size="48" color="#67c23a"><Box /></el-icon>
            <div class="stat-info">
              <span class="stat-label">商品数量</span>
              <span class="stat-value">{{ stats.productCount }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon size="48" color="#e6a23c"><Document /></el-icon>
            <div class="stat-info">
              <span class="stat-label">待处理订单</span>
              <span class="stat-value">{{ stats.pendingOrders }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/merchant/products')">
              <el-icon><Plus /></el-icon>
              添加商品
            </el-button>
            <el-button @click="$router.push('/merchant/inventory')">
              <el-icon><Box /></el-icon>
              管理库存
            </el-button>
            <el-button @click="$router.push('/merchant/orders')">
              <el-icon><Document /></el-icon>
              查看订单
            </el-button>
            <el-button @click="$router.push('/merchant/settlements')">
              <el-icon><Money /></el-icon>
              结算记录
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>商家信息</span>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="商家ID">
              {{ merchantStore.merchant?.id }}
            </el-descriptions-item>
            <el-descriptions-item label="用户名">
              {{ merchantStore.merchant?.username }}
            </el-descriptions-item>
            <el-descriptions-item label="店铺名称">
              {{ merchantStore.merchant?.shopName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="账户余额">
              ¥{{ formatPrice(merchantStore.balance) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useMerchantStore } from '@/stores/merchant'
import { merchantApi } from '@/api/merchant'
import { Wallet, Box, Document, Plus, Money } from '@element-plus/icons-vue'

const merchantStore = useMerchantStore()

const stats = reactive({
  productCount: 0,
  pendingOrders: 0
})

const formatPrice = (price) => {
  if (price === null || price === undefined) return '0.00'
  return Number(price).toFixed(2)
}

const fetchStats = async () => {
  if (!merchantStore.merchant?.id) return
  
  try {
    const data = await merchantApi.getStats(merchantStore.merchant.id)
    stats.productCount = data.productCount || 0
    stats.pendingOrders = data.pendingOrders || 0
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error(error.message || '获取统计数据失败')
  }
}

onMounted(async () => {
  await merchantStore.fetchBalance()
  await fetchStats()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}

h2 {
  margin: 0 0 24px;
  font-size: 24px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 10px;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
}

.card-header {
  font-weight: 500;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
