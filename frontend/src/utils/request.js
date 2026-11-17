import axios from 'axios'
import router from '../router'
import { getToken, getRefreshToken, setTokens, clearTokens } from './auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token
  }
  return config
})

let isLoggingOut = false
let isRefreshing = false
let pendingRequests = []

function forceLogout() {
  clearTokens()
  if (isLoggingOut) return
  isLoggingOut = true
  ElMessage.warning('登录已失效，请重新登录')
  const currentPath = router.currentRoute.value.fullPath
  if (currentPath !== '/login') {
    router.replace({ path: '/login', query: { redirect: currentPath } }).finally(() => {
      isLoggingOut = false
    })
  } else {
    isLoggingOut = false
  }
}

function flushPending(token) {
  pendingRequests.forEach((callback) => callback(token))
  pendingRequests = []
}

async function refreshAccessToken() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) throw new Error('没有可用的刷新令牌')

  // 使用原生 axios，避免再次进入本实例的 401 拦截导致递归
  const { data } = await axios.post(
    API_BASE_URL + '/user/refresh',
    { refreshToken },
    {
      headers: { 'Content-Type': 'application/json' }
    }
  )
  if (data?.code !== 200 || !data.data?.token) {
    throw new Error(data?.message || '刷新令牌已失效')
  }
  setTokens(data.data.token, data.data.refreshToken)
  return data.data.token
}

request.interceptors.response.use(
  (response) => {
    const body = response.data

    if (body == null) return null

    if (body.code === 401) {
      forceLogout()
      return Promise.reject(new Error(body.message || '未授权'))
    }
    if (body.code !== 200) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }

    return body.data
  },
  async (error) => {
    if (error.code === 'ERR_CANCELED') return Promise.reject(error)

    const status = error.response?.status
    const config = error.config || {}

    // 主动登出时的 401 不触发续期/强制跳转，交给 logout 自己清理会话
    if (status === 401 && config._skipAuthRefresh) {
      return Promise.reject(error)
    }

    if (status === 401 && !config._retry && getRefreshToken()) {
      config._retry = true
      if (!isRefreshing) {
        isRefreshing = true
        try {
          const token = await refreshAccessToken()
          flushPending(token)
          config.headers.Authorization = 'Bearer ' + token
          return request(config)
        } catch (refreshError) {
          flushPending(null)
          forceLogout()
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      }
      // 并发 401：共享同一次刷新，刷新后用新 token 重放原请求
      return new Promise((resolve, reject) => {
        pendingRequests.push((token) => {
          if (!token) return reject(error)
          config.headers.Authorization = 'Bearer ' + token
          resolve(request(config))
        })
      })
    }

    if (status === 401) {
      forceLogout()
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
