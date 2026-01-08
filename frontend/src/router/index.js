import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMerchantStore } from '@/stores/merchant'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import MerchantLayout from '@/layouts/MerchantLayout.vue'

const routes = [
  // User routes
  {
    path: '/',
    component: DefaultLayout,
    children: [
      {
        path: '',
        name: 'Home',
        redirect: '/products'
      },
      {
        path: 'products',
        name: 'Products',
        component: () => import('@/views/user/Products.vue')
      },
      {
        path: 'products/:id',
        name: 'ProductDetail',
        component: () => import('@/views/user/ProductDetail.vue')
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/user/Cart.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'checkout',
        name: 'Checkout',
        component: () => import('@/views/user/Checkout.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/user/Orders.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/user/OrderDetail.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { requiresAuth: true }
      }
    ]
  },
  
  // Auth routes (no layout)
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  },
  
  // Merchant auth routes
  {
    path: '/merchant/login',
    name: 'MerchantLogin',
    component: () => import('@/views/merchant/Login.vue')
  },
  {
    path: '/merchant/register',
    name: 'MerchantRegister',
    component: () => import('@/views/merchant/Register.vue')
  },
  
  // Merchant routes
  {
    path: '/merchant',
    component: MerchantLayout,
    meta: { requiresMerchant: true },
    children: [
      {
        path: '',
        name: 'MerchantDashboard',
        component: () => import('@/views/merchant/Dashboard.vue')
      },
      {
        path: 'products',
        name: 'MerchantProducts',
        component: () => import('@/views/merchant/Products.vue')
      },
      {
        path: 'inventory',
        name: 'MerchantInventory',
        component: () => import('@/views/merchant/Inventory.vue')
      },
      {
        path: 'orders',
        name: 'MerchantOrders',
        component: () => import('@/views/merchant/Orders.vue')
      },
      {
        path: 'settlements',
        name: 'MerchantSettlements',
        component: () => import('@/views/merchant/Settlements.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guards
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const merchantStore = useMerchantStore()
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresMerchant && !merchantStore.isLoggedIn) {
    next({ path: '/merchant/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
