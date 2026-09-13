import request from './request'
import type {
  CreateEventRequest,
  EventResponse,
  SortItem,
  UpdateEventRequest
} from './types'

/**
 * 事件 API —— 对应 tag「事件」
 * 注意：event 挂在 book 下，不是 page 下。
 *
 * GET    /api/books/{bookId}/events        获取事件列表
 * POST   /api/books/{bookId}/events        创建事件
 * PUT    /api/events/{id}                  更新事件
 * DELETE /api/events/{id}                  删除事件
 * PUT    /api/books/{bookId}/events/sort   批量排序事件
 */
export const eventApi = {
  listEvents(bookId: number): Promise<EventResponse[]> {
    return request.get(`/books/${bookId}/events`)
  },

  createEvent(bookId: number, data: CreateEventRequest): Promise<EventResponse> {
    return request.post(`/books/${bookId}/events`, data)
  },

  updateEvent(id: number, data: UpdateEventRequest): Promise<EventResponse> {
    return request.put(`/events/${id}`, data)
  },

  deleteEvent(id: number): Promise<void> {
    return request.delete(`/events/${id}`)
  },

  sortEvents(bookId: number, items: SortItem[]): Promise<void> {
    return request.put(`/books/${bookId}/events/sort`, items)
  }
}
