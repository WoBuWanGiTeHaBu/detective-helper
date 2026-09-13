import request from './request'
import type {
  CreateRelationGraphRequest,
  ExtractRelationGraphRequest,
  RelationGraphData,
  RelationGraphDetailResponse,
  RelationGraphResponse,
  UpdateRelationGraphRequest
} from './types'

/**
 * 关系图 API —— 对应 tag「关系图」
 *
 * GET    /api/books/{bookId}/relation-graphs          获取关系图列表
 * POST   /api/books/{bookId}/relation-graphs          创建关系图
 * GET    /api/relation-graphs/{id}                    获取关系图详情
 * PUT    /api/relation-graphs/{id}                    更新关系图元信息
 * DELETE /api/relation-graphs/{id}                    删除关系图
 * PUT    /api/relation-graphs/{id}/data               保存关系图数据
 * POST   /api/books/{bookId}/relation-graphs/extract  从画布提取关系图数据
 */
export const relationGraphApi = {
  listRelationGraphs(bookId: number): Promise<RelationGraphResponse[]> {
    return request.get(`/books/${bookId}/relation-graphs`)
  },

  createRelationGraph(
    bookId: number,
    data: CreateRelationGraphRequest
  ): Promise<RelationGraphResponse> {
    return request.post(`/books/${bookId}/relation-graphs`, data)
  },

  getRelationGraph(id: number): Promise<RelationGraphDetailResponse> {
    return request.get(`/relation-graphs/${id}`)
  },

  updateRelationGraph(
    id: number,
    data: UpdateRelationGraphRequest
  ): Promise<RelationGraphResponse> {
    return request.put(`/relation-graphs/${id}`, data)
  },

  deleteRelationGraph(id: number): Promise<void> {
    return request.delete(`/relation-graphs/${id}`)
  },

  saveRelationGraphData(id: number, data: RelationGraphData): Promise<void> {
    return request.put(`/relation-graphs/${id}/data`, data)
  },

  extractRelationGraph(
    bookId: number,
    data: ExtractRelationGraphRequest
  ): Promise<RelationGraphData> {
    return request.post(`/books/${bookId}/relation-graphs/extract`, data)
  }
}
