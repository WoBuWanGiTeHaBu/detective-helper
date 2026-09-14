/**
 * 画布尺寸
 *
 * 画布是一张**有明确尺寸**的纸（不再是拖到哪长到哪的无限画布）：
 *   · 尺寸随画布存进后端（`CanvasResponse.canvasWidth` / `canvasHeight`）；
 *   · 这里的 localStorage 只作为**离线 / 后端未支持时的兜底**，按 page 隔离；
 *   · 超出可视区域的部分交给画布容器滚（滚轮 / 滚动条）。
 */

export interface CanvasSize {
  w: number
  h: number
}

/** 缺省画布尺寸 */
export const DEFAULT_CANVAS_W = 1600
export const DEFAULT_CANVAS_H = 1200

export const MIN_CANVAS_W = 600
export const MAX_CANVAS_W = 8000
export const MIN_CANVAS_H = 400
export const MAX_CANVAS_H = 8000

export interface CanvasPreset {
  key: string
  label: string
  w: number
  h: number
}

/** 常用尺寸预设 */
export const CANVAS_PRESETS: CanvasPreset[] = [
  { key: 'standard', label: '标准', w: 1600, h: 1200 },
  { key: 'wide', label: '横版', w: 1920, h: 1080 },
  { key: 'tall', label: '竖版', w: 1200, h: 1920 },
  { key: 'band', label: '宽幅', w: 2400, h: 1200 },
  { key: 'large', label: '大幅', w: 3200, h: 2400 }
]

const STORAGE_KEY = 'wb.canvasSize'

function readAll(): Record<string, CanvasSize> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return typeof parsed === 'object' && parsed ? (parsed as Record<string, CanvasSize>) : {}
  } catch {
    return {}
  }
}

function isSize(v: unknown): v is CanvasSize {
  return (
    !!v &&
    typeof v === 'object' &&
    typeof (v as CanvasSize).w === 'number' &&
    typeof (v as CanvasSize).h === 'number'
  )
}

/** 取某个 page 的兜底尺寸，未设置时回落到默认 */
export function getLocalSize(pageId: number | null): CanvasSize {
  if (pageId == null) return { w: DEFAULT_CANVAS_W, h: DEFAULT_CANVAS_H }
  const v = readAll()[String(pageId)]
  return isSize(v) ? v : { w: DEFAULT_CANVAS_W, h: DEFAULT_CANVAS_H }
}

/** 记下某个 page 的尺寸（本地兜底，后端支持后以后端值为准） */
export function setLocalSize(pageId: number | null, size: CanvasSize): void {
  if (pageId == null) return
  try {
    const all = readAll()
    all[String(pageId)] = size
    localStorage.setItem(STORAGE_KEY, JSON.stringify(all))
  } catch {
    /* 隐私模式下 localStorage 可能不可用，静默降级 */
  }
}

/** 把任意输入夹到合法范围（输入框失焦 / 应用时用） */
export function clampSize(w: number, h: number): CanvasSize {
  const cw = Math.min(Math.max(Math.round(w) || DEFAULT_CANVAS_W, MIN_CANVAS_W), MAX_CANVAS_W)
  const ch = Math.min(Math.max(Math.round(h) || DEFAULT_CANVAS_H, MIN_CANVAS_H), MAX_CANVAS_H)
  return { w: cw, h: ch }
}
