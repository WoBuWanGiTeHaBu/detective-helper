import request from './request'
import type { ApiResponse } from './request'

// 关系图相关类型
export interface RelationGraphVO {
  id: number
  bookId: number
  title: string
  description: string
  graphData: string
  createdAt: string
  updatedAt: string
}

export interface RelationGraphDetailVO {
  id: number
  bookId: number
  title: string
  description: string
  graphData: string
  nodeCount: number
  edgeCount: number
  entityTypes: string[]
  relationTypes: string[]
  createdAt: string
  updatedAt: string
}

export interface RelationGraphCreateDTO {
  bookId: number
  title: string
  description?: string
  graphData?: string
}

export interface RelationGraphUpdateDTO {
  title: string
  description?: string
  graphData?: string
}

export interface RelationGraphExtractDTO {
  bookId: number
  entityTypes?: string
  relationTypes?: string
}

// 关系图API
export const relationGraphApi = {
  // 创建关系图
  createRelationGraph(data: RelationGraphCreateDTO): Promise<ApiResponse<RelationGraphVO>> {
    return request.post('/relation-graphs', data)
  },

  // 更新关系图
  updateRelationGraph(id: number, data: RelationGraphUpdateDTO): Promise<ApiResponse<RelationGraphVO>> {
    return request.put(`/relation-graphs/${id}`, data)
  },

  // 删除关系图
  deleteRelationGraph(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/relation-graphs/${id}`)
  },

  // 获取关系图详情
  getRelationGraphById(id: number): Promise<ApiResponse<RelationGraphDetailVO>> {
    return request.get(`/relation-graphs/${id}`)
  },

  // 获取书籍的所有关系图
  getRelationGraphsByBookId(bookId: number): Promise<ApiResponse<RelationGraphVO[]>> {
    return request.get(`/relation-graphs/book/${bookId}`)
  },

  // 从案件中提取关系图
  extractRelationGraph(data: RelationGraphExtractDTO): Promise<ApiResponse<RelationGraphVO>> {
    return request.post('/relation-graphs/extract', data)
  }
}