<template>
  <div class="merchant-login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <el-icon size="32" color="#409eff"><Shop /></el-icon>
          <h2>商家登录</h2>
        </div>
      </template>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入商家用户名"
            prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <span>还没有商家账号？</span>
        <el-button type="primary" link @click="$router.push('/merchant/register')">
          立即注册
        </el-button>
      </div>
      
      <el-divider />
      
      <div class="switch-portal">
        <el-button type="info" link @click="$router.push('/login')">
          切换到用户登录
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMerchantStore } from '@/stores/merchant'
import { ElMessage } from 'element-plus'
import { Shop } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const merchantStore = useMerchantStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  try {
    await merchantStore.login(form)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/merchant'
    router.push(redirect)
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.merchant-login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.card-header h2 {
  margin: 0;
  font-size: 24px;
}

.login-footer {
  text-align: center;
  color: #909399;
}

.switch-portal {
  text-align: center;
}
</style>
