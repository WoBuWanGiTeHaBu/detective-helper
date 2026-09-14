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
  /**
   * 内容最后改动时间（可选，后端第十节新增）：
   * 只有事件/页面/画布/图类等**内容**写入才刷新；改书名、换封面不动它。
   * 书架「按最近编辑」排序用它，缺省时前端回退 updatedAt。
   */
  contentUpdatedAt?: string | null
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
  /** 横向时的面板宽度（后端新增字段，缺省 760） */
  width?: number
  /** 纵向时的面板高度（后端新增字段，缺省 400） */
  height?: number
}

/** 画布背景：纯色 / 点阵 / 方格 / 横线 */
export type CanvasBackground = 'plain' | 'grid' | 'dot' | 'line'

/**
 * GET /api/pages/{id}/canvas 返回体，同时是 PUT 的请求体
 * 后端保证四个数组非 null（最坏是 []），可直接原样回传
 */
export interface CanvasResponse {
  /** 缺省 'plain' */
  background?: CanvasBackground | string | null
  /**
   * 画布尺寸（逻辑单位，与对象坐标同一坐标系）：
   * 画布是一张有明确尺寸的纸，超出可视区域的部分由容器滚动，
   * 不再「拖到哪长到哪」。缺省时前端按 1600×1200 兜底。
   */
  canvasWidth?: number | null
  canvasHeight?: number | null
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

/**
 * 详情接口的原始返回。
 * 注意：后端 `data` 是库里的 JSON **字符串**，同时把解析好的 nodes / edges
 * 平铺在顶层（RelationGraphDetailVO）。页面统一用下面的 RelationGraphDetailResponse。
 */
export interface RelationGraphDetailRaw {
  id: number
  bookId: number
  name: string
  data: string | null
  nodes?: GraphNode[] | null
  edges?: GraphEdge[] | null
  createdAt: string
  updatedAt: string
}

/** 归一后给页面用的形状：data 一定是对象，下游可以放心 .data.nodes */
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

/* ---------- 族谱图 · FamilyTree ----------
 * 与关系图同构：列表 / 创建 / 详情 / 更新 / 删除 / 存数据，
 * 只是载荷从 nodes+edges 换成 members+relations。
 * 详情同样「顶层 members/relations 为准 + data 字符串兜底」。
 * ------------------------------------------ */

export type FamilyGender = 'male' | 'female' | 'unknown'

/** parent-child：from 是父/母，to 是子女；spouse：from / to 互为配偶 */
export type FamilyRelType = 'parent-child' | 'spouse'

export interface FamilyMember {
  id: string
  name: string
  gender: FamilyGender
  /** 生卒年，纯文本，如 "1901" / "1901-1968" */
  birth?: string
  death?: string
  note?: string
}

export interface FamilyRelation {
  id: string
  type: FamilyRelType
  from: string
  to: string
}

export interface FamilyTreeData {
  members: FamilyMember[]
  relations: FamilyRelation[]
}

export interface FamilyTreeResponse {
  id: number
  bookId: number
  name: string
  createdAt: string
  updatedAt: string
}

/** 详情接口的原始返回：data 是字符串，members / relations 平铺在顶层 */
export interface FamilyTreeDetailRaw {
  id: number
  bookId: number
  name: string
  data: string | null
  members?: FamilyMember[] | null
  relations?: FamilyRelation[] | null
  createdAt: string
  updatedAt: string
}

/** 归一后给页面用的形状：data 一定是对象 */
export interface FamilyTreeDetailResponse {
  id: number
  bookId: number
  name: string
  data: FamilyTreeData
  createdAt: string
  updatedAt: string
}

export interface CreateFamilyTreeRequest {
  name: string
}

export interface UpdateFamilyTreeRequest {
  name?: string | null
}

/* ---------- 用户资料 · Profile（第十一节，可后置实现） ---------- */

/** 界面主题：light 现已启用；dark / sepia 为预留值，UI 上先占位 */
export type ThemeId = 'light' | 'dark' | 'sepia'

export interface UserProfileResponse {
  /** 展示名，缺省「用户」 */
  displayName: string
  /** 主题标识，缺省 light */
  theme: string
}

export interface UpdateProfileRequest {
  displayName?: string
  theme?: string
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

/** 画布左侧工具条的工具 */
export type CanvasTool =
  | 'select'
  | 'moveContent'
  | 'canvasSize'
  | 'person'
  | 'event'
  | 'thing'
  | 'relation'
  | 'timeline'
  | 'annotation'
  | 'relationGraph'
