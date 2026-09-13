/**
 * 封面图片 URL 拼接 —— **单点配置**
 *
 * 接口文档只说明 `POST /api/books/{id}/cover` 返回 `{ coverValue }`
 * （形如 `covers/book_1.webp`），没有写静态资源的访问前缀。
 *
 * 后端给出静态映射规则后，**只需要改本文件的 STATIC_BASE**，
 * 另外同步 vite.config.ts 里对应的 dev proxy 即可，其余代码无需改动。
 */

/** 静态资源前缀（dev 下由 vite proxy 转发到 localhost:8081） */
const STATIC_BASE = '/files'

/**
 * 把后端返回的 coverValue 转成可直接放进 <img src> 的地址。
 *
 * - coverValue 为空 → 返回 null（调用方回退到色板封面）
 * - 已是完整 URL（http/https 或 data:）→ 原样返回
 * - 相对路径 → 拼上 STATIC_BASE，并处理重复/缺失的斜杠
 */
export function coverUrl(coverValue: string | null | undefined): string | null {
  if (!coverValue) return null

  const v = coverValue.trim()
  if (!v) return null

  if (/^(https?:)?\/\//i.test(v) || v.startsWith('data:')) return v

  const base = STATIC_BASE.replace(/\/+$/, '')
  const path = v.replace(/^\/+/, '')
  return `${base}/${path}`
}
