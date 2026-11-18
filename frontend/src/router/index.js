import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/home', name: 'Home', component: () => import('@/views/Home.vue') },
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
  { path: '/orders', name: 'Orders', component: () => import('@/views/Orders.vue') },
  { path: '/profile', name: 'Profile', component: () => import('@/views/Profile.vue') },
  { path: '/messages', name: 'Messages', component: () => import('@/views/Messages.vue') },
  { path: '/dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAdmin: true }
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFound.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  const store = useUserStore()
  if ((to.path === '/orders' || to.path === '/profile' || to.path === '/messages') && !store.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !store.isAdmin) {
    return { path: '/home' }
  }
})

export default router
