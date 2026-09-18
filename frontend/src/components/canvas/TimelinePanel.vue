<template>
  <div
    class="tl-panel"
    :class="{ vertical: isVertical, dragging }"
    :style="panelStyle"
  >
    <!--
      刻度宽度探针：纵向轴左侧要留多少白，取决于本机等宽字体把最宽那条刻度
      渲染成多少像素。这里放一个不可见、不占位的同字体元素，由脚本喂文字量宽。
      模板里刻意不写插值 —— 让 Vue 别去动它的子节点，宽度测量才不会被覆盖。
    -->
    <span ref="tickProbeRef" class="tick-probe" aria-hidden="true" />

    <!-- 右缘 / 下缘拖拽手柄：调整面板大小 -->
    <div
      class="tl-resize"
      :class="isVertical ? 'v' : 'h'"
      title="拖动调整大小"
      @mousedown.stop.prevent="startResize"
    />
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
        <a-select
          v-model:value="draft.timeType"
          class="f-type"
          size="small"
          :dropdown-match-select-width="false"
        >
          <a-select-option value="exact">确定时刻</a-select-option>
          <a-select-option value="range">时间段</a-select-option>
          <a-select-option value="fuzzy">模糊时段</a-select-option>
        </a-select>
      </div>

      <div class="f-row">
        <!-- 确定时刻 -->
        <a-date-picker
          v-if="draft.timeType === 'exact'"
          v-model:value="timeModel"
          class="f-time"
          size="small"
          show-time
          format="YYYY-MM-DD HH:mm"
          placeholder="选择日期与时刻"
          :allow-clear="false"
        />

        <!-- 时间段 -->
        <template v-else-if="draft.timeType === 'range'">
          <a-date-picker
            v-model:value="startModel"
            class="f-time"
            size="small"
            show-time
            format="YYYY-MM-DD HH:mm"
            placeholder="开始"
            :allow-clear="false"
          />
          <span class="f-tilde">~</span>
          <a-date-picker
            v-model:value="endModel"
            class="f-time"
            size="small"
            show-time
            format="YYYY-MM-DD HH:mm"
            placeholder="结束"
            :allow-clear="false"
          />
        </template>

        <!-- 模糊时段 -->
        <template v-else>
          <a-date-picker
            v-model:value="fuzzyDateModel"
            class="f-time"
            size="small"
            format="YYYY-MM-DD"
            placeholder="选择日期"
            :allow-clear="false"
          />
          <a-select
            v-model:value="draft.fuzzyPeriod"
            class="f-period"
            size="small"
            :dropdown-match-select-width="false"
          >
            <a-select-option value="morning">清晨</a-select-option>
            <a-select-option value="noon">正午</a-select-option>
            <a-select-option value="afternoon">下午</a-select-option>
            <a-select-option value="evening">傍晚</a-select-option>
            <a-select-option value="night">深夜</a-select-option>
            <a-select-option value="unknown">不详</a-select-option>
          </a-select>
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
      <!-- ══════ 横向 ══════ -->
      <svg
        v-if="!isVertical"
        class="tl-svg"
        :width="axisWidth"
        :height="axisH"
        :viewBox="`0 0 ${axisWidth} ${axisH}`"
      >
        <defs>
          <marker id="tl-arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
            <polygon points="0 0, 8 4, 0 8" fill="#8C877E" />
          </marker>
        </defs>

        <!-- ① 时间段：轴线上的加粗带子（单层胶囊，压在轴线下面） -->
        <g class="tl-marker">
          <rect
            v-for="m in markers"
            :key="`band-${m.id}`"
            class="mk-band"
            :x="m.hx"
            :y="m.hy"
            :width="m.hw"
            :height="m.hh"
            rx="4"
          />
        </g>

        <!-- ② 卡片 → 轴线的引出线（先画，压在所有卡片下面） -->
        <g class="tl-link">
          <path v-for="g in hGeoms" :key="`link-${g.id}`" :d="g.link" />
        </g>

        <!-- ③ 主轴线（带箭头） -->
        <line
          :x1="8"
          :y1="axisY"
          :x2="axisWidth - 4"
          :y2="axisY"
          stroke="#D9D4CA"
          stroke-width="1"
          marker-end="url(#tl-arrow)"
        />

        <!-- ④ 已确定段 / 推定段：按确定度给轴线上色 -->
        <line
          v-for="seg in segments"
          :key="`seg-${seg.key}`"
          :x1="seg.from"
          :y1="axisY"
          :x2="seg.to"
          :y2="axisY"
          :stroke="seg.color"
          stroke-width="1"
        />

        <!-- ⑤ 事件卡片（分道错开，不再压字） -->
        <g
          v-for="g in hGeoms"
          :key="`card-${g.id}`"
          class="tl-card"
          @click="openPointMenu(g.point, $event)"
        >
          <rect
            class="card-body"
            :class="g.certainty"
            :x="g.cardX"
            :y="g.cardY"
            :width="g.cardW"
            :height="CARD_H"
            rx="6"
          />
          <text
            class="card-text"
            :x="g.cardX + g.cardW / 2"
            :y="g.cardY + CARD_H / 2 + 3.6"
            text-anchor="middle"
          >
            {{ cardLabel(g.point.label) }}
            <title>{{ g.point.label }}</title>
          </text>
        </g>

        <!-- ⑥ 轴上节点 + 刻度 -->
        <g v-for="p in laidOut" :key="`pt-${p.point.id}`">
          <circle
            :cx="px(p.pos)"
            :cy="axisY"
            :r="p.certainty === 'fuzzy' ? 5 : 5.5"
            :fill="dotFill(p)"
            :stroke="dotStroke(p)"
            :stroke-width="p.certainty === 'fuzzy' ? 1.5 : 0"
            class="tl-dot"
            @click="openPointMenu(p.point, $event)"
          />
          <!-- 时间段终点：空心小点，标出跨度边界 -->
          <circle
            v-if="p.hasSpan"
            :cx="px(p.endPos)"
            :cy="axisY"
            r="3.4"
            fill="#FFF"
            stroke="#C3BCA6"
            stroke-width="1.2"
            class="tl-dot"
            @click="openPointMenu(p.point, $event)"
          />

          <text
            v-if="!p.tickHidden"
            :x="px(p.pos)"
            :y="tickYOf(p.tickRow)"
            text-anchor="middle"
            class="tl-tick"
          >
            {{ p.tick }}
          </text>
          <!-- 时间段不能只标起点：终点没有时刻，「从什么时候到什么时候」就只读得到前半截 -->
          <text
            v-if="p.endTick && !p.endTickHidden"
            :x="px(p.endPos)"
            :y="tickYOf(p.endTickRow)"
            text-anchor="middle"
            class="tl-tick"
          >
            {{ p.endTick }}
          </text>
        </g>

        <!-- ⑦ 时间段时长 / 压缩说明：独立一行，不压刻度 -->
        <text
          v-for="g in hGeoms.filter((x) => x.note)"
          :key="`note-${g.id}`"
          :x="Math.min(Math.max(g.cardX + g.cardW / 2, 26), axisWidth - 26)"
          :y="noteY(g.row)"
          text-anchor="middle"
          class="tl-note"
        >
          {{ g.note }}
        </text>

        <!-- 空态 -->
        <text
          v-if="!laidOut.length"
          :x="axisWidth / 2"
          :y="axisY + 4"
          text-anchor="middle"
          class="tl-empty"
        >
          还没有时间点
        </text>
      </svg>

      <!-- ══════ 纵向 ══════ -->
      <svg
        v-else
        class="tl-svg"
        :width="V_AXIS_W"
        :height="axisWidth"
        :viewBox="`0 0 ${V_AXIS_W} ${axisWidth}`"
      >
        <!-- ① 时间段：轴线上的加粗带子（单层胶囊） -->
        <g class="tl-marker">
          <rect
            v-for="m in markers"
            :key="`vband-${m.id}`"
            class="mk-band"
            :x="m.vx"
            :y="m.vy"
            :width="m.vw"
            :height="m.vh"
            rx="4"
          />
        </g>

        <!-- ② 引出线 -->
        <g class="tl-link">
          <path v-for="g in vGeoms" :key="`vlink-${g.id}`" :d="g.link" />
        </g>

        <!-- ③ 主轴 -->
        <line :x1="axisVX" :y1="8" :x2="axisVX" :y2="axisWidth - 4" stroke="#D9D4CA" stroke-width="1" />

        <!-- ④ 确定度区段 -->
        <line
          v-for="seg in segments"
          :key="`vseg-${seg.key}`"
          :x1="axisVX"
          :y1="seg.from"
          :x2="axisVX"
          :y2="seg.to"
          :stroke="seg.color"
          stroke-width="1"
        />

        <!-- ⑤ 卡片 -->
        <g
          v-for="g in vGeoms"
          :key="`vcard-${g.id}`"
          class="tl-card"
          @click="openPointMenu(g.point, $event)"
        >
          <rect
            class="card-body"
            :class="g.certainty"
            :x="g.cardX"
            :y="g.cardY"
            :width="g.cardW"
            :height="CARD_H"
            rx="6"
          />
          <text
            class="card-text"
            :x="g.cardX + 8"
            :y="g.cardY + CARD_H / 2 + 3.6"
            text-anchor="start"
          >
            {{ cardLabel(g.point.label) }}
            <title>{{ g.point.label }}</title>
          </text>
        </g>

        <!-- ⑥ 节点 + 刻度 -->
        <g v-for="p in laidOut" :key="`vpt-${p.point.id}`">
          <circle
            :cx="axisVX"
            :cy="px(p.pos)"
            :r="p.certainty === 'fuzzy' ? 5 : 5.5"
            :fill="dotFill(p)"
            :stroke="dotStroke(p)"
            :stroke-width="p.certainty === 'fuzzy' ? 1.5 : 0"
            class="tl-dot"
            @click="openPointMenu(p.point, $event)"
          />
          <circle
            v-if="p.hasSpan"
            :cx="axisVX"
            :cy="px(p.endPos)"
            r="3.4"
            fill="#FFF"
            stroke="#C3BCA6"
            stroke-width="1.2"
            class="tl-dot"
            @click="openPointMenu(p.point, $event)"
          />

          <text
            v-if="!p.tickHidden"
            :x="axisVX - 10"
            :y="vTickY(p.pos, p.tickRow)"
            text-anchor="end"
            class="tl-tick"
          >
            {{ p.tick }}
          </text>
          <!-- 时间段终点时刻：纵向轴同样要标，否则只看得到一个端点 -->
          <text
            v-if="p.endTick && !p.endTickHidden"
            :x="axisVX - 10"
            :y="vTickY(p.endPos, p.endTickRow)"
            text-anchor="end"
            class="tl-tick"
          >
            {{ p.endTick }}
          </text>
        </g>

        <!-- ⑦ 说明文字 -->
        <text
          v-for="g in vGeoms.filter((x) => x.note)"
          :key="`vnote-${g.id}`"
          :x="g.cardX"
          :y="g.cardY + CARD_H + 11"
          class="tl-note"
        >
          {{ g.note }}
        </text>

        <text
          v-if="!laidOut.length"
          :x="V_AXIS_W / 2"
          :y="axisWidth / 2"
          text-anchor="middle"
          class="tl-empty"
        >
          还没有时间点
        </text>
      </svg>
    </div>

    <!-- 图例 -->
    <div v-if="laidOut.length" class="tl-legend">
      <span class="legend-item"><i class="legend-dot" style="background: #7c9a88" />已确定</span>
      <span class="legend-item"><i class="legend-dot" style="background: #a8bdb0" />推定</span>
      <span class="legend-item"><i class="legend-dot" style="background: #b8c9be" />模糊</span>
      <span v-if="markers.length" class="legend-item">
        <span class="legend-sep" />
        <i class="legend-mark" />时间段
      </span>
      <span class="legend-item legend-hint">
        <span class="legend-sep" />
        按真实间隔动态压缩 · 相近时间点自动错行
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
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
  /** 拖缘调整面板大小结束：横向回传 width，纵向回传 height */
  resize: [timelineId: string, size: { width?: number; height?: number }]
  /** 移除整条时间线 */
  close: [timelineId: string]
}>()

/* ---------------- 面板大小调整 ---------------- */
const panelW = ref(props.timeline.width ?? props.width ?? 760)
const panelH = ref(props.timeline.height ?? 400)
const resizing = ref(false)

watch(
  () => [props.timeline.width, props.timeline.height],
  ([w, h]) => {
    if (resizing.value) return
    if (typeof w === 'number') panelW.value = w
    if (typeof h === 'number') panelH.value = h
  }
)

const panelStyle = computed(() =>
  isVertical.value
    ? { width: `${V_PANEL_W.value}px`, height: `${panelH.value}px` }
    : { width: `${panelW.value}px` }
)

function startResize(e: MouseEvent) {
  resizing.value = true
  const scale = props.zoom && props.zoom > 0 ? props.zoom : 1
  const sx = e.clientX
  const sy = e.clientY
  const ow = panelW.value
  const oh = panelH.value
  const vertical = isVertical.value

  const onMove = (ev: MouseEvent) => {
    if (vertical) {
      panelH.value = Math.min(1400, Math.max(260, Math.round((oh + (ev.clientY - sy) / scale) / 8) * 8))
    } else {
      panelW.value = Math.min(1440, Math.max(320, Math.round((ow + (ev.clientX - sx) / scale) / 8) * 8))
    }
    nextTick(measure)
  }
  const onUp = () => {
    resizing.value = false
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
    emit(
      'resize',
      props.timeline.id,
      vertical ? { height: panelH.value } : { width: panelW.value }
    )
  }

  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

/* ---------------- 拖动移动 ---------------- */
const dragging = ref(false)

function startHeadDrag(e: MouseEvent) {
  const t = e.target as HTMLElement
  // 头部里的输入框 / 按钮 / 下拉不参与拖动。
  // .ant-select / .ant-picker 要单独列出来：它们的触发区是 div 而不是原生控件，
  // 只判 input/button/select 的话，点在下拉框空白处会把整条时间线拖走。
  if (t.closest('input, button, select, .ant-select, .ant-picker')) return

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

/*
  原本这里用的是 <input type="datetime-local"> / type="date"。
  那是浏览器原生控件：Chrome 会弹系统选择器，但当时的外壳 JavaFX WebView（WebKit）
  **不支持这两个类型** —— 不弹选择器、也不接受手输，表现就是「时间根本选不了」。

  外壳现在换成了 Electron（Chromium），datetime-local 本可以用了，但这里**继续用 antd**：
  原生日期面板是另一套视觉语言，混进这套低饱和绿灰里很突兀；
  何况 antd 那份面板已经和界面上其它下拉框对齐过了。

  Draft 里的时间仍是 'YYYY-MM-DDTHH:mm' 字符串，submit / toDraft / normalizeDateTime 一律不改，
  中间垫一层 computed 做 dayjs ↔ 字符串的换算。
  （这两个格式本身就是 ISO-8601，dayjs 直接解析即可，不用引 customParseFormat 插件。）
*/
function makeDateTimeModel(key: 'time' | 'startTime' | 'endTime') {
  return computed<Dayjs | null>({
    get: () => (draft.value[key] ? dayjs(draft.value[key]) : null),
    set: (v) => {
      draft.value[key] = v ? v.format('YYYY-MM-DDTHH:mm') : ''
    }
  })
}

const timeModel = makeDateTimeModel('time')
const startModel = makeDateTimeModel('startTime')
const endModel = makeDateTimeModel('endTime')

const fuzzyDateModel = computed<Dayjs | null>({
  get: () => (draft.value.fuzzyDate ? dayjs(draft.value.fuzzyDate) : null),
  set: (v) => {
    draft.value.fuzzyDate = v ? v.format('YYYY-MM-DD') : ''
  }
})

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

/* ============================================================
 * 轴线布局
 * ========================================================== */
const axisRef = ref<HTMLElement | null>(null)
const axisWidth = ref(700)

const isVertical = computed(() => props.timeline.direction === 'vertical')

/**
 * 排布：横向轴卡片最宽 104px，所以要隔开 ~110px 才会分道；
 * 纵向轴把卡片顺着轴推 26px（卡片高 18 + 间隙 8），
 * 于是「同一时刻的多个事件」天然被推开，不叠在一起。
 */
const layout = computed(() =>
  layoutTimeline(props.timeline, axisWidth.value, {
    cardExtent: isVertical.value ? CARD_H : CARD_MAX_W,
    cardGap: isVertical.value ? 10 : 6,
    cardShift: isVertical.value ? V_LANE_STEP : 0,
    tickExtent: isVertical.value ? 14 : 58,
    tickGap: isVertical.value ? 4 : 8,
    tickShift: isVertical.value ? 14 : 0,
    maxLanes: isVertical.value ? 4 : 3
  })
)
const laidOut = computed<LaidOutPoint[]>(() => layout.value.points)

/* ---------------- 几何常量 ---------------- */
const CARD_H = 18
/** 卡片最大宽度（和分道参数里的 cardExtent 保持一致） */
const CARD_MAX_W = 104
const LANE_STEP = 28
/** 纵向轴每往下一道推多远（卡片高 + 间隙） */
const V_LANE_STEP = 26
const BUS_OFF = 10
const BUS_GAP = 6
const TICK_STEP = 14

/* ---------------- 纵向：左侧留白按实测刻度宽度倒推 ----------------
 * 原来这里是写死的「轴线 x = 66」，而刻度以 axisX - 10 右对齐。
 * 于是「9/11 23:11」这类十来字符的刻度会把左边缘推到 x < 0，
 * 被 SVG 视口直接裁掉开头几个字 —— 就是竖向时间轴里刻度「缺前半截」的原因。
 * 更麻烦的是它随字体变：本机没装 Sarasa Mono 就会落到 Consolas，
 * 字宽不同、吃掉的像素也不同，写死一个更大的常数只是把问题推给别的机器。
 *
 * 所以改成实测：探针元素（同样式字体）量出最宽那条刻度，再倒推轴线该站哪。
 */
const TICK_FONT_GAP = 14 // 刻度右端到轴线：10px 空隙 + 光晕与节点的余量
const V_AXIS_MIN_X = 66 // 刻度都很短时，保持原来的视觉位置不动
/** 面板比 SVG 宽出来的部分（左右各 14，给拖拽手柄和呼吸留白） */
const V_PANEL_PAD = 28

const tickProbeRef = ref<HTMLElement | null>(null)
/** 最宽刻度的像素宽（布局像素，不含画布缩放） */
const tickMaxWidth = ref(0)

/** 纵向轴所在的 x 坐标 */
const axisVX = computed(() => Math.max(V_AXIS_MIN_X, tickMaxWidth.value + TICK_FONT_GAP))
/** 纵向面板的 SVG 宽度 = 左侧刻度留白 + 右侧卡片列 */
const V_AXIS_W = computed(() => Math.max(292, axisVX.value + 16 + CARD_MAX_W + 24))
const V_PANEL_W = computed(() => V_AXIS_W.value + V_PANEL_PAD)

/** 轴线的 y：随卡片道数下移，最上面一道卡片永远贴着 y=12 */
const axisY = computed(() => 46 + layout.value.labelRows * LANE_STEP)
const axisH = computed(() => {
  const { tickRows, points } = layout.value
  const hasNote = points.some((p) => p.compressNote)
  return axisY.value + 30 + tickRows * TICK_STEP + (hasNote ? 16 : 0)
})

const tickBase = computed(() => axisY.value + 20)

/* ---------------- 卡片宽度 ---------------- */
/** 卡片里最多显示 8 个字，完整名称交给原生 title */
function cardLabel(label: string): string {
  return label.length > 8 ? `${label.slice(0, 8)}…` : label
}

function cardWidth(label: string): number {
  const n = cardLabel(label).length
  return Math.min(CARD_MAX_W, Math.max(44, 16 + n * 10.5))
}

/* ---------------- 横向几何 ---------------- */
interface HGeom {
  id: string
  point: TimelinePoint
  certainty: string
  cardX: number
  cardY: number
  cardW: number
  link: string
  row: number
  note: string
}

const hGeoms = computed<HGeom[]>(() =>
  laidOut.value.map((p) => {
    const w = cardWidth(p.point.label)
    const mx = px(p.pos)
    const row = p.labelRow
    const busY = axisY.value - BUS_OFF - row * LANE_STEP
    const cardBottom = busY - BUS_GAP
    const cardY = cardBottom - CARD_H
    const cardX = Math.min(
      Math.max(mx - w / 2, 2),
      Math.max(axisWidth.value - w - 2, 2)
    )
    const cx = cardX + w / 2
    // 竖直（卡片底 → 母线）→ 水平（母线 → 该点 x）→ 竖直（母线 → 轴）
    const link = `M ${round(cx)} ${round(cardBottom)} V ${round(busY)} H ${round(mx)} V ${round(axisY.value - 7)}`
    return {
      id: p.point.id,
      point: p.point,
      certainty: p.certainty,
      cardX,
      cardY,
      cardW: w,
      link,
      row,
      note: [p.spanText, p.compressNote].filter(Boolean).join(' · ')
    }
  })
)

/* ---------------- 纵向几何 ---------------- */
interface VGeom {
  id: string
  point: TimelinePoint
  certainty: string
  cardX: number
  cardY: number
  cardW: number
  link: string
  note: string
}

const vGeoms = computed<VGeom[]>(() =>
  laidOut.value.map((p) => {
    const w = cardWidth(p.point.label)
    const my = px(p.pos)
    const cardX = axisVX.value + 16
    const cardY = Math.min(
      Math.max(my - CARD_H / 2 + p.labelRow * V_LANE_STEP, 2),
      Math.max(axisWidth.value - CARD_H - 2, 2)
    )
    const midY = cardY + CARD_H / 2
    // 水平（卡片左缘 → 母线）→ 竖直（母线 → 该点 y）→ 水平（母线 → 轴）
    const link = `M ${round(cardX)} ${round(midY)} H ${round(axisVX.value + 7)} V ${round(my)} H ${round(axisVX.value)}`
    return {
      id: p.point.id,
      point: p.point,
      certainty: p.certainty,
      cardX,
      cardY,
      cardW: w,
      link,
      note: [p.spanText, p.compressNote].filter(Boolean).join(' · ')
    }
  })
)

function round(n: number): number {
  return Math.round(n * 10) / 10
}

/* ---------------- 刻度 / 说明的 y ---------------- */
/** 刻度基线：横向轴按分道号往下错行；起止两个刻度各自带自己的道号 */
function tickYOf(row: number): number {
  return tickBase.value + row * TICK_STEP
}

function noteY(row: number): number {
  return tickBase.value + (layout.value.tickRows + 1) * TICK_STEP + row * 12
}

function px(pos: number): number {
  return (pos / 100) * axisWidth.value
}

/**
 * 纵向刻度基线：分道错行是**往下推**的，靠底部的点被推过去就会掉出 SVG 视口
 * （纵向的 SVG 高度由面板高度决定，不像横向那样能自己长高）。
 * 与其让它消失，不如退回自己那一行 —— 挤一点，但读得到。
 */
function vTickY(pos: number, row: number): number {
  const base = px(pos) + 3.5
  const shifted = base + row * TICK_STEP
  return shifted > axisWidth.value - 3 ? base : shifted
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

/* ---------------- 轴线上按确定度着色的区段 ---------------- */
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

/* ---------------- 时间段：轴线上的加粗带子 ---------------- */
/**
 * 之前是双层矩形叠出来的「马克笔」（30px 的淡晕 + 16px 的主笔触），
 * 观感像用荧光笔来回涂了两遍，视觉噪音比它承载的信息还多。
 *
 * 现在只留一层：一条贴着轴线、高 8px 的圆角胶囊。读起来就是
 * 「轴线在这一段被加粗了」，干净，也不跟刻度、圆点抢位置。
 */
const MARKER_THICK = 8

const markers = computed(() =>
  laidOut.value
    .filter((p) => p.hasSpan)
    .map((p) => {
      const a = px(p.pos)
      const b = px(p.endPos)
      const left = Math.min(a, b)
      const len = Math.max(Math.abs(b - a), 4)
      const half = MARKER_THICK / 2
      return {
        id: p.point.id,
        // 横向：带子沿 x 铺开，垂直居中在轴线上
        hx: left,
        hy: axisY.value - half,
        hw: len,
        hh: MARKER_THICK,
        // 纵向：带子沿 y 铺开，水平居中在轴线上
        vx: axisVX.value - half,
        vy: left,
        vw: MARKER_THICK,
        vh: len
      }
    })
)

/* ---------------- 名称 ---------------- */
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

/**
 * 估算文字在等宽字体下的宽度权重：CJK / 全角是拉丁字符的两倍宽。
 * 只用来挑"最可能是最宽的那几条"，不是精确值。
 */
function textUnits(text: string): number {
  let units = 0
  for (const ch of text) {
    units += ch.charCodeAt(0) > 0x7f ? 2 : 1
  }
  return units
}

/**
 * 量出最宽刻度占多少像素，纵向轴据此决定左侧留白。
 *
 * 为什么用 offsetWidth 而不是 getBoundingClientRect：
 * 面板挂在 .stage 的 `transform: scale(zoom)` 里，getBoundingClientRect 返回的是
 * **缩放后**的视觉尺寸，缩小画布会把刻度量窄、留白跟着算少。
 * offsetWidth 取的是布局盒宽度，不受祖先 transform 影响 —— 正是指标文字在
 * SVG 用户坐标系里的宽度。
 *
 * 读 offsetWidth 会强制一次同步布局，所以**不能**把每条刻度都量一遍：
 * 刻度一多、再叠上拖动面板宽度时的连续重算，这里会变成卡顿源。
 * 等宽字体下"单位数最多"的那条就是最宽的，量前几名取最大值即可；
 * 取前几名而不是第一名，是为了万一本机把 --font-mono 落到了比例字体时不会排错。
 */
const MEASURE_CANDIDATES = 3

function measureTicks() {
  const el = tickProbeRef.value
  if (!el) {
    tickMaxWidth.value = 0
    return
  }

  const seen = new Set<string>()
  for (const p of laidOut.value) {
    if (!p.tickHidden) seen.add(p.tick)
    if (p.endTick && !p.endTickHidden) seen.add(p.endTick)
  }

  const candidates = [...seen]
    .map((text) => ({ text, units: textUnits(text) }))
    .sort((a, b) => b.units - a.units)
    .slice(0, MEASURE_CANDIDATES)

  let max = 0
  for (const { text } of candidates) {
    el.textContent = text
    const w = el.offsetWidth
    if (w > max) max = w
  }
  tickMaxWidth.value = Math.ceil(max)
}

onMounted(async () => {
  // 先量刻度再量轴线：首次渲染就要拿到正确留白，否则会看到刻度闪一下才归位
  measureTicks()
  await nextTick()
  measure()
  measureTicks()
  window.addEventListener('resize', measure)
})

watch(
  () => [props.timeline.direction, props.timeline.points.length],
  async () => {
    await nextTick()
    measure()
    measureTicks()
  }
)

// 刻度文案/显隐会随改时间、改类型、拖面板宽度而变，留白要跟着重算
watch(
  () =>
    laidOut.value
      .map((p) => `${p.tickHidden ? '-' : p.tick}|${p.endTickHidden ? '-' : p.endTick}`)
      .join(','),
  async () => {
    await nextTick()
    measureTicks()
  },
  { flush: 'post' }
)

defineExpose({ measure })
</script>

<style scoped>
/*
 * 去卡片化：无底色、无描边、无阴影、无圆角，
 * 让轴线像注解一样直接「画」在画布上。
 */
.tl-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  background: transparent;
  border: 0;
  box-shadow: none;
  overflow: visible;
  pointer-events: auto;
}

.tl-panel.vertical {
  min-height: 260px;
}

/*
  刻度宽度探针：只用来量字宽，不参与布局、不可见、不接事件。
  字体字号必须和 .tl-tick 一致，否则量出来的宽度没有意义。
  inline-block + white-space:pre 让它有确定的盒子宽度（纯 inline 元素的
  offsetWidth 在部分引擎里不稳），同时保留空格不被折行吞掉。
*/
.tick-probe {
  position: absolute;
  top: 0;
  left: 0;
  display: inline-block;
  white-space: pre;
  font-family: var(--font-mono);
  font-size: 10.5px;
  pointer-events: none;
  visibility: hidden;
}

/* 右缘 / 下缘拖拽手柄 */
.tl-resize {
  position: absolute;
  z-index: 2;
  border-radius: 3px;
}

.tl-resize.h {
  top: 0;
  right: -4px;
  bottom: 0;
  width: 8px;
  cursor: ew-resize;
}

.tl-resize.v {
  left: 0;
  right: 0;
  bottom: -4px;
  height: 8px;
  cursor: ns-resize;
}

.tl-resize:hover {
  background: rgba(124, 154, 136, 0.28);
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

/*
  .f-type / .f-period / .f-time 这三个类现在挂在 antd 的 <a-select> / <a-date-picker> 上，
  结构不再是原生控件，所以外观统一交给 styles/global.css 里那组 .f-time.ant-picker 规则。
  这里只保留宽度分配 —— antd 的内层节点不带 scoped 的 data-v 属性，写在 scoped 里也够不着。
*/
.f-time {
  flex: 1 1 150px;
  min-width: 0;
}

.f-type,
.f-period {
  flex: none;
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

.f-label:focus {
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

/* ── 时间段：轴线上的加粗带子（单层，不再是双层涂抹） ── */
.tl-marker {
  pointer-events: none;
}

.mk-band {
  fill: #9db4a7;
  opacity: 0.5;
}

/* ── 卡片 → 轴线的引出线 ── */
.tl-link path {
  fill: none;
  stroke: #cdc7bb;
  stroke-width: 1;
  stroke-linejoin: round;
}

/* ── 事件卡片 ── */
.tl-card {
  cursor: pointer;
}

.card-body {
  fill: rgba(255, 255, 255, 0.9);
  stroke: #e2ddd2;
  stroke-width: 1;
  transition: fill 140ms var(--ease), stroke 140ms var(--ease);
}

.tl-card:hover .card-body {
  fill: #fff;
  stroke: #a8bdb0;
}

/* 推定 / 模糊的时间点，卡片描边相应地虚一点、淡一点 */
.card-body.estimated {
  stroke-dasharray: 4 3;
}

.card-body.fuzzy {
  stroke-dasharray: 2 3;
  fill: rgba(255, 255, 255, 0.72);
}

.card-text {
  font-family: var(--font-sans);
  font-size: 10.5px;
  fill: #3f5b4c;
  pointer-events: none;
  user-select: none;
}

/*
 * 轴线上的刻度与说明文字垫一层白色光晕：
 * 分道只能解决「贴太近」，有光晕至少保证两边都能读出来。
 */
.tl-tick,
.tl-note {
  paint-order: stroke;
  stroke: rgba(255, 255, 255, 0.92);
  stroke-width: 3px;
  stroke-linejoin: round;
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
  gap: 10px;
  flex: none;
  height: 22px;
  padding: 0 6px;
  font-size: 10px;
  color: var(--ink-5);
  white-space: nowrap;
  overflow: hidden;
}

/* 每个图例项不可压缩，避免窄面板里逐字换行 */
.legend-item {
  display: inline-flex;
  align-items: center;
  flex: none;
  white-space: nowrap;
}

.legend-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 4px;
  flex: none;
}

/* 窄面板（纵向）放不下长提示，交给色块即可 */
.tl-panel.vertical .legend-hint {
  display: none;
}

.legend-sep {
  width: 1px;
  height: 10px;
  background: var(--line-3);
  margin: 0 2px 0 0;
  flex: none;
}

/* 图例里的「时间段」色块：与轴上的带子同一厚度、同一颜色 */
.legend-mark {
  width: 16px;
  height: 8px;
  border-radius: 4px;
  margin-right: 4px;
  flex: none;
  background: rgba(157, 180, 167, 0.5);
}
</style>
