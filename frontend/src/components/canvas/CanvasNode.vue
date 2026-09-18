<template>
  <!-- 人物节点：胶囊 -->
  <g
    v-if="shapeKind === 'person'"
    class="node node-person"
    :class="stateClass"
    @mousedown.stop="$emit('dragstart', $event, object)"
    @dblclick.stop="$emit('edit', object)"
    @contextmenu.prevent="$emit('menu', $event, object)"
    @mouseenter="$emit('hover', object.id)"
    @mouseleave="$emit('hover', null)"
  >
    <rect
      class="node-shape"
      :x="object.x"
      :y="object.y"
      :width="object.width"
      :height="object.height"
      :rx="object.height / 2"
      :ry="object.height / 2"
      fill="#E8EFEA"
      stroke="#A6BFB0"
      stroke-width="1"
    />
    <text
      :x="object.x + object.width / 2"
      :y="object.y + object.height / 2 - 3"
      text-anchor="middle"
      :class="['node-name', { compact: object.name.length > 6 }]"
    >
      {{ object.name }}
    </text>
    <text
      :x="object.x + object.width / 2"
      :y="object.y + object.height / 2 + 14"
      text-anchor="middle"
      class="node-sub"
      fill="#7C9A88"
    >
      {{ subLine }}
    </text>

    <!-- 选中 / 起点 / 悬停描边提示 -->
    <rect
      v-if="selected"
      :x="object.x - 4"
      :y="object.y - 4"
      :width="object.width + 8"
      :height="object.height + 8"
      :rx="(object.height + 8) / 2"
      fill="none"
      stroke="#7C9A88"
      stroke-width="1"
      stroke-dasharray="4 3"
    />
    <rect
      v-else-if="locked"
      :x="object.x - 5"
      :y="object.y - 5"
      :width="object.width + 10"
      :height="object.height + 10"
      :rx="(object.height + 10) / 2"
      fill="none"
      stroke="#5C7F6B"
      stroke-width="2"
    />
  </g>

  <!-- 事件节点：方框 -->
  <g
    v-else-if="shapeKind === 'event'"
    class="node node-event"
    :class="stateClass"
    @mousedown.stop="$emit('dragstart', $event, object)"
    @dblclick.stop="$emit('edit', object)"
    @contextmenu.prevent="$emit('menu', $event, object)"
    @mouseenter="$emit('hover', object.id)"
    @mouseleave="$emit('hover', null)"
  >
    <rect
      class="node-shape"
      :x="object.x"
      :y="object.y"
      :width="object.width"
      :height="object.height"
      rx="12"
      ry="12"
      fill="#EFEDE7"
      stroke="#D1CCC2"
      stroke-width="1"
    />
    <text
      :x="object.x + object.width / 2"
      :y="object.y + object.height / 2 - 3"
      text-anchor="middle"
      :class="['node-name', { compact: object.name.length > 6 }]"
      fill="#3E3A33"
    >
      {{ object.name }}
    </text>
    <text
      :x="object.x + object.width / 2"
      :y="object.y + object.height / 2 + 14"
      text-anchor="middle"
      class="node-sub"
      fill="#A0917C"
    >
      {{ subLine }}
    </text>

    <rect
      v-if="selected"
      :x="object.x - 4"
      :y="object.y - 4"
      :width="object.width + 8"
      :height="object.height + 8"
      rx="15"
      ry="15"
      fill="none"
      stroke="#7C9A88"
      stroke-width="1"
      stroke-dasharray="4 3"
    />
    <rect
      v-else-if="locked"
      :x="object.x - 5"
      :y="object.y - 5"
      :width="object.width + 10"
      :height="object.height + 10"
      rx="16"
      ry="16"
      fill="none"
      stroke="#5C7F6B"
      stroke-width="2"
    />
  </g>

  <!-- 事物节点：菱形 -->
  <g
    v-else
    class="node node-thing"
    :class="stateClass"
    @mousedown.stop="$emit('dragstart', $event, object)"
    @dblclick.stop="$emit('edit', object)"
    @contextmenu.prevent="$emit('menu', $event, object)"
    @mouseenter="$emit('hover', object.id)"
    @mouseleave="$emit('hover', null)"
  >
    <!-- 菱形底：外框 160×100，可用空间约 60% -->
    <path
      class="node-shape"
      :d="diamondPath"
      fill="#EDEAF0"
      stroke="#B8B0C6"
      stroke-width="1"
    />
    <text
      :x="object.x + object.width / 2"
      :y="object.y + object.height / 2 - 2"
      text-anchor="middle"
      :class="['node-name', { compact: object.name.length > 5 }]"
      fill="#3B3740"
    >
      {{ object.name }}
    </text>
    <text
      :x="object.x + object.width / 2"
      :y="object.y + object.height / 2 + 14"
      text-anchor="middle"
      class="node-sub thing-sub"
      fill="#8F86A0"
    >
      {{ subLine }}
    </text>

    <path
      v-if="selected"
      :d="outerDiamondPath(4)"
      fill="none"
      stroke="#7C9A88"
      stroke-width="1"
      stroke-dasharray="4 3"
    />
    <path
      v-else-if="locked"
      :d="outerDiamondPath(5)"
      fill="none"
      stroke="#5C7F6B"
      stroke-width="2"
    />
  </g>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CanvasObject } from '@/api/types'

const props = defineProps<{
  object: CanvasObject
  selected?: boolean
  dragging?: boolean
  /** 关系连线模式下鼠标悬停：轻微放大 */
  hovered?: boolean
  /** 关系连线模式下已选为起点：放大 + 实心描边 */
  locked?: boolean
}>()

defineEmits<{
  dragstart: [event: MouseEvent, object: CanvasObject]
  /** 双击节点 → 打开资料面板（回显） */
  edit: [object: CanvasObject]
  /** 悬停变化，参数为节点 id 或 null */
  hover: [id: string | null]
  /** 右键 → 打开对象菜单（编辑资料 / 删除） */
  menu: [event: MouseEvent, object: CanvasObject]
}>()

/** 由 type 决定视觉形态（type 为自由字符串，做 3 类归一） */
const shapeKind = computed<'person' | 'event' | 'thing'>(() => {
  const t = (props.object.type || '').toLowerCase()
  if (t === 'person' || t === '人物') return 'person'
  if (t === 'event' || t === '事件' || t === 'time') return 'event'
  return 'thing'
})

const stateClass = computed(() => ({
  selected: props.selected,
  dragging: props.dragging,
  scaled: props.hovered || props.locked
}))

function cf(key: string): string {
  const v = (props.object.customFields ?? {})[key]
  if (typeof v === 'string') return v.trim()
  if (typeof v === 'number') return String(v)
  return ''
}

/**
 * 副标题：优先用资料面板里填的结构化字段，其次退回 description。
 * 人物 → `职业 · 35 岁`；事件 → `时间`；事物 → `类别`
 */
const subLine = computed(() => {
  const parts: string[] = []
  if (shapeKind.value === 'person') {
    const occ = cf('occupation')
    const age = cf('age')
    if (occ) parts.push(occ)
    if (age) parts.push(/^\d+(\.\d+)?$/.test(age) ? `${age} 岁` : age)
  } else if (shapeKind.value === 'event') {
    const t = cf('time')
    if (t) parts.push(t)
  } else {
    const c = cf('category')
    if (c) parts.push(c)
  }
  const d = cf('description') || props.object.description || ''
  if (d) parts.push(d)
  return parts.length ? parts.join(' · ') : props.object.type || 'object'
})

const diamondPath = computed(() => {
  const { x, y, width: w, height: h } = props.object
  return `M${x + w / 2} ${y + 3} L${x + w - 3} ${y + h / 2} L${x + w / 2} ${y + h - 3} L${x + 3} ${y + h / 2} Z`
})

/** 菱形外扩描边（选中 / 起点） */
function outerDiamondPath(pad: number): string {
  const { x, y, width: w, height: h } = props.object
  return `M${x + w / 2} ${y + 3 - pad} L${x + w - 3 + pad} ${y + h / 2} L${x + w / 2} ${y + h - 3 + pad} L${x + 3 - pad} ${y + h / 2} Z`
}
</script>

<style scoped>
.node {
  cursor: grab;
  /* 宿主 node-layer SVG 是 pointer-events: none（放行画布平移），
     节点本体在这里恢复命中 */
  pointer-events: auto;
}

.node:active {
  cursor: grabbing;
}

/*
 * 悬停 / 起点放大：transform-box: fill-box 让 SVG 元素绕自身中心缩放，
 * 不依赖节点的 x/y/width/height，连线和命中区域都不受影响。
 */
.node.scaled {
  transform-box: fill-box;
  transform-origin: center;
  transform: scale(1.06);
  transition: transform 140ms var(--ease);
}

/*
 * 阴影只挂在「底形」上（人物胶囊 / 事件方框 / 事物菱形），
 * 不挂在整个 <g>，否则文字和选中虚线框也会被投出阴影。
 * 三类节点用同一组阴影参数，视觉厚度保持一致。
 */
.node-shape {
  filter: drop-shadow(0 6px 14px rgba(89, 107, 94, 0.13));
  transition: filter 160ms var(--ease);
}

.node.dragging .node-shape,
.node:active .node-shape {
  filter: drop-shadow(0 8px 18px rgba(89, 107, 94, 0.2));
}

.node:hover .node-name {
  fill: #2E3D34;
}

.node-name {
  font-family: var(--font-sans);
  font-size: 15px;
  font-weight: 500;
  fill: #2E3D34;
  pointer-events: none;
  user-select: none;
}

.node-name.compact {
  font-size: 12px;
}

.node-sub {
  font-family: var(--font-sans);
  font-size: 10.5px;
  pointer-events: none;
  user-select: none;
}

.thing-sub {
  font-size: 10px;
}
</style>
