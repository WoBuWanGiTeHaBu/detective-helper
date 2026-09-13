/* ============================================================
 * 案件封面渐变 —— PROMPT.md §4.4 的 11 组精确色值
 * 规则：45° 右上→左下，终点 = 起点色相不变、亮度 −17%
 *       阴影 0 12px 20px -4px rgba(同色相, 0.20)
 * ============================================================ */

export interface CoverToken {
  /** 渐变起点 */
  from: string
  /** 渐变终点 */
  to: string
  /** 阴影色调 */
  shadow: string
}

export const COVER_TOKENS: CoverToken[] = [
  { from: '#B8CCBF', to: '#8CA699', shadow: 'rgba(31,46,38,0.20)' }, // 01 绿
  { from: '#A8B8CC', to: '#808FA8', shadow: 'rgba(36,43,56,0.20)' }, // 02 蓝
  { from: '#C2B39C', to: '#9E8C73', shadow: 'rgba(51,41,28,0.20)' }, // 03 驼
  { from: '#BFA8A1', to: '#998078', shadow: 'rgba(51,36,31,0.20)' }, // 04 赭
  { from: '#B5B3A1', to: '#8F8C75', shadow: 'rgba(46,43,31,0.20)' }, // 05 橄榄
  { from: '#B8B3BD', to: '#918A99', shadow: 'rgba(43,38,51,0.20)' }, // 06 紫
  { from: '#B3BFC2', to: '#8A9CA1', shadow: 'rgba(36,46,51,0.20)' }, // 07 青
  { from: '#B0B8AD', to: '#879482', shadow: 'rgba(38,46,33,0.20)' }, // 08 苔
  { from: '#C4ADA6', to: '#9E857B', shadow: 'rgba(54,38,31,0.20)' }, // 09 陶
  { from: '#BAC2AD', to: '#949E82', shadow: 'rgba(43,51,33,0.20)' }, // 10 黄绿
  { from: '#C2B5C2', to: '#9C8C9E', shadow: 'rgba(51,41,54,0.20)' }, // 11 藕
]

/** 根据 book.id 稳定取一个封面令牌（同一本书永远同一色） */
export function coverTokenFor(id: number): CoverToken {
  const n = COVER_TOKENS.length
  const idx = ((id % n) + n) % n
  return COVER_TOKENS[idx]
}

/** 生成 45° 线性渐变（沿用设计稿 gradientTransform 的视觉方向） */
export function coverGradient(token: CoverToken): string {
  return `linear-gradient(135deg, ${token.from} 0%, ${token.to} 100%)`
}

/** 封面卡片阴影：0 12px 20px -4px */
export function coverShadow(token: CoverToken): string {
  return `0 12px 20px -4px ${token.shadow}`
}

/** 卷宗编号，如「卷宗 01」 */
export function caseLabel(index: number): string {
  return `卷宗 ${String(index + 1).padStart(2, '0')}`
}
