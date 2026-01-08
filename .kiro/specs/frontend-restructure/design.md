# Design Document: Frontend Restructure

## Overview

本设计文档描述了将商品交易系统重构为前后端分离架构的技术方案。后端Spring Boot应用移至`backend/`目录，新建Vue 3前端应用于`frontend/`目录，实现完整的用户和商家功能界面。

### Technology Stack

**Backend (existing)**:
- Java 17, Spring Boot 3.x
- H2/MySQL Database
- REST API

**Frontend (new)**:
- Vue 3 with Composition API
- Vue Router 4 for routing
- Pinia for state management
- Axios for HTTP requests
- Element Plus for UI components
- Vite for build tooling

## Architecture

### Project Structure

```
trading-system/
├── backend/                    # Spring Boot后端
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/trading/
│   │   │   └── resources/
│   │   └── test/
│   ├── pom.xml
│   └── .jqwik-database
├── frontend/                   # Vue 3前端
│   ├── src/
│   │   ├── api/               # API调用模块
│   │   ├── assets/            # 静态资源
│   │   ├── components/        # 通用组件
│   │   ├── composables/       # 组合式函数
│   │   ├── layouts/           # 布局组件
│   │   ├── router/            # 路由配置
│   │   ├── stores/            # Pinia状态管理
│   │   ├── views/             # 页面组件
│   │   │   ├── user/          # 用户页面
│   │   │   └── merchant/      # 商家页面
│   │   ├── App.vue
│   │   └── main.js
│   ├── public/
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── .kiro/
└── README.md
```

### Frontend Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Vue Application                          │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      Vue Router                           │   │
│  │  /login, /register, /products, /cart, /orders, /merchant │   │
│  └──────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │   Views     │ │  Components │ │   Layouts   │               │
│  │  (Pages)    │ │  (Reusable) │ │  (Wrappers) │               │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘               │
│         │               │               │                       │
│         └───────────────┼───────────────┘                       │
│                         │                                       │
│  ┌──────────────────────┴───────────────────────────────────┐   │
│  │                    Pinia Stores                           │   │
│  │  userStore, cartStore, productStore, orderStore, etc.    │   │
│  └──────────────────────┬───────────────────────────────────┘   │
│                         │                                       │
│  ┌──────────────────────┴───────────────────────────────────┐   │
│  │                    API Module (Axios)                     │   │
│  │  userApi, productApi, cartApi, orderApi, merchantApi     │   │
│  └──────────────────────┬───────────────────────────────────┘   │
└─────────────────────────┼───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Backend REST API                              │
│                  http://localhost:8080/api/v1                   │
└─────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### API Module

```javascript
// api/index.js
import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// Request interceptor for auth token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Response interceptor for error handling
api.interceptors.response.use(
  response => response.data,
  error => {
    const message = error.response?.data?.message || 'Network error'
    return Promise.reject(new Error(message))
  }
)

export default api
```

### Pinia Stores

```javascript
// stores/user.js
import { defineStore } from 'pinia'
import { userApi } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    balance: 0,
    isLoggedIn: false
  }),
  actions: {
    async login(credentials) { /* ... */ },
    async register(data) { /* ... */ },
    async fetchBalance() { /* ... */ },
    async deposit(amount) { /* ... */ },
    logout() { /* ... */ }
  }
})

// stores/cart.js
export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [],
    total: 0
  }),
  actions: {
    async fetchCart() { /* ... */ },
    async addItem(sku, quantity) { /* ... */ },
    async updateQuantity(sku, quantity) { /* ... */ },
    async removeItem(sku) { /* ... */ },
    async clearCart() { /* ... */ }
  }
})
```

### Vue Router Configuration

```javascript
// router/index.js
const routes = [
  // Public routes
  { path: '/login', component: () => import('@/views/Login.vue') },
  { path: '/register', component: () => import('@/views/Register.vue') },
  
  // User routes
  { path: '/', component: () => import('@/views/user/Home.vue'), meta: { requiresAuth: true } },
  { path: '/products', component: () => import('@/views/user/Products.vue') },
  { path: '/products/:id', component: () => import('@/views/user/ProductDetail.vue') },
  { path: '/cart', component: () => import('@/views/user/Cart.vue'), meta: { requiresAuth: true } },
  { path: '/orders', component: () => import('@/views/user/Orders.vue'), meta: { requiresAuth: true } },
  { path: '/orders/:id', component: () => import('@/views/user/OrderDetail.vue'), meta: { requiresAuth: true } },
  { path: '/profile', component: () => import('@/views/user/Profile.vue'), meta: { requiresAuth: true } },
  
  // Merchant routes
  { path: '/merchant/login', component: () => import('@/views/merchant/Login.vue') },
  { path: '/merchant/register', component: () => import('@/views/merchant/Register.vue') },
  { path: '/merchant', component: () => import('@/views/merchant/Dashboard.vue'), meta: { requiresMerchant: true } },
  { path: '/merchant/products', component: () => import('@/views/merchant/Products.vue'), meta: { requiresMerchant: true } },
  { path: '/merchant/inventory', component: () => import('@/views/merchant/Inventory.vue'), meta: { requiresMerchant: true } },
  { path: '/merchant/orders', component: () => import('@/views/merchant/Orders.vue'), meta: { requiresMerchant: true } },
  { path: '/merchant/settlements', component: () => import('@/views/merchant/Settlements.vue'), meta: { requiresMerchant: true } }
]
```

## Data Models

### Frontend State Models

```typescript
// User State
interface UserState {
  id: number | null
  username: string
  balance: number
  isLoggedIn: boolean
  role: 'user' | 'merchant' | null
}

// Cart Item
interface CartItem {
  sku: string
  productName: string
  quantity: number
  unitPrice: number
  subtotal: number
  available: boolean
}

// Product
interface Product {
  id: number
  name: string
  description: string
  category: string
  merchantId: number
  merchantName: string
  priceRange: { min: number, max: number }
}

// Order
interface Order {
  id: number
  orderNumber: string
  totalAmount: number
  status: 'PENDING' | 'PAID' | 'SHIPPED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED'
  items: OrderItem[]
  createdAt: string
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

由于前端主要是UI交互，大部分需求是示例测试而非属性测试。以下是可以进行属性测试的核心逻辑：

### Property 1: Cart Total Calculation Consistency

*For any* shopping cart with items, the displayed total SHALL equal the sum of (quantity × unitPrice) for all items.

**Validates: Requirements 5.5**

### Property 2: API Error Handling Consistency

*For any* API error response, the frontend SHALL display a user-friendly error message without exposing technical details.

**Validates: Requirements 9.6**

## Error Handling

### Frontend Error Handling Strategy

```javascript
// composables/useErrorHandler.js
export function useErrorHandler() {
  const handleError = (error) => {
    if (error.response) {
      // Server responded with error
      const status = error.response.status
      const message = error.response.data?.message || 'Unknown error'
      
      switch (status) {
        case 400: return { type: 'validation', message }
        case 401: return { type: 'auth', message: 'Please login again' }
        case 404: return { type: 'notFound', message: 'Resource not found' }
        case 409: return { type: 'conflict', message }
        default: return { type: 'server', message: 'Server error, please try again' }
      }
    }
    return { type: 'network', message: 'Network error, please check connection' }
  }
  
  return { handleError }
}
```

### Backend CORS Configuration

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173", "http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

## Testing Strategy

### Frontend Testing

- **Unit Tests**: Vitest for component and store testing
- **E2E Tests**: Cypress or Playwright for user flow testing
- **Component Tests**: Vue Test Utils for isolated component testing

### Test Structure

```
frontend/
├── src/
└── tests/
    ├── unit/
    │   ├── stores/
    │   │   ├── user.spec.js
    │   │   └── cart.spec.js
    │   └── components/
    │       └── ProductCard.spec.js
    └── e2e/
        ├── auth.spec.js
        ├── shopping.spec.js
        └── merchant.spec.js
```

## UI Components

### Page Layout

```
┌─────────────────────────────────────────────────────────────────┐
│  Header: Logo | Navigation | User Info | Cart Icon | Logout     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                        Main Content                             │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Page Content                          │   │
│  │                                                          │   │
│  │                                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│  Footer: Copyright | Links                                      │
└─────────────────────────────────────────────────────────────────┘
```

### Key Pages

1. **Login/Register**: Form with validation
2. **Products**: Grid/list view with search and filters
3. **Product Detail**: Product info, SKU selection, add to cart
4. **Cart**: Item list, quantity controls, checkout button
5. **Orders**: Order list with status badges
6. **Order Detail**: Items, status timeline, action buttons
7. **Merchant Dashboard**: Stats overview, quick actions
8. **Merchant Inventory**: Table with add/edit forms
9. **Merchant Orders**: Order management table
10. **Merchant Settlements**: Settlement records table

