<template>
  <div class="relation-graph-canvas">
    <div class="graph-toolbar">
      <a-space>
        <a-button size="small" @click="addNode('person')">
          <UserOutlined /> 添加人物
        </a-button>
        <a-button size="small" @click="addNode('location')">
          <EnvironmentOutlined /> 添加地点
        </a-button>
        <a-button size="small" @click="addNode('time')">
          <ClockCircleOutlined /> 添加时间
        </a-button>
        <a-button size="small" @click="addNode('evidence')">
          <SafetyOutlined /> 添加证据
        </a-button>
        <a-divider type="vertical" />
        <a-select
          v-model:value="currentRelationType"
          size="small"
          style="width: 120px"
          placeholder="选择关系类型"
        >
          <a-select-option value="认识">认识</a-select-option>
          <a-select-option value="在现场">在现场</a-select-option>
          <a-select-option value="使用">使用</a-select-option>
          <a-select-option value="拥有">拥有</a-select-option>
          <a-select-option value="参与">参与</a-select-option>
        </a-select>
        <a-divider type="vertical" />
        <a-button size="small" @click="autoLayout">
          <ApartmentOutlined /> 自动布局
        </a-button>
        <a-button size="small" @click="exportGraph">
          <ExportOutlined /> 导出图片
        </a-button>
      </a-space>
    </div>

    <div ref="graphContainer" class="graph-container">
      <svg
        :width="graphWidth"
        :height="graphHeight"
        @mousedown="handleGraphMouseDown"
        @mousemove="handleGraphMouseMove"
        @mouseup="handleGraphMouseUp"
        @wheel="handleWheel"
      >
        <defs>
          <marker
            id="arrowhead"
            markerWidth="10"
            markerHeight="7"
            refX="9"
            refY="3.5"
            orient="auto"
          >
            <polygon points="0 0, 10 3.5, 0 7" fill="#666" />
          </marker>
        </defs>

        <!-- 背景网格 -->
        <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
          <path d="M 20 0 L 0 0 0 20" fill="none" stroke="#f0f0f0" stroke-width="1"/>
        </pattern>
        <rect width="100%" height="100%" fill="url(#grid)" />

        <!-- 关系组 -->
        <g :transform="`translate(${pan.x}, ${pan.y}) scale(${zoom})`">
          <!-- 绘制连接线 -->
          <g v-for="edge in graphEdges" :key="edge.id">
            <path
              :d="getEdgePath(edge)"
              :stroke="edge.color"
              :stroke-width="edge.width"
              fill="none"
              marker-end="url(#arrowhead)"
              class="edge"
            />
            <text
              :x="(edge.x1 + edge.x2) / 2"
              :y="(edge.y1 + edge.y2) / 2"
              text-anchor="middle"
              font-size="12"
              fill="#666"
              class="edge-label"
              style="pointer-events: none"
            >
              {{ edge.label }}
            </text>
          </g>

          <!-- 绘制节点 -->
          <g
            v-for="node in graphNodes"
            :key="node.id"
            :transform="`translate(${node.x}, ${node.y})`"
            @mousedown.stop="startNodeDrag(node)"
            class="node"
          >
            <!-- 节点圆形 -->
            <circle
              :r="node.radius"
              :fill="node.color"
              :stroke="node.borderColor"
              :stroke-width="3"
            />

            <!-- 节点图标 -->
            <text x="0" y="0" text-anchor="middle" dominant-baseline="middle" font-size="20">
              {{ getNodeIcon(node.type) }}
            </text>

            <!-- 节点标签 -->
            <text
              x="0"
              :y="node.radius + 16"
              text-anchor="middle"
              font-size="12"
              fill="#333"
              class="node-label"
              style="pointer-events: none"
            >
              {{ node.label }}
            </text>

            <!-- 删除按钮 -->
            <g
              :transform="`translate(${node.radius}, ${-node.radius})`"
              @click.stop="deleteNode(node)"
              class="delete-btn"
            >
              <circle r="10" fill="#ff4d4f" />
              <text x="0" y="4" text-anchor="middle" font-size="14" fill="#fff">×</text>
            </g>
          </g>

          <!-- 连接线 -->
          <line
            v-if="connecting && connectingNode"
            :x1="connectingNode.x"
            :y1="connectingNode.y"
            :x2="(mouseX - pan.x) / zoom"
            :y2="(mouseY - pan.y) / zoom"
            stroke="#1890ff"
            stroke-width="2"
            stroke-dasharray="5,5"
          />
        </g>
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  UserOutlined,
  EnvironmentOutlined,
  ClockCircleOutlined,
  SafetyOutlined,
  ApartmentOutlined,
  ExportOutlined
} from '@ant-design/icons-vue'

interface GraphNode {
  id: string
  type: string
  x: number
  y: number
  radius: number
  label: string
  color: string
  borderColor: string
}

interface GraphEdge {
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
  graphData?: string
}>()

const emit = defineEmits<{
  update: [data: string]
}>()

const graphContainer = ref<HTMLDivElement>()
const graphWidth = ref(800)
const graphHeight = ref(600)

const pan = ref({ x: 0, y: 0 })
const zoom = ref(1)
const mouseX = ref(0)
const mouseY = ref(0)

const currentRelationType = ref('认识')
const connecting = ref(false)
const connectingNode = ref<GraphNode | null>(null)
const draggingNode = ref<GraphNode | null>(null)
const isPanning = ref(false)
const dragStart = ref({ x: 0, y: 0 })

// 节点颜色配置
const nodeColors: Record<string, { bg: string; border: string }> = {
  person: { bg: '#e6f7ff', border: '#1890ff' },
  location: { bg: '#fff7e6', border: '#fa8c16' },
  time: { bg: '#f6ffed', border: '#52c41a' },
  evidence: { bg: '#fff1f0', border: '#f5222d' }
}

// 解析图数据
const graphNodes = ref<GraphNode[]>([])
const graphEdges = ref<GraphEdge[]>([])

onMounted(() => {
  initGraph()
})

watch(() => props.graphData, (newData) => {
  if (newData) {
    loadGraphData(newData)
  }
})

function initGraph() {
  if (props.graphData) {
    loadGraphData(props.graphData)
  }
}

function loadGraphData(data: string) {
  try {
    const parsed = JSON.parse(data)

    if (parsed.nodes && Array.isArray(parsed.nodes)) {
      graphNodes.value = parsed.nodes.map((node: any) => ({
        id: node.id,
        type: node.type,
        x: node.x || 0,
        y: node.y || 0,
        radius: node.radius || 30,
        label: node.label || node.type,
        color: nodeColors[node.type]?.bg || '#e6f7ff',
        borderColor: nodeColors[node.type]?.border || '#1890ff'
      }))
    }

    if (parsed.edges && Array.isArray(parsed.edges)) {
      graphEdges.value = parsed.edges.map((edge: any) => ({
        id: edge.id,
        x1: edge.source?.x || 0,
        y1: edge.source?.y || 0,
        x2: edge.target?.x || 0,
        y2: edge.target?.y || 0,
        label: edge.label || edge.type,
        color: '#666',
        width: 2
      }))
    }
  } catch (error) {
    console.error('加载图数据失败:', error)
  }
}

function getNodeIcon(type: string): string {
  const icons: Record<string, string> = {
    person: '👤',
    location: '📍',
    time: '⏰',
    evidence: '🔍'
  }
  return icons[type] || '⬜'
}

function addNode(type: string) {
  const id = `node-${Date.now()}`
  const colors = nodeColors[type] || nodeColors.person

  // 随机位置
  const x = 100 + Math.random() * (graphWidth.value - 200)
  const y = 100 + Math.random() * (graphHeight.value - 200)

  const newNode: GraphNode = {
    id,
    type,
    x,
    y,
    radius: 30,
    label: type === 'person' ? '人物' : type === 'location' ? '地点' : type === 'time' ? '时间' : '证据',
    color: colors.bg,
    borderColor: colors.border
  }

  graphNodes.value.push(newNode)
  emitGraphUpdate()
}

function deleteNode(node: GraphNode) {
  graphNodes.value = graphNodes.value.filter(n => n.id !== node.id)
  graphEdges.value = graphEdges.value.filter(e =>
    !e.id.includes(node.id) &&
    !(e.x1 === node.x && e.y1 === node.y) &&
    !(e.x2 === node.x && e.y2 === node.y)
  )
  emitGraphUpdate()
}

function startNodeDrag(node: GraphNode) {
  // 检查是否按住Shift键
  if (event && (event as MouseEvent).shiftKey) {
    connecting.value = true
    connectingNode.value = node
  } else {
    draggingNode.value = node
  }
}

function handleGraphMouseDown(event: MouseEvent) {
  const rect = graphContainer.value?.getBoundingClientRect()
  if (!rect) return

  mouseX.value = event.clientX - rect.left
  mouseY.value = event.clientY - rect.top

  if (!draggingNode.value && !connecting.value) {
    isPanning.value = true
    dragStart.value = {
      x: mouseX.value - pan.value.x,
      y: mouseY.value - pan.value.y
    }
  }
}

function handleGraphMouseMove(event: MouseEvent) {
  const rect = graphContainer.value?.getBoundingClientRect()
  if (!rect) return

  mouseX.value = event.clientX - rect.left
  mouseY.value = event.clientY - rect.top

  if (isPanning.value) {
    pan.value.x = mouseX.value - dragStart.value.x
    pan.value.y = mouseY.value - dragStart.value.y
  } else if (draggingNode.value) {
    const nodeX = (mouseX.value - pan.value.x) / zoom.value
    const nodeY = (mouseY.value - pan.value.y) / zoom.value

    draggingNode.value.x = nodeX
    draggingNode.value.y = nodeY

    // 更新相关边
    graphEdges.value.forEach(edge => {
      if (edge.id.includes(draggingNode.value!.id)) {
        if (edge.x1 === draggingNode.value!.x && edge.y1 === draggingNode.value!.y) {
          edge.x1 = nodeX
          edge.y1 = nodeY
        } else if (edge.x2 === draggingNode.value!.x && edge.y2 === draggingNode.value!.y) {
          edge.x2 = nodeX
          edge.y2 = nodeY
        }
      }
    })

    emitGraphUpdate()
  }
}

function handleGraphMouseUp(event: MouseEvent) {
  if (connecting.value && connectingNode.value) {
    const nodeX = (mouseX.value - pan.value.x) / zoom.value
    const nodeY = (mouseY.value - pan.value.y) / zoom.value

    // 查找目标节点
    const targetNode = graphNodes.value.find(node => {
      const distance = Math.sqrt(
        Math.pow(node.x - nodeX, 2) + Math.pow(node.y - nodeY, 2)
      )
      return distance <= node.radius
    })

    if (targetNode && targetNode.id !== connectingNode.value.id) {
      // 创建连接
      createEdge(connectingNode.value, targetNode)
    }
  }

  draggingNode.value = null
  connecting.value = false
  connectingNode.value = null
  isPanning.value = false
}

function createEdge(source: GraphNode, target: GraphNode) {
  const edgeId = `${source.id}-${target.id}`
  const existingEdge = graphEdges.value.find(e => e.id === edgeId)

  if (!existingEdge) {
    graphEdges.value.push({
      id: edgeId,
      x1: source.x,
      y1: source.y,
      x2: target.x,
      y2: target.y,
      label: currentRelationType.value,
      color: '#666',
      width: 2
    })

    emitGraphUpdate()
  }
}

function getEdgePath(edge: GraphEdge): string {
  const dx = edge.x2 - edge.x1
  const dy = edge.y2 - edge.y1
  const length = Math.sqrt(dx * dx + dy * dy)

  // 调整线的起点和终点到节点边缘
  const startX = edge.x1 + (dx / length) * 30
  const startY = edge.y1 + (dy / length) * 30
  const endX = edge.x2 - (dx / length) * 30
  const endY = edge.y2 - (dy / length) * 30

  // 创建曲线
  const midX = (startX + endX) / 2
  const midY = (startY + endY) / 2
  const controlX = midX + (dy / length) * 20
  const controlY = midY - (dx / length) * 20

  return `M ${startX} ${startY} Q ${controlX} ${controlY} ${endX} ${endY}`
}

function handleWheel(event: WheelEvent) {
  event.preventDefault()
  const zoomFactor = event.deltaY > 0 ? 0.9 : 1.1
  zoom.value = Math.min(Math.max(zoom.value * zoomFactor, 0.1), 5)
}

function autoLayout() {
  // 简单的圆形布局算法
  const centerX = graphWidth.value / 2
  const centerY = graphHeight.value / 2
  const radius = Math.min(graphWidth.value, graphHeight.value) / 3

  graphNodes.value.forEach((node, index) => {
    const angle = (index / graphNodes.value.length) * 2 * Math.PI
    node.x = centerX + radius * Math.cos(angle) - pan.value.x
    node.y = centerY + radius * Math.sin(angle) - pan.value.y
  })

  // 更新边的位置
  graphEdges.value.forEach(edge => {
    const sourceNode = graphNodes.value.find(n => edge.id.includes(n.id) && edge.x1 === n.x)
    const targetNode = graphNodes.value.find(n => edge.id.includes(n.id) && edge.x2 === n.x)

    if (sourceNode) {
      edge.x1 = sourceNode.x
      edge.y1 = sourceNode.y
    }
    if (targetNode) {
      edge.x2 = targetNode.x
      edge.y2 = targetNode.y
    }
  })

  emitGraphUpdate()
}

function exportGraph() {
  message.info('导出功能开发中')
}

function emitGraphUpdate() {
  const data = JSON.stringify({
    nodes: graphNodes.value.map(node => ({
      id: node.id,
      type: node.type,
      x: node.x,
      y: node.y,
      radius: node.radius,
      label: node.label,
      color: node.color,
      borderColor: node.borderColor
    })),
    edges: graphEdges.value.map(edge => ({
      id: edge.id,
      source: { x: edge.x1, y: edge.y1 },
      target: { x: edge.x2, y: edge.y2 },
      label: edge.label,
      color: edge.color,
      width: edge.width
    }))
  })

  emit('update', data)
}
</script>

<style scoped>
.relation-graph-canvas {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.graph-toolbar {
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  overflow-x: auto;
}

.graph-container {
  flex: 1;
  background: #fafafa;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: auto;
}

svg {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

.node {
  cursor: grab;
}

.node:active {
  cursor: grabbing;
}

.node:hover circle {
  filter: drop-shadow(0 0 6px rgba(0, 0, 0, 0.3));
}

.edge {
  pointer-events: none;
}

.edge-label {
  background: rgba(255, 255, 255, 0.9);
  padding: 2px 4px;
  border-radius: 2px;
}

.delete-btn {
  cursor: pointer;
  display: none;
}

.node:hover .delete-btn {
  display: block;
}

.delete-btn circle:hover {
  fill: #cf1322;
}
</style>