/* ============================================================
 * 关系线束（edge bundle）—— 同一对对象之间的多条关系如何排布
 *
 * 背景：A 与 B 之间常常不止一条关系（师徒 + 怀疑 + 血缘…），
 * 如果每条都从中心连线算起，就会**完全重合**成一条线。
 * 这里给同束内的每条关系算一个「法线方向偏移量」，
 * 让它们像一束平行线一样并排存在。
 *
 * 无向归一：A→B 与 B→A 视为同一束（几何上是同一条线段）。
 * ============================================================ */

/** 同束内相邻两条线的间距（画布坐标 px） */
export const BUNDLE_GAP = 26

/** 把 source / target 归一成无向的束 key */
export function pairKey(source: string, target: string): string {
  return source <= target ? `${source}__${target}` : `${target}__${source}`
}

/**
 * 无向法线方向系数。
 *
 * 同一对对象之间，A→B 与 B→A 的「起点→终点」方向是相反的，
 * 各自的法线也就相反。如果直接用各自法线去偏移，两条线的位移会**互相抵消**，
 * 又叠回一条线上。所以用 id 的字典序定一个基准方向，
 * 让同一束里的所有线共用一个法线方向。
 */
export function normalFlip(source: string, target: string): 1 | -1 {
  return source <= target ? 1 : -1
}

/**
 * 计算每条关系的法线偏移量。
 * 同束内按出现顺序居中分布：
 *   1 条 → [0]
 *   2 条 → [-13, +13]
 *   3 条 → [-26, 0, +26]
 *
 * @param items 至少含 id / source / target 的关系数组
 * @param gap   同束间距，横向面板可传小一点
 */
export function bundleOffsets(
  items: ReadonlyArray<{ id: string; source: string; target: string }>,
  gap = BUNDLE_GAP
): Record<string, number> {
  const groups = new Map<string, string[]>()
  for (const it of items) {
    const k = pairKey(it.source, it.target)
    const list = groups.get(k)
    if (list) list.push(it.id)
    else groups.set(k, [it.id])
  }

  const out: Record<string, number> = {}
  for (const ids of groups.values()) {
    const n = ids.length
    if (n === 1) {
      out[ids[0]] = 0
      continue
    }
    ids.forEach((id, i) => {
      out[id] = (i - (n - 1) / 2) * gap
    })
  }
  return out
}
