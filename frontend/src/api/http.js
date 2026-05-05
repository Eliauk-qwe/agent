import axios from 'axios'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// 创建 axios 实例
const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000, // 60秒超时
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
http.interceptors.request.use(
  config => {
    // 可以在这里添加认证token等
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
http.interceptors.response.use(
  response => {
    return response
  },
  error => {
    console.error('API请求错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 检查后端健康状态
 */
export function checkHealth() {
  return http.get('/health')
}

/**
 * 创建 SSE URL
 */
export function createSseUrl(path, params = {}) {
  const url = new URL(`${API_BASE_URL}${path}`, window.location.origin)
  
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, value)
    }
  })
  
  return url.toString()
}

// 导出 http 实例（默认导出）
export default http