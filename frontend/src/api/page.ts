import request from './request'
import type { ApiResponse } from './request'

// 页面相关类型
export interface PageVO {
  id: number
  bookId: number
  title: string
  content: string
  canvasData: string
  sortOrder: number
  eventCount: number
  createdAt: string
  updatedAt: string
}

export interface PageCreateDTO {
  bookId: number
  title?: string
  content?: string
  canvasData?: string
}

export interface PageUpdateDTO {
  title?: string
  content?: string
  canvasData?: string
  sortOrder?: number
}

export interface PageSortDTO {
  pageIds: number[]
}

// 页面API
export const pageApi = {
  // 创建页面
  createPage(data: PageCreateDTO): Promise<ApiResponse<PageVO>> {
    return request.post('/pages', data)
  },

  // 更新页面
  updatePage(id: number, data: PageUpdateDTO): Promise<ApiResponse<PageVO>> {
    return request.put(`/pages/${id}`, data)
  },

  // 删除页面
  deletePage(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/pages/${id}`)
  },

  // 获取页面详情
  getPageById(id: number): Promise<ApiResponse<PageVO>> {
    return request.get(`/pages/${id}`)
  },

  // 获取书籍的所有页面
  getPagesByBookId(bookId: number): Promise<ApiResponse<PageVO[]>> {
    return request.get(`/pages/book/${bookId}`)
  },

  // 保存页面画布
  savePageCanvas(id: number, canvasData: string): Promise<ApiResponse<PageVO>> {
    return request.put(`/pages/${id}/canvas`, canvasData, {
      headers: { 'Content-Type': 'text/plain' }
    })
  },

  // 批量排序页面
  sortPages(bookId: number, data: PageSortDTO): Promise<ApiResponse<void>> {
    return request.post(`/pages/book/${bookId}/sort`, data)
  }
}