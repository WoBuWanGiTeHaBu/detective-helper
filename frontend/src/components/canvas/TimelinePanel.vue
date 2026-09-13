<template>
  <div
    class="tl-panel"
    :class="{ vertical: isVertical, dragging }"
    :style="{ width: isVertical ? '300px' : `${width}px` }"
  >
    <!-- 头部：整条即拖动握把 -->
    <div class="tl-head" title="拖动可移动时间线" @mousedown.stop="startHeadDrag">
      <svg class="tl-grip" viewBox="0 0 6 12" fill="none">
        <circle cx="1.5" cy="2" r="1" fill="currentColor" />
        <circle cx="4.5" cy="2" r="1" fill="currentColor" />
        <circle cx="1.5" cy="6" r="1" fill="currentColor" />
        <circle cx="4.5" cy="6" r="1" fill="currentColor" />
        <circle cx="1.5" cy="10" r="1" fill="currentColor" />
        <circle cx="4.5" cy="10" r="1" fill="currentColor" />
      </svg>

      <input
        class="tl-name"
        :value="timeline.name"
        @input="onRename"
        @blur="commitName"
        @keyup.enter="($event.target as HTMLInputElement).blur()"
      />

      <span class="tl-count">{{ timeline.points.length }} 个时间点</span>

      <!-- 添加时间点：只负责「打开录入表单」 -->
      <button
        type="button"
        class="tl-act add"
        :class="{ on: formOpen && !editingId }"
        :title="formOpen && !editingId ? '正在录入时间点' : '添加时间点'"
        @mousedown.stop
        @click="openCreateForm"
      >
        <svg viewBox="0 0 12 12" fill="none">
          <path d="M6 2.2v7.6M2.2 6h7.6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
        </svg>
        <span>时间点</span>
      </button>

      <!-- 方向切换 -->
      <div class="dir-switch" @mousedown.stop>
        <button
          type="button"
          class="dir-seg"
          :class="{ active: !isVertical }"
          title="横向"
          @click="setDirection('horizontal')"
        >
          <svg viewBox="0 0 12 12" fill="none">
            <path d="M1.6 6h8.8" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
            <path d="M8.4 3.6 10.6 6l-2.2 2.4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
        <button
          type="button"
          class="dir-seg"
          :class="{ active: isVertical }"
          title="纵向"
          @click="setDirection('vertical')"
        >
          <svg viewBox="0 0 12 12" fill="none">
            <path d="M6 1.6v8.8" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
            <path d="M3.6 8.4 6 10.6l2.4-2.2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
      </div>

      <!-- 关闭 / 移除时间线：与「添加」分开，不再共用一个按钮 -->
      <button
        type="button"
        class="tl-act close"
        title="移除这条时间线"
        @mousedown.stop
        @click="$emit('close', timeline.id)"
      >
        <svg viewBox="0 0 12 12" fill="none">
          <path d="m3.2 3.2 5.6 5.6M8.8 3.2 3.2 8.8" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
        </svg>
      </button>
    </div>

    <!-- 时间点录入 / 编辑表单 -->
    <div v-if="formOpen" class="tl-form" @mousedown.stop>
      <div class="f-row">
        <span class="f-mode">{{ editingId ? '编辑时间点' : '新增时间点' }}</span>
        <button v-if="editingId" type="button" class="f-del" @click="removeCurrent">删除</button>
        <button type="button" class="f-close" title="收起表单" @click="closeForm">
          <svg viewBox="0 0 12 12" fill="none">
            <path d="m3.2 3.2 5.6 5.6M8.8 3.2 3.2 8.8" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
          </svg>
        </button>
      </div>

      <div class="f-row">
        <input
          ref="labelInput"
          v-model="draft.label"
          class="f-label"
          placeholder="事件名称"
          @keyup.enter="submit"
          @keyup.esc="closeForm"
        />
        <select v-model="draft.timeType" class="f-type">
          <option value="exact">确定时刻</option>
          <option value="range">时间段</option>
          <option value="fuzzy">模糊时段</option>
        </select>
      </div>

      <div class="f-row">
        <!-- 确定时刻 -->
        <input
          v-if="draft.timeType === 'exact'"
          v-model="draft.time"
          class="f-time"
          type="datetime-local"
          @keyup.esc="closeForm"
        />

        <!-- 时间段 -->
        <template v-else-if="draft.timeType === 'range'">
          <input v-model="draft.startTime" class="f-time" type="datetime-local" />
          <span class="f-tilde">~</span>
          <input v-model="draft.endTime" class="f-time" type="datetime-local" />
        </template>

        <!-- 模糊时段 -->
        <template v-else>
          <input v-model="draft.fuzzyDate" class="f-time" type="date" />
          <select v-model="draft.fuzzyPeriod" class="f-period">
            <option value="morning">清晨</option>
            <option value="noon">正午</option>
            <option value="afternoon">下午</option>
            <option value="evening">傍晚</option>
            <option value="night">深夜</option>
            <option value="unknown">不详</option>
          </select>
        </template>
      </div>

      <div class="f-row f-actions">
        <button type="button" class="f-ok" :disabled="!canSubmit" @click="submit">
          {{ editingId ? '保存' : '添加' }}
        </button>
        <button type="button" class="f-cancel" @click="closeForm">取消</button>
        <span class="f-hint">Enter 确认 · Esc 取消</span>
      </div>
    </div>

    <!-- 轴线区 -->
    <div ref="axisRef" class="tl-axis-wrap">
      <svg
        v-if="!isVertical"
        class="tl-svg"
        :width="axisWidth"
        :height="128"
        :viewBox="`0 0 ${axisWidth} 128`"
      >
        <defs>
          <marker id="tl-arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
            <polygon points="0 0, 8 4, 0 8" fill="#8C877E" />
          </marker>
        </defs>

        <!-- 主轴线 -->
        <line
          :x1="8"
          :y1="64"
          :x2="axisWidth - 4"
          :y2="64"
          stroke="#D9D4CA"
          stroke-width="1"
          marker-end="url(#tl-arrow)"
        />

        <!-- 已确定段 / 推定段：按确定度给轴线上色 -->
        <line
          v-for="seg in segments"
          :key="`seg-${seg.key}`"
          :x1="seg.from"
          :y1="64"
          :x2="seg.to"
          :y2="64"
          :stroke="seg.color"
          stroke-width="1"
        />

        <!-- 节点 + 刻度 -->
        <g v-for="p in laidOut" :key="p.point.id">
          <circle
            :cx="px(p.pos)"
            cy="64"
            :r="p.certainty === 'fuzzy' ? 5 : 5.5"
            :fill="dotFill(p)"
            :stroke="dotStroke(p)"
            :stroke-width="p.certainty === 'fuzzy' ? 1.5 : 0"
            class="tl-dot"
            @click="openPointMenu(p.point, $event)"
          />

          <text
            :x="px(p.pos)"
            y="40"
            text-anchor="middle"
            class="tl-event-label"
            :fill="p.certainty === 'exact' ? '#3F5B4C' : '#8C877E'"
          >
            {{ p.point.label }}
          </text>

          <text :x="px(p.pos)" y="88" text-anchor="middle" class="tl-tick">
            {{ p.tick }}
          </text>

          <text
            v-if="p.compressNote"
            :x="px(p.pos)"
            y="106"
            text-anchor="middle"
            class="tl-note"
          >
            {{ p.compressNote }}
          </text>
        </g>

        <!-- 空态 -->
        <text
          v-if="!laidOut.length"
          :x="axisWidth / 2"
          y="70"
          text-anchor="middle"
          class="tl-empty"
        >
          还没有时间点
        </text>
      </svg>

      <svg
        v-else
        class="tl-svg"
        :width="268"
        :height="axisWidth"
        :viewBox="`0 0 268 ${axisWidth}`"
      >
        <!-- 纵向：轴线转 90°，刻度移到左侧 -->
        <line :x1="72" :y1="8" :x2="72" :y2="axisWidth - 4" stroke="#D9D4CA" stroke-width="1" />

        <line
          v-for="seg in segments"
          :key="`vseg-${seg.key}`"
          :x1="72"
          :y1="seg.from"
          :x2="72"
          :y2="seg.to"
          :stroke="seg.color"
          stroke-width="1"
        />

        <g v-for="p in laidOut" :key="`v-${p.point.id}`">
          <circle
            :cx="72"
            :cy="px(p.pos)"
            r="5.5"
            :fill="dotFill(p)"
            :stroke="dotStroke(p)"
            :stroke-width="p.certainty === 'fuzzy' ? 1.5 : 0"
            class="tl-dot"
            @click="openPointMenu(p.point, $event)"
          />

          <text :x="62" :y="px(p.pos) + 3.5" text-anchor="end" class="tl-tick">
            {{ p.tick }}
          </text>

          <text
            :x="86"
            :y="px(p.pos) - 3"
            class="tl-event-label"
            :fill="p.certainty === 'exact' ? '#3F5B4C' : '#8C877E'"
          >
            {{ p.point.label }}
          </text>

          <text v-if="p.compressNote" :x="86" :y="px(p.pos) + 12" class="tl-note">
            {{ p.compressNote }}
          </text>
        </g>

        <text
          v-if="!laidOut.length"
          x="134"
          :y="axisWidth / 2"
          text-anchor="middle"
          class="tl-empty"
        >
          还没有时间点
        </text>
      </svg>
    </div>

    <!-- 压缩图例 -->
    <div v-if="layout.hasCompression" class="tl-legend">
      <span class="legend-dot" style="background: #7c9a88" />已确定
      <span class="legend-dot" style="background: #a8bdb0" />推定
      <span class="legend-dot" style="background: #b8c9be" />模糊
      <span class="legend-sep" />
      模糊时段已按设计规范压缩显示
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import type { FuzzyPeriod, TimeType, Timeline, TimelinePoint } from '@/api/types'
import { layoutTimeline, type LaidOutPoint } from '@/utils/timelineLayout'
import { localId } from '@/stores/workspaceStore'

const props = defineProps<{
  timeline: Timeline
  /** 横向时的面板宽（设计稿 760） */
  width?: number
  /** 面板左上角坐标（画布坐标系） */
  x?: number
  y?: number
  /** 当前画布缩放，用于把屏幕位移换算成画布位移 */
  zoom?: number
}>()

const emit = defineEmits<{
  rename: [id: string, name: string]
  direction: [id: string, direction: Timeline['direction']]
  addPoint: [timelineId: string, point: TimelinePoint]
  updatePoint: [timelineId: string, pointId: string, patch: Partial<TimelinePoint>]
  removePoint: [timelineId: string, pointId: string]
  /** 拖动结束，返回画布坐标 */
  move: [timelineId: string, x: number, y: number]
  /** 移除整条时间线 */
  close: [timelineId: string]
}>()

/* ---------------- 拖动移动 ---------------- */
const dragging = ref(false)

function startHeadDrag(e: MouseEvent) {
  const t = e.target as HTMLElement
  // 头部里的输入框 / 按钮不参与拖动
  if (t.closest('input, button, select')) return

  dragging.value = true
  const sx = e.clientX
  const sy = e.clientY
  const ox = props.x ?? 0
  const oy = props.y ?? 0
  const scale = props.zoom && props.zoom > 0 ? props.zoom : 1

  const onMove = (ev: MouseEvent) => {
    const nx = Math.round((ox + (ev.clientX - sx) / scale) / 8) * 8
    const ny = Math.round((oy + (ev.clientY - sy) / scale) / 8) * 8
    emit('move', props.timeline.id, nx, ny)
  }
  const onUp = () => {
    dragging.value = false
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }

  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

/* ---------------- 时间点录入 / 编辑 ---------------- */
const formOpen = ref(false)
const editingId = ref<string | null>(null)
const labelInput = ref<HTMLInputElement | null>(null)

interface Draft {
  label: string
  timeType: TimeType
  time: string
  startTime: string
  endTime: string
  fuzzyDate: string
  fuzzyPeriod: FuzzyPeriod
}

function emptyDraft(): Draft {
  return {
    label: '',
    timeType: 'exact',
    time: '',
    startTime: '',
    endTime: '',
    fuzzyDate: '',
    fuzzyPeriod: 'unknown'
  }
}

const draft = ref<Draft>(emptyDraft())

const canSubmit = computed(() => {
  const d = draft.value
  if (!d.label.trim()) return false
  if (d.timeType === 'exact') return !!d.time
  if (d.timeType === 'range') return !!d.startTime && !!d.endTime
  return !!d.fuzzyDate
})

/** 「＋ 时间点」：只管打开新增表单 */
async function openCreateForm() {
  editingId.value = null
  draft.value = emptyDraft()
  formOpen.value = true
  await nextTick()
  labelInput.value?.focus()
}

function closeForm() {
  formOpen.value = false
  editingId.value = null
  draft.value = emptyDraft()
}

/* ---------------- 编辑已有时间点 ---------------- */
function openPointMenu(point: TimelinePoint, e: MouseEvent) {
  e.stopPropagation()
  editingId.value = point.id
  draft.value = toDraft(point)
  formOpen.value = true
  nextTick(() => labelInput.value?.focus())
}

/** TimelinePoint → 表单草稿（把后端的 `YYYY-MM-DD HH:mm:ss` 转回 datetime-local 需要的格式） */
function toDraft(p: TimelinePoint): Draft {
  return {
    label: p.label,
    timeType: p.timeType,
    time: fromDateTime(p.time),
    startTime: fromDateTime(p.startTime),
    endTime: fromDateTime(p.endTime),
    fuzzyDate: p.fuzzyDate ?? '',
    fuzzyPeriod: p.fuzzyPeriod ?? 'unknown'
  }
}

/** `2026-03-14 20:00:00` → `2026-03-14T20:00` */
function fromDateTime(v?: string | null): string {
  if (!v) return ''
  const s = v.replace(' ', 'T')
  return s.length >= 16 ? s.slice(0, 16) : s
}

function removeCurrent() {
  if (!editingId.value) return
  emit('removePoint', props.timeline.id, editingId.value)
  closeForm()
}

watch(
  () => props.timeline.points.map((p) => p.id).join(','),
  (ids) => {
    // 正在编辑的点被外部删掉了 → 收起表单
    if (editingId.value && !ids.split(',').includes(editingId.value)) closeForm()
  }
)

/** datetime-local 给的是 `2026-03-14T20:00`，补成后端认的 `YYYY-MM-DD HH:mm:ss` */
function normalizeDateTime(v: string): string {
  if (!v) return v
  const [date, time = '00:00'] = v.split('T')
  const t = time.length === 5 ? `${time}:00` : time
  return `${date} ${t}`
}

function submit() {
  if (!canSubmit.value) return
  const d = draft.value
  const base: Partial<TimelinePoint> = {
    timeType: d.timeType,
    label: d.label.trim(),
    // 先清空所有时间字段，再按当前类型填，避免切换类型后残留旧值
    time: null,
    startTime: null,
    endTime: null,
    fuzzyDate: null,
    fuzzyPeriod: null
  }

  if (d.timeType === 'exact') {
    base.time = normalizeDateTime(d.time)
  } else if (d.timeType === 'range') {
    base.startTime = normalizeDateTime(d.startTime)
    base.endTime = normalizeDateTime(d.endTime)
  } else {
    base.fuzzyDate = d.fuzzyDate
    base.fuzzyPeriod = d.fuzzyPeriod
  }

  if (editingId.value) {
    emit('updatePoint', props.timeline.id, editingId.value, base)
    closeForm()
    return
  }

  emit('addPoint', props.timeline.id, { id: localId('pt'), ...base } as TimelinePoint)
  // 连续录入：保留类型选择，清掉内容
  const keepType = d.timeType
  draft.value = { ...emptyDraft(), timeType: keepType }
  nextTick(() => labelInput.value?.focus())
}

/* ---------------- 轴线布局 ---------------- */
const axisRef = ref<HTMLElement | null>(null)
const axisWidth = ref(700)

const isVertical = computed(() => props.timeline.direction === 'vertical')

const layout = computed(() => layoutTimeline(props.timeline, axisWidth.value))
const laidOut = computed<LaidOutPoint[]>(() => layout.value.points)

/** 轴线上按确定度着色的区段 */
const segments = computed(() => {
  const pts = laidOut.value
  const out: { key: string; from: number; to: number; color: string }[] = []
  for (let i = 0; i < pts.length - 1; i++) {
    const a = pts[i]
    const b = pts[i + 1]
    // 段色取两端中较弱的一个确定度
    const rank = { exact: 2, estimated: 1, fuzzy: 0 } as const
    const weaker = rank[a.certainty] <= rank[b.certainty] ? a.certainty : b.certainty
    const color = weaker === 'exact' ? '#7C9A88' : weaker === 'estimated' ? '#A8BDB0' : '#B8C9BE'
    out.push({ key: `${a.point.id}__${b.point.id}`, from: px(a.pos), to: px(b.pos), color })
  }
  return out
})

function px(pos: number): number {
  return (pos / 100) * axisWidth.value
}

function dotFill(p: LaidOutPoint): string {
  switch (p.certainty) {
    case 'exact':
      return '#7C9A88'
    case 'estimated':
      return '#9BAFA4'
    default:
      return '#FFF'
  }
}

function dotStroke(p: LaidOutPoint): string {
  return p.certainty === 'fuzzy' ? '#9BAFA4' : 'none'
}

const nameDraft = ref(props.timeline.name)

watch(
  () => props.timeline.name,
  (v) => {
    nameDraft.value = v
  }
)

function onRename(e: Event) {
  nameDraft.value = (e.target as HTMLInputElement).value
}

function commitName() {
  const name = nameDraft.value.trim()
  if (name && name !== props.timeline.name) emit('rename', props.timeline.id, name)
  else nameDraft.value = props.timeline.name
}

function setDirection(dir: Timeline['direction']) {
  if (dir !== props.timeline.direction) emit('direction', props.timeline.id, dir)
}

function measure() {
  const el = axisRef.value
  if (!el) return
  const vertical = isVertical.value
  const w = vertical
    ? Math.max(el.clientHeight - 24, 240)
    : Math.max(el.clientWidth - 24, 240)
  axisWidth.value = w
}

onMounted(async () => {
  await nextTick()
  measure()
  window.addEventListener('resize', measure)
})

watch(
  () => [props.timeline.direction, props.timeline.points.length],
  async () => {
    await nextTick()
    measure()
  }
)

defineExpose({ measure })
</script>

<style scoped>
/*
 * 去卡片化：无底色、无描边、无阴影、无圆角，
 * 让轴线像注解一样直接「画」在画布上。
 */
.tl-panel {
  display: flex;
  flex-direction: column;
  background: transparent;
  border: 0;
  box-shadow: none;
  overflow: visible;
  pointer-events: auto;
}

.tl-panel.vertical {
  max-height: 460px;
}

/* 头部：整条是拖动握把，只在悬停时给一点底色提示可拖 */
.tl-head {
  display: flex;
  align-items: center;
  gap: 7px;
  flex: none;
  height: 32px;
  padding: 0 4px 0 2px;
  border-radius: 7px;
  cursor: grab;
  transition: background 160ms var(--ease);
}

.tl-head:hover {
  background: rgba(232, 228, 219, 0.5);
}

.tl-panel.dragging .tl-head {
  cursor: grabbing;
  background: rgba(228, 234, 229, 0.65);
}

.tl-grip {
  width: 6px;
  height: 12px;
  flex: none;
  color: #b8b2a8;
}

.tl-head:hover .tl-grip {
  color: #8c877e;
}

.tl-name {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink-1);
  padding: 2px 0;
}

.tl-count {
  flex: none;
  font-size: 10px;
  color: var(--ink-5);
}

/* 头部小按钮：添加 / 关闭 完全分开 */
.tl-act {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  height: 22px;
  padding: 0 7px;
  border: 0;
  border-radius: 6px;
  background: rgba(237, 234, 228, 0.72);
  color: var(--ink-2);
  font-family: inherit;
  font-size: 10px;
  cursor: pointer;
  transition: background 140ms var(--ease), color 140ms var(--ease);
}

.tl-act svg {
  width: 11px;
  height: 11px;
}

.tl-act:hover {
  background: #e2eae5;
  color: var(--green-ink, #3f5b4c);
}

.tl-act.add.on {
  background: #5c7f6b;
  color: #fff;
}

.tl-act.close {
  width: 22px;
  padding: 0;
  justify-content: center;
  color: #b3aca2;
  background: transparent;
}

.tl-act.close:hover {
  background: rgba(240, 226, 222, 0.75);
  color: #a8756b;
}

/* ---------------- 时间点录入表单 ---------------- */
.tl-form {
  flex: none;
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin: 4px 0 6px;
  padding: 9px 10px;
  border-radius: var(--r-lg);
  background: rgba(246, 244, 239, 0.94);
}

.f-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.f-mode {
  font-size: 10.5px;
  color: var(--ink-4);
}

.f-del {
  margin-left: auto;
  height: 22px;
  padding: 0 9px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #a8756b;
  font-family: inherit;
  font-size: 10.5px;
  cursor: pointer;
  transition: background 140ms var(--ease);
}

.f-del:hover {
  background: #f5ebe8;
}

.f-close {
  margin-left: auto;
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #a8a29a;
  cursor: pointer;
}

.f-close svg {
  width: 11px;
  height: 11px;
}

.f-close:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--ink-2);
}

.f-del + .f-close {
  margin-left: 2px;
}

/* 时间点圆点可点击进入编辑 */
.tl-dot {
  cursor: pointer;
}

.tl-dot:hover {
  filter: brightness(0.94);
}

.f-row .f-label {
  flex: 1;
  min-width: 0;
}

.f-type,
.f-period,
.f-time {
  flex: none;
  height: 26px;
  padding: 0 7px;
  border: 1px solid #e0dacc;
  border-radius: 6px;
  background: #fff;
  font-family: inherit;
  font-size: 11px;
  color: var(--ink-1);
  outline: 0;
  transition: border-color 140ms var(--ease);
}

.f-time {
  flex: 1;
  min-width: 0;
}

.f-label {
  height: 26px;
  padding: 0 8px;
  border: 1px solid #e0dacc;
  border-radius: 6px;
  background: #fff;
  font-family: inherit;
  font-size: 12px;
  color: var(--ink-1);
  outline: 0;
  transition: border-color 140ms var(--ease);
}

.f-label:focus,
.f-type:focus,
.f-period:focus,
.f-time:focus {
  border-color: #a6bfb0;
}

.f-tilde {
  flex: none;
  font-size: 11px;
  color: var(--ink-4);
}

.f-actions {
  justify-content: flex-start;
}

.f-ok,
.f-cancel {
  height: 24px;
  padding: 0 11px;
  border: 0;
  border-radius: 6px;
  font-family: inherit;
  font-size: 11px;
  cursor: pointer;
  transition: background 140ms var(--ease), opacity 140ms var(--ease);
}

.f-ok {
  background: #5c7f6b;
  color: #fff;
}

.f-ok:hover:not(:disabled) {
  background: #4e6f5c;
}

.f-ok:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.f-cancel {
  background: transparent;
  color: var(--ink-3);
}

.f-cancel:hover {
  background: #edeae4;
}

.f-hint {
  margin-left: auto;
  font-size: 10px;
  color: var(--ink-5);
}

/* 方向切换 */
.dir-switch {
  display: flex;
  flex: none;
  gap: 0;
  width: 46px;
  height: 22px;
  padding: 2px;
  border-radius: 6px;
  background: rgba(240, 238, 234, 0.9);
}

.dir-seg {
  flex: 1;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--ink-3);
  cursor: pointer;
  transition: background 140ms var(--ease), color 140ms var(--ease);
}

.dir-seg svg {
  width: 11px;
  height: 11px;
}

.dir-seg.active {
  background: #fff;
  color: var(--ink-1);
}

/* 轴线区 */
.tl-axis-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  justify-content: center;
  overflow: hidden;
}

.tl-svg {
  display: block;
}

.tl-event-label {
  font-family: var(--font-mono);
  font-size: 10.5px;
}

.tl-tick {
  font-family: var(--font-mono);
  font-size: 10.5px;
  fill: #6b665e;
}

.tl-note {
  font-family: var(--font-mono);
  font-size: 10px;
  fill: #b5afa6;
}

.tl-empty {
  font-family: var(--font-sans);
  font-size: 12px;
  fill: var(--ink-5);
}

/* 图例：无分隔线，纯文字 */
.tl-legend {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: none;
  height: 22px;
  padding: 0 6px;
  font-size: 10px;
  color: var(--ink-5);
}

.legend-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-left: 6px;
}

.legend-dot:first-child {
  margin-left: 0;
}

.legend-sep {
  width: 1px;
  height: 10px;
  background: var(--line-3);
  margin: 0 4px;
}
</style>
