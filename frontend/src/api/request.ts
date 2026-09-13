import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'

/**
 * 统一 Result<T> 包装 —— 对应 OpenAPI `Result*` schema
 *
 * ⚠️ 成功码说明
 * 接口文档（`推理笔记 API.openapi.json`）里写的是 `code === 0`，
 * 但后端**实际实现返回的是 `code === 200`**（与 HTTP 状态码同值）。
 * 已实测确认：GET /books、GET /books/{id}、GET /books/{id}/workspace 等
 * 全部返回 `{"code":200,"message":"操作成功","data":...}`。
 *
 * 为兼容两种实现，这里把 0 和 200 都视为成功。
 */
export interface ApiResult<T = void> {
  code: number
  message: string
  data: T
}

/** 业务成功码：文档写 0，后端实际返回 200，两者都接受 */
export const CODE_SUCCESS = 0
export const CODE_SUCCESS_HTTP = 200

export function isSuccessCode(code: number): boolean {
  return code === CODE_SUCCESS || code === CODE_SUCCESS_HTTP
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' }
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => config,
  (error) => Promise.reject(error)
)

/**
 * 响应拦截器：把 Result<T> 解包成 data 直接返回，
 * 调用方拿到的就是 T，不再是 { code, message, data }。
 */
service.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const res = response.data

    // 非 Result 包裹（如二进制）直接放行
    if (res === null || typeof res !== 'object' || !('code' in res)) {
      return response.data as never
    }

    if (isSuccessCode(res.code)) {
      return res.data as never
    }

    message.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    let errorMsg = '请求失败'
    if (error.response) {
      const body = error.response.data as ApiResult | undefined
      if (body?.message) {
        errorMsg = body.message
      } else {
        switch (error.response.status) {
          case 400: errorMsg = '请求参数错误'; break
          case 404: errorMsg = '资源不存在'; break
          case 500: errorMsg = '服务器内部错误'; break
        }
      }
    } else if (error.request) {
      // 走 vite proxy 时 /api 是相对路径，真实后端地址由 vite.config.ts 的 proxy target 决定；
      // 这里不要写死后端端口，否则会误导排查方向。
      errorMsg = import.meta.env.DEV
        ? '后端服务无响应，请确认 Spring Boot 已启动（端口需与 vite proxy target 一致）'
        : '无法连接到服务，请检查网络'
    } else {
      errorMsg = error.message || '请求失败'
    }
    message.error(errorMsg)
    return Promise.reject(error)
  }
)

export default service
