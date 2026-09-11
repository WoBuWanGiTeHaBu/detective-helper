import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'

// 定义响应类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    // 可以在这里添加token等认证信息
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    if (res.code === 200) {
      return res
    } else {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  (error) => {
    let errorMsg = '请求失败'

    if (error.response) {
      switch (error.response.status) {
        case 400:
          errorMsg = '请求参数错误'
          break
        case 401:
          errorMsg = '未授权访问'
          break
        case 403:
          errorMsg = '禁止访问'
          break
        case 404:
          errorMsg = '资源不存在'
          break
        case 500:
          errorMsg = '服务器内部错误'
          break
        default:
          errorMsg = error.response.data?.message || '请求失败'
      }
    } else if (error.request) {
      errorMsg = '网络连接失败'
    } else {
      errorMsg = error.message || '请求失败'
    }

    message.error(errorMsg)
    return Promise.reject(error)
  }
)

export default service