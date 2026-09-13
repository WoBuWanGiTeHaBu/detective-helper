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
        <span class="bar-meta">
          {{ nodes.length }} 个节点 · {{ edges.length }} 条关系
        </span>
      </div>

      <div class="bar-actions">
        <button class="ghost-btn" type="button" @click="extract">
          从画布提取
        </button>
        <button class="primary-btn" type="button" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </div>
    </header>

    <!-- 工具区 -->
    <div class="tools">
      <div class="tool-group">
        <span class="tool-label">来源</span>
        <select v-model="source" class="mini-select">
          <option value="auto">自动提取</option>
          <option value="custom">自定义</option>
        </select>
      </div>

      <div class="tool-group">
        <span class="tool-label">类型</span>
        <div class="pill-group">
          <button
            v-for="f in FILTERS"
            :key="f.value"
            type="button"
            class="pill"
            :class="{ active: filter === f.value }"
            @click="filter = f.value"
          >
            {{ f.label }}
          </button>
        </div>
      </div>

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
        <div v-if="!rows.length" class="state">还没有关系数据</div>
        <div v-for="row in rows" :key="row.id" class="rel-row">
          <span class="rel-name" :title="row.from.name">{{ row.from.name }}</span>
          <span class="rel-word">{{ row.label }}</span>
          <span class="rel-name right" :title="row.to.name">{{ row.to.name }}</span>
        </div>
      </div>

      <!-- 图形视图 -->
      <div v-else class="graph-wrap">
        <svg :width="canvasW" :height="canvasH" class="graph-svg">
          <defs>
            <marker id="rg-arrow" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto">
              <polygon points="0 0, 9 4.5, 0 9" fill="#8C877E" />
            </marker>
          </defs>

          <!-- 连线 -->
          <g v-for="e in visibleEdges" :key="e.id">
            <line
              :x1="posOf(e.source).x"
              :y1="posOf(e.source).y"
              :x2="posOf(e.target).x"
              :y2="posOf(e.target).y"
              stroke="#8C877E"
              stroke-width="1"
              marker-end="url(#rg-arrow)"
            />
            <text
              :x="(posOf(e.source).x + posOf(e.target).x) / 2"
              :y="(posOf(e.source).y + posOf(e.target).y) / 2 - 6"
              text-anchor="middle"
              class="edge-label"
            >
              {{ e.label }}
            </text>
          </g>

          <!-- 人物：胶囊 / 事物：菱形 -->
          <g v-for="n in visibleNodes" :key="n.id">
            <template v-if="isPerson(n)">
              <rect
                :x="posOf(n.id).x - 58"
                :y="posOf(n.id).y - 18"
                width="116"
                height="36"
                rx="18"
                fill="#E8EFEA"
                stroke="#A6BFB0"
                stroke-width="1"
              />
              <text :x="posOf(n.id).x" :y="posOf(n.id).y - 2" text-anchor="middle" class="node-name">
                {{ n.name }}
              </text>
              <text :x="posOf(n.id).x" :y="posOf(n.id).y + 12" text-anchor="middle" class="node-sub person-sub">
                {{ n.type }}
              </text>
            </template>
            <template v-else>
              <path :d="diamond(n)" fill="#EDEAF0" stroke="#B8B0C6" stroke-width="1" />
              <text :x="posOf(n.id).x" :y="posOf(n.id).y - 1" text-anchor="middle" class="node-name dark">
                {{ n.name }}
              </text>
              <text :x="posOf(n.id).x" :y="posOf(n.id).y + 13" text-anchor="middle" class="node-sub thing-sub">
                {{ n.type }}
              </text>
            </template>
          </g>

          <text v-if="!visibleNodes.length" :x="canvasW / 2" :y="canvasH / 2" text-anchor="middle" class="empty">
            还没有关系数据
          </text>
        </svg>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { relationGraphApi } from '@/api/relationGraph'
import type { GraphEdge, GraphNode, RelationGraphDetailResponse } from '@/api/types'

const route = useRoute()
const router = useRouter()

const bookId = computed(() => Number(route.params.bookId))
const graphId = computed(() => Number(route.params.graphId))

const loading = ref(false)
const saving = ref(false)
const detail = ref<RelationGraphDetailResponse | null>(null)

const nodes = ref<GraphNode[]>([])
const edges = ref<GraphEdge[]>([])

const source = ref<'auto' | 'custom'>('auto')
const filter = ref<'all' | 'person' | 'thing'>('all')
const view = ref<'list' | 'graph'>('graph')

const FILTERS = [
  { value: 'all' as const, label: '全部' },
  { value: 'person' as const, label: '人物↔人物' },
  { value: 'thing' as const, label: '事物↔事物' }
]

const canvasW = 980
const canvasH = 640

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

function isPerson(n: GraphNode) {
  return n.type === 'person'
}

const visibleNodes = computed(() => {
  if (filter.value === 'all') return nodes.value
  if (filter.value === 'person') return nodes.value.filter((n) => n.type === 'person')
  return nodes.value
})

const visibleEdges = computed(() => {
  const ids = new Set(visibleNodes.value.map((n) => n.id))
  return edges.value.filter((e) => {
    if (!ids.has(e.source) || !ids.has(e.target)) return false
    if (filter.value === 'person') {
      const s = nodes.value.find((n) => n.id === e.source)
      const t = nodes.value.find((n) => n.id === e.target)
      return s?.type === 'person' || t?.type === 'person'
    }
    if (filter.value === 'thing') {
      const s = nodes.value.find((n) => n.id === e.source)
      const t = nodes.value.find((n) => n.id === e.target)
      return !(s?.type === 'person' && t?.type === 'person')
    }
    return true
  })
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

/** 圆形布局 */
const layout = computed(() => {
  const map: Record<string, { x: number; y: number }> = {}
  const list = visibleNodes.value
  const n = Math.max(list.length, 1)
  const cx = canvasW / 2
  const cy = canvasH / 2
  const r = Math.min(canvasW, canvasH) / 2 - 96
  list.forEach((node, i) => {
    const angle = (i / n) * Math.PI * 2 - Math.PI / 2
    map[node.id] = { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle) }
  })
  return map
})

function posOf(id: string) {
  return layout.value[id] ?? { x: canvasW / 2, y: canvasH / 2 }
}

function diamond(n: GraphNode) {
  const c = posOf(n.id)
  return `M${c.x} ${c.y - 26} L${c.x + 56} ${c.y} L${c.x} ${c.y + 26} L${c.x - 56} ${c.y} Z`
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
  try {
    const data = await relationGraphApi.extractRelationGraph(bookId.value, {
      objectTypes: ['person', 'thing'],
      relationTypes: ['unidirectional', 'bidirectional', 'dashed']
    })
    nodes.value = data.nodes
    edges.value = data.edges
    message.success(`提取完成：${data.nodes.length} 个节点 · ${data.edges.length} 条关系`)
  } catch {
    /* 拦截器已提示 */
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

.mini-select {
  height: 30px;
  padding: 0 10px;
  border: 1px solid var(--line-1);
  border-radius: var(--r-md);
  background: #fff;
  font-family: inherit;
  font-size: 12.5px;
  color: var(--ink-3);
  outline: 0;
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
  background: #F0EEEA;
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
  color: #6B665E;
  font-size: 10.5px;
}

.graph-wrap {
  display: flex;
  justify-content: center;
}

.graph-svg {
  display: block;
  background: var(--bg-canvas);
  border: 1px solid var(--line-1);
  border-radius: var(--r-2xl);
}

.node-name {
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 500;
  fill: #2E3D34;
  pointer-events: none;
}

.node-name.dark {
  fill: #3B3740;
}

.node-sub {
  font-size: 10px;
  pointer-events: none;
}

.person-sub { fill: #7C9A88; }
.thing-sub { fill: #8F86A0; }

.edge-label {
  font-family: var(--font-sans);
  font-size: 10.5px;
  fill: #6B665E;
  pointer-events: none;
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

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}
</style>
