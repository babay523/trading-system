import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Response interceptor
api.interceptors.response.use(
  response => {
    const res = response.data
    // 处理成功响应：200 OK 和 201 Created
    if (res.code === 200 || res.code === 201) {
      return res.data
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  error => {
    const message = error.response?.data?.message || error.message || '网络错误'
    
    // 如果是用户不存在的错误，清除登录状态
    if (message.includes('User not found') || message.includes('Merchant not found')) {
      console.error('用户信息失效:', message)
      ElMessage.error('用户信息已失效，请重新登录')
      
      // 清除本地存储
      localStorage.removeItem('user')
      localStorage.removeItem('token')
      
      // 只在非登录/注册页面时跳转
      const currentPath = window.location.pathname
      const isAuthPage = currentPath === '/login' || 
                        currentPath === '/register' || 
                        currentPath === '/merchant/login' || 
                        currentPath === '/merchant/register'
      
      if (!isAuthPage) {
        // 延迟跳转，避免与其他导航冲突
        setTimeout(() => {
          window.location.href = '/login'
        }, 100)
      }
      
      return Promise.reject(error)
    }
    
    // 处理401未授权错误
    if (error.response?.status === 401) {
      const currentPath = window.location.pathname
      const isAuthPage = currentPath === '/login' || 
                        currentPath === '/register' || 
                        currentPath === '/merchant/login' || 
                        currentPath === '/merchant/register'
      
      if (!isAuthPage) {
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('user')
        localStorage.removeItem('token')
        setTimeout(() => {
          window.location.href = '/login'
        }, 100)
      }
      return Promise.reject(error)
    }
    
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default api
