import request from './request'
import type {
  CreateRelationGraphRequest,
  ExtractRelationGraphRequest,
  GraphEdge,
  GraphNode,
  RelationGraphData,
  RelationGraphDetailRaw,
  RelationGraphDetailResponse,
  RelationGraphResponse,
  UpdateRelationGraphRequest
} from './types'

/**
 * 归一关系图数据。
 *
 * 后端详情接口（RelationGraphDetailVO）把 `data` 当 JSON **字符串** 返回，
 * 解析后的 nodes / edges 平铺在顶层。历史前端却读 `detail.data.nodes`，
 * 于是「新建时正常（数据在内存里来自 extract）、重新打开就成了空图」。
 * 这里做一次兜底：优先用顶层数组，缺失再解析 data 字符串。
 */
function normalizeGraphData(raw: RelationGraphDetailRaw): RelationGraphData {
  let nodes: GraphNode[] | null = raw.nodes ?? null
  let edges: GraphEdge[] | null = raw.edges ?? null

  if ((!nodes || !edges) && typeof raw.data === 'string' && raw.data.trim()) {
    try {
      const parsed = JSON.parse(raw.data) as Partial<RelationGraphData>
      nodes = nodes ?? parsed.nodes ?? []
      edges = edges ?? parsed.edges ?? []
    } catch {
      /* 库里是脏数据就当空图，不要连累整个详情 */
    }
  }

  return { nodes: nodes ?? [], edges: edges ?? [] }
}

function normalizeGraphDetail(raw: RelationGraphDetailRaw): RelationGraphDetailResponse {
  return {
    id: raw.id,
    bookId: raw.bookId,
    name: raw.name,
    data: normalizeGraphData(raw),
    createdAt: raw.createdAt,
    updatedAt: raw.updatedAt
  }
}

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

  /** 详情：把后端的「字符串 data + 顶层 nodes/edges」归一成 data 对象 */
  getRelationGraph(id: number): Promise<RelationGraphDetailResponse> {
    // 拦截器已把 Result<T> 解包成 data，故这里拿到的是 RelationGraphDetailRaw 本体；
    // axios 的 AxiosResponse 类型不反映该行为，用 unknown 过桥。
    return request
      .get(`/relation-graphs/${id}`)
      .then((res) => normalizeGraphDetail(res as unknown as RelationGraphDetailRaw))
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
