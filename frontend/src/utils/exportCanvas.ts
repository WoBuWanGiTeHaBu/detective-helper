/* ============================================================
 * 画布导出为图片
 *
 * 画布是「SVG + HTML」混合渲染的：
 *   .canvas-origin
 *     ├ .canvas-paper    纸张底色（HTML）
 *     ├ .grid-layer      背景图案（SVG）
 *     └ .stage           内容层（CSS scale(zoom)）
 *         ├ .edge-layer    关系连线（SVG）
 *         ├ .node-layer    对象（SVG）
 *         ├ .anno × N      注解（HTML 富文本）
 *         └ .tl-host × N   时间线面板（HTML + 内嵌 SVG）
 *
 * 所以不能只挑某一张 SVG 出来序列化 —— 富文本注解和时间线面板都会丢。
 * 做法是克隆整棵子树、把当前文档的样式表一起塞进 <style>，再用
 * <foreignObject> 整体装进一张 SVG；浏览器渲染这张 SVG 时会把 HTML
 * 与内嵌 SVG 一起画出来，最后 drawImage 到 canvas 落成 PNG。
 *
 * 三个必须注意的点：
 *   ① 克隆前调用方要把 zoom 归 1 —— 背景图案的 pattern 尺寸是按 zoom
 *      算出来的属性值，带着缩放克隆会让网格间距与纸张尺寸对不上。
 *   ② 样式必须内联 —— 导出的 SVG 是一份独立文档，拿不到外部的
 *      <style> / <link>，而 scoped 样式带的 data-v 属性在克隆里仍在，
 *      所以整份样式表搬过去即可精确匹配。
 *   ③ SVG 必须以 data: URL 交给 <img>，不能用 URL.createObjectURL()
 *      生成的 blob: URL —— Chromium 把 blob: 载入的图片按非同源处理，
 *      画完就把 canvas 标记成污染，toBlob 抛 SecurityError。
 *      细节见 svgToPngBlob 的注释。
 *   ④ 画布内容里若混入跨域资源（外链图片 / 外链字体），仍会污染 canvas。
 *      当前画布只有形状与文字，不触碰外链，所以安全。
 * ============================================================ */

const SVG_NS = 'http://www.w3.org/2000/svg'
const XHTML_NS = 'http://www.w3.org/1999/xhtml'

/**
 * 收集页面上的全部 CSS 规则。
 *
 * 跨域样式表读 cssRules 会抛 SecurityError，这里吞掉继续 ——
 * 本项目样式都是同源内联的，正常都能读到；真读不到时由兜底样式保底，
 * 至少纸面与定位不出错，不会导出一张空图。
 */
function collectStyleText(): string {
  let css = ''
  for (const sheet of Array.from(document.styleSheets)) {
    try {
      for (const rule of Array.from(sheet.cssRules)) css += `${rule.cssText}\n`
    } catch {
      /* 读不到就跳过这一张 */
    }
  }
  return css
}

/** 样式一条都收集不到时的兜底：保证纸面、图层定位、文字颜色不丢 */
function fallbackStyleText(W: number, H: number): string {
  return `
    .canvas-paper { position: absolute; left: 0; top: 0; background: #fff; }
    .stage, .tl-host { position: absolute; top: 0; left: 0; }
    .edge-layer, .node-layer { position: absolute; top: 0; left: 0; overflow: visible; }
    .anno { position: absolute; }
    .node-name, .card-text, .tl-tick { fill: #2e3d34; font-family: "Microsoft YaHei", sans-serif; }
    .anno-body, .anno-title { color: #3a3630; font-family: "Microsoft YaHei", sans-serif; }
    .canvas-export-host { width: ${W}px; height: ${H}px; }
  `
}

/**
 * 把画布子树组装成一张独立的 SVG。
 * 纸张范围就是输出范围，超出纸张的内容会被 foreignObject 裁掉 ——
 * 与画布上「纸多大、能写多大」的约定一致。
 */
function buildExportSvg(origin: HTMLElement, W: number, H: number): SVGSVGElement {
  const svg = document.createElementNS(SVG_NS, 'svg')
  svg.setAttribute('xmlns', SVG_NS)
  svg.setAttribute('xmlns:xlink', 'http://www.w3.org/1999/xlink')
  svg.setAttribute('width', String(W))
  svg.setAttribute('height', String(H))
  svg.setAttribute('viewBox', `0 0 ${W} ${H}`)

  const style = document.createElementNS(SVG_NS, 'style')
  style.textContent = collectStyleText() || fallbackStyleText(W, H)
  svg.appendChild(style)

  /* ── 纸张底色：直接用渲染时的计算色，省得再查一遍背景枚举 ── */
  const paperEl = origin.querySelector<HTMLElement>('.canvas-paper')
  const computed = paperEl ? getComputedStyle(paperEl).backgroundColor : ''
  const paperFill = !computed || computed === 'rgba(0, 0, 0, 0)' ? '#ffffff' : computed
  const paper = document.createElementNS(SVG_NS, 'rect')
  paper.setAttribute('x', '0')
  paper.setAttribute('y', '0')
  paper.setAttribute('width', String(W))
  paper.setAttribute('height', String(H))
  paper.setAttribute('fill', paperFill)
  svg.appendChild(paper)

  /* ── 背景图案层：它本身就是一张 SVG，改掉尺寸后直接嵌进来 ──
     嵌套 <svg> 里的 <defs>/<pattern> 与外层共享同一文档，
     fill="url(#bg-dot)" 这类引用照样生效。 */
  const grid = origin.querySelector<SVGElement>('.grid-layer')
  if (grid) {
    const g = grid.cloneNode(true) as SVGElement
    g.setAttribute('x', '0')
    g.setAttribute('y', '0')
    g.setAttribute('width', String(W))
    g.setAttribute('height', String(H))
    svg.appendChild(g)
  }

  /* ── 内容层：连线 + 对象 + 注解 + 时间线，整体装进 foreignObject ── */
  const stage = origin.querySelector<HTMLElement>('.stage')
  const fo = document.createElementNS(SVG_NS, 'foreignObject')
  fo.setAttribute('x', '0')
  fo.setAttribute('y', '0')
  fo.setAttribute('width', String(W))
  fo.setAttribute('height', String(H))

  const host = document.createElementNS(XHTML_NS, 'div')
  host.setAttribute('xmlns', XHTML_NS)
  // 宽度给足：.stage 是 absolute + shrink-to-fit，宿主没有确定宽度时会塌成 0
  host.setAttribute('style', `position:relative;width:${W}px;height:${H}px;`)

  if (stage) {
    const clone = stage.cloneNode(true) as HTMLElement
    // 导出与屏幕缩放无关：纸张原始尺寸就是输出尺寸
    clone.style.transform = 'none'
    clone.style.transformOrigin = '0 0'
    host.appendChild(clone)
  }

  fo.appendChild(host)
  svg.appendChild(fo)
  return svg
}

/** Blob → data: URL。FileReader 自己处理 UTF-8 → base64，比手写 btoa/encodeURIComponent 稳。 */
function blobToDataUrl(blob: Blob): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(new Error('画布序列化失败，无法导出'))
    reader.readAsDataURL(blob)
  })
}

/**
 * SVG → PNG。SVG 里的文字与形状由浏览器自己渲染，所以字体、阴影都保真。
 *
 * ⚠ 这里**必须**用 data: URL，不能用 URL.createObjectURL() 出来的 blob: URL。
 * Chromium 会把 blob: 载入的图片按「非同源」处理，drawImage 之后 canvas 立刻被
 * 标记为污染（tainted），随后的 toBlob 抛：
 *   Failed to execute 'toBlob' on 'HTMLCanvasElement':
 *   Tainted canvases may not be exported.
 * 换成 data: URL 后这张图被当成同源，导出正常。同一份 SVG 用两种 URL 做过无头
 * Chromium 对照实验：blob: 必抛，data: 必过。污染的画布**照样能显示**，只有
 * toBlob / toDataURL / getImageData 会被拒 —— 只看「图渲染出来了」会漏掉这个问题。
 */
async function svgToPngBlob(
  svg: SVGSVGElement,
  W: number,
  H: number,
  scale: number
): Promise<Blob> {
  const text = new XMLSerializer().serializeToString(svg)
  const url = await blobToDataUrl(
    new Blob([text], { type: 'image/svg+xml;charset=utf-8' })
  )

  const img = new Image()
  img.width = W
  img.height = H
  await new Promise<void>((resolve, reject) => {
    img.onload = () => resolve()
    img.onerror = () => reject(new Error('画布渲染失败，无法导出'))
    img.src = url
  })

  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(W * scale))
  canvas.height = Math.max(1, Math.round(H * scale))
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('当前环境不支持画布导出')

  // 先铺一层白：纸张之外的边缘不至于透明，贴到文档里不会发灰
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  ctx.drawImage(img, 0, 0, canvas.width, canvas.height)

  try {
    return await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('导出 PNG 失败'))), 'image/png')
    })
  } catch (e) {
    // 只剩一种可能：画布里混进了跨域资源，浏览器不允许导出
    if (e instanceof DOMException && e.name === 'SecurityError') {
      throw new Error('画布中包含跨域资源（外链图片或字体），浏览器不允许导出')
    }
    throw e
  }
}

/**
 * 文件名安全化：去掉 Windows / macOS 都不接受的字符。
 * 一律限长，免得「案件名 + 页名」拼出来超长导致保存失败。
 */
export function safeFileName(name: string): string {
  const cleaned = (name || '')
    .replace(/[\\/:*?"<>|\r\n\t]+/g, '_')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 60)
  return cleaned || '画布'
}

/** 触发一次下载 */
function downloadBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName.endsWith('.png') ? fileName : `${fileName}.png`
  document.body.appendChild(a)
  a.click()
  a.remove()
  // 立刻 revoke 会让部分环境来不及取数据，延后一拍再释放
  window.setTimeout(() => URL.revokeObjectURL(url), 4000)
}

export interface ExportCanvasOptions {
  /** .canvas-origin 元素（画布的根容器） */
  origin: HTMLElement
  /** 纸张逻辑宽度 */
  width: number
  /** 纸张逻辑高度 */
  height: number
  /** 文件名（不含 .png 也可以） */
  fileName: string
  /** 输出倍率，默认 2 —— 屏幕上一倍大的图放到文档里会糊 */
  scale?: number
}

export async function exportCanvasToPng(opts: ExportCanvasOptions): Promise<void> {
  const W = Math.max(1, Math.round(opts.width))
  const H = Math.max(1, Math.round(opts.height))
  const svg = buildExportSvg(opts.origin, W, H)
  const blob = await svgToPngBlob(svg, W, H, opts.scale ?? 2)
  downloadBlob(blob, safeFileName(opts.fileName))
}
