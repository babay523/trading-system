<template>
  <div class="inventory-container">
    <div class="page-header">
      <h2>库存管理</h2>
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        添加库存
      </el-button>
    </div>
    
    <el-card v-loading="loading">
      <el-table :data="inventory" style="width: 100%">
        <el-table-column prop="sku" label="SKU" width="150" />
        <el-table-column prop="productId" label="商品ID" width="100" />
        <el-table-column prop="quantity" label="库存数量" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.quantity > 0 ? 'success' : 'danger'">
              {{ row.quantity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120" align="center">
          <template #default="{ row }">
            ¥{{ formatPrice(row.price) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.inStock ? 'success' : 'info'">
              {{ row.inStock ? '有货' : '缺货' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openPriceDialog(row)">
              修改价格
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchInventory"
        style="margin-top: 16px; justify-content: center;"
      />
    </el-card>
    
    <!-- Add Inventory Dialog -->
    <el-dialog v-model="showAddDialog" title="添加库存" width="500px">
      <el-form
        ref="addFormRef"
        :model="addForm"
        :rules="addRules"
        label-width="80px"
      >
        <el-form-item label="商品ID" prop="productId">
          <el-input v-model.number="addForm.productId" placeholder="请输入商品ID" />
        </el-form-item>
        <el-form-item label="SKU" prop="sku">
          <el-input v-model="addForm.sku" placeholder="请输入SKU编码" />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="addForm.quantity" :min="1" />
        </el-form-item>
        <el-form-item label="单价" prop="price">
          <el-input-number v-model="addForm.price" :min="0.01" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAdd">
          添加
        </el-button>
      </template>
    </el-dialog>
    
    <!-- Update Price Dialog -->
    <el-dialog v-model="showPriceDialog" title="修改价格" width="400px">
      <el-form label-width="80px">
        <el-form-item label="SKU">
          <el-input :value="selectedItem?.sku" disabled />
        </el-form-item>
        <el-form-item label="新价格">
          <el-input-number v-model="newPrice" :min="0.01" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPriceDialog = false">取消</el-button>
        <el-button type="primary" :loading="updating" @click="handleUpdatePrice">
          更新
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useMerchantStore } from '@/stores/merchant'
import { merchantApi } from '@/api/merchant'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const route = useRoute()
const merchantStore = useMerchantStore()

const loading = ref(false)
const inventory = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20

const showAddDialog = ref(false)
const adding = ref(false)
const addFormRef = ref()

const addForm = reactive({
  productId: null,
  sku: '',
  quantity: 1,
  price: 0
})

const addRules = {
  productId: [{ required: true, message: '请输入商品ID', trigger: 'blur' }],
  sku: [{ required: true, message: '请输入SKU', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

const showPriceDialog = ref(false)
const updating = ref(false)
const selectedItem = ref(null)
const newPrice = ref(0)

const formatPrice = (price) => {
  if (price === null || price === undefined) return '0.00'
  return Number(price).toFixed(2)
}

const fetchInventory = async () => {
  if (!merchantStore.merchant?.id && merchantStore.merchant?.id !== 0) return
  
  loading.value = true
  try {
    // 管理员使用id=0获取所有库存
    const merchantId = merchantStore.merchant.role === 'ADMIN' ? 0 : merchantStore.merchant.id
    const data = await merchantApi.getInventory(merchantId, {
      page: currentPage.value - 1,
      size: pageSize
    })
    inventory.value = data.content || data || []
    total.value = data.totalElements || inventory.value.length
  } finally {
    loading.value = false
  }
}

const handleAdd = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return
  
  adding.value = true
  try {
    await merchantApi.addInventory(merchantStore.merchant.id, addForm)
    ElMessage.success('库存添加成功')
    showAddDialog.value = false
    addForm.productId = null
    addForm.sku = ''
    addForm.quantity = 1
    addForm.price = 0
    await fetchInventory()
  } catch (e) {
    ElMessage.error(e.message || '添加失败')
  } finally {
    adding.value = false
  }
}

const openPriceDialog = (item) => {
  selectedItem.value = item
  newPrice.value = item.price
  showPriceDialog.value = true
}

const handleUpdatePrice = async () => {
  if (!selectedItem.value) return
  
  updating.value = true
  try {
    await merchantApi.updatePrice(
      merchantStore.merchant.id,
      selectedItem.value.sku,
      newPrice.value
    )
    ElMessage.success('价格更新成功')
    showPriceDialog.value = false
    await fetchInventory()
  } catch (e) {
    ElMessage.error(e.message || '更新失败')
  } finally {
    updating.value = false
  }
}

onMounted(() => {
  // Check if productId is in query params
  if (route.query.productId) {
    addForm.productId = Number(route.query.productId)
  }
  fetchInventory()
})
</script>

<style scoped>
.inventory-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
}
</style>
