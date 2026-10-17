import axios from 'axios'
import router from '../router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = sessionStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token
  }
  return config
})

let isLoggingOut = false

function forceLogout() {
  if (isLoggingOut) return
  isLoggingOut = true
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('user')
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

request.interceptors.response.use(
  response => {
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
  error => {
    if (error.code === 'ERR_CANCELED') return Promise.reject(error)

    const status = error.response?.status
    if (status === 401) {
      forceLogout()
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request