import request from './request'
import type { ApiResponse } from './request'

// 事件相关类型
export interface EventVO {
  id: number
  pageId: number
  title: string
  description: string
  eventTime: string
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface EventCreateDTO {
  pageId: number
  title?: string
  description?: string
  eventTime?: string
}

export interface EventUpdateDTO {
  title?: string
  description?: string
  eventTime?: string
  sortOrder?: number
}

export interface EventSortDTO {
  eventIds: number[]
}

// 事件API
export const eventApi = {
  // 创建事件
  createEvent(data: EventCreateDTO): Promise<ApiResponse<EventVO>> {
    return request.post('/events', data)
  },

  // 更新事件
  updateEvent(id: number, data: EventUpdateDTO): Promise<ApiResponse<EventVO>> {
    return request.put(`/events/${id}`, data)
  },

  // 删除事件
  deleteEvent(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/events/${id}`)
  },

  // 获取事件详情
  getEventById(id: number): Promise<ApiResponse<EventVO>> {
    return request.get(`/events/${id}`)
  },

  // 获取页面的所有事件
  getEventsByPageId(pageId: number): Promise<ApiResponse<EventVO[]>> {
    return request.get(`/events/page/${pageId}`)
  },

  // 批量排序事件
  sortEvents(pageId: number, data: EventSortDTO): Promise<ApiResponse<void>> {
    return request.post(`/events/page/${pageId}/sort`, data)
  }
}