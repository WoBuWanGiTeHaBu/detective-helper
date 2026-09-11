<template>
  <div class="canvas-editor">
    <div class="toolbar">
      <a-space>
        <a-button size="small" @click="addNode('person')">
          <UserOutlined /> 添加人物
        </a-button>
        <a-button size="small" @click="addNode('event')">
          <ClockCircleOutlined /> 添加事件
        </a-button>
        <a-button size="small" @click="addNode('thing')">
          <ApiOutlined /> 添加物品
        </a-button>
        <a-divider type="vertical" />
        <a-button size="small" @click="clearCanvas">
          <ClearOutlined /> 清空画布
        </a-button>
        <a-button size="small" @click="exportCanvas">
          <ExportOutlined /> 导出图片
        </a-button>
      </a-space>
    </div>

    <div ref="canvasRef" class="canvas-area">
      <svg
        :width="canvasWidth"
        :height="canvasHeight"
        @mousedown="handleMouseDown"
        @mousemove="handleMouseMove"
        @mouseup="handleMouseUp"
        @mouseleave="handleMouseUp"
      >
        <!-- 绘制连接线 -->
        <g v-for="edge in edges" :key="edge.id">
          <line
            :x1="edge.x1"
            :y1="edge.y1"
            :x2="edge.x2"
            :y2="edge.y2"
            :stroke="edge.color"
            :stroke-width="edge.width"
            marker-end="url(#arrowhead)"
          />
          <text
            :x="(edge.x1 + edge.x2) / 2"
            :y="(edge.y1 + edge.y2) / 2 - 5"
            text-anchor="middle"
            font-size="12"
            fill="#666"
          >
            {{ edge.label }}
          </text>
        </g>

        <!-- 绘制节点 -->
        <g
          v-for="node in nodes"
          :key="node.id"
          :transform="`translate(${node.x}, ${node.y})`"
          @mousedown.stop="startDrag(node)"
        >
          <!-- 节点背景 -->
          <rect
            :width="node.width"
            :height="node.height"
            :fill="node.color"
            :stroke="node.borderColor"
            :stroke-width="2"
            :rx="8"
            :ry="8"
          />

          <!-- 节点图标 -->
          <g v-if="node.type === 'person'">
            <text x="10" y="20" font-size="16">👤</text>
          </g>
          <g v-else-if="node.type === 'event'">
            <text x="10" y="20" font-size="16">📅</text>
          </g>
          <g v-else>
            <text x="10" y="20" font-size="16">📦</text>
          </g>

          <!-- 节点文本 -->
          <text
            x="35"
            y="20"
            font-size="12"
            fill="#333"
            :text-length="node.width - 45"
            lengthAdjust="spacingAndGlyphs"
          >
            {{ node.label }}
          </text>

          <!-- 删除按钮 -->
          <g
            @click.stop="deleteNode(node)"
            style="cursor: pointer"
          >
            <circle
              cx="node.width - 10"
              cy="10"
              r="8"
              fill="#ff4d4f"
            />
            <text
              :x="node.width - 10"
              y="14"
              text-anchor="middle"
              font-size="12"
              fill="#fff"
            >
              ×
            </text>
          </g>
        </g>

        <!-- 连接目标指示器 -->
        <circle
          v-if="connecting"
          :cx="mouseX"
          :cy="mouseY"
          r="5"
          fill="#1890ff"
        />
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  UserOutlined,
  ClockCircleOutlined,
  ApiOutlined,
  ClearOutlined,
  ExportOutlined
} from '@ant-design/icons-vue'

interface CanvasNode {
  id: string
  type: string
  x: number
  y: number
  width: number
  height: number
  label: string
  color: string
  borderColor: string
}

interface CanvasEdge {
  id: string
  x1: number
  y1: number
  x2: number
  y2: number
  label: string
  color: string
  width: number
}

const props = defineProps<{
  pageId: number
  canvasData?: string
}>()

const emit = defineEmits<{
  save: [data: string]
}>()

const canvasRef = ref<HTMLDivElement>()
const canvasWidth = ref(800)
const canvasHeight = ref(600)

const nodes = ref<CanvasNode[]>([])
const edges = ref<CanvasEdge[]>([])

const dragging = ref<CanvasNode | null>(null)
const connecting = ref(false)
const connectingNode = ref<CanvasNode | null>(null)
const dragOffset = ref({ x: 0, y: 0 })
const mouseX = ref(0)
const mouseY = ref(0)

// 节点颜色配置
const nodeColors: Record<string, { bg: string; border: string }> = {
  person: { bg: '#e6f7ff', border: '#1890ff' },
  event: { bg: '#fff7e6', border: '#fa8c16' },
  thing: { bg: '#f6ffed', border: '#52c41a' }
}

onMounted(() => {
  initCanvas()
})

watch(() => props.canvasData, (newData) => {
  if (newData) {
    loadCanvasData(newData)
  }
})

function initCanvas() {
  if (props.canvasData) {
    loadCanvasData(props.canvasData)
  }
}

function loadCanvasData(data: string) {
  try {
    const parsed = JSON.parse(data)
    nodes.value = parsed.nodes || []
    edges.value = parsed.edges || []
  } catch (error) {
    console.error('加载画布数据失败:', error)
    nodes.value = []
    edges.value = []
  }
}

function addNode(type: string) {
  const id = `node-${Date.now()}`
  const colors = nodeColors[type] || nodeColors.person

  const newNode: CanvasNode = {
    id,
    type,
    x: 50 + Math.random() * (canvasWidth.value - 150),
    y: 50 + Math.random() * (canvasHeight.value - 100),
    width: 120,
    height: 40,
    label: type === 'person' ? '人物' : type === 'event' ? '事件' : '物品',
    color: colors.bg,
    borderColor: colors.border
  }

  nodes.value.push(newNode)
  saveCanvas()
}

function deleteNode(node: CanvasNode) {
  nodes.value = nodes.value.filter(n => n.id !== node.id)
  edges.value = edges.value.filter(e => e.id.includes(node.id))
  saveCanvas()
}

function startDrag(node: CanvasNode) {
  dragging.value = node
  dragOffset.value = {
    x: mouseX.value - node.x,
    y: mouseY.value - node.y
  }
}

function handleMouseDown(event: MouseEvent) {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) return

  mouseX.value = event.clientX - rect.left
  mouseY.value = event.clientY - rect.top

  // 检查是否点击了节点
  const clickedNode = nodes.value.find(node =>
    mouseX.value >= node.x && mouseX.value <= node.x + node.width &&
    mouseY.value >= node.y && mouseY.value <= node.y + node.height
  )

  if (clickedNode) {
    // 如果按住Shift键，开始连接模式
    if (event.shiftKey) {
      connecting.value = true
      connectingNode.value = clickedNode
    }
  }
}

function handleMouseMove(event: MouseEvent) {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) return

  mouseX.value = event.clientX - rect.left
  mouseY.value = event.clientY - rect.top

  if (dragging.value) {
    dragging.value.x = mouseX.value - dragOffset.value.x
    dragging.value.y = mouseY.value - dragOffset.value.y

    // 更新相关的边
    edges.value.forEach(edge => {
      if (edge.id.includes(dragging.value!.id)) {
        const centerX = dragging.value!.x + dragging.value!.width / 2
        const centerY = dragging.value!.y + dragging.value!.height / 2

        if (edge.id.startsWith(dragging.value!.id)) {
          edge.x1 = centerX
          edge.y1 = centerY
        } else if (edge.id.endsWith(dragging.value!.id)) {
          edge.x2 = centerX
          edge.y2 = centerY
        }
      }
    })

    saveCanvas()
  }
}

function handleMouseUp(event: MouseEvent) {
  if (connecting.value && connectingNode.value) {
    const targetNode = nodes.value.find(node =>
      mouseX.value >= node.x && mouseX.value <= node.x + node.width &&
      mouseY.value >= node.y && mouseY.value <= node.y + node.height
    )

    if (targetNode && targetNode.id !== connectingNode.value.id) {
      // 创建连接
      const edgeId = `${connectingNode.value.id}-${targetNode.id}`
      const centerX1 = connectingNode.value.x + connectingNode.value.width / 2
      const centerY1 = connectingNode.value.y + connectingNode.value.height / 2
      const centerX2 = targetNode.x + targetNode.width / 2
      const centerY2 = targetNode.y + targetNode.height / 2

      edges.value.push({
        id: edgeId,
        x1: centerX1,
        y1: centerY1,
        x2: centerX2,
        y2: centerY2,
        label: '关系',
        color: '#1890ff',
        width: 2
      })

      saveCanvas()
    }
  }

  dragging.value = null
  connecting.value = false
  connectingNode.value = null
}

function clearCanvas() {
  nodes.value = []
  edges.value = []
  saveCanvas()
}

function exportCanvas() {
  // 这里可以实现SVG导出功能
  message.info('导出功能开发中')
}

function saveCanvas() {
  const data = JSON.stringify({
    nodes: nodes.value,
    edges: edges.value
  })

  emit('save', data)
}
</script>

<style scoped>
.canvas-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.toolbar {
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.canvas-area {
  flex: 1;
  overflow: auto;
  background: #fafafa;
  display: flex;
  justify-content: center;
  align-items: center;
}

svg {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}
</style>