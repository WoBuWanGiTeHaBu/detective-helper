/* ============================================================
 * 自适应时间线排布算法
 *
 * 两条线：
 *
 *  A. 比例（横向 / 纵向通用）
 *     1. 把每个时间点的起点摊成一条有序断点序列；时间段再补一个终点断点，
 *        这样「时间段」才有真实长度可以画。
 *     2. 相邻断点的真实间隔 gap，以全部正间隔的**中位数 med** 为基准归一。
 *        （中位数比平均数稳，不会被一个离群点带跑）
 *     3. 渲染权重 w = BASE + log2(1 + gap / med)，并用 LOG_CAP 封顶：
 *          gap = 0      同一时刻   → w = BASE，仍留出可读间距
 *          gap = med    正常相邻   → w = BASE + 1
 *          gap = 100×med 离群点    → 只吃掉 LOG_CAP 个正常步长
 *        ——「突然冒出一个离现在很远的时间点」不会把主时间簇挤成一坨。
 *     4. 权重归一到可用像素，首尾各留 42px 呼吸。
 *
 *  B. 分道（这一版新增，解决「挤在一起看不清」）
 *     5. 事件卡片按 x（纵向按 y）**贪心分道**：能放进第 0 道就放第 0 道，
 *        放不下才往下加一道。于是：
 *          · 相邻很近的时间点 → 自动上下错开，不压字
 *          · 同一时刻的多个事件 → 各自占道，全部可读
 *        道数受 maxLanes 限制，超出后复用最空的一条。
 *     6. 刻度文字同理分道，并且**同刻只画一次**（tickHidden），
 *        免得同一时刻堆出三行一模一样的 "3/14 20:00"。
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
  /** 已解算的真实区间 */
  start: number
  end: number
  /** 轴上的百分比位置（起点，或单点位置） */
  pos: number
  /** 时间段的终点位置；无跨度时等于 pos */
  endPos: number
  /** 是否存在真实跨度（用于画「马克笔」时间段） */
  hasSpan: boolean
  /** 时间段的时长文案，如「3 天」 */
  spanText: string
  certainty: Certainty
  /** 刻度文案 */
  tick: string
  /** 事件卡片分道号（0 / 1 / 2 …） */
  labelRow: number
  /** 刻度文字分道号 */
  tickRow: number
  /** 同刻刻度已被前一个点画过 → 这个点不再重复画 */
  tickHidden: boolean
  /**
   * 时间段的**终点**刻度文案；非时间段为空串。
   * 只标起点的话，「从 3/14 20:00 到什么时候」是读不出来的 —— 轴上明明有
   * 一个有长度的区间，却只有一个时刻能看。
   */
  endTick: string
  /** 终点刻度的分道号 */
  endTickRow: number
  /** 终点刻度与别的刻度重合 → 不画 */
  endTickHidden: boolean
  /** 是否处于被压缩的跨度之后 */
  compressed: boolean
  /** 压缩段标注文案 */
  compressNote?: string
}

export interface TimelineLayout {
  points: LaidOutPoint[]
  /** 卡片用到的最大道号（0 表示只有一道） */
  labelRows: number
  /** 刻度用到的最大道号 */
  tickRows: number
  /** 是否出现过跨度压缩 */
  hasCompression: boolean
  /** 时间跨度是否过于接近（只有一个点或全部同一时刻） */
  degenerate: boolean
}

export interface LayoutOptions {
  /** 卡片在排布轴上的尺寸（横向 = 卡片宽 ~104，纵向 = 卡片高 18） */
  cardExtent?: number
  /** 同一条道上两张卡片之间的最小空隙 */
  cardGap?: number
  /** 每往下一道，卡片沿轴额外偏移多少（纵向轴靠它把卡片推开） */
  cardShift?: number
  /** 刻度文字在排布轴上的尺寸 */
  tickExtent?: number
  tickGap?: number
  tickShift?: number
  /** 最多分几道，防止极端数据把面板撑爆 */
  maxLanes?: number
}

/** 边缘呼吸 42px */
const BREATH_PX = 42
/** 同一时刻也至少给的基础权重 */
const BASE_WEIGHT = 0.75
/** 离群跨度最多占据的权重上限（防止一个远点吃掉整条轴） */
const LOG_CAP = 4
/** gap / med 超过它算「离群跨度」，会标注已压缩 */
const COMPRESS_RATIO = 3

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

/** 时间段时长文案：跨天说天，跨小时说小时，都很短就省略 */
function formatSpan(start: number, end: number): string {
  const ms = end - start
  if (!(ms > 0)) return ''
  const day = ms / 86400000
  if (day >= 1) {
    const n = Math.round(day * 10) / 10
    return `${Number.isInteger(n) ? n : n.toFixed(1)} 天`
  }
  const hour = ms / 3600000
  if (hour >= 1) {
    const n = Math.round(hour * 10) / 10
    return `${Number.isInteger(n) ? n : n.toFixed(1)} 小时`
  }
  return ''
}

function median(sorted: number[]): number {
  if (!sorted.length) return 0
  const mid = sorted.length >> 1
  return sorted.length % 2 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2
}

/**
 * 贪心分道：pxs 必须已升序。
 *
 * 每道上的卡片占 [pos + lane*shift - extent/2, pos + lane*shift + extent/2]，
 * 于是「跨道」也能正确判定：
 *   · shift = 0（横向轴）→ 同道的两张卡片必须隔开 extent + gap，
 *     所以相近的时间点会被顶到下一道；
 *   · shift > 0（纵向轴）→ 卡片顺着轴再推 shift，于是「同一时刻的事件」
 *     天然被推开 extent + shift，不需要额外的横向空间。
 *
 * 优先放第 0 道（稀疏时大家高度一致），放不下才顺延；全满则复用最靠上的一道。
 */
function allocateLanes(
  pxs: number[],
  extent: number,
  gap: number,
  maxLanes: number,
  shift = 0
): { lanes: number[]; used: number } {
  /** 已放好的卡片：在排布轴上的区间 + 所在道 */
  const placed: { top: number; bottom: number; lane: number }[] = []
  const half = extent / 2

  /**
   * 两张卡片会不会打架：
   *   · shift = 0（横向轴）→ 不同道是**垂直于轴**错开的，永远不会打架，
   *     所以只比同道的。相近的时间点才会被顶到下一道去。
   *   · shift > 0（纵向轴）→ 各道是**沿着轴**推开的，异道也可能重叠，
   *     得全量比较。
   */
  const clash = (top: number, bottom: number, lane: number): boolean =>
    placed.some((p) => {
      if (p.lane !== lane && shift === 0) return false
      return !(bottom + gap <= p.top || top >= p.bottom + gap)
    })

  const lanes = pxs.map((px) => {
    let chosen = 0
    let found = false
    for (let l = 0; l < maxLanes; l++) {
      const top = px + l * shift - half
      if (!clash(top, top + extent, l)) {
        chosen = l
        found = true
        break
      }
    }
    if (!found) {
      // 全部道都占着：选「压得最少」的那条，至少保证不越界
      let bestScore = Number.POSITIVE_INFINITY
      for (let l = 0; l < maxLanes; l++) {
        const top = px + l * shift - half
        let score = 0
        placed.forEach((p) => {
          if (p.lane !== l && shift === 0) return
          const dy = Math.min(top + extent, p.bottom) - Math.max(top, p.top)
          if (dy > 0) score += dy
        })
        if (score < bestScore) {
          bestScore = score
          chosen = l
        }
      }
    }
    const top = px + chosen * shift - half
    placed.push({ top, bottom: top + extent, lane: chosen })
    return chosen
  })

  let used = 0
  for (const l of lanes) if (l > used) used = l
  return { lanes, used }
}

/**
 * 解算一条时间线的排布。
 * @param timeline  接口返回的 Timeline
 * @param axisWidth 轴的实际像素长度（横向取宽、纵向取高）
 * @param opts      卡片 / 刻度的尺寸与分道参数（见 LayoutOptions）
 */
export function layoutTimeline(
  timeline: Timeline,
  axisWidth: number,
  opts: LayoutOptions = {}
): TimelineLayout {
  const cardExtent = opts.cardExtent ?? 104
  const cardGap = opts.cardGap ?? 8
  const cardShift = opts.cardShift ?? 0
  const tickExtent = opts.tickExtent ?? 56
  const tickGap = opts.tickGap ?? 8
  const tickShift = opts.tickShift ?? 0
  const maxLanes = Math.max(1, opts.maxLanes ?? 3)
  const raw = timeline.points ?? []
  if (!raw.length) {
    return { points: [], labelRows: 0, tickRows: 0, hasCompression: false, degenerate: true }
  }

  /* ---- 1. 归一化出每个点的起止时间戳 ---- */
  const normalized = raw
    .map((point) => {
      // 模糊时段：先取 fuzzyDate 当天，再用 fuzzyPeriod 折算时刻。
      // 它没有真实跨度 → end 就取 start，免得被当成「时间段」涂一大片。
      if (point.timeType === 'fuzzy') {
        const base = point.fuzzyDate ? +new Date(`${point.fuzzyDate}T00:00:00`) : NaN
        if (!Number.isNaN(base)) {
          const hour = point.fuzzyPeriod ? FUZZY_HOUR[point.fuzzyPeriod] : 12
          const ts = base + hour * 3600 * 1000
          return { point, ts, start: ts, end: ts }
        }
      }
      const ts = toTs(point)
      // 只有 timeType = range 才认它是「有起止的时间段」
      const isRange = point.timeType === 'range'
      const endRaw = isRange ? point.resolvedEnd ?? point.endTime ?? null : null
      const endTs = endRaw ? +new Date(endRaw) : Number.NaN
      const end = Number.isNaN(endTs) ? ts : endTs
      return { point, ts, start: ts, end }
    })
    .filter((x) => !Number.isNaN(x.ts))

  if (!normalized.length) {
    return { points: [], labelRows: 0, tickRows: 0, hasCompression: false, degenerate: true }
  }

  normalized.sort((a, b) => a.start - b.start || a.end - b.end)

  const width = Math.max(axisWidth, 160)
  const usable = Math.max(width - BREATH_PX * 2, 60)

  /* ---- 2. 断点序列：每个点的起点一个断点，有跨度的补一个终点断点 ---- */
  interface BP {
    ts: number
    pi: number
    isEnd: boolean
  }
  const bps: BP[] = []
  const startBp: BP[] = []
  const endBp: (BP | null)[] = []
  normalized.forEach((x, i) => {
    const s: BP = { ts: x.start, pi: i, isEnd: false }
    bps.push(s)
    startBp[i] = s
    if (x.end > x.start) {
      const e: BP = { ts: x.end, pi: i, isEnd: true }
      bps.push(e)
      endBp[i] = e
    } else {
      endBp[i] = null
    }
  })
  // 同一时刻：起点排在终点之前（保证「先发生、后结束」的阅读顺序）。
  // 注意比较器必须自洽：两者同类时返回 0，否则稳定排序会把同刻的点排反。
  bps.sort((a, b) => a.ts - b.ts || (a.isEnd === b.isEnd ? 0 : a.isEnd ? 1 : -1))
  const bpIndex = new Map<BP, number>()
  bps.forEach((bp, i) => bpIndex.set(bp, i))

  /* ---- 3. 间隔 → 权重（以中位数为基准做对数动态压缩） ---- */
  const gaps: number[] = []
  for (let i = 1; i < bps.length; i++) {
    gaps.push(Math.max(bps[i].ts - bps[i - 1].ts, 0))
  }
  const positives = gaps.filter((g) => g > 0).sort((a, b) => a - b)
  const med = median(positives)

  const weights = gaps.map((g) => {
    if (med <= 0) return BASE_WEIGHT
    // LOG_CAP 是「离群跨度最多吃掉的步长」上限：
    // 不封顶的话，一个 3 天后的点能占掉七成轴长，主时间簇反被挤扁。
    return BASE_WEIGHT + Math.min(LOG_CAP, Math.log2(1 + g / med))
  })

  const totalWeight = weights.reduce((a, b) => a + b, 0)

  /* ---- 4. 权重 → 像素，并保证相邻断点有最小可读间距 ---- */
  let gapPx: number[]
  if (totalWeight <= 0) {
    // 只有一个断点（单点时间线）：居中
    gapPx = []
  } else {
    const scale = usable / totalWeight
    gapPx = weights.map((w) => w * scale)
    // 同一时刻（gap = 0）会得到很小的像素，这里抬到最小可读步长
    // 同一时刻（gap = 0）会得到 0 像素，这里抬到最小可读步长，
    // 免得同刻的几个圆点在轴上完全叠成一个
    const minGapPx = Math.min(
      Math.max(tickExtent, 18) * 0.9,
      usable / Math.max(gapPx.length, 1)
    )
    gapPx = gapPx.map((g) => Math.max(g, minGapPx))
    const sum = gapPx.reduce((a, b) => a + b, 0)
    if (sum > usable) {
      const k = usable / sum
      gapPx = gapPx.map((g) => g * k)
    }
  }

  const pxOfBp: number[] = []
  let cursor = 0
  for (let i = 0; i < bps.length; i++) {
    if (i > 0) cursor += gapPx[i - 1] ?? 0
    pxOfBp.push(cursor)
  }

  const bx = (bp: BP) => pxOfBp[bpIndex.get(bp) ?? 0] ?? 0
  const toPct = (px: number) => ((BREATH_PX + px) / width) * 100

  const single = bps.length <= 1
  const startPx = normalized.map((_, i) => (single ? usable / 2 : bx(startBp[i])))

  /* ---- 5a. 卡片分道 ---- */
  const labelLanes = allocateLanes(startPx, cardExtent, cardGap, maxLanes, cardShift)

  /* ---- 5b. 刻度分道 + 同刻去重（起点与时间段终点一起参与） ---- */
  const tickTexts = normalized.map((x) => formatTick(x.ts, x.point))
  // 时间段的终点也要标时刻，所以终点刻度和起点刻度是平权的两类刻度，
  // 一起去重、一起分道 —— 否则「前一个时间段的终点」和「后一个点的起点」
  // 会各自以为自己是唯一的，叠在同一行上。
  const endTickTexts = normalized.map((x, i) =>
    endBp[i] ? formatTick(x.end, x.point) : ''
  )

  interface TickEntry {
    pi: number
    isEnd: boolean
    text: string
    px: number
  }
  const tickEntries: TickEntry[] = []
  normalized.forEach((_, i) => {
    tickEntries.push({ pi: i, isEnd: false, text: tickTexts[i], px: startPx[i] })
    if (endBp[i]) {
      tickEntries.push({ pi: i, isEnd: true, text: endTickTexts[i], px: bx(endBp[i] as BP) })
    }
  })
  // 按位置升序；同一位置起点在前，和断点序列的阅读顺序保持一致。
  // （allocateLanes 要求入参升序，这里的排序同时满足它）
  tickEntries.sort((a, b) => a.px - b.px || (a.isEnd === b.isEnd ? 0 : a.isEnd ? 1 : -1))

  const n = normalized.length
  const startHidden = new Array<boolean>(n).fill(false)
  const endHidden = new Array<boolean>(n).fill(false)
  const startSlot = new Array<number>(n).fill(-1)
  const endSlot = new Array<number>(n).fill(-1)
  const visibleTickPx: number[] = []

  let prevTick = ''
  let prevTickPx = Number.NEGATIVE_INFINITY
  for (const entry of tickEntries) {
    // 同刻的点会被抬开一点间距，所以判定用「一个刻度宽」而不是 1px：
    // 只要文字一样、又挨得近，就只画一次，免得堆出两行一模一样的 "3/14 20:00"
    const dup = entry.text === prevTick && entry.px - prevTickPx < tickExtent + tickGap
    if (entry.isEnd) endHidden[entry.pi] = dup
    else startHidden[entry.pi] = dup

    if (!dup) {
      prevTick = entry.text
      prevTickPx = entry.px
      const slot = visibleTickPx.length
      visibleTickPx.push(entry.px)
      if (entry.isEnd) endSlot[entry.pi] = slot
      else startSlot[entry.pi] = slot
    }
  }

  const tickLanes = allocateLanes(visibleTickPx, tickExtent, tickGap, maxLanes, tickShift)
  const rowOfSlot = (slot: number): number => (slot < 0 ? 0 : tickLanes.lanes[slot] ?? 0)

  const hasCompression = gaps.some((g) => med > 0 && g > med * COMPRESS_RATIO)

  /* ---- 6. 输出 ---- */
  const result: LaidOutPoint[] = normalized.map((x, i) => {
    const pos = single ? 50 : toPct(startPx[i])
    const eBp = endBp[i]
    const endPos = eBp ? toPct(bx(eBp)) : pos
    const isFuzzy = x.point.timeType === 'fuzzy'
    const startIdx = bpIndex.get(startBp[i]) ?? 0
    const entering = startIdx > 0 ? gaps[startIdx - 1] : 0
    const compressed = med > 0 && entering > med * COMPRESS_RATIO

    let compressNote: string | undefined
    // 模糊时段本身没有跨度，刻度里已经写了「清晨 / 傍晚」，
    // 再单独挂一行说明只会重复，所以只保留真正的跨度压缩提示。
    if (!isFuzzy && compressed) {
      compressNote = '跨度已压缩'
    }

    return {
      point: x.point,
      ts: x.ts,
      start: x.start,
      end: x.end,
      pos: Math.min(Math.max(pos, 0), 100),
      endPos: Math.min(Math.max(endPos, 0), 100),
      // 只有「时间段」且真的有长度，才画马克笔
      hasSpan: x.point.timeType === 'range' && x.end > x.start && endPos - pos > 0.4,
      spanText: formatSpan(x.start, x.end),
      certainty: certaintyOf(x.point),
      tick: tickTexts[i],
      labelRow: labelLanes.lanes[i] ?? 0,
      tickRow: rowOfSlot(startSlot[i]),
      tickHidden: startHidden[i],
      endTick: endTickTexts[i],
      endTickRow: rowOfSlot(endSlot[i]),
      endTickHidden: endBp[i] ? endHidden[i] : true,
      compressed: isFuzzy || compressed,
      compressNote
    }
  })

  return {
    points: result,
    labelRows: labelLanes.used,
    tickRows: tickLanes.used,
    hasCompression,
    degenerate: single
  }
}

/** 坐标 → 轴上的 px 位置（用于绝对定位渲染） */
export function posToPx(pos: number, axisWidth: number): number {
  return (pos / 100) * axisWidth
}
