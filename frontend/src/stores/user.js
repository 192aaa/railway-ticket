import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'

const TOKEN_KEY = 'token'
const USER_KEY = 'user'

function readUserFromSession() {
  try {
    const raw = sessionStorage.getItem(USER_KEY)
    if (raw && raw !== 'undefined') return JSON.parse(raw)
  } catch {}
  return null
}

export const useUserStore = defineStore('user', () => {
  const token = ref(sessionStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref(readUserFromSession())

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')

  async function login(username, password) {
    const payload = await request.post('/user/login', { username, password })
    if (!payload?.token) throw new Error('登录返回缺少 token')
    token.value = payload.token
    userInfo.value = payload.user || payload
    sessionStorage.setItem(TOKEN_KEY, payload.token)
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

  function logout() {
    token.value = ''
    userInfo.value = null
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
  }

  return { token, userInfo, isLoggedIn, username, login, register, fetchUserInfo, logout }
})