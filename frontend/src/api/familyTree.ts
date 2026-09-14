import request from './request'
import type {
  CreateFamilyTreeRequest,
  FamilyMember,
  FamilyRelation,
  FamilyTreeData,
  FamilyTreeDetailRaw,
  FamilyTreeDetailResponse,
  FamilyTreeResponse,
  UpdateFamilyTreeRequest
} from './types'

/**
 * 归一族谱图数据。
 *
 * 与关系图同一套约定：详情接口把 `data` 当 JSON **字符串**返回，
 * 解析后的 members / relations 平铺在顶层。这里优先用顶层数组，
 * 缺失再解析字符串，保证下游 `detail.data.members` 永远可用。
 */
function normalizeFamilyData(raw: FamilyTreeDetailRaw): FamilyTreeData {
  let members: FamilyMember[] | null = raw.members ?? null
  let relations: FamilyRelation[] | null = raw.relations ?? null

  if ((!members || !relations) && typeof raw.data === 'string' && raw.data.trim()) {
    try {
      const parsed = JSON.parse(raw.data) as Partial<FamilyTreeData>
      members = members ?? parsed.members ?? []
      relations = relations ?? parsed.relations ?? []
    } catch {
      /* 库里是脏数据就当空族谱，不要连累整个详情 */
    }
  }

  return { members: members ?? [], relations: relations ?? [] }
}

function normalizeFamilyDetail(raw: FamilyTreeDetailRaw): FamilyTreeDetailResponse {
  return {
    id: raw.id,
    bookId: raw.bookId,
    name: raw.name,
    data: normalizeFamilyData(raw),
    createdAt: raw.createdAt,
    updatedAt: raw.updatedAt
  }
}

/**
 * 族谱图 API —— 对应 tag「族谱图」，与关系图接口一一对应。
 *
 * GET    /api/books/{bookId}/family-trees          获取族谱图列表
 * POST   /api/books/{bookId}/family-trees          创建族谱图
 * GET    /api/family-trees/{id}                    获取族谱图详情
 * PUT    /api/family-trees/{id}                    更新族谱图元信息
 * DELETE /api/family-trees/{id}                    删除族谱图
 * PUT    /api/family-trees/{id}/data               保存族谱图数据（members + relations）
 */
export const familyTreeApi = {
  listFamilyTrees(bookId: number): Promise<FamilyTreeResponse[]> {
    return request.get(`/books/${bookId}/family-trees`)
  },

  createFamilyTree(
    bookId: number,
    data: CreateFamilyTreeRequest
  ): Promise<FamilyTreeResponse> {
    return request.post(`/books/${bookId}/family-trees`, data)
  },

  /** 详情：把后端的「字符串 data + 顶层 members/relations」归一成 data 对象 */
  getFamilyTree(id: number): Promise<FamilyTreeDetailResponse> {
    // 拦截器已把 Result<T> 解包成 data；axios 的 AxiosResponse 类型不反映该行为，用 unknown 过桥。
    return request
      .get(`/family-trees/${id}`)
      .then((res) => normalizeFamilyDetail(res as unknown as FamilyTreeDetailRaw))
  },

  updateFamilyTree(
    id: number,
    data: UpdateFamilyTreeRequest
  ): Promise<FamilyTreeResponse> {
    return request.put(`/family-trees/${id}`, data)
  },

  deleteFamilyTree(id: number): Promise<void> {
    return request.delete(`/family-trees/${id}`)
  },

  saveFamilyTreeData(id: number, data: FamilyTreeData): Promise<void> {
    return request.put(`/family-trees/${id}/data`, data)
  }
}
