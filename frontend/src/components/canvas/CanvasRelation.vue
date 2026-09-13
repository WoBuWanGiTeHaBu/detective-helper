<template>
  <g v-if="source && target" class="rel" :class="`rel-${relationship.type}`">
    <!-- 前段：起点 → 标签左缘 -->
    <line
      :x1="a.x"
      :y1="a.y"
      :x2="seg1.x"
      :y2="seg1.y"
      :stroke="stroke"
      stroke-width="1"
      :stroke-dasharray="dash"
      :marker-start="hasStartArrow ? 'url(#arrow-start)' : undefined"
    />

    <!-- 后段：标签右缘 → 终点（箭头在这里） -->
    <line
      :x1="seg2.x"
      :y1="seg2.y"
      :x2="b.x"
      :y2="b.y"
      :stroke="stroke"
      stroke-width="1"
      :stroke-dasharray="dash"
      :marker-end="'url(#arrow-end)'"
    />

    <!-- 关系标签：嵌在断开的线缝里，与线身贴合 -->
    <g
      class="rel-label-g"
      :transform="`translate(${mid.x}, ${mid.y})`"
      @dblclick.stop="$emit('edit', relationship)"
    >
      <rect
        :x="-labelW / 2"
        :y="-9"
        :width="labelW"
        :height="18"
        rx="4"
        ry="4"
        :fill="labelFill"
        :stroke="labelStroke"
        stroke-width="1"
      />
      <text x="0" y="3.5" text-anchor="middle" class="rel-label">
        {{ relationship.label }}
      </text>
    </g>
  </g>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CanvasObject, Relationship } from '@/api/types'

const props = defineProps<{
  relationship: Relationship
  source?: CanvasObject
  target?: CanvasObject
}>()

defineEmits<{
  /** 双击标签 → 改名 / 改线型 */
  edit: [relationship: Relationship]
}>()

/** 取对象中心点；菱形/胶囊都按外接矩形中心近似 */
function centerOf(o?: CanvasObject) {
  if (!o) return { x: 0, y: 0 }
  return { x: o.x + o.width / 2, y: o.y + o.height / 2 }
}

const a = computed(() => centerOf(props.source))
const b = computed(() => centerOf(props.target))

const mid = computed(() => ({ x: (a.value.x + b.value.x) / 2, y: (a.value.y + b.value.y) / 2 }))

/** 单位方向向量，用于把线身断开一个标签宽度的缝 */
const unit = computed(() => {
  const dx = b.value.x - a.value.x
  const dy = b.value.y - a.value.y
  const len = Math.hypot(dx, dy) || 1
  return { x: dx / len, y: dy / len }
})

/** 标签宽度：46 基准，长文案按字数放宽 */
const labelW = computed(() => {
  const n = (props.relationship.label || '').length
  return Math.max(46, n * 11 + 16)
})

const gap = computed(() => labelW.value / 2 + 5)

const seg1 = computed(() => ({
  x: mid.value.x - unit.value.x * gap.value,
  y: mid.value.y - unit.value.y * gap.value
}))

const seg2 = computed(() => ({
  x: mid.value.x + unit.value.x * gap.value,
  y: mid.value.y + unit.value.y * gap.value
}))

/* ---------------- 三种线型：单向 / 双向 / 虚线 ---------------- */
const isDashed = computed(() => props.relationship.type === 'dashed')
const hasStartArrow = computed(() => props.relationship.type === 'bidirectional')

const stroke = computed(() => (isDashed.value ? '#A9A296' : '#8C877E'))
const dash = computed(() => (isDashed.value ? '5 4' : undefined))

const labelFill = computed(() => (isDashed.value ? '#F6F4F0' : '#FBFAF8'))
const labelStroke = computed(() => (isDashed.value ? '#DDD6C9' : '#E2DCD1'))
</script>

<style scoped>
.rel-label {
  font-family: var(--font-sans);
  font-size: 10.5px;
  fill: #5F5A52;
  pointer-events: none;
  user-select: none;
}

.rel-label-g {
  cursor: pointer;
}

.rel line {
  transition: stroke-width 140ms ease, stroke 140ms ease;
}

.rel:hover line {
  stroke-width: 2;
}

.rel-label-g:hover rect {
  fill: #fff;
  stroke: #A6BFB0;
}
</style>
