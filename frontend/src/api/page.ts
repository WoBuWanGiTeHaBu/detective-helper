import request from './request'
import type {
  CanvasResponse,
  CreatePageRequest,
  PageResponse,
  SortItem,
  UpdatePageRequest
} from './types'

/**
 * 页面 API —— 对应 tag「页面」「画布」
 * 注意：page 挂在 event 下，不是 book 下。
 *
 * GET    /api/events/{eventId}/pages        获取页面列表
 * POST   /api/events/{eventId}/pages        创建页面
 * PUT    /api/pages/{id}                    更新页面
 * DELETE /api/pages/{id}                    删除页面
 * PUT    /api/events/{eventId}/pages/sort   批量排序页面
 * GET    /api/pages/{id}/canvas             获取画布数据
 * PUT    /api/pages/{id}/canvas             保存画布数据
 */
export const pageApi = {
  listPages(eventId: number): Promise<PageResponse[]> {
    return request.get(`/events/${eventId}/pages`)
  },

  createPage(eventId: number, data: CreatePageRequest): Promise<PageResponse> {
    return request.post(`/events/${eventId}/pages`, data)
  },

  updatePage(id: number, data: UpdatePageRequest): Promise<PageResponse> {
    return request.put(`/pages/${id}`, data)
  },

  deletePage(id: number): Promise<void> {
    return request.delete(`/pages/${id}`)
  },

  sortPages(eventId: number, items: SortItem[]): Promise<void> {
    return request.put(`/events/${eventId}/pages/sort`, items)
  },

  getCanvas(pageId: number): Promise<CanvasResponse> {
    return request.get(`/pages/${pageId}/canvas`)
  },

  saveCanvas(pageId: number, data: CanvasResponse): Promise<void> {
    return request.put(`/pages/${pageId}/canvas`, data)
  }
}
