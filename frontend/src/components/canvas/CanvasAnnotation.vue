<template>
  <!-- OneNote 式注解：无底色、无描边、无阴影 -->
  <div
    class="anno"
    :class="{ dragging, hover: hovering || editing, editing, snapped }"
    :style="{
      left: `${local.x}px`,
      top: `${local.y}px`,
      width: `${local.width}px`
    }"
    @mouseenter="hovering = true"
    @mouseleave="hovering = false"
  >
    <!-- 拖动虚框：仅在悬停 / 拖动 / 编辑时显示 -->
    <div v-if="hovering || dragging || editing" class="guide" />
    <!-- 吸附指示线 -->
    <div v-if="snapped" class="snap-line" :style="{ left: `${snapX}px` }" />

    <div class="anno-head">
      <!-- 拖动只从握把发起，正文区域留给文本选择 -->
      <span class="grip" title="拖动" @mousedown.stop="startDrag">
        <span class="pin" />
      </span>

      <input
        ref="titleEl"
        v-model="titleDraft"
        class="anno-title"
        placeholder="推理批注"
        @blur="commitContent"
        @keyup.enter="($event.target as HTMLInputElement).blur()"
      />

      <!-- 唯一的编辑入口：点开才带出具体格式工具 -->
      <button
        type="button"
        class="anno-edit"
        :class="{ on: editing }"
        :title="editing ? '收起格式工具' : '编辑格式'"
        @click="toggleEditing"
      >
        <svg viewBox="0 0 14 14" fill="none">
          <path
            d="M9.1 2.2l2.7 2.7-7 7-3.2.5.5-3.2z"
            stroke="currentColor"
            stroke-width="1.2"
            stroke-linejoin="round"
          />
        </svg>
      </button>

      <!-- 删除：与编辑并排，同样只在悬停时露面，避免平时喧宾夺主 -->
      <button
        type="button"
        class="anno-del"
        title="删除这条注解"
        @click.stop="$emit('remove', annotation.id)"
      >
        <svg viewBox="0 0 14 14" fill="none">
          <path
            d="M3.9 4.2h6.2l-.62 6.5a1.1 1.1 0 0 1-1.1 1H5.62a1.1 1.1 0 0 1-1.1-1z"
            stroke="currentColor"
            stroke-width="1.2"
            stroke-linejoin="round"
          />
          <path
            d="M6.1 4.2V3a.8.8 0 0 1 .8-.8h.2a.8.8 0 0 1 .8.8v1.2M2.9 4.2h8.2"
            stroke="currentColor"
            stroke-width="1.2"
            stroke-linecap="round"
          />
        </svg>
      </button>
    </div>

    <div
      ref="bodyRef"
      class="anno-body"
      contenteditable="true"
      spellcheck="false"
      @input="onBodyInput"
      @blur="commitContent"
      @keyup.esc="editing = false"
    />

    <!-- 富文本工具栏：由编辑图标带出 -->
    <div v-if="editing" class="anno-tools" @mousedown.prevent>
      <button type="button" class="tool fmt b" title="加粗" @click="fmt('bold')">B</button>
      <button type="button" class="tool fmt i" title="斜体" @click="fmt('italic')">I</button>
      <button type="button" class="tool fmt u" title="下划线" @click="fmt('underline')">U</button>
      <button type="button" class="tool fmt s" title="删除线" @click="fmt('strikeThrough')">S</button>

      <span class="tool-sep" />

      <button
        v-for="s in SIZES"
        :key="s"
        type="button"
        class="tool size"
        :title="`字号 ${s}px`"
        @click="applyStyle({ fontSize: `${s}px` })"
      >
        {{ s }}
      </button>

      <span class="tool-sep" />

      <button
        type="button"
        class="tool swatch-btn"
        :class="{ on: palette === 'color' }"
        title="文字颜色"
        @click="palette = palette === 'color' ? null : 'color'"
      >
        <span class="s-btn-a">A</span>
        <span class="s-btn-bar" :style="{ background: lastColor }" />
      </button>

      <button
        type="button"
        class="tool swatch-btn"
        :class="{ on: palette === 'bg' }"
        title="背景颜色"
        @click="palette = palette === 'bg' ? null : 'bg'"
      >
        <span class="s-btn-hl" />
        <span class="s-btn-bar" :style="{ background: lastBg }" />
      </button>

      <span class="tool-sep" />

      <button type="button" class="tool clear" title="清除格式" @click="clearFormat">清除格式</button>
    </div>

    <!-- 色板 -->
    <div v-if="editing && palette" class="anno-palette" @mousedown.prevent>
      <button
        v-for="c in palette === 'color' ? TEXT_COLORS : BG_COLORS"
        :key="c.value"
        type="button"
        class="palette-dot"
        :title="c.name"
        :style="{ background: c.value }"
        @click="pickColor(c.value)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import type { Annotation } from '@/api/types'

const props = defineProps<{
  annotation: Annotation
  /** 画布上其它对象的矩形，用于 12px 内吸附提示 */
  obstacles?: { x: number; y: number; width: number; height: number }[]
  /** 8px 网格吸附 */
  grid?: number
}>()

const emit = defineEmits<{
  update: [id: string, patch: Partial<Annotation>]
  /** 内容 / 位置变更结束，用于触发自动保存 */
  commit: []
  /** 删除这条注解（确认与落盘交给父组件，这里只上报意图） */
  remove: [id: string]
}>()

const local = reactive({
  x: props.annotation.x,
  y: props.annotation.y,
  width: props.annotation.width || 240
})

const dragging = ref(false)
const hovering = ref(false)
const snapped = ref(false)
const snapX = ref(0)
const bodyRef = ref<HTMLElement | null>(null)
const editing = ref(false)
const palette = ref<'color' | 'bg' | null>(null)

/* ---------------- 内容结构 ----------------
 * content 里以 <h4 class="anno-h"> 存标题，其余为正文 HTML。
 * 标题也放在 content 内部，保证只依赖 content 一个字段就能完整往返后端。
 * -------------------------------------------- */

function stripTags(html: string): string {
  return (html || '').replace(/<[^>]+>/g, '').trim()
}

const parsed = computed(() => parse(props.annotation.content || ''))

function parse(content: string): { title: string; body: string } {
  const html = content || ''
  // 兼容旧数据：首行 ［批注］xxx
  const legacy = html.match(/^\s*<p>\s*［批注］([\s\S]*?)<\/p>/i)
  if (legacy) {
    return { title: stripTags(legacy[1]), body: html.replace(legacy[0], '').trim() }
  }
  const m = html.match(/^\s*<h4[^>]*>([\s\S]*?)<\/h4>/i)
  if (m) {
    return { title: stripTags(m[1]), body: html.slice(m[0].length).trim()
    }
  }
  return { title: '', body: html.trim() }
}

const title = computed(() => parsed.value.title)

/**
 * 标题输入框的本地草稿。
 * 不能用 :value="title" 受控绑定——hovering 等任何响应式状态变化触发重渲染时，
 * Vue 会把 DOM value 拍回旧的 title，用户刚打的字就被清掉了（正是「打几个字立马清空」的原因）。
 */
const titleDraft = ref('')

/** 标题草稿与 props 同步：标题输入框未聚焦时才回填，避免打断输入 */
watch(
  () => props.annotation.content,
  () => {
    if (document.activeElement !== titleEl.value) titleDraft.value = title.value
  }
)

// 初始回填（含挂载后 DOM 就绪的时机）
onMounted(() => {
  titleDraft.value = title.value
})

/** 标题输入框：提交时读草稿，清空标题也能正确生效 */
const titleEl = ref<HTMLInputElement | null>(null)

/** 拼回 content：标题 <h4> + 正文 HTML */
function compose(bodyHtml: string, titleText: string): string {
  const t = titleText.trim()
  const head = t ? `<h4 class="anno-h">${t.replace(/[<>&]/g, '')}</h4>` : ''
  return head + (bodyHtml || '')
}

function currentTitle(): string {
  return titleDraft.value
}

/** 把 DOM 里的正文写回 content 并触发保存 */
function commitContent() {
  const body = bodyRef.value?.innerHTML ?? ''
  const next = compose(body, currentTitle())
  if (next !== (props.annotation.content || '')) {
    emit('update', props.annotation.id, { content: next })
  }
  emit('commit')
}

function onBodyInput() {
  // 输入中不动 DOM，避免光标跳动；离开时统一提交
}

onMounted(async () => {
  await nextTick()
  syncBody()
})

watch(
  () => props.annotation.content,
  async () => {
    await nextTick()
    syncBody()
  }
)

/** 仅在与当前 DOM 不一致、且用户没在编辑时回填，避免打断输入 */
function syncBody() {
  const el = bodyRef.value
  if (!el) return
  const want = parsed.value.body
  if (document.activeElement === el) return
  if (el.innerHTML !== want) el.innerHTML = want
}

/* ---------------- 编辑入口 ---------------- */
async function toggleEditing() {
  editing.value = !editing.value
  if (editing.value) {
    await nextTick()
    bodyRef.value?.focus()
  } else {
    commitContent()
  }
}

/* ---------------- 富文本 ---------------- */
const SIZES = [12, 14, 16, 20]

const TEXT_COLORS = [
  { name: '默认', value: '#3A3630' },
  { name: '墨黑', value: '#1F1C18' },
  { name: '深绿', value: '#3F5B4C' },
  { name: '松绿', value: '#5C7F6B' },
  { name: '暗红', value: '#9C5A50' },
  { name: '赭石', value: '#A8756B' },
  { name: '靛蓝', value: '#4E6480' },
  { name: '灰', value: '#8C877E' }
]

const BG_COLORS = [
  { name: '无', value: 'transparent' },
  { name: '淡绿', value: '#E4EFE7' },
  { name: '淡黄', value: '#F5EFD8' },
  { name: '淡粉', value: '#F6E6E3' },
  { name: '淡蓝', value: '#E3EBF3' },
  { name: '淡紫', value: '#ECE7F2' },
  { name: '浅灰', value: '#ECEAE5' },
  { name: '纸色', value: '#F7F4EE' }
]

const lastColor = ref('#3F5B4C')
const lastBg = ref('#F5EFD8')

function fmt(cmd: 'bold' | 'italic' | 'underline' | 'strikeThrough') {
  bodyRef.value?.focus()
  document.execCommand(cmd, false)
  commitContent()
}

/** 给选区套一层带内联样式的 span —— 生成的 HTML 干净、可往返 */
function applyStyle(styles: Record<string, string>) {
  const el = bodyRef.value
  if (!el) return
  el.focus()
  const sel = window.getSelection()
  if (!sel || !sel.rangeCount) return
  const range = sel.getRangeAt(0)
  if (range.collapsed) return

  const span = document.createElement('span')
  for (const [k, v] of Object.entries(styles)) {
    span.style.setProperty(k.replace(/[A-Z]/g, (m) => `-${m.toLowerCase()}`), v)
  }

  try {
    range.surroundContents(span)
  } catch {
    // 选区跨多个节点时 surroundContents 会抛错，改走 extract + 包装
    const frag = range.extractContents()
    span.appendChild(frag)
    range.insertNode(span)
  }

  sel.removeAllRanges()
  const r = document.createRange()
  r.selectNodeContents(span)
  sel.addRange(r)

  palette.value = null
  commitContent()
}

function pickColor(value: string) {
  if (palette.value === 'color') {
    lastColor.value = value
    applyStyle({ color: value })
  } else {
    lastBg.value = value
    applyStyle({ backgroundColor: value })
  }
}

function clearFormat() {
  const el = bodyRef.value
  if (!el) return
  el.focus()
  const sel = window.getSelection()
  if (!sel || !sel.rangeCount) return
  const range = sel.getRangeAt(0)
  if (range.collapsed) return

  const frag = range.extractContents()
  const box = document.createElement('div')
  box.appendChild(frag)

  box.querySelectorAll('b,strong,i,em,u,s,strike,font,span').forEach((n) => {
    const parent = n.parentNode
    if (!parent) return
    while (n.firstChild) parent.insertBefore(n.firstChild, n)
    parent.removeChild(n)
  })
  box.querySelectorAll('*').forEach((n) => n.removeAttribute('style'))

  const out = document.createDocumentFragment()
  while (box.firstChild) out.appendChild(box.firstChild)
  range.insertNode(out)
  commitContent()
}

/* ---------------- 拖动 + 吸附 ---------------- */
let startX = 0
let startY = 0
let originX = 0
let originY = 0
let dragMoved = false

function startDrag(e: MouseEvent) {
  dragging.value = true
  dragMoved = false
  startX = e.clientX
  startY = e.clientY
  originX = local.x
  originY = local.y

  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', endDrag)
}

function onMove(e: MouseEvent) {
  if (!dragging.value) return
  const dx = e.clientX - startX
  const dy = e.clientY - startY
  if (Math.abs(dx) > 2 || Math.abs(dy) > 2) dragMoved = true
  const grid = props.grid ?? 8

  let nx = originX + dx
  let ny = originY + dy

  // 吸附：靠近其它对象边缘 12px 内自动贴合
  snapped.value = false
  for (const o of props.obstacles ?? []) {
    const targets = [o.x, o.x + o.width]
    for (const t of targets) {
      if (Math.abs(nx - t) <= 12) {
        nx = t
        snapX.value = t
        snapped.value = true
        break
      }
    }
    if (Math.abs(ny - o.y) <= 12) ny = o.y
    if (Math.abs(ny - (o.y + o.height)) <= 12) ny = o.y + o.height
  }

  // 8px 网格吸附
  local.x = snapped.value ? nx : Math.round(nx / grid) * grid
  local.y = Math.round(ny / grid) * grid
}

function endDrag() {
  dragging.value = false
  snapped.value = false
  window.removeEventListener('mousemove', onMove)
  window.removeEventListener('mouseup', endDrag)
  if (dragMoved) {
    emit('update', props.annotation.id, { x: local.x, y: local.y })
    emit('commit')
  }
}

watch(
  () => [props.annotation.x, props.annotation.y, props.annotation.width],
  ([x, y, w]) => {
    if (!dragging.value) {
      local.x = Number(x)
      local.y = Number(y)
      if (w) local.width = Number(w)
    }
  }
)
</script>

<style scoped>
.anno {
  position: absolute;
  padding: 6px 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  user-select: none;
}

.guide {
  position: absolute;
  inset: -8px;
  border: 1.5px dashed var(--anno-guide);
  border-radius: var(--r-md);
  pointer-events: none;
}

.anno.editing .guide {
  border-style: solid;
  border-color: rgba(125, 153, 135, 0.45);
}

.snap-line {
  position: absolute;
  top: -8px;
  bottom: -8px;
  width: 1px;
  background: rgba(125, 153, 135, 0.5);
  pointer-events: none;
}

.anno-head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.grip {
  flex: none;
  display: flex;
  align-items: center;
  cursor: move;
  padding-right: 2px;
}

.pin {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #c9b87e;
}

.anno-title {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  font-family: inherit;
  font-size: 11px;
  font-weight: 500;
  color: var(--anno-label);
  user-select: text;
}

.anno-title::placeholder {
  color: var(--anno-label);
  opacity: 0.7;
}

/* 唯一编辑入口 */
.anno-edit {
  flex: none;
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #b3aca2;
  cursor: pointer;
  opacity: 0;
  transition: opacity 140ms var(--ease), background 140ms var(--ease), color 140ms var(--ease);
}

.anno-edit svg {
  width: 13px;
  height: 13px;
}

.anno:hover .anno-edit,
.anno.editing .anno-edit {
  opacity: 1;
}

.anno-edit:hover {
  background: rgba(0, 0, 0, 0.045);
  color: var(--ink-2);
}

.anno-edit.on {
  background: #e4eae5;
  color: var(--green-ink, #3f5b4c);
}

/* 删除：与编辑按钮同形同排，只在悬停 / 编辑时现身；自己悬停时转成暖红 */
.anno-del {
  flex: none;
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #b3aca2;
  cursor: pointer;
  opacity: 0;
  transition: opacity 140ms var(--ease), background 140ms var(--ease), color 140ms var(--ease);
}

.anno-del svg {
  width: 13px;
  height: 13px;
}

.anno:hover .anno-del,
.anno.editing .anno-del {
  opacity: 1;
}

.anno-del:hover {
  background: #f5ebe8;
  color: #a8756b;
}

.anno-body {
  font-family: var(--font-sans);
  font-size: 12.5px;
  line-height: 21px;
  color: var(--anno-ink);
  outline: 0;
  min-height: 42px;
  max-height: 320px;
  overflow-y: auto;
  cursor: text;
  user-select: text;
  word-break: break-word;
}

.anno-body :deep(h4),
.anno-body :deep(p) {
  margin: 0;
}

/* ---------------- 工具栏 ---------------- */
.anno-tools {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
  padding: 4px 5px;
  border-radius: 8px;
  background: rgba(253, 252, 250, 0.96);
  border: 1px solid #e6e0d4;
  box-shadow: 0 6px 16px -6px rgba(72, 66, 55, 0.16);
}

.tool {
  min-width: 22px;
  height: 22px;
  padding: 0 5px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #6b665e;
  font-family: inherit;
  font-size: 11px;
  line-height: 1;
  cursor: pointer;
  transition: background 120ms var(--ease), color 120ms var(--ease);
}

.tool:hover {
  background: #edeae4;
  color: var(--ink-1);
}

.tool.b {
  font-weight: 700;
}
.tool.i {
  font-style: italic;
}
.tool.u {
  text-decoration: underline;
}
.tool.s {
  text-decoration: line-through;
}

.tool.size {
  font-size: 10px;
  color: #8c877e;
}

.tool.clear {
  font-size: 10px;
  color: #8c877e;
}

.tool-sep {
  width: 1px;
  height: 12px;
  margin: 0 3px;
  background: var(--line-3);
}

.swatch-btn {
  position: relative;
  padding-bottom: 3px;
}

.s-btn-a {
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
}

.s-btn-hl {
  font-size: 10px;
  line-height: 1;
  padding: 0 2px;
  background: #f5efd8;
}

.s-btn-bar {
  position: absolute;
  left: 4px;
  right: 4px;
  bottom: 2px;
  height: 2px;
  border-radius: 1px;
}

.swatch-btn.on {
  background: #edeae4;
}

/* 色板 */
.anno-palette {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 7px;
  border-radius: 8px;
  background: rgba(253, 252, 250, 0.96);
  border: 1px solid #e6e0d4;
  box-shadow: 0 6px 16px -6px rgba(72, 66, 55, 0.16);
}

.palette-dot {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  cursor: pointer;
  transition: transform 120ms var(--ease);
}

.palette-dot:hover {
  transform: scale(1.15);
}
</style>
