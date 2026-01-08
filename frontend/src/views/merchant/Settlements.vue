<template>
  <div class="merchant-settlements">
    <div class="page-header">
      <h2>结算记录</h2>
      <p class="description">查看每日交易结算情况</p>
    </div>

    <!-- 结算列表 -->
    <el-card shadow="never">
      <el-table :data="settlements" v-loading="loading" stripe>
        <el-table-column label="结算日期" width="150">
          <template #default="{ row }">
            {{ formatDate(row.settlementDate) }}
          </template>
        </el-table-column>
        <el-table-column label="订单总额" width="150">
          <template #default="{ row }">
            <span class="amount positive">¥{{ row.totalOrderAmount?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="退款总额" width="150">
          <template #default="{ row }">
            <span class="amount negative">-¥{{ row.totalRefundAmount?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="净收入" width="150">
          <template #default="{ row }">
            <span class="amount" :class="row.netAmount >= 0 ? 'positive' : 'negative'">
              ¥{{ row.netAmount?.toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="期初余额" width="150">
          <template #default="{ row }">
            ¥{{ row.balanceBefore?.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="期末余额" width="150">
          <template #default="{ row }">
            ¥{{ row.balanceAfter?.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchSettlements"
          @current-change="fetchSettlements"
        />
      </div>
    </el-card>

    <!-- 空状态 -->
    <el-empty v-if="!loading && settlements.length === 0" description="暂无结算记录" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useMerchantStore } from '@/stores/merchant'
import { merchantApi } from '@/api/merchant'

const merchantStore = useMerchantStore()

const loading = ref(false)
const settlements = ref([])

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const statusMap = {
  MATCHED: { text: '已核对', type: 'success' },
  MISMATCHED: { text: '差异', type: 'danger' },
  PENDING: { text: '待核对', type: 'warning' }
}

const getStatusText = (status) => statusMap[status]?.text || status
const getStatusType = (status) => statusMap[status]?.type || 'info'

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const fetchSettlements = async () => {
  if (!merchantStore.merchant?.id) return
  
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size
    }
    
    const data = await merchantApi.getSettlements(merchantStore.merchant.id, params)
    settlements.value = data.content || data || []
    pagination.total = data.totalElements || settlements.value.length
  } catch (error) {
    ElMessage.error(error.message || '获取结算记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchSettlements()
})
</script>

<style scoped>
.merchant-settlements {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #303133;
}

.page-header .description {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.amount {
  font-weight: 500;
}

.amount.positive {
  color: #67c23a;
}

.amount.negative {
  color: #f56c6c;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
