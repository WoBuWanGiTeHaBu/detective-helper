<template>
  <g v-if="source && target" class="rel" :class="`rel-${relationship.type}`">
    <!-- 线身：不再用 SVG marker（部分环境渲染不出来），箭头由下面的 polygon 显式画出 -->
    <line
      :x1="edgeA.x"
      :y1="edgeA.y"
      :x2="edgeB.x"
      :y2="edgeB.y"
      :stroke="stroke"
      stroke-width="1"
      :stroke-dasharray="dash"
    />

    <!-- 终点箭头：三角顶点贴在目标轮廓上 -->
    <polygon :points="arrowB.points" :fill="stroke" />

    <!-- 起点箭头：仅双向关系 -->
    <polygon v-if="isBidirectional" :points="arrowA.points" :fill="stroke" />

    <!-- 关系标签：无方框，沿法线偏移贴在线旁；双击可编辑 -->
    <text
      class="rel-label"
      :x="labelPos.x"
      :y="labelPos.y"
      text-anchor="middle"
      :transform="`rotate(${labelAngle} ${labelPos.x} ${labelPos.y})`"
      @dblclick.stop="$emit('edit', relationship)"
    >
      {{ relationship.label }}
    </text>
  </g>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CanvasObject, Relationship } from '@/api/types'
import { normalFlip } from '@/utils/edgeBundle'

const props = withDefaults(
  defineProps<{
    relationship: Relationship
    source?: CanvasObject
    target?: CanvasObject
    /**
     * 同束偏移（px）：同一对对象之间有多条关系时，
     * 由父级用 bundleOffsets() 算好，让它们平行排布而不是叠成一条。
     */
    offset?: number
  }>(),
  { offset: 0 }
)

defineEmits<{
  /** 双击标签 → 改名 / 改线型 */
  edit: [relationship: Relationship]
}>()

/** 线段与对象外接矩形的交点（从中心朝另一端方向推出轮廓） */
function edgePoint(o: CanvasObject, toward: { x: number; y: number }) {
  const cx = o.x + o.width / 2
  const cy = o.y + o.height / 2
  const dx = toward.x - cx
  const dy = toward.y - cy
  const sx = o.width / 2
  const sy = o.height / 2
  const scale = Math.min(sx / Math.abs(dx || 1e-9), sy / Math.abs(dy || 1e-9))
  return { x: cx + dx * scale, y: cy + dy * scale }
}

const centerA = computed(() => ({
  x: props.source!.x + props.source!.width / 2,
  y: props.source!.y + props.source!.height / 2
}))

const centerB = computed(() => ({
  x: props.target!.x + props.target!.width / 2,
  y: props.target!.y + props.target!.height / 2
}))

/**
 * 两端中心沿法线各推 offset —— 这样一条束里的每条线都整体平移，
 * 既保持平行，端点依旧落在各自轮廓附近，箭头不会飘出去。
 *
 * 法线用一个「无向基准方向」（按 id 字典序）算，
 * 否则 A→B 与 B→A 的法线相反，两条线的偏移会互相抵消、又叠回一条。
 */
const shifted = computed(() => {
  const a = centerA.value
  const b = centerB.value
  const dx = b.x - a.x
  const dy = b.y - a.y
  const len = Math.hypot(dx, dy) || 1
  const flip = normalFlip(props.relationship.source, props.relationship.target)
  const nx = (-dy / len) * flip
  const ny = (dx / len) * flip
  const o = props.offset
  return {
    a: { x: a.x + nx * o, y: a.y + ny * o },
    b: { x: b.x + nx * o, y: b.y + ny * o }
  }
})

/** 线身两端：落在两个对象的轮廓上，箭头正好顶在形状边缘 */
const edgeA = computed(() => edgePoint(props.source!, shifted.value.b))
const edgeB = computed(() => edgePoint(props.target!, shifted.value.a))

/** 单位方向向量与法线 */
const dir = computed(() => {
  const dx = edgeB.value.x - edgeA.value.x
  const dy = edgeB.value.y - edgeA.value.y
  const len = Math.hypot(dx, dy) || 1
  return { x: dx / len, y: dy / len }
})

const normal = computed(() => ({ x: -dir.value.y, y: dir.value.x }))

/** 箭头三角形：tip 在轮廓上，向线身方向收回 arrowLen */
function makeArrow(tip: { x: number; y: number }, toward: { x: number; y: number }) {
  const dx = toward.x - tip.x
  const dy = toward.y - tip.y
  const len = Math.hypot(dx, dy) || 1
  const ux = dx / len
  const uy = dy / len
  const nx = -uy
  const ny = ux
  const back = 9
  const half = 3.6
  const bx = tip.x + ux * back
  const by = tip.y + uy * back
  const p1 = `${tip.x},${tip.y}`
  const p2 = `${bx + nx * half},${by + ny * half}`
  const p3 = `${bx - nx * half},${by - ny * half}`
  return { points: `${p1} ${p2} ${p3}` }
}

/** 终点箭头：tip 贴目标轮廓，尖朝目标 */
const arrowB = computed(() => makeArrow(edgeB.value, edgeA.value))

/** 起点箭头：tip 贴起点轮廓，尖朝起点（双向关系才画） */
const arrowA = computed(() => makeArrow(edgeA.value, edgeB.value))

/* ---------------- 标签：贴在线旁，不与线重合 ---------------- */
const mid = computed(() => ({
  x: (edgeA.value.x + edgeB.value.x) / 2,
  y: (edgeA.value.y + edgeB.value.y) / 2
}))

/** 法线偏移 11px，让文字悬在线的上方一侧，不压线身 */
const labelPos = computed(() => ({
  x: mid.value.x + normal.value.x * 11,
  y: mid.value.y + normal.value.y * 11 + 3.5 // +3.5 校正文字基线
}))

/** 标签随线身角度微微倾斜（限制在 ±34°，太斜会难读） */
const labelAngle = computed(() => {
  const deg = (Math.atan2(dir.value.y, dir.value.x) * 180) / Math.PI
  let a = deg
  if (a > 90) a -= 180
  if (a < -90) a += 180
  return Math.max(-34, Math.min(34, a))
})

/* ---------------- 三种线型：单向 / 双向 / 虚线（虚线同样带箭头） ---------------- */
const isDashed = computed(() => props.relationship.type === 'dashed')
const isBidirectional = computed(() => props.relationship.type === 'bidirectional')

const stroke = computed(() => (isDashed.value ? '#A9A296' : '#8C877E'))
const dash = computed(() => (isDashed.value ? '5 4' : undefined))
</script>

<style scoped>
.rel-label {
  font-family: var(--font-sans);
  font-size: 10.5px;
  fill: #5f5a52;
  /* 白色描边光晕垫底：压在点阵 / 方格背景上也可读，且没有方框感 */
  paint-order: stroke;
  stroke: rgba(255, 255, 255, 0.88);
  stroke-width: 3px;
  stroke-linejoin: round;
  pointer-events: auto;
  cursor: pointer;
  user-select: none;
}

.rel-label:hover {
  fill: #3f5b4c;
}

.rel line {
  transition: stroke-width 140ms ease, stroke 140ms ease;
}

.rel:hover line {
  stroke-width: 1.8;
}
</style>
