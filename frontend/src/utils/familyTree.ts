/* ============================================================
 * 族谱图 —— 数据模型 + 排布
 *
 * 与「人物关系图」的关系：
 *   · 交互模式一样 —— 同样是「列表层 → 详情层」，同样有 列表 / 图形 两种视图，
 *     同样在详情层里增删对象与关系；
 *   · 数据来源不同 —— 关系图可以「从画布 / 全书自动提取」，
 *     族谱图的**所有成员必须手工建立**，因此没有提取入口；
 *   · 排布不同 —— 关系图是中心切换的环状排布，
 *     族谱图是「一代一行」的层级排布（父母在上、子女在下，配偶并排）。
 *
 * 数据一律走后端（tag「族谱图」，见 api/familyTree.ts），
 * **不使用 localStorage**，所以换设备、换浏览器都能拿到同一份族谱。
 * 本文件只负责：类型（从 api/types 转出）+ 纯排布算法。
 * ============================================================ */

import type {
  FamilyGender,
  FamilyMember,
  FamilyRelType,
  FamilyTreeData
} from '@/api/types'

// 数据模型统一放在 api/types.ts，这里转出，页面只认这一个来源
export type {
  FamilyGender,
  FamilyMember,
  FamilyRelation,
  FamilyRelType,
  FamilyTreeData
} from '@/api/types'

/** 前后端共用的族谱结构：id 是后端给的主键 */
export interface FamilyTree extends FamilyTreeData {
  id: number
  name: string
  createdAt: string
  updatedAt: string
}

export const GENDER_LABEL: Record<FamilyGender, string> = {
  male: '男',
  female: '女',
  unknown: '不详'
}

export const FAMILY_REL_LABEL: Record<FamilyRelType, string> = {
  'parent-child': '父母 → 子女',
  spouse: '配偶'
}

function localId(prefix: string): string {
  return `${prefix}_${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}`
}

export function newMemberId(): string {
  return localId('fm')
}

export function newRelationId(): string {
  return localId('fr')
}

/* ============================================================
 * 排布：一代一行
 * ========================================================== */

export interface FamilyLayoutNode {
  id: string
  name: string
  gender: FamilyGender
  meta: string
  /** 节点左上角 */
  x: number
  y: number
  w: number
  h: number
  /** 第几代（0 为最早一代） */
  gen: number
}

export interface FamilyLayoutEdge {
  id: string
  kind: FamilyRelType
  path: string
}

export interface FamilyLayout {
  nodes: FamilyLayoutNode[]
  edges: FamilyLayoutEdge[]
  /** 夫妻之间的横线落在哪些位置（画一个小圆点更好认） */
  spouses: { id: string; x: number; y: number }[]
  width: number
  height: number
  generations: number
}

export interface FamilyLayoutOptions {
  nodeW?: number
  nodeH?: number
  memberGap?: number
  unitGap?: number
  rowGap?: number
  pad?: number
}

const DEFAULT_OPT: Required<FamilyLayoutOptions> = {
  nodeW: 104,
  nodeH: 34,
  memberGap: 12,
  unitGap: 48,
  rowGap: 96,
  pad: 28
}

/** 生卒年文案：1901 / 1901-1968 */
function lifeText(m: FamilyMember): string {
  const b = (m.birth ?? '').trim()
  const d = (m.death ?? '').trim()
  if (b && d) return `${b}-${d}`
  return b || d || ''
}

/** 从生年里抠出四位年份，用于同一代内部的先后排序 */
function yearOf(m: FamilyMember): number | undefined {
  const hit = (m.birth ?? '').match(/(\d{4})/)
  return hit ? Number(hit[1]) : undefined
}

export function layoutFamilyTree(tree: FamilyTree, opts: FamilyLayoutOptions = {}): FamilyLayout {
  const O = { ...DEFAULT_OPT, ...opts }
  const members = tree.members ?? []
  const relations = tree.relations ?? []
  const empty: FamilyLayout = {
    nodes: [],
    edges: [],
    spouses: [],
    width: 320,
    height: 220,
    generations: 0
  }
  if (!members.length) return empty

  const byId = new Map(members.map((m) => [m.id, m]))
  const index = new Map(members.map((m, i) => [m.id, i]))
  const pcEdges = relations.filter(
    (r) => r.type === 'parent-child' && byId.has(r.from) && byId.has(r.to)
  )
  const spEdges = relations.filter(
    (r) => r.type === 'spouse' && byId.has(r.from) && byId.has(r.to)
  )

  /* ---- 1. 配偶并查集：一对夫妻算一个「单元」，保证并排 ---- */
  const uf = new Map<string, string>()
  members.forEach((m) => uf.set(m.id, m.id))
  const find = (a: string): string => {
    let r = a
    while (uf.get(r) !== r) r = uf.get(r) as string
    // 路径压缩
    let c = a
    while (uf.get(c) !== r) {
      const n = uf.get(c) as string
      uf.set(c, r)
      c = n
    }
    return r
  }
  const union = (a: string, b: string): void => {
    const ra = find(a)
    const rb = find(b)
    if (ra === rb) return
    // 让「先出现的成员」当根，结果稳定可预期
    const ia = index.get(ra) ?? 0
    const ib = index.get(rb) ?? 0
    if (ia <= ib) uf.set(rb, ra)
    else uf.set(ra, rb)
  }
  spEdges.forEach((e) => union(e.from, e.to))

  /* ---- 2. 代数：按「配偶单元」松弛，单元内成员共享代数 ----
   * ⚠️ 这里必须按「单元」算，不能按「个人」算。
   * 若按个人算：配偶里若是「外来者」（自己父母没录进族谱），
   * 它的个人代数会一直停在 0，那么它与血亲所生的子女只会得到 1 ——
   * 于是第三代往后的子孙被排到上面几行、连线向上折返，
   * 表现为「三代之后接不下去」。
   * 按单元算：夫妻共享代数，子女的单元 = 父母单元 + 1，代数一路往下传。
   * ------------------------------------------------------ */
  const unitGen = new Map<string, number>()
  members.forEach((m) => unitGen.set(find(m.id), 0))
  for (let iter = 0; iter <= members.length; iter++) {
    let changed = false
    for (const e of pcEdges) {
      const up = find(e.from)
      const uc = find(e.to)
      // 同一单元内部的亲子关系（数据异常）跳过，否则会自增不停
      if (up === uc) continue
      const want = (unitGen.get(up) ?? 0) + 1
      if ((unitGen.get(uc) ?? 0) < want) {
        unitGen.set(uc, want)
        changed = true
      }
    }
    if (!changed) break
  }
  const genOf = (id: string): number => unitGen.get(find(id)) ?? 0

  /* ---- 3. 单元 ---- */
  interface Unit {
    id: string
    members: FamilyMember[]
    gen: number
    order: number
    key: number
    year?: number
    width: number
  }
  const unitMap = new Map<string, Unit>()
  members.forEach((m) => {
    const root = find(m.id)
    let u = unitMap.get(root)
    if (!u) {
      u = {
        id: root,
        members: [],
        gen: 0,
        order: Number.POSITIVE_INFINITY,
        key: 0,
        width: 0
      }
      unitMap.set(root, u)
    }
    u.members.push(m)
  })
  unitMap.forEach((u) => {
    u.gen = genOf(u.id)
    u.order = Math.min(...u.members.map((m) => index.get(m.id) ?? 0))
    // 单元内部按「先出现的排左边」，观感稳定
    u.members.sort((a, b) => (index.get(a.id) ?? 0) - (index.get(b.id) ?? 0))
    const years = u.members.map(yearOf).filter((y): y is number => y !== undefined)
    u.year = years.length ? Math.min(...years) : undefined
    u.width = u.members.length * O.nodeW + (u.members.length - 1) * O.memberGap
  })

  /* ---- 4. 分行 + 行内排序（尽量让子女落在父母正下方） ---- */
  const maxGen = unitMap.size ? Math.max(...[...unitMap.values()].map((u) => u.gen)) : 0
  const rows: Unit[][] = []
  for (let g = 0; g <= maxGen; g++) rows.push([])
  unitMap.forEach((u) => rows[u.gen].push(u))

  const unitIndexInRow = new Map<string, number>()
  rows.forEach((row, g) => {
    if (g > 0) {
      row.forEach((u) => {
        const keys: number[] = []
        u.members.forEach((m) => {
          pcEdges.forEach((e) => {
            if (e.to !== m.id) return
            const pi = unitIndexInRow.get(find(e.from))
            if (pi !== undefined) keys.push(pi)
          })
        })
        u.key = keys.length ? keys.reduce((a, b) => a + b, 0) / keys.length : Number.POSITIVE_INFINITY
      })
    }
    row.sort((a, b) => {
      // ① 尽量落在父母正下方
      if (a.key !== b.key) return a.key - b.key
      // ② 都有生年时，年长的排左边（兄弟次序）
      if (a.year !== undefined && b.year !== undefined && a.year !== b.year) {
        return a.year - b.year
      }
      // ③ 最后按录入顺序，保证结果稳定
      return a.order - b.order
    })
    row.forEach((u, i) => unitIndexInRow.set(u.id, i))
  })

  /* ---- 5. 定位 ---- */
  const rowWidths = rows.map(
    (row) =>
      row.reduce((s, u) => s + u.width, 0) + Math.max(row.length - 1, 0) * O.unitGap
  )
  const contentW = Math.max(...rowWidths, 0)
  const width = contentW + O.pad * 2
  const height = O.pad * 2 + Math.max(rows.length - 1, 0) * O.rowGap + O.nodeH

  const nodes: FamilyLayoutNode[] = []
  const pos = new Map<string, { x: number; y: number }>()
  const unitBox = new Map<string, { left: number; right: number; bottomY: number }>()

  rows.forEach((row, g) => {
    const y = O.pad + g * O.rowGap
    let x = O.pad + (contentW - rowWidths[g]) / 2
    row.forEach((u) => {
      let mx = x
      u.members.forEach((m) => {
        pos.set(m.id, { x: mx, y })
        nodes.push({
          id: m.id,
          name: m.name || '未命名',
          gender: m.gender ?? 'unknown',
          meta: lifeText(m),
          x: mx,
          y,
          w: O.nodeW,
          h: O.nodeH,
          gen: g
        })
        mx += O.nodeW + O.memberGap
      })
      unitBox.set(u.id, {
        left: x,
        right: x + u.width,
        bottomY: y + O.nodeH
      })
      x += u.width + O.unitGap
    })
  })

  /* ---- 6. 连线 ---- */
  const edges: FamilyLayoutEdge[] = []
  const spouses: { id: string; x: number; y: number }[] = []

  // 夫妻：并排两人之间一条横线，中间一个小圆点
  spEdges.forEach((r) => {
    const a = pos.get(r.from)
    const b = pos.get(r.to)
    if (!a || !b) return
    const [l, rt] = a.x <= b.x ? [a, b] : [b, a]
    const y = l.y + O.nodeH / 2
    edges.push({
      id: r.id,
      kind: 'spouse',
      path: `M ${l.x + O.nodeW} ${y} H ${rt.x}`
    })
    spouses.push({ id: r.id, x: (l.x + O.nodeW + rt.x) / 2, y })
  })

  // 亲子：父母下缘 → 一条母线 → 子女上缘（同一对父母共用一个下探点）
  const parentsOf = new Map<string, string[]>()
  pcEdges.forEach((e) => {
    const list = parentsOf.get(e.to) ?? []
    list.push(e.from)
    parentsOf.set(e.to, list)
  })

  parentsOf.forEach((parents, childId) => {
    const c = pos.get(childId)
    if (!c) return
    const roots = new Set(parents.map((p) => find(p)))
    let sx: number
    let sy: number
    if (roots.size === 1) {
      // 同属一个单元（一对夫妻）：从两人中间下探，视觉上是一根主干
      const box = unitBox.get([...roots][0])
      const p0 = pos.get(parents[0])
      if (!box || !p0) return
      sx = (box.left + box.right) / 2
      sy = box.bottomY
    } else {
      const pts = parents.map((p) => pos.get(p)).filter(Boolean) as { x: number; y: number }[]
      if (!pts.length) return
      sx = pts.reduce((s, p) => s + p.x + O.nodeW / 2, 0) / pts.length
      sy = Math.max(...pts.map((p) => p.y)) + O.nodeH
    }
    const tx = c.x + O.nodeW / 2
    const ty = c.y
    const busY = sy + Math.max(24, O.rowGap * 0.3)
    edges.push({
      id: `pc_${childId}`,
      kind: 'parent-child',
      path: `M ${sx} ${sy} V ${busY} H ${tx} V ${ty}`
    })
  })

  return { nodes, edges, spouses, width, height, generations: rows.length }
}
