<template>
  <el-container class="merchant-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon size="24"><Shop /></el-icon>
        <span>商家后台</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        router
      >
        <el-menu-item index="/merchant">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/merchant/products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/inventory">
          <el-icon><Box /></el-icon>
          <span>库存管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/orders">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/settlements">
          <el-icon><Wallet /></el-icon>
          <span>结算记录</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-content">
          <span class="page-title">{{ pageTitle }}</span>
          
          <div class="header-right">
            <el-tag type="success">
              余额: ¥{{ merchantStore.balance.toFixed(2) }}
            </el-tag>
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-icon><User /></el-icon>
                {{ merchantStore.merchant?.businessName }}
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="user">切换到用户端</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMerchantStore } from '@/stores/merchant'

const router = useRouter()
const route = useRoute()
const merchantStore = useMerchantStore()

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  const titles = {
    '/merchant': '仪表盘',
    '/merchant/products': '商品管理',
    '/merchant/inventory': '库存管理',
    '/merchant/orders': '订单管理',
    '/merchant/settlements': '结算记录'
  }
  return titles[route.path] || '商家后台'
})

const handleCommand = (command) => {
  switch (command) {
    case 'user':
      router.push('/')
      break
    case 'logout':
      merchantStore.logout()
      router.push('/merchant/login')
      break
  }
}
</script>

<style scoped>
.merchant-layout {
  min-height: 100vh;
}

.sidebar {
  background: #304156;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  overflow-y: auto;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #3d4a5a;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
}

.sidebar-menu .el-menu-item {
  color: #bfcbd9;
}

.sidebar-menu .el-menu-item:hover,
.sidebar-menu .el-menu-item.is-active {
  background: #263445;
  color: #409eff;
}

.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  padding: 0 20px;
  margin-left: 220px;
}

.header-content {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 18px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #606266;
}

.main-content {
  margin-left: 220px;
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}
</style>
