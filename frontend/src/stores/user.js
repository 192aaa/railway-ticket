import { defineStore } from 'pinia'
import { ref, computed, onScopeDispose } from 'vue'
import request from '../utils/request'
import { getToken, getRefreshToken, setTokens, clearTokens, USER_KEY, TOKENS_CHANGED } from '../utils/auth'

function readUserFromSession() {
  try {
    const raw = sessionStorage.getItem(USER_KEY)
    if (raw && raw !== 'undefined') return JSON.parse(raw)
  } catch {}
  return null
}

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref(readUserFromSession())

  // axios 刷新令牌后同步更新 store（令牌统一由 auth.js 写入 sessionStorage）
  const syncTokens = () => {
    token.value = getToken()
  }
  window.addEventListener(TOKENS_CHANGED, syncTokens)
  onScopeDispose(() => window.removeEventListener(TOKENS_CHANGED, syncTokens))

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')

  async function login(username, password) {
    const payload = await request.post('/user/login', { username, password })
    if (!payload?.token) throw new Error('登录返回缺少 token')
    setTokens(payload.token, payload.refreshToken)
    token.value = payload.token
    userInfo.value = payload.user || payload
    sessionStorage.setItem(USER_KEY, JSON.stringify(payload.user || payload))
    return payload
  }

  async function register(data) {
    return request.post('/user/register', data)
  }

  async function fetchUserInfo() {
    const info = await request.get('/user/info')
    userInfo.value = info
    sessionStorage.setItem(USER_KEY, JSON.stringify(info))
    return info
  }

  async function logout() {
    try {
      await request.post('/user/logout', { refreshToken: getRefreshToken() }, { _skipAuthRefresh: true })
    } catch {
      // 后端不可用时仍清理本地会话，避免卡在登录态
    } finally {
      clearTokens()
      token.value = ''
      userInfo.value = null
    }
  }

  return { token, userInfo, isLoggedIn, username, isAdmin, login, register, fetchUserInfo, logout }
})
