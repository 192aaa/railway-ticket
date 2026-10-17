<template>
  <header class="nav-bar glass" :class="{ scrolled: isScrolled }">
    <div class="nav-glow"></div>
    <div class="nav-inner">
      <div class="brand" @click="$router.push('/home')">
        <div class="logo-wrap">
          <svg class="train-icon" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="appnav-tg" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#60a5fa"/>
                <stop offset="100%" stop-color="#a78bfa"/>
              </linearGradient>
              <filter id="glow">
                <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
                <feMerge><feMergeNode in="coloredBlur"/><feMergeNode in="SourceGraphic"/></feMerge>
              </filter>
            </defs>
            <rect x="8" y="12" width="48" height="32" rx="6" fill="url(#appnav-tg)" filter="url(#glow)"/>
            <rect x="14" y="20" width="10" height="10" rx="2" fill="#fff" opacity="0.95"/>
            <rect x="28" y="20" width="10" height="10" rx="2" fill="#fff" opacity="0.95"/>
            <rect x="42" y="20" width="8" height="10" rx="2" fill="#fff" opacity="0.95"/>
            <rect x="14" y="36" width="36" height="3" rx="1.5" fill="#1e3a8a" opacity="0.35"/>
            <circle cx="18" cy="48" r="5" fill="#0f172a" stroke="#60a5fa" stroke-width="2"/>
            <circle cx="46" cy="48" r="5" fill="#0f172a" stroke="#60a5fa" stroke-width="2"/>
            <line x1="2" y1="56" x2="62" y2="56" stroke="#475569" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-title">RailFlow</span>
          <span class="brand-sub">铁 路 购 票 系 统</span>
        </div>
      </div>
      <div class="nav">
        <template v-if="userStore.isLoggedIn">
          <div class="user-chip">
            <div class="avatar">
              {{ (userStore.userInfo?.realName || userStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}
            </div>
            <span class="username">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
          </div>
          <button v-ripple class="nav-btn" :class="{ active: isActive('/home') }" @click="$router.push('/home')">
            <el-icon><HomeFilled /></el-icon>
            首页
          </button>
          <button v-ripple class="nav-btn" :class="{ active: isActive('/profile') }" @click="$router.push('/profile')">
            <el-icon><User /></el-icon>
            个人中心
          </button>
          <button v-ripple class="nav-btn msg-btn" :class="{ active: isActive('/messages') }" @click="$router.push('/messages')">
            <div class="msg-icon-wrap">
              <el-icon><Bell /></el-icon>
              <span v-if="unreadCount > 0" class="msg-red-dot">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </div>
            消息
          </button>
          <button v-ripple class="nav-btn primary" :class="{ active: isActive('/orders') }" @click="$router.push('/orders')">
            <el-icon><Tickets /></el-icon>
            我的订单
          </button>
          <button v-ripple class="nav-btn logout" :disabled="logoutLoading" @click="logout">
            <el-icon><SwitchButton /></el-icon>
            {{ logoutLoading ? '退出中...' : '退出' }}
          </button>
        </template>
        <template v-else>
          <button v-ripple class="nav-btn" :class="{ active: isActive('/login') }" @click="$router.push('/login')">
            <el-icon><User /></el-icon>
            登录
          </button>
          <button v-ripple class="nav-btn primary" :class="{ active: isActive('/register') }" @click="$router.push('/register')">
            <el-icon><Plus /></el-icon>
            注册
          </button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { HomeFilled, Tickets, SwitchButton, User, Plus, Bell } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const logoutLoading = ref(false)
const isScrolled = ref(false)
const unreadCount = ref(0)

let unreadTimer = null

async function fetchUnread() {
  if (!userStore.isLoggedIn) { unreadCount.value = 0; return }
  try {
    unreadCount.value = await request.get('/message/unread-count')
  } catch {}
}

function startUnreadPoll() {
  if (unreadTimer) return
  fetchUnread()
  unreadTimer = setInterval(fetchUnread, 5000)
}

function stopUnreadPoll() {
  if (unreadTimer) { clearInterval(unreadTimer); unreadTimer = null }
  unreadCount.value = 0
}

watch(() => userStore.isLoggedIn, (loggedIn) => {
  if (loggedIn) startUnreadPoll()
  else stopUnreadPoll()
})

watch(() => route.path, (p) => {
  if (p === '/messages') fetchUnread()
})

function onScroll() {
  isScrolled.value = window.scrollY > 12
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  if (userStore.isLoggedIn) startUnreadPoll()
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  stopUnreadPoll()
})

function isActive(path) {
  return route.path === path || (path === '/home' && route.path === '/')
}

async function logout() {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '退出确认', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  logoutLoading.value = true
  try {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } finally {
    logoutLoading.value = false
  }
}
</script>

<style scoped>
.nav-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  border-radius: 0;
  border-left: none;
  border-right: none;
  border-top: none;
  margin: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(16px) saturate(160%);
  -webkit-backdrop-filter: blur(16px) saturate(160%);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.nav-bar.scrolled {
  background: rgba(15, 23, 42, 0.82);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-bottom: 1px solid rgba(59, 130, 246, 0.18);
}

.nav-bar.scrolled .nav-inner {
  padding: 10px 32px;
}

.nav-glow {
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(59, 130, 246, 0.6) 20%,
    rgba(139, 92, 246, 0.8) 50%,
    rgba(236, 72, 153, 0.5) 80%,
    transparent 100%
  );
  filter: blur(1px);
  opacity: 0.7;
}

.nav-bar.scrolled .nav-glow {
  opacity: 1;
  height: 2px;
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.4);
}

.nav-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 14px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: padding 0.35s ease;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.brand:hover {
  transform: scale(1.02);
}

.logo-wrap {
  position: relative;
}

.logo-wrap::after {
  content: '';
  position: absolute;
  inset: -6px;
  border-radius: 12px;
  background: radial-gradient(circle, rgba(96, 165, 250, 0.25), transparent 70%);
  opacity: 0;
  transition: opacity 0.4s ease;
}

.brand:hover .logo-wrap::after {
  opacity: 1;
}

.train-icon {
  width: 40px;
  height: 40px;
  animation: trainMove 4s ease-in-out infinite;
  position: relative;
  z-index: 1;
}

@keyframes trainMove {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(2px); }
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(135deg, #60a5fa 0%, #a78bfa 50%, #ec4899 100%);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
  animation: gradientShift 6s ease infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.brand-sub {
  font-size: 10px;
  color: #64748b;
  letter-spacing: 4px;
}

.nav {
  display: flex;
  gap: 10px;
  align-items: center;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 16px 4px 4px;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 100px;
  margin-right: 4px;
  transition: all 0.3s ease;
}

.user-chip:hover {
  background: rgba(59, 130, 246, 0.14);
  border-color: rgba(59, 130, 246, 0.35);
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: white;
  box-shadow: 0 0 12px rgba(59, 130, 246, 0.4);
}

.username {
  font-size: 14px;
  color: #cbd5e1;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(51, 65, 85, 0.4);
  color: #cbd5e1;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.nav-btn:hover:not(:disabled) {
  background: rgba(71, 85, 105, 0.75);
  transform: translateY(-2px);
  border-color: rgba(148, 163, 184, 0.35);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
}

.nav-btn.active {
  background: rgba(59, 130, 246, 0.15);
  border-color: rgba(96, 165, 250, 0.5);
  color: #93c5fd;
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.12), inset 0 0 20px rgba(59, 130, 246, 0.08);
}

.nav-btn.primary {
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  border: none;
  color: white;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.35);
}

.nav-btn.primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 22px rgba(59, 130, 246, 0.55);
}

.nav-btn.primary.active {
  background: linear-gradient(135deg, #60a5fa 0%, #818cf8 100%);
  box-shadow: 0 0 0 1px rgba(96, 165, 250, 0.3), 0 8px 28px rgba(59, 130, 246, 0.5);
}

.nav-btn.logout {
  border-color: rgba(239, 68, 68, 0.25);
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.06);
}

.nav-btn.logout:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.14);
  border-color: rgba(239, 68, 68, 0.45);
  color: #fecaca;
}

.nav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.msg-btn { position: relative; }

.msg-icon-wrap {
  position: relative;
  display: inline-flex;
}

.msg-red-dot {
  position: absolute;
  top: -8px;
  right: -10px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 10px;
  background: linear-gradient(135deg, #ef4444, #f97316);
  color: white;
  font-size: 10px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.5);
  animation: dotPop 0.4s ease-out;
}

@keyframes dotPop {
  0% { transform: scale(0); }
  70% { transform: scale(1.2); }
  100% { transform: scale(1); }
}
</style>