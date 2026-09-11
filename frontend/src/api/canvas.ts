import request from './request'
import type { ApiResponse } from './request'

// 画布相关类型
export interface CanvasVO {
  pageId: number
  canvasData: string
}

// 画布API
export const canvasApi = {
  // 获取页面画布数据
  getPageCanvas(pageId: number): Promise<ApiResponse<CanvasVO>> {
    return request.get(`/canvas/page/${pageId}`)
  },

  // 保存页面画布数据
  savePageCanvas(pageId: number, canvasData: string): Promise<ApiResponse<CanvasVO>> {
    return request.put(`/canvas/page/${pageId}`, canvasData, {
      headers: { 'Content-Type': 'text/plain' }
    })
  }
}