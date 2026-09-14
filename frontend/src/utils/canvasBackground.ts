/**
 * 画布背景偏好
 *
 * 背景已随画布存进后端（`CanvasResponse.background`，缺省 'plain'），
 * 这里的 localStorage 只作为**离线 / 后端不可用时的兜底**，不再是唯一数据源：
 * 读取时优先用后端下发值，写入时两边都写。
 */

export type CanvasBg = 'plain' | 'grid' | 'dot' | 'line'

export interface BgOption {
  key: CanvasBg
  label: string
  /** 缩略图预览用的 CSS background-image */
  preview: string
}

export const BG_PLAIN: CanvasBg = 'plain'

/** 默认纯白 —— 用户明确要求 */
export const DEFAULT_BG: CanvasBg = BG_PLAIN

const STORAGE_KEY = 'tn:canvas-bg'

export const BG_OPTIONS: BgOption[] = [
  {
    key: 'plain',
    label: '纯色',
    preview: '#FFFFFF'
  },
  {
    key: 'dot',
    label: '点阵',
    preview:
      'radial-gradient(circle, #DDD8CE 1px, transparent 1px) 0 0 / 8px 8px, #FFFFFF'
  },
  {
    key: 'grid',
    label: '方格',
    preview:
      'linear-gradient(#EBE7DF 1px, transparent 1px) 0 0 / 8px 8px, linear-gradient(90deg, #EBE7DF 1px, transparent 1px) 0 0 / 8px 8px, #FFFFFF'
  },
  {
    key: 'line',
    label: '横线',
    preview:
      'linear-gradient(#E8E9EA 1px, transparent 1px) 0 0 / 100% 24px, #FFFFFF'
  }
]

function readAll(): Record<string, CanvasBg> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return typeof parsed === 'object' && parsed ? parsed : {}
  } catch {
    return {}
  }
}

/** 取某个 page 的背景，未设置时回落到默认纯白 */
export function getBg(pageId: number | null): CanvasBg {
  if (pageId == null) return DEFAULT_BG
  const all = readAll()
  const v = all[String(pageId)]
  return isBg(v) ? v : DEFAULT_BG
}

/** 设置某个 page 的背景 */
export function setBg(pageId: number, bg: CanvasBg): void {
  try {
    const all = readAll()
    if (bg === DEFAULT_BG) delete all[String(pageId)]
    else all[String(pageId)] = bg
    localStorage.setItem(STORAGE_KEY, JSON.stringify(all))
  } catch {
    /* 隐私模式下 localStorage 可能不可用，静默降级 */
  }
}

function isBg(v: unknown): v is CanvasBg {
  return v === 'plain' || v === 'grid' || v === 'dot' || v === 'line'
}
