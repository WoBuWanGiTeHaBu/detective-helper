<template>
  <div class="graph-view">
    <!-- 顶栏 -->
    <header class="topbar">
      <button class="back" type="button" @click="goBack">
        <svg viewBox="0 0 14 14" fill="none">
          <path d="M8.6 3 4.6 7l4 4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <span>返回推演</span>
      </button>

      <div class="bar-mid">
        <h1 class="bar-title">{{ detail?.name || '关系图' }}</h1>
        <span class="bar-meta">{{ nodes.length }} 个节点 · {{ edges.length }} 条关系</span>
      </div>

      <div class="bar-actions">
        <button class="ghost-btn" type="button" :disabled="saving" @click="extract">从画布提取</button>
        <button class="primary-btn" type="button" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </div>
    </header>

    <!-- 工具区 -->
    <div class="tools">
      <div class="tool-group">
        <span class="tool-label">类型</span>
        <div class="pill-group">
          <button
            v-for="f in FILTERS"
            :key="f.value"
            type="button"
            class="pill"
            :class="{ active: filter === f.value }"
            @click="setFilter(f.value)"
          >
            {{ f.label }}
          </button>
        </div>
      </div>

      <span class="tools-hint">
        {{ filter === 'person' ? '只看人物之间的关系' : filter === 'thing' ? '只看事务之间的关系' : '显示全部关系' }}
      </span>

      <div class="dir-switch">
        <button
          type="button"
          class="dir-seg"
          :class="{ active: view === 'list' }"
          @click="view = 'list'"
        >
          列表
        </button>
        <button
          type="button"
          class="dir-seg"
          :class="{ active: view === 'graph' }"
          @click="view = 'graph'"
        >
          图形
        </button>
      </div>
    </div>

    <!-- 主体 -->
    <main class="body">
      <div v-if="loading" class="state">加载中…</div>

      <!-- 列表视图：A ——关系—— B -->
      <div v-else-if="view === 'list'" class="list">
        <div v-if="!rows.length" class="state">这个筛选下没有关系数据</div>
        <div v-for="row in rows" :key="row.id" class="rel-row">
          <span class="rel-name" :title="row.from.name">{{ row.from.name }}</span>
          <span class="rel-word">{{ row.label }}</span>
          <span class="rel-name right" :title="row.to.name">{{ row.to.name }}</span>
        </div>
      </div>

      <!-- 图形视图：中心切换式排布 -->
      <div v-else class="graph-wrap">
        <div class="graph-bar">
          <span class="graph-hint">点圈上任一对象，把它切换成新的中心</span>
          <button v-if="focusStack.length" class="link-btn" type="button" @click="backFocus">
            返回上一个中心
          </button>
          <span class="graph-cur">当前中心：{{ centerName }}</span>
        </div>

        <svg :width="canvasW" :height="canvasH" class="graph-svg">
          <defs>
            <marker id="rg-arrow" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto">
              <polygon points="0 0, 9 4.5, 0 9" fill="#8C877E" />
            </marker>
          </defs>

          <!-- 连线 -->
          <g v-for="e in edgeGeoms" :key="e.id">
            <line
              :x1="e.x1"
              :y1="e.y1"
              :x2="e.x2"
              :y2="e.y2"
              stroke="#8C877E"
              stroke-width="1"
              :stroke-dasharray="e.dash ? '5 4' : undefined"
              marker-end="url(#rg-arrow)"
            />
            <text :x="e.lx" :y="e.ly" text-anchor="middle" class="edge-label">
              {{ e.label }}
            </text>
          </g>

          <!-- 人物：圆形 + 名字 / 事件：方框 / 事物：菱形 -->
          <g
            v-for="n in drawnNodes"
            :key="n.id"
            class="node"
            :class="{ center: n.focus }"
            @click="setFocus(n.id)"
          >
            <template v-if="n.kind === 'person'">
              <circle :cx="n.x" :cy="n.y" :r="n.r" class="node-shape k-person" />
              <text :x="n.x" :y="n.y + 4" text-anchor="middle" class="node-name">{{ n.name }}</text>
            </template>
            <template v-else-if="n.kind === 'event'">
              <rect
                :x="n.x - 58"
                :y="n.y - 19"
                width="116"
                height="38"
                rx="9"
                class="node-shape k-event"
              />
              <text :x="n.x" :y="n.y - 1" text-anchor="middle" class="node-name dark">{{ n.name }}</text>
              <text :x="n.x" :y="n.y + 14" text-anchor="middle" class="node-sub event-sub">
                {{ n.type }}
              </text>
            </template>
            <template v-else>
              <path :d="diamond(n.x, n.y, 74, 34)" class="node-shape k-thing" />
              <text :x="n.x" :y="n.y - 1" text-anchor="middle" class="node-name dark">{{ n.name }}</text>
              <text :x="n.x" :y="n.y + 14" text-anchor="middle" class="node-sub thing-sub">
                {{ n.type }}
              </text>
            </template>
          </g>

          <text v-if="!drawnNodes.length" :x="canvasW / 2" :y="canvasH / 2" text-anchor="middle" class="empty">
            {{ nodes.length ? '这个筛选下没有可显示的关系' : '还没有关系数据' }}
          </text>
        </svg>

        <div class="legend">
          <span class="lg-key k-person" />人物
          <span class="lg-key k-thing" />事物
          <span class="lg-key k-event" />事件
          <span class="lg-sep" />
          实心描边 = 当前中心
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { relationGraphApi } from '@/api/relationGraph'
import type { GraphEdge, GraphNode, RelationGraphDetailResponse } from '@/api/types'
import { bundleOffsets, normalFlip } from '@/utils/edgeBundle'
import { neighborsOf, nodeShapeOf, personRadius, radialLayout, ringRadius, type NodeKind } from '@/utils/graphLayout'

const route = useRoute()
const router = useRouter()

const bookId = computed(() => Number(route.params.bookId))
const graphId = computed(() => Number(route.params.graphId))

const loading = ref(false)
const saving = ref(false)
const detail = ref<RelationGraphDetailResponse | null>(null)

const nodes = ref<GraphNode[]>([])
const edges = ref<GraphEdge[]>([])

type FilterValue = 'all' | 'person' | 'thing'
const filter = ref<FilterValue>('all')
const view = ref<'list' | 'graph'>('graph')

const FILTERS = [
  { value: 'all' as const, label: '全部' },
  { value: 'person' as const, label: '人物↔人物' },
  { value: 'thing' as const, label: '事物↔事物' }
]

const canvasW = 980
const canvasH = 620

onMounted(load)

async function load() {
  loading.value = true
  try {
    const d = await relationGraphApi.getRelationGraph(graphId.value)
    detail.value = d
    nodes.value = d.data?.nodes ?? []
    edges.value = d.data?.edges ?? []
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

const kindOf = (id: string): NodeKind | undefined => {
  const n = nodes.value.find((x) => x.id === id)
  return n ? nodeShapeOf(n.type) : undefined
}

/**
 * 类型筛选：两端**同时**落在同一类里，才算这一类的关系。
 *   人物↔人物 → 两端都是人物
 *   事物↔事物 → 两端都不是人物（事物 / 事件）
 * 顺带丢掉自环（自己连自己）——画出来只是一段零长线加个箭头，纯属噪音。
 */
const visibleEdges = computed(() => {
  const base = edges.value.filter((e) => e.source !== e.target)
  if (filter.value === 'all') return base
  const want = (k?: NodeKind) =>
    filter.value === 'person' ? k === 'person' : !!k && k !== 'person'
  return base.filter((e) => want(kindOf(e.source)) && want(kindOf(e.target)))
})

const rows = computed(() =>
  visibleEdges.value
    .map((e) => {
      const from = nodes.value.find((n) => n.id === e.source)
      const to = nodes.value.find((n) => n.id === e.target)
      if (!from || !to) return null
      return { id: e.id, from, to, label: e.label }
    })
    .filter((x): x is { id: string; from: GraphNode; to: GraphNode; label: string } => x !== null)
)

/* ---------------- 中心切换式排布 ---------------- */

const focusId = ref('')
const focusStack = ref<string[]>([])

/** 筛选后仍出现在关系里的节点；没有关系时退化成全部节点 */
const allowedIds = computed(() => {
  const set = new Set<string>()
  visibleEdges.value.forEach((e) => {
    set.add(e.source)
    set.add(e.target)
  })
  if (!set.size) nodes.value.forEach((n) => set.add(n.id))
  return set
})

watch(
  allowedIds,
  (set) => {
    if (!set.size) {
      focusId.value = ''
      focusStack.value = []
      return
    }
    if (!focusId.value || !set.has(focusId.value)) {
      focusId.value = [...set][0]
      focusStack.value = []
    }
  },
  { immediate: true }
)

const centerName = computed(
  () => nodes.value.find((n) => n.id === focusId.value)?.name ?? '—'
)

interface DrawnNode {
  id: string
  name: string
  type: string
  kind: NodeKind
  x: number
  y: number
  /** 人物圆形半径（其余形态不用） */
  r: number
  focus: boolean
}

const drawnNodes = computed<DrawnNode[]>(() => {
  const center = focusId.value
  if (!center) return []
  const nbs = neighborsOf(center, visibleEdges.value, (id) => allowedIds.value.has(id))
  const rOf = (id: string) => {
    const meta = nodes.value.find((n) => n.id === id)
    return meta && nodeShapeOf(meta.type) === 'person' ? personRadius(meta.name) : 0
  }
  // 环形半径：既要舒展（间距够大），又不能越出画布 —— 圆心到上下左右都要留足余量。
  // 圆本身已经收小，这里的呼吸量相应放宽：78 → 64，环可以铺得更开。
  const radius = Math.min(
    Math.min(canvasW, canvasH) / 2 - 64,
    Math.max(200, ringRadius(rOf(center), nbs.map(rOf), 72))
  )
  const posMap = radialLayout(center, nbs, {
    cx: canvasW / 2,
    cy: canvasH / 2,
    radius
  })
  const out: DrawnNode[] = []
  for (const id of [center, ...nbs]) {
    const meta = nodes.value.find((n) => n.id === id)
    const p = posMap[id]
    if (!meta || !p) continue
    const kind = nodeShapeOf(meta.type)
    out.push({
      id,
      name: meta.name,
      type: meta.type,
      kind,
      x: p.x,
      y: p.y,
      r: kind === 'person' ? personRadius(meta.name) : 0,
      focus: id === center
    })
  }
  return out
})

/** 同一对对象之间的多条关系平行排开，不叠成一条；只画「中心 ↔ 邻居」的辐条
 *  —— 邻居之间的连线横穿圆心，看着杂乱还压住中心，等它成为中心时自然会展开 */
const edgeGeoms = computed(() => {
  const ids = new Set(drawnNodes.value.map((n) => n.id))
  const center = focusId.value
  const rel = visibleEdges.value.filter(
    (e) =>
      ids.has(e.source) &&
      ids.has(e.target) &&
      (e.source === center || e.target === center)
  )
  const offsets = bundleOffsets(rel, 20)
  const at = (id: string) => drawnNodes.value.find((n) => n.id === id)
  return rel
    .map((e) => {
      const a = at(e.source)
      const b = at(e.target)
      if (!a || !b) return null
      const dx = b.x - a.x
      const dy = b.y - a.y
      const len = Math.hypot(dx, dy) || 1
      const o = (offsets[e.id] ?? 0) * normalFlip(e.source, e.target)
      const nx = (-dy / len) * o
      const ny = (dx / len) * o
      return {
        id: e.id,
        x1: a.x + nx,
        y1: a.y + ny,
        x2: b.x + nx,
        y2: b.y + ny,
        lx: (a.x + b.x) / 2 + nx,
        ly: (a.y + b.y) / 2 + ny - 6,
        label: e.label,
        dash: e.type === 'dashed'
      }
    })
    .filter((x): x is NonNullable<typeof x> => x !== null)
})

function setFilter(v: FilterValue) {
  filter.value = v
  focusId.value = ''
  focusStack.value = []
}

function setFocus(id: string) {
  if (id === focusId.value) return
  focusStack.value = [...focusStack.value, focusId.value].filter(Boolean)
  focusId.value = id
}

function backFocus() {
  const stack = [...focusStack.value]
  const prev = stack.pop()
  focusStack.value = stack
  if (prev) focusId.value = prev
}

function diamond(cx: number, cy: number, halfW: number, halfH: number): string {
  return `M${cx} ${cy - halfH} L${cx + halfW} ${cy} L${cx} ${cy + halfH} L${cx - halfW} ${cy} Z`
}

async function save() {
  saving.value = true
  try {
    await relationGraphApi.saveRelationGraphData(graphId.value, {
      nodes: nodes.value,
      edges: edges.value
    })
    message.success('关系图已保存')
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

async function extract() {
  saving.value = true
  try {
    const data = await relationGraphApi.extractRelationGraph(bookId.value, {
      objectTypes: ['person', 'thing'],
      relationTypes: ['unidirectional', 'bidirectional', 'dashed']
    })
    nodes.value = data.nodes ?? []
    edges.value = data.edges ?? []
    message.success(`提取完成：${nodes.value.length} 个节点 · ${edges.value.length} 条关系`)
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push(`/workspace/${bookId.value}`)
}
</script>

<style scoped>
.graph-view {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background: var(--bg-app);
}

.topbar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: none;
  height: 64px;
  padding: 0 32px;
  background: var(--bg-bar);
  box-shadow: var(--sh-bar);
}

.back {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: none;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ink-3);
  font-family: inherit;
  font-size: 11px;
  cursor: pointer;
}

.back svg {
  width: 14px;
  height: 14px;
}

.back:hover {
  color: var(--ink-1);
}

.bar-mid {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.bar-title {
  margin: 0;
  font-family: var(--font-serif);
  font-weight: 700;
  font-size: 18px;
  color: var(--ink-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bar-meta {
  flex: none;
  font-size: 10.5px;
  color: var(--ink-4);
}

.bar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
}

.tools {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: none;
  padding: 14px 32px;
  border-bottom: 1px solid var(--line-4);
  background: var(--bg-bar);
}

.tool-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-label {
  font-size: 10.5px;
  color: var(--ink-4);
}

.tools-hint {
  font-size: 10.5px;
  color: var(--ink-5);
}

.pill-group {
  display: flex;
  gap: 6px;
}

.pill {
  height: 28px;
  padding: 0 12px;
  border: 0;
  border-radius: 14px;
  background: rgba(0, 0, 0, 0.03);
  color: var(--ink-3);
  font-family: inherit;
  font-size: 11px;
  cursor: pointer;
  transition: all 140ms var(--ease);
}

.pill.active {
  background: var(--green-tint);
  color: var(--green-ink);
  font-weight: 500;
}

.dir-switch {
  display: flex;
  gap: 0;
  width: 52px;
  height: 24px;
  padding: 2px;
  margin-left: auto;
  border-radius: 6px;
  background: #f0eeea;
}

.dir-seg {
  flex: 1;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--ink-3);
  font-family: inherit;
  font-size: 10px;
  cursor: pointer;
}

.dir-seg.active {
  background: #fff;
  color: var(--ink-1);
  font-weight: 500;
}

.body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 24px 32px 32px;
}

.state {
  padding: 60px 0;
  text-align: center;
  font-size: 12.5px;
  color: var(--ink-4);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 680px;
}

.rel-row {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 44px;
  padding: 0 14px;
  background: #fff;
  border: 1px solid var(--line-1);
  border-radius: var(--r-lg);
}

.rel-name {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  color: var(--ink-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rel-name.right {
  text-align: right;
}

.rel-word {
  flex: none;
  padding: 3px 10px;
  border-radius: var(--r-sm);
  background: var(--event-fill);
  color: #6b665e;
  font-size: 10.5px;
}

.graph-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.graph-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  align-self: stretch;
  font-size: 11px;
  color: var(--ink-4);
}

.graph-hint {
  color: var(--ink-4);
}

.graph-cur {
  margin-left: auto;
  font-size: 11px;
  color: var(--green-ink);
}

.link-btn {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--green-ink);
  font-family: inherit;
  font-size: 11px;
  text-decoration: underline;
  cursor: pointer;
}

.graph-svg {
  display: block;
  background: var(--bg-canvas);
  border: 1px solid var(--line-1);
  border-radius: var(--r-2xl);
}

.node {
  cursor: pointer;
}

.node-shape {
  stroke-width: 1;
  filter: drop-shadow(0 4px 10px rgba(89, 107, 94, 0.1));
  transition: filter 140ms var(--ease), stroke 140ms var(--ease), fill 140ms var(--ease);
}

.node:hover .node-shape {
  filter: drop-shadow(0 7px 16px rgba(89, 107, 94, 0.2));
}

.node.center .node-shape {
  stroke-width: 2;
  filter: drop-shadow(0 7px 18px rgba(93, 122, 106, 0.26));
}

.node-shape.k-person {
  fill: #e8efea;
  stroke: #a6bfb0;
}

.node-shape.k-thing {
  fill: #edeaf0;
  stroke: #b8b0c6;
}

.node-shape.k-event {
  fill: #efede7;
  stroke: #d1ccc2;
}

.node.center .node-shape.k-person {
  fill: #dde5df;
  stroke: #5d7a6a;
}

.node.center .node-shape.k-thing {
  fill: #e4e0ea;
  stroke: #8f86a0;
}

.node.center .node-shape.k-event {
  fill: #e9e5dc;
  stroke: #a0917c;
}

.node-name {
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 500;
  fill: #2e3d34;
  pointer-events: none;
  user-select: none;
}

.node-name.dark {
  fill: #3b3740;
}

.node-sub {
  font-size: 10px;
  pointer-events: none;
  user-select: none;
}

.thing-sub {
  fill: #8f86a0;
}

.event-sub {
  fill: #a0917c;
}

.edge-label {
  font-family: var(--font-sans);
  font-size: 10.5px;
  fill: #6b665e;
  pointer-events: none;
  paint-order: stroke;
  stroke: rgba(247, 245, 241, 0.92);
  stroke-width: 3px;
  stroke-linejoin: round;
}

.legend {
  display: flex;
  align-items: center;
  gap: 5px;
  align-self: stretch;
  font-size: 10px;
  color: var(--ink-5);
}

.lg-key {
  width: 15px;
  height: 10px;
  margin-left: 8px;
  border-radius: 6px;
  border: 1px solid #a6bfb0;
  background: #e8efea;
}

.lg-key:first-child {
  margin-left: 0;
}

/* 人物 = 圆形 */
.lg-key.k-person {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.lg-key.k-thing {
  width: 10px;
  height: 10px;
  border-radius: 1px;
  border-color: #b8b0c6;
  background: #edeaf0;
  transform: rotate(45deg);
}

.lg-key.k-event {
  border-radius: 2px;
  border-color: #d1ccc2;
  background: #efede7;
}

.lg-sep {
  width: 1px;
  height: 10px;
  margin: 0 5px;
  background: var(--line-3);
}

.empty {
  font-family: var(--font-sans);
  font-size: 12.5px;
  fill: var(--ink-5);
}

.ghost-btn,
.primary-btn {
  height: 34px;
  padding: 0 16px;
  border-radius: var(--r-lg);
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 160ms var(--ease);
}

.ghost-btn {
  border: 1px solid var(--line-1);
  background: #fff;
  color: var(--ink-3);
}

.ghost-btn:hover {
  color: var(--ink-2);
  border-color: var(--green-line);
}

.primary-btn {
  border: 0;
  background: var(--green);
  color: #fff;
  box-shadow: var(--sh-btn);
}

.primary-btn:hover:not(:disabled) {
  background: var(--green-deep);
}

.primary-btn:disabled,
.ghost-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}
</style>
