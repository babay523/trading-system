<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>账户信息</span>
          </template>
          <div class="user-info">
            <el-avatar :size="80" icon="User" />
            <h3>{{ userStore.user?.username }}</h3>
            <p class="user-id">ID: {{ userStore.user?.id }}</p>
          </div>
          <el-divider />
          <div class="balance-info">
            <span class="label">账户余额</span>
            <span class="amount">¥{{ userStore.balance.toFixed(2) }}</span>
          </div>
          <el-button type="primary" style="width: 100%; margin-top: 16px" @click="showDepositDialog = true">
            充值
          </el-button>
        </el-card>
      </el-col>
      
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>交易记录</span>
          </template>
          <el-table :data="transactions" v-loading="loading" stripe>
            <el-table-column prop="id" label="交易ID" width="80" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="getTypeTag(row.type)">{{ getTypeName(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="120">
              <template #default="{ row }">
                <span :class="row.amount > 0 ? 'text-success' : 'text-danger'">
                  {{ row.amount > 0 ? '+' : '' }}¥{{ row.amount.toFixed(2) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="balanceAfter" label="余额" width="120">
              <template #default="{ row }">
                ¥{{ row.balanceAfter?.toFixed(2) || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
          
          <el-pagination
            v-if="total > 0"
            style="margin-top: 16px; justify-content: center"
            :current-page="page"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </el-card>
      </el-col>
    </el-row>
    
    <!-- Deposit Dialog -->
    <el-dialog v-model="showDepositDialog" title="账户充值" width="400px">
      <el-form :model="depositForm" label-width="80px">
        <el-form-item label="充值金额">
          <el-input-number
            v-model="depositForm.amount"
            :min="1"
            :max="100000"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <div class="quick-amounts">
          <el-button v-for="amt in [100, 500, 1000, 5000]" :key="amt" @click="depositForm.amount = amt">
            ¥{{ amt }}
          </el-button>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showDepositDialog = false">取消</el-button>
        <el-button type="primary" :loading="depositing" @click="handleDeposit">
          确认充值
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const transactions = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showDepositDialog = ref(false)
const depositing = ref(false)
const depositForm = reactive({ amount: 100 })

const fetchTransactions = async () => {
  if (!userStore.user?.id) return
  loading.value = true
  try {
    const data = await userApi.getTransactions(userStore.user.id, {
      page: page.value - 1,
      size: pageSize.value
    })
    transactions.value = data.content || data || []
    total.value = data.totalElements || transactions.value.length
  } catch (e) {
    transactions.value = []
  } finally {
    loading.value = false
  }
}

const handlePageChange = (newPage) => {
  page.value = newPage
  fetchTransactions()
}

const handleDeposit = async () => {
  if (depositForm.amount <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }
  
  depositing.value = true
  try {
    await userStore.deposit(depositForm.amount)
    ElMessage.success('充值成功')
    showDepositDialog.value = false
    fetchTransactions()
  } catch (e) {
    // Error handled by interceptor
  } finally {
    depositing.value = false
  }
}

const getTypeName = (type) => {
  const names = {
    DEPOSIT: '充值',
    PURCHASE: '消费',
    REFUND_IN: '退款'
  }
  return names[type] || type
}

const getTypeTag = (type) => {
  const tags = {
    DEPOSIT: 'success',
    PURCHASE: 'danger',
    REFUND_IN: 'warning'
  }
  return tags[type] || 'info'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  userStore.fetchBalance()
  fetchTransactions()
})
</script>

<style scoped>
.profile-container {
  max-width: 1200px;
  margin: 0 auto;
}

.user-info {
  text-align: center;
  padding: 20px 0;
}

.user-info h3 {
  margin: 16px 0 8px;
}

.user-id {
  color: #909399;
  font-size: 14px;
}

.balance-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.balance-info .label {
  color: #606266;
}

.balance-info .amount {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.quick-amounts {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.text-success {
  color: #67c23a;
}

.text-danger {
  color: #f56c6c;
}
</style>
