/* ============================================================
 * 自适应时间线排布算法 —— PROMPT.md §5.5
 *
 * 视觉规范：
 *  1. 计算所有时间点的区间跨度，得出平均间隔 avg
 *  2. 若某间隔 > avg × 2.5 → 判定为「跨度异常」，做对数压缩：
 *       rendered = base_gap × ln(1 + span / avg) / ln(2)
 *  3. 模糊时段（fuzzyPeriod）整段压缩到固定宽度，并在下方标注
 *     「（模糊时段 · 已压缩）」
 *  4. 首尾节点距边缘保留 42px 呼吸
 * ============================================================ */

import type { FuzzyPeriod, Timeline, TimelinePoint } from '@/api/types'

/** 模糊时段 → 用于排布的兜底时刻（当天 24h 制） */
const FUZZY_HOUR: Record<FuzzyPeriod, number> = {
  morning: 8,
  noon: 12,
  afternoon: 15,
  evening: 19,
  night: 23,
  unknown: 12
}

/** 模糊时段 → 中文刻度文案 */
export const FUZZY_LABEL: Record<FuzzyPeriod, string> = {
  morning: '清晨',
  noon: '正午',
  afternoon: '下午',
  evening: '傍晚',
  night: '深夜',
  unknown: '时间不明'
}

/** 时间点渲染出的确定度 */
export type Certainty = 'exact' | 'estimated' | 'fuzzy'

/** 已解算的时间点，坐标为轴上的百分比 (0–100) */
export interface LaidOutPoint {
  point: TimelinePoint
  /** 排序 / 计算用的毫秒时间戳 */
  ts: number
  /** 已解算的真实区间（用于 range 类型） */
  start: number
  end: number
  /** 轴上的百分比位置 */
  pos: number
  certainty: Certainty
  /** 刻度文案 */
  tick: string
  /** 是否处于被压缩的模糊时段 */
  compressed: boolean
  /** 压缩段标注文案 */
  compressNote?: string
}

export interface TimelineLayout {
  points: LaidOutPoint[]
  /** 是否存在被压缩的时段 */
  hasCompression: boolean
  /** 时间跨度是否过于接近（只有一个点或全部同一时刻） */
  degenerate: boolean
}

/** 边缘呼吸 42px，按轴长折算成百分比由调用方传入 axisWidth */
const BREATH_PX = 42
/** 异常跨度阈值倍数 */
const ANOMALY_RATIO = 2.5
/** 模糊时段压缩后的固定宽度（px） */
const FUZZY_COMPRESS_PX = 76

function toTs(point: TimelinePoint): number {
  const raw =
    point.resolvedStart ??
    point.time ??
    point.startTime ??
    point.fuzzyDate ??
    null
  if (raw) {
    const t = +new Date(raw)
    if (!Number.isNaN(t)) return t
  }
  if (point.fuzzyDate) {
    const d = +new Date(`${point.fuzzyDate}T12:00:00`)
    if (!Number.isNaN(d)) return d
  }
  return Number.NaN
}

function certaintyOf(point: TimelinePoint): Certainty {
  switch (point.timeType) {
    case 'exact':
      return 'exact'
    case 'range':
      return 'estimated'
    case 'fuzzy':
    default:
      return 'fuzzy'
  }
}

function formatTick(ts: number, point: TimelinePoint): string {
  const d = new Date(ts)
  if (Number.isNaN(d.getTime())) return point.label
  const M = d.getMonth() + 1
  const D = d.getDate()

  if (point.timeType === 'fuzzy') {
    const period = point.fuzzyPeriod ? FUZZY_LABEL[point.fuzzyPeriod] : ''
    return period ? `${M}/${D} ${period}` : `${M}/${D}`
  }

  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  // 整点不显示分钟，降低刻度噪音
  return mm === '00' ? `${M}/${D} ${hh}:00` : `${M}/${D} ${hh}:${mm}`
}

/**
 * 解算一条时间线的排布。
 * @param timeline 接口返回的 Timeline
 * @param axisWidth 轴的实际像素长度（横向取宽、纵向取高）
 */
export function layoutTimeline(timeline: Timeline, axisWidth: number): TimelineLayout {
  const raw = timeline.points ?? []
  if (!raw.length) {
    return { points: [], hasCompression: false, degenerate: true }
  }

  // ---- 1. 归一化出每个点的起止时间戳 ----
  const normalized = raw
    .map((point) => {
      // 模糊时段：先取 fuzzyDate 当天，再用 fuzzyPeriod 折算时刻
      if (point.timeType === 'fuzzy') {
        const base = point.fuzzyDate ? +new Date(`${point.fuzzyDate}T00:00:00`) : NaN
        if (!Number.isNaN(base)) {
          const hour = point.fuzzyPeriod ? FUZZY_HOUR[point.fuzzyPeriod] : 12
          const ts = base + hour * 3600 * 1000
          return { point, ts, start: ts, end: ts + 3600 * 1000 }
        }
      }
      const ts = toTs(point)
      const start = ts
      const end = point.resolvedEnd ? +new Date(point.resolvedEnd) : ts
      return { point, ts, start, end: Number.isNaN(end) ? ts : end }
    })
    .filter((x) => !Number.isNaN(x.ts))

  if (!normalized.length) {
    return { points: [], hasCompression: false, degenerate: true }
  }

  normalized.sort((a, b) => a.start - b.start)

  const min = normalized[0].start
  const max = normalized.reduce((m, x) => Math.max(m, x.end), normalized[0].end)
  const spanTotal = max - min

  // ---- 只有一个时刻 / 全部重合：全部居中排布 ----
  if (spanTotal <= 0) {
    return {
      points: normalized.map((x) => ({
        point: x.point,
        ts: x.ts,
        start: x.start,
        end: x.end,
        pos: 50,
        certainty: certaintyOf(x.point),
        tick: formatTick(x.ts, x.point),
        compressed: false
      })),
      hasCompression: false,
      degenerate: true
    }
  }

  // ---- 2. 计算相邻间隔，得到 avg ----
  const gaps: number[] = []
  for (let i = 1; i < normalized.length; i++) {
    gaps.push(normalized[i].start - normalized[i - 1].start)
  }
  const positive = gaps.filter((g) => g > 0)
  const avg = positive.length
    ? positive.reduce((a, b) => a + b, 0) / positive.length
    : spanTotal

  // ---- 3. 逐段累计渲染位置（对数压缩异常跨度） ----
  const usable = Math.max(axisWidth - BREATH_PX * 2, 120)
  const segments: { rendered: number; compressed: boolean; fuzzy: boolean }[] = []

  for (let i = 0; i < normalized.length - 1; i++) {
    const span = Math.max(normalized[i + 1].start - normalized[i].start, 0)
    const isFuzzy = normalized[i + 1].point.timeType === 'fuzzy'
    let rendered: number
    let compressed = false

    if (isFuzzy) {
      // 模糊时段整段压到固定宽度
      rendered = FUZZY_COMPRESS_PX
      compressed = true
    } else if (span > avg * ANOMALY_RATIO) {
      rendered = Math.log(1 + span / avg) / Math.log(2)
      compressed = true
    } else {
      rendered = span / avg
    }
    segments.push({ rendered, compressed, fuzzy: isFuzzy })
  }

  const totalRendered = segments.reduce((s, x) => s + x.rendered, 0) || 1
  const scale = usable / totalRendered

  // ---- 4. 输出百分比坐标（首尾保留 42px 呼吸） ----
  const breathPct = (BREATH_PX / Math.max(axisWidth, 1)) * 100
  const usablePct = 100 - breathPct * 2

  const result: LaidOutPoint[] = []
  let cursorPx = 0

  normalized.forEach((x, i) => {
    if (i > 0) cursorPx += segments[i - 1].rendered * scale
    const pos = breathPct + (cursorPx / usable) * usablePct
    const isFuzzy = x.point.timeType === 'fuzzy'
    const prevSeg = i > 0 ? segments[i - 1] : null

    result.push({
      point: x.point,
      ts: x.ts,
      start: x.start,
      end: x.end,
      pos: Math.min(Math.max(pos, 0), 100),
      certainty: certaintyOf(x.point),
      tick: formatTick(x.ts, x.point),
      compressed: isFuzzy,
      compressNote: isFuzzy
        ? `（${x.point.fuzzyPeriod ? FUZZY_LABEL[x.point.fuzzyPeriod] : '模糊时段'} · 已压缩）`
        : prevSeg?.compressed && !prevSeg.fuzzy
          ? '（跨度已压缩）'
          : undefined
    })
  })

  return {
    points: result,
    hasCompression: result.some((p) => p.compressed),
    degenerate: false
  }
}

/** 坐标 → 轴上的 px 位置（用于绝对定位渲染） */
export function posToPx(pos: number, axisWidth: number): number {
  return (pos / 100) * axisWidth
}
