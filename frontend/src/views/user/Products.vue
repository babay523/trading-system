<template>
  <div class="products-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="搜索">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索商品名称或描述"
            clearable
            style="width: 300px"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button icon="Search" @click="handleSearch" />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="分类">
          <el-select 
            v-model="searchForm.category" 
            placeholder="请选择分类" 
            clearable 
            @change="handleCategoryChange"
            @clear="handleCategoryChange"
            style="width: 200px"
            filterable
          >
            <el-option label="全部分类" value="" />
            <!-- 数码产品 -->
            <el-option label="手机" value="手机" />
            <el-option label="电脑" value="电脑" />
            <el-option label="耳机" value="耳机" />
            <el-option label="平板" value="平板" />
            <!-- 服装配饰 -->
            <el-option label="男装" value="男装" />
            <el-option label="女装" value="女装" />
            <el-option label="鞋类" value="鞋类" />
            <el-option label="箱包" value="箱包" />
            <!-- 食品饮料 -->
            <el-option label="粮油" value="粮油" />
            <el-option label="肉类" value="肉类" />
            <el-option label="酒水" value="酒水" />
            <el-option label="零食" value="零食" />
            <!-- 家居用品 -->
            <el-option label="灯具" value="灯具" />
            <el-option label="床品" value="床品" />
            <el-option label="电器" value="电器" />
            <el-option label="厨电" value="厨电" />
            <!-- 运动健身 -->
            <el-option label="健身器材" value="健身器材" />
            <el-option label="营养补剂" value="营养补剂" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>
    
    <div v-loading="productStore.loading" class="products-grid">
      <el-empty v-if="productStore.products.length === 0" description="暂无商品" />
      <el-card
        v-for="product in productStore.products"
        :key="product.id"
        class="product-card"
        shadow="hover"
        @click="goToDetail(product.id)"
      >
        <div class="product-image">
          <el-icon size="80" color="#909399"><Box /></el-icon>
        </div>
        <div class="product-info">
          <h3 class="product-name">{{ product.name }}</h3>
          <p class="product-desc">{{ product.description || '暂无描述' }}</p>
          <div class="product-footer">
            <span class="product-price">
              ¥{{ product.minPrice?.toFixed(2) || '0.00' }}
              <span v-if="product.maxPrice && product.maxPrice !== product.minPrice">
                - ¥{{ product.maxPrice.toFixed(2) }}
              </span>
            </span>
            <el-tag size="small">{{ product.category || '其他' }}</el-tag>
          </div>
          <div class="product-merchant">
            <el-icon size="14"><Shop /></el-icon>
            <span>{{ product.merchantName || '未知商家' }}</span>
          </div>
        </div>
      </el-card>
    </div>
    
    <el-pagination
      v-if="productStore.total > 0"
      class="pagination"
      :current-page="page"
      :page-size="pageSize"
      :total="productStore.total"
      layout="total, prev, pager, next"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useProductStore } from '@/stores/product'

const router = useRouter()
const productStore = useProductStore()

const page = ref(1)
const pageSize = ref(12)

const searchForm = reactive({
  keyword: '',
  category: ''
})

const fetchProducts = () => {
  const params = {
    page: page.value - 1,
    size: pageSize.value
  }
  if (searchForm.keyword) params.keyword = searchForm.keyword
  if (searchForm.category) params.category = searchForm.category
  
  productStore.fetchProducts(params)
}

const handleSearch = () => {
  page.value = 1
  console.log('搜索参数:', searchForm) // 添加调试日志
  fetchProducts()
}

const handleCategoryChange = (value) => {
  console.log('分类选择:', value) // 添加调试日志
  // 处理清除的情况，value可能是undefined
  searchForm.category = value || ''
  handleSearch()
}

const handlePageChange = (newPage) => {
  page.value = newPage
  fetchProducts()
}

const goToDetail = (id) => {
  router.push(`/products/${id}`)
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.products-container {
  max-width: 1200px;
  margin: 0 auto;
}

.search-card {
  margin-bottom: 20px;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.product-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.product-card:hover {
  transform: translateY(-4px);
}

.product-image {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;
}

.product-info {
  padding: 0 4px;
}

.product-name {
  font-size: 16px;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desc {
  font-size: 14px;
  color: #909399;
  margin: 0 0 12px;
  height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.product-price {
  font-size: 20px;
  font-weight: bold;
  color: #f56c6c;
}

.product-merchant {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.pagination {
  display: flex;
  justify-content: center;
}
</style>
