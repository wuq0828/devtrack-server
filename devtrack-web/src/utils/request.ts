import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

// Lazily-imported store accessors to avoid circular imports at module load time.
function getToken(): string | null {
  return localStorage.getItem('devtrack_token')
}

function redirectToLogin() {
  localStorage.removeItem('devtrack_token')
  localStorage.removeItem('devtrack_user')
  // Avoid redirect loops if we're already on the login page.
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

const service: AxiosInstance = axios.create({
  // Use the dev proxy / same-origin base path.
  baseURL: '/devtrack',
  timeout: 15000,
})

// ---- Request interceptor: attach Authorization header ----
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`)
    }
    return config
  },
  (error) => Promise.reject(error),
)

// ---- Response interceptor: unwrap envelope, handle errors ----
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    const body = response.data
    // Some endpoints (e.g. health) may not return the envelope; pass through if so.
    if (body == null || typeof body.code !== 'number') {
      return response
    }
    if (body.code === 0) {
      return response
    }
    // Non-zero business code: surface the message.
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message || `业务错误 code=${body.code}`))
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      redirectToLogin()
    } else {
      const msg = error?.response?.data?.message || error?.message || '网络错误'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  },
)

// Generic request helper returning the unwrapped `data` payload.
export async function request<T>(config: InternalAxiosRequestConfig | Parameters<AxiosInstance['request']>[0]): Promise<T> {
  const response = await service.request<ApiResponse<T>>(config)
  return response.data.data
}

export default service
