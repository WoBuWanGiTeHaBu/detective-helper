/* ============================================================
 * 关系图布局工具
 *
 * 1. nodeShapeOf —— 把自由字符串 type 归一成三类视觉形态
 *    （人物 / 事件 / 事物），画布、抽屉、独立关系图页共用同一套判定，
 *    保证「人物与人物、事物与事物」在所有地方都长得不一样。
 *
 * 2. radialLayout —— 中心切换式布局（参照给的示例）：
 *    把当前焦点人物放在正中，它的直接关联对象均匀排在一圈上。
 *    点圈上的任一对象即可把它切成新的中心，于是整张图重新排布。
 * ============================================================ */

export type NodeKind = 'person' | 'event' | 'thing'

/** type 是自由字符串，这里做三类归一（和画布节点保持一致） */
export function nodeShapeOf(type?: string | null): NodeKind {
  const t = (type ?? '').toLowerCase()
  if (t === 'person' || t === '人物') return 'person'
  if (t === 'event' || t === '事件' || t === 'time') return 'event'
  return 'thing'
}

export function kindLabel(kind: NodeKind): string {
  return kind === 'person' ? '人物' : kind === 'event' ? '事件' : '事物'
}

/**
 * 人物节点是「圆形 + 名字」，半径按名字长度自适应。
 *
 * 这一版整体收小了一档：原来 38–46 的圆摆在环上显得笨重，几个邻居就把
 * 画面填满了，勾连关系的那点留白全被吃掉。现在是两个字 28、三个字 34、
 * 四个字及以上 40，配 12px 的名字仍然排得开，视觉重量却轻得多。
 */
export function personRadius(name?: string | null): number {
  const len = (name ?? '').trim().length || 2
  return Math.max(28, Math.min(40, 16 + len * 6))
}

/**
 * 环形布局的推荐半径。
 * 两个约束取大者：
 *   ① 中心圆 + 邻居圆不能互相压住；
 *   ② 圆周长度够放下所有邻居（各自直径 + 间距），否则一多就叠成一坨。
 *
 * gap 是**相邻两圆边缘之间的净空隙**，不是圆心距 —— 圆变小之后视图侧的
 * 调用都把它调大了（见 RelationGraphView / BookWorkspaceView），
 * 否则圆一小、圆心距没变，读起来反而更挤。
 */
export function ringRadius(centerR: number, neighborRs: number[], gap = 34): number {
  const n = neighborRs.length
  if (!n) return centerR + gap
  const cleared = centerR + Math.max(...neighborRs) + gap
  const need = neighborRs.reduce((s, r) => s + r * 2 + gap, 0)
  return Math.max(cleared, need / (Math.PI * 2))
}

export interface Point {
  x: number
  y: number
}

export interface RadialOptions {
  cx: number
  cy: number
  /** 环绕半径 */
  radius: number
  /** 起始角度（弧度），默认从正上方开始 */
  startAngle?: number
}

/**
 * 中心 + 一圈邻居。
 * @param centerId    中心对象 id
 * @param neighborIds 直接关联的对象 id（顺序决定排布顺序，越靠前越先出现）
 */
export function radialLayout(
  centerId: string,
  neighborIds: string[],
  opts: RadialOptions
): Record<string, Point> {
  const { cx, cy, radius, startAngle = -Math.PI / 2 } = opts
  const out: Record<string, Point> = {}
  if (centerId) out[centerId] = { x: cx, y: cy }

  const n = neighborIds.length
  if (!n) return out

  neighborIds.forEach((id, i) => {
    const angle = startAngle + (i / n) * Math.PI * 2
    out[id] = {
      x: cx + radius * Math.cos(angle),
      y: cy + radius * Math.sin(angle)
    }
  })
  return out
}

/**
 * 从一组「关系」里挑出与 centerId 直接相关、并且仍然通过类型过滤的那些 id。
 * keep 用来做「人物↔人物 / 事物↔事物」过滤。
 */
export function neighborsOf(
  centerId: string,
  edges: { source: string; target: string }[],
  keep: (id: string) => boolean
): string[] {
  const seen = new Set<string>()
  const out: string[] = []
  for (const e of edges) {
    const other = e.source === centerId ? e.target : e.target === centerId ? e.source : null
    if (!other || other === centerId) continue
    if (seen.has(other)) continue
    if (!keep(other)) continue
    seen.add(other)
    out.push(other)
  }
  return out
}
