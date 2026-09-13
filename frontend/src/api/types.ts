/* ============================================================
 * 推理笔记 API · 类型定义
 * 严格 1:1 对应 D:\文本\推理笔记 API.openapi.json 的 components.schemas
 * 字段名一律照抄文档，不做任何"顺手改名"
 * ============================================================ */

/* ---------- 书籍 · Book ---------- */

export type CoverType = 'color' | 'image'

export interface BookResponse {
  id: number
  name: string
  coverType: CoverType
  coverValue: string | null
  coverText: string | null
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface CreateBookRequest {
  name: string
  coverType?: CoverType
  coverValue?: string | null
  coverText?: string | null
}

export interface UpdateBookRequest {
  name?: string | null
  coverType?: CoverType | null
  coverValue?: string | null
  coverText?: string | null
}

export interface CoverResponse {
  coverValue: string
}

/* ---------- 事件 · Event ---------- */

export interface EventResponse {
  id: number
  bookId: number
  name: string
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface CreateEventRequest {
  name: string
}

export interface UpdateEventRequest {
  name?: string | null
}

/* ---------- 页面 · Page ---------- */

export interface PageResponse {
  id: number
  eventId: number
  name: string
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface CreatePageRequest {
  name: string
}

export interface UpdatePageRequest {
  name?: string | null
}

/* ---------- 排序 ---------- */

export interface SortItem {
  id: number
  sortOrder: number
}

/* ---------- 画布 · Canvas ---------- */

export type ObjectShape = 'ellipse' | 'rect' | 'diamond' | 'custom'

/** 画布对象 type 的自由取值（文档为 string）；语义色映射见 canvasTokens.ts */
export type CanvasObjectType = 'person' | 'event' | 'thing' | string

export interface CanvasObject {
  id: string
  type: CanvasObjectType
  shape: ObjectShape
  name: string
  description?: string | null
  x: number
  y: number
  width: number
  height: number
  style?: Record<string, unknown>
  customFields?: Record<string, unknown>
}

export type RelationshipType = 'unidirectional' | 'bidirectional' | 'dashed'

export interface Relationship {
  id: string
  source: string
  target: string
  type: RelationshipType
  label: string
  style?: Record<string, unknown>
}

export interface Annotation {
  id: string
  x: number
  y: number
  width: number
  height: number
  /** HTML 片段，如 "<p>注解内容</p>" */
  content: string
  style?: Record<string, unknown>
}

export type TimelineDirection = 'horizontal' | 'vertical'
export type TimeType = 'exact' | 'range' | 'fuzzy'
export type FuzzyPeriod =
  | 'morning'
  | 'afternoon'
  | 'evening'
  | 'night'
  | 'noon'
  | 'unknown'

export interface TimelinePoint {
  id: string
  timeType: TimeType
  time?: string | null
  startTime?: string | null
  endTime?: string | null
  fuzzyDate?: string | null
  fuzzyPeriod?: FuzzyPeriod | null
  /** 后端解析出的真实区间，前端排布优先用它 */
  resolvedStart?: string | null
  resolvedEnd?: string | null
  label: string
  description?: string | null
  labelOffset?: { x: number; y: number }
}

export interface Timeline {
  id: string
  name: string
  direction: TimelineDirection
  points: TimelinePoint[]
  /** 面板在画布上的左上角坐标（后端新增字段，缺省时前端按序落位） */
  x?: number
  y?: number
}

/** GET /api/pages/{id}/canvas 返回体，同时是 PUT 的请求体 */
export interface CanvasResponse {
  objects: CanvasObject[]
  relationships: Relationship[]
  annotations: Annotation[]
  timelines: Timeline[]
}

/* ---------- 关系图 · RelationGraph ---------- */

export interface RelationGraphResponse {
  id: number
  bookId: number
  name: string
  createdAt: string
  updatedAt: string
}

export interface GraphNode {
  id: string
  name: string
  type: string
}

export interface GraphEdge {
  id: string
  source: string
  target: string
  label: string
  type: string
}

export interface RelationGraphData {
  nodes: GraphNode[]
  edges: GraphEdge[]
}

export interface RelationGraphDetailResponse {
  id: number
  bookId: number
  name: string
  data: RelationGraphData
  createdAt: string
  updatedAt: string
}

export interface CreateRelationGraphRequest {
  name: string
}

export interface UpdateRelationGraphRequest {
  name?: string | null
}

export interface ExtractRelationGraphRequest {
  objectTypes?: string[]
  relationTypes?: string[]
}

/* ---------- 工作区 · Workspace ---------- */

export interface WorkspacePage {
  id: number
  name: string
  sortOrder: number
}

export interface WorkspaceEvent {
  id: number
  name: string
  sortOrder: number
  pages: WorkspacePage[]
}

export interface WorkspaceRelationGraph {
  id: number
  name: string
}

export interface BookWorkspaceVO {
  book: BookResponse
  events: WorkspaceEvent[]
  relationGraphs: WorkspaceRelationGraph[]
}

/* ---------- 前端本地枚举（非接口字段） ---------- */

/** 画布左侧工具条的 8 个工具 */
export type CanvasTool =
  | 'select'
  | 'person'
  | 'event'
  | 'thing'
  | 'relation'
  | 'timeline'
  | 'annotation'
  | 'relationGraph'
