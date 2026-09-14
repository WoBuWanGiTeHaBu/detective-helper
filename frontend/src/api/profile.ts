import axios from 'axios'
import type { AxiosResponse } from 'axios'
import type { UpdateProfileRequest, UserProfileResponse } from './types'

/**
 * 用户资料 API —— GET/PUT /api/profile（文档第十一节）
 * 存储以后端为唯一权威；这里**不走** `request.ts` 的共享实例：
 * 后端尚未实现时（404）要静默降级到本地缓存，不能像业务接口那样弹全局错误提示。
 * 所有方法失败时返回 null，由 userStore 决定兜底值。
 */

const http = axios.create({ baseURL: '/api', timeout: 8000 })

/** 解包 Result<T>；形状不对就当没有 */
function unwrap(res: AxiosResponse): unknown {
  const body = res.data as unknown
  if (body && typeof body === 'object' && 'code' in (body as Record<string, unknown>)) {
    return (body as { data: unknown }).data
  }
  return body
}

function normalize(raw: unknown): UserProfileResponse | null {
  if (!raw || typeof raw !== 'object') return null
  const d = raw as Record<string, unknown>
  return {
    displayName: typeof d.displayName === 'string' ? d.displayName : '',
    theme: typeof d.theme === 'string' && d.theme ? d.theme : 'light'
  }
}

export const profileApi = {
  async getProfile(): Promise<UserProfileResponse | null> {
    try {
      return normalize(unwrap(await http.get('/profile')))
    } catch {
      return null
    }
  },

  async updateProfile(data: UpdateProfileRequest): Promise<UserProfileResponse | null> {
    try {
      return normalize(unwrap(await http.put('/profile', data)))
    } catch {
      return null
    }
  }
}
