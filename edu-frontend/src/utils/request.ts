import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus/es/components/message/index'
import router, { getRoleHome } from '@/router'

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export interface EduRequestConfig extends AxiosRequestConfig {
  __authToken?: string
  silentError?: boolean
}

const service: AxiosInstance = axios.create({
  baseURL: '',
  timeout: 30000
})

// 请求拦截器：附加 Token
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    const url = config.url || ''
    const isPublicAuthApi = url.includes('/auth/login') || url.includes('/auth/register')
    ;(config as EduRequestConfig).__authToken = token || ''
    if (token && !isPublicAuthApi) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    const url = response.config.url || ''
    const isPublicAuthApi = isPublicAuthRequest(url)
    const showError = shouldShowError(response.config)
    if (res.code === 200) {
      return res as any
    }
    if (res.code === 401) {
      if (isPublicAuthApi) {
        ElMessage.error(res.message || '用户名或密码错误')
      } else if (isStaleUnauthorized(response.config)) {
        return Promise.reject(new Error(res.message))
      } else {
        ElMessage.error(res.message || '登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message))
    }
    if (res.code === 428) {
      markPasswordChangeRequired()
      ElMessage.warning(res.message || '请先修改初始密码')
      return Promise.reject(new Error(res.message))
    }
    if (showError) {
      ElMessage.error(res.message || '操作失败')
    }
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    const status = error.response?.status
    const url = error.config?.url || ''
    const message = error.response?.data?.message
    const showError = shouldShowError(error.config)
    if (status === 401) {
      if (isPublicAuthRequest(url)) {
        ElMessage.error(message || '用户名或密码错误')
      } else if (isStaleUnauthorized(error.config)) {
        return Promise.reject(error)
      } else {
        ElMessage.error(message || '登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
      }
    } else if (status === 403) {
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
      if (userInfo?.role) {
        ElMessage.error(`当前账号为${roleLabel(userInfo.role)}，无权限访问该功能`)
        const home = getRoleHome(userInfo.role)
        if (router.currentRoute.value.path !== home) {
          router.replace(home)
        }
      } else {
        ElMessage.error('权限不足')
      }
    } else if (status === 428) {
      markPasswordChangeRequired()
      ElMessage.warning(message || '请先修改初始密码')
    } else if (status === 404) {
      if (showError) {
        ElMessage.error('请求资源不存在')
      }
    } else if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      if (showError) {
        ElMessage.error('请求超时，请稍后重试')
      }
    } else {
      if (showError) {
        ElMessage.error(message || '服务器错误')
      }
    }
    return Promise.reject(error)
  }
)

function isPublicAuthRequest(url: string) {
  return url.includes('/auth/login') || url.includes('/auth/register')
}

function isStaleUnauthorized(config: AxiosRequestConfig | undefined) {
  const requestToken = (config as EduRequestConfig | undefined)?.__authToken
  const currentToken = localStorage.getItem('token') || ''
  return !!requestToken && !!currentToken && requestToken !== currentToken
}

function shouldShowError(config: AxiosRequestConfig | undefined) {
  return !(config as EduRequestConfig | undefined)?.silentError
}

function markPasswordChangeRequired() {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
  if (userInfo) {
    userInfo.mustChangePassword = true
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
  }
  window.dispatchEvent(new CustomEvent('password-change-required'))
}

function roleLabel(role: string) {
  const map: Record<string, string> = {
    ADMIN: '管理员',
    TEACHER: '讲师',
    ASSISTANT: '班主任',
    STUDENT: '学生'
  }
  return map[role] || role
}

export default service
