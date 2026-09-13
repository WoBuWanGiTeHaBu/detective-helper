<template>
  <div class="workspace">
    <!-- ═══════════════ 左侧栏 372 ═══════════════ -->
    <aside class="sidebar">
      <!-- 侧栏头部 -->
      <div class="side-head">
        <button class="back-row" type="button" @click="goBack">
          <svg viewBox="0 0 14 14" fill="none">
            <path d="M8.6 3 4.6 7l4 4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span>全部案件</span>
        </button>

        <h1 class="side-title" :title="book?.name">{{ book?.name || '未命名案件' }}</h1>
        <p class="side-meta">
          <span>{{ eventCount }} 个事件 · {{ totalPageCount }} 页</span>
          <button class="side-edit" type="button" @click="openBookDialog">编辑</button>
        </p>
      </div>

      <div class="side-divider" />

      <!-- ═══ 两列容器：event 列 180 + page 列 192 = 372 ═══ -->
      <div class="cols">
        <!-- ── event 列 ── -->
        <section class="col-event">
          <div class="col-head">event</div>

          <div class="col-body">
            <div
              v-for="(ev, index) in events"
              :key="ev.id"
              class="ev-item"
              :class="{
                active: ev.id === currentEventId,
                'drag-over': dragOverEventIndex === index && dragEventIndex !== index,
                'dragging': dragEventIndex === index
              }"
              :draggable="renamingEventId !== ev.id"
              @click="workspaceStore.selectEvent(ev.id)"
              @dblclick="startRenameEvent(ev)"
              @contextmenu.prevent="openEventMenu($event, ev)"
              @dragstart="onEventDragStart($event, index)"
              @dragover.prevent="dragOverEventIndex = index"
              @dragleave="onEventDragLeave(index)"
              @drop.prevent="onEventDrop(index)"
              @dragend="onEventDragEnd"
            >
              <svg class="ev-caret" viewBox="0 0 11 11" fill="none">
                <path
                  d="M3.4 1.8 7.2 5.5 3.4 9.2"
                  stroke="currentColor"
                  stroke-width="1.3"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>

              <input
                v-if="renamingEventId === ev.id"
                :ref="setEventInput"
                class="inline-input"
                :value="ev.name"
                @click.stop
                @keyup.enter="commitRenameEvent(ev)"
                @blur="commitRenameEvent(ev)"
                @keyup.esc="renamingEventId = null"
              />
              <span v-else class="item-name" :title="ev.name">{{ ev.name }}</span>

              <span v-if="renamingEventId !== ev.id" class="item-count">
                {{ pagesByEvent[ev.id]?.length ?? 0 }}
              </span>
            </div>

            <button class="add-row" type="button" @click="openCreateEvent">
              <svg viewBox="0 0 11 11" fill="none">
                <path d="M5.5 1.4v8.2M1.4 5.5h8.2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
              </svg>
              <span>新增 event</span>
            </button>
          </div>
        </section>

        <!-- ── page 列 ── -->
        <section class="col-page">
          <div class="col-head">page</div>
          <div class="col-rule" />

          <div class="col-body">
            <div
              v-for="(p, index) in pages"
              :key="p.id"
              class="pg-item"
              :class="{
                active: p.id === currentPageId,
                'drag-over': dragOverPageIndex === index && dragPageIndex !== index,
                'dragging': dragPageIndex === index
              }"
              :draggable="renamingPageId !== p.id"
              @click="workspaceStore.selectPage(p.id)"
              @dblclick="startRenamePage(p)"
              @dragstart="onPageDragStart($event, index)"
              @dragover.prevent="dragOverPageIndex = index"
              @dragleave="onPageDragLeave(index)"
              @drop.prevent="onPageDrop(index)"
              @dragend="onPageDragEnd"
            >
              <svg class="pg-icon" viewBox="0 0 13 13" fill="none">
                <path
                  d="M3 1.6h4.6L10.4 4.4v7H3z"
                  stroke="currentColor"
                  stroke-width="1.15"
                  stroke-linejoin="round"
                />
                <path d="M7.4 1.6v3h3" stroke="currentColor" stroke-width="1.15" stroke-linejoin="round" />
              </svg>

              <input
                v-if="renamingPageId === p.id"
                :ref="setPageInput"
                class="inline-input"
                :value="p.name"
                @click.stop
                @keyup.enter="commitRenamePage(p)"
                @blur="commitRenamePage(p)"
                @keyup.esc="renamingPageId = null"
              />
              <span v-else class="item-name" :title="p.name">{{ p.name }}</span>
            </div>

            <button
              class="add-row page-add"
              type="button"
              :disabled="currentEventId == null"
              @click="openCreatePage"
            >
              <svg viewBox="0 0 11 11" fill="none">
                <path d="M5.5 1.4v8.2M1.4 5.5h8.2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
              </svg>
              <span>新增 page</span>
            </button>

            <p v-if="currentEventId == null" class="col-hint">先选择一个 event</p>
          </div>
        </section>
      </div>

      <!-- 扩展功能入口（关系图 / 族谱） -->
      <div class="ext-rail">
        <button class="ext-btn" type="button" @click="openRelationGraphs">
          <svg viewBox="0 0 14 14" fill="none">
            <circle cx="3.4" cy="3.6" r="1.9" stroke="currentColor" stroke-width="1.2" />
            <circle cx="10.6" cy="3.6" r="1.9" stroke="currentColor" stroke-width="1.2" />
            <circle cx="7" cy="10.6" r="1.9" stroke="currentColor" stroke-width="1.2" />
            <path d="M4.9 4.6 6.2 9M9.1 4.6 7.8 9" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" />
          </svg>
          <span>人物关系图</span>
        </button>
        <button class="ext-btn" type="button" @click="addTimeline">
          <svg viewBox="0 0 14 14" fill="none">
            <path d="M1.6 7h10.8" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" />
            <circle cx="4" cy="7" r="1.7" fill="currentColor" />
            <circle cx="9.6" cy="7" r="1.7" stroke="currentColor" stroke-width="1.2" />
          </svg>
          <span>自适应时间线</span>
        </button>
        <button class="ext-btn" type="button" @click="openFamilyTree">
          <svg viewBox="0 0 14 14" fill="none">
            <rect x="5.2" y="1.6" width="3.6" height="3" rx="1" stroke="currentColor" stroke-width="1.2" />
            <rect x="1.4" y="9.4" width="3.6" height="3" rx="1" stroke="currentColor" stroke-width="1.2" />
            <rect x="9" y="9.4" width="3.6" height="3" rx="1" stroke="currentColor" stroke-width="1.2" />
            <path d="M7 4.6v2.2M3.2 9.4V6.8h7.6v2.6" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span>族谱图</span>
        </button>
      </div>
    </aside>

    <!-- ═══════════════ 主工作区 ═══════════════ -->
    <section class="main">
      <!-- 画布顶栏 h60 -->
      <header class="canvas-bar">
        <nav class="crumbs">
          <span class="crumb">{{ book?.name || '—' }}</span>
          <svg class="crumb-sep" viewBox="0 0 12 12" fill="none">
            <path d="M4.6 2.6 8 6l-3.4 3.4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span class="crumb">{{ currentEvent?.name || '未选择事件' }}</span>
          <svg class="crumb-sep" viewBox="0 0 12 12" fill="none">
            <path d="M4.6 2.6 8 6l-3.4 3.4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span class="crumb current">{{ currentPage?.name || '未选择页' }}</span>
        </nav>

        <div class="canvas-bar-right">
          <span class="save-pill" :class="workspaceStore.saveState">
            <svg v-if="workspaceStore.saveState !== 'saving'" viewBox="0 0 12 12" fill="none">
              <path d="M2.2 6.4 4.7 8.9 9.8 3.4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            <span v-else class="spin" />
            <span>{{ saveText }}</span>
          </span>
        </div>
      </header>

      <!-- 画布区 -->
      <div
        ref="canvasRef"
        class="canvas-area"
        :class="[
          { grabbing: panning, 'has-bg': currentPageId != null },
          currentPageId != null ? `bg-${bgMode}` : ''
        ]"
        @mousedown="onCanvasMouseDown"
        @wheel.ctrl.prevent="onWheel"
      >
        <!-- 无页面 -->
        <div v-if="currentPageId == null" class="canvas-empty">
          <div class="canvas-empty-card">
            <svg viewBox="0 0 40 40" fill="none">
              <path d="M9 6h13.6L31 14.4V34H9z" stroke="#A8A29A" stroke-width="1.6" stroke-linejoin="round" />
              <path d="M22.6 6v8.4H31" stroke="#A8A29A" stroke-width="1.6" stroke-linejoin="round" />
            </svg>
            <p>{{ currentEventId == null ? '左侧先选一个 event' : '这个 event 下还没有 page' }}</p>
            <button v-if="currentEventId != null" class="primary-btn" type="button" @click="openCreatePage">
              新建 page
            </button>
          </div>
        </div>

        <template v-else>
          <!-- 背景层：纯色 / 点阵 / 方格 / 横线 -->
          <svg
            v-if="bgMode !== 'plain'"
            class="grid-layer"
            :width="stageW"
            :height="stageH"
          >
            <defs>
              <!-- 点阵 -->
              <pattern
                id="bg-dot"
                :width="8 * zoom"
                :height="8 * zoom"
                patternUnits="userSpaceOnUse"
              >
                <circle
                  :cx="8 * zoom >= 6 ? 1 : 0.5"
                  :cy="8 * zoom >= 6 ? 1 : 0.5"
                  :r="0.5"
                  fill="#DDD8CE"
                />
              </pattern>

              <!-- 方格 -->
              <pattern
                id="bg-grid"
                :width="8 * zoom"
                :height="8 * zoom"
                patternUnits="userSpaceOnUse"
              >
                <path
                  :d="`M ${8 * zoom} 0 L 0 0 0 ${8 * zoom}`"
                  fill="none"
                  stroke="#EBE7DF"
                  stroke-width="1"
                />
              </pattern>

              <!-- 横线 -->
              <pattern
                id="bg-line"
                :width="stageW"
                :height="24 * zoom"
                patternUnits="userSpaceOnUse"
              >
                <line
                  x1="0"
                  :y1="24 * zoom"
                  :x2="stageW"
                  :y2="24 * zoom"
                  stroke="#E8E9EA"
                  stroke-width="1"
                />
              </pattern>
            </defs>
            <rect
              width="100%"
              height="100%"
              :fill="`url(#bg-${bgMode})`"
            />
          </svg>

          <!-- 内容层（拖动 / 缩放） -->
          <div
            class="stage"
            :style="{
              transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`
            }"
          >
            <!-- 连线 -->
            <svg class="edge-layer" :width="stageW" :height="stageH">
              <defs>
                <marker id="arrow-end" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto">
                  <polygon points="0 0, 9 4.5, 0 9" fill="#8C877E" />
                </marker>
                <marker id="arrow-start" markerWidth="9" markerHeight="9" refX="1" refY="4.5" orient="auto-start-reverse">
                  <polygon points="0 0, 9 4.5, 0 9" fill="#8C877E" />
                </marker>
              </defs>

              <CanvasRelation
                v-for="rel in canvas.relationships"
                :key="rel.id"
                :relationship="rel"
                :source="objectById(rel.source)"
                :target="objectById(rel.target)"
                @edit="openRelationDialog"
              />

              <!-- 连线预览：起点已选 → 指向当前悬停的对象 -->
              <line
                v-if="linkPreview"
                :x1="linkPreview.x1"
                :y1="linkPreview.y1"
                :x2="linkPreview.x2"
                :y2="linkPreview.y2"
                :stroke="relKind === 'dashed' ? '#A9A296' : '#7C9A88'"
                stroke-width="1"
                :stroke-dasharray="relKind === 'dashed' ? '5 4' : '4 3'"
              />
            </svg>

            <!-- 节点 -->
            <svg class="node-layer" :width="stageW" :height="stageH">
              <CanvasNode
                v-for="obj in canvas.objects"
                :key="obj.id"
                :object="obj"
                :selected="selectedId === obj.id"
                :dragging="draggingId === obj.id"
                :hovered="activeTool === 'relation' && linkHoverId === obj.id && linkSourceId !== obj.id"
                :locked="linkSourceId === obj.id"
                @dragstart="onNodeDragStart"
                @edit="openNodeEditor"
                @hover="onNodeHover"
              />
            </svg>

            <!-- 注解（HTML 层，可编辑 + 可拖动） -->
            <CanvasAnnotation
              v-for="ann in canvas.annotations"
              :key="ann.id"
              :annotation="ann"
              :obstacles="annotationObstacles"
              @update="onAnnotationUpdate"
              @commit="scheduleSave"
            />

            <!-- 自适应时间线（绝对定位面板，可直接拖动移动） -->
            <div
              v-for="(tl, index) in canvas.timelines"
              :key="tl.id"
              class="tl-host"
              :style="{
                left: `${tl.x ?? 44 + index * 24}px`,
                top: `${tl.y ?? 44 + index * 24}px`
              }"
              @mousedown.stop
            >
              <TimelinePanel
                :timeline="tl"
                :width="760"
                :x="tl.x ?? 44 + index * 24"
                :y="tl.y ?? 44 + index * 24"
                :zoom="zoom"
                @rename="onTimelineRename"
                @direction="onTimelineDirection"
                @add-point="onAddTimelinePoint"
                @update-point="onUpdateTimelinePoint"
                @remove-point="onRemoveTimelinePoint"
                @move="onTimelineMove"
                @close="onTimelineClose"
              />
            </div>
          </div>

          <!-- 浮动工具条 -->
          <div class="toolbar">
            <template v-for="tool in TOOLS" :key="tool.key">
              <div v-if="tool.dividerBefore" class="tb-sep" />
              <div class="tb-slot">
                <button
                  class="tb-btn"
                  type="button"
                  :class="{ active: activeTool === tool.key || (tool.key === 'relation' && relPopOpen) }"
                  :title="tool.label"
                  @click="onToolClick(tool.key)"
                >
                  <span v-html="tool.icon" />
                </button>

                <!-- 关系线型：点图标带出三种线型，选完自动收起 -->
                <transition name="fade">
                  <div v-if="tool.key === 'relation' && relPopOpen" class="rel-pop" @mousedown.stop>
                    <div class="rel-pop-title">关系线型</div>
                    <button
                      v-for="k in REL_KINDS"
                      :key="k.key"
                      type="button"
                      class="rel-opt"
                      :class="{ active: relKind === k.key }"
                      @click="chooseRelKind(k.key)"
                    >
                      <svg class="rel-prev" viewBox="0 0 44 14" fill="none">
                        <line
                          x1="3"
                          y1="7"
                          x2="41"
                          y2="7"
                          stroke="currentColor"
                          stroke-width="1.4"
                          :stroke-dasharray="k.key === 'dashed' ? '5 4' : undefined"
                        />
                        <polygon points="34,3.4 41,7 34,10.6" fill="currentColor" />
                        <polygon
                          v-if="k.key === 'bidirectional'"
                          points="10,3.4 3,7 10,10.6"
                          fill="currentColor"
                        />
                      </svg>
                      <span>{{ k.label }}</span>
                    </button>
                  </div>
                </transition>
              </div>
            </template>
          </div>

          <!-- 连线模式提示条 -->
          <div v-if="linkHint" class="link-hint">
            <svg viewBox="0 0 14 14" fill="none">
              <circle cx="4" cy="10" r="2" stroke="currentColor" stroke-width="1.3" />
              <circle cx="10" cy="4" r="2" stroke="currentColor" stroke-width="1.3" />
              <path d="M5.4 8.6 8.6 5.4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
            </svg>
            <span>{{ linkHint }}</span>
            <button type="button" class="link-cancel" @click="cancelLinking">退出</button>
          </div>

          <!-- 缩放条 -->
          <div class="zoombar">
            <button type="button" class="zb-btn" title="缩小" @click="zoomOut">
              <svg viewBox="0 0 12 12" fill="none">
                <path d="M2.5 6h7" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
              </svg>
            </button>
            <span class="zb-value">{{ zoomLabel }}</span>
            <button type="button" class="zb-btn" title="放大" @click="zoomIn">
              <svg viewBox="0 0 12 12" fill="none">
                <path d="M6 2.5v7M2.5 6h7" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
              </svg>
            </button>
            <span class="zb-sep" />
            <button type="button" class="zb-btn" title="适配画布" @click="fitCanvas">
              <svg viewBox="0 0 13 13" fill="none">
                <path d="M2 4.6V2h2.6M8.4 2H11v2.6M11 8.4V11H8.4M4.6 11H2V8.4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </button>

            <span class="zb-sep" />

            <!-- 背景切换 -->
            <div class="bg-picker">
              <button
                type="button"
                class="zb-btn"
                :class="{ active: bgOpen }"
                title="笔记背景"
                @click="bgOpen = !bgOpen"
              >
                <svg viewBox="0 0 13 13" fill="none">
                  <rect x="1.6" y="1.6" width="9.8" height="9.8" rx="2" stroke="currentColor" stroke-width="1.3" />
                  <circle cx="4.6" cy="4.6" r="0.9" fill="currentColor" />
                  <circle cx="8.4" cy="8.4" r="0.9" fill="currentColor" />
                </svg>
              </button>

              <transition name="fade">
                <div v-if="bgOpen" class="bg-pop" @mousedown.stop>
                  <div class="bg-pop-title">
                    笔记背景
                    <span class="bg-pop-note">本地偏好 · 不同步</span>
                  </div>
                  <button
                    v-for="opt in BG_OPTIONS"
                    :key="opt.key"
                    type="button"
                    class="bg-item"
                    :class="{ active: bgMode === opt.key }"
                    @click="chooseBg(opt.key)"
                  >
                    <span class="bg-swatch" :style="{ background: opt.preview }" />
                    <span class="bg-label">{{ opt.label }}</span>
                    <svg v-if="bgMode === opt.key" class="bg-check" viewBox="0 0 12 12" fill="none">
                      <path d="M2.6 6.2l2.4 2.4 4.4-4.8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                  </button>
                </div>
              </transition>
            </div>
          </div>
        </template>
      </div>
    </section>

    <!-- ═══════════════ 右侧：关系图抽屉 ═══════════════ -->
    <transition name="slide">
      <div v-if="graphDrawer" class="drawer">
        <div class="drawer-head">
          <h2 class="drawer-title">人物关系图</h2>
          <button class="drawer-close" type="button" @click="graphDrawer = false">
            <svg viewBox="0 0 12 12" fill="none">
              <path d="m3 3 6 6M9 3l-6 6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
            </svg>
          </button>
        </div>

        <!-- 顶部工具区 -->
        <div class="drawer-tools">
          <div class="tool-group">
            <span class="tool-label">来源</span>
            <select v-model="graphSource" class="mini-select">
              <option value="auto">自动提取</option>
              <option value="custom">自定义</option>
            </select>
          </div>

          <div class="tool-group">
            <span class="tool-label">类型</span>
            <div class="pill-group">
              <button
                v-for="f in GRAPH_FILTERS"
                :key="f.value"
                type="button"
                class="pill"
                :class="{ active: graphFilter === f.value }"
                @click="graphFilter = f.value"
              >
                {{ f.label }}
              </button>
            </div>
          </div>

          <div class="dir-switch">
            <button
              type="button"
              class="dir-seg"
              :class="{ active: graphView === 'list' }"
              @click="graphView = 'list'"
            >
              列表
            </button>
            <button
              type="button"
              class="dir-seg"
              :class="{ active: graphView === 'graph' }"
              @click="graphView = 'graph'"
            >
              图形
            </button>
          </div>
        </div>

        <!-- 列表视图 -->
        <div v-if="graphView === 'list'" class="drawer-body">
          <div v-if="!graphRows.length" class="drawer-empty">
            还没有关系数据
            <button class="link-btn" type="button" @click="extractFromCanvas">从画布提取</button>
          </div>
          <div v-for="row in graphRows" :key="row.id" class="rel-row">
            <span class="rel-name" :title="row.from">{{ row.from }}</span>
            <span class="rel-word">{{ row.label }}</span>
            <span class="rel-name" :title="row.to">{{ row.to }}</span>
          </div>
        </div>

        <!-- 图形视图 -->
        <div v-else class="drawer-body graph-body">
          <svg :width="400" :height="460" class="graph-svg">
            <line
              v-for="e in graphEdges"
              :key="e.id"
              :x1="pos(e.source).x"
              :y1="pos(e.source).y"
              :x2="pos(e.target).x"
              :y2="pos(e.target).y"
              stroke="#8C877E"
              stroke-width="1"
              marker-end="url(#arrow-end)"
            />
            <g v-for="n in graphNodes" :key="n.id">
              <rect
                :x="pos(n.id).x - 52"
                :y="pos(n.id).y - 16"
                width="104"
                height="32"
                :rx="16"
                fill="#E8EFEA"
                stroke="#A6BFB0"
                stroke-width="1"
              />
              <text
                :x="pos(n.id).x"
                :y="pos(n.id).y + 4"
                text-anchor="middle"
                class="graph-node-label"
              >
                {{ n.name }}
              </text>
            </g>
            <text v-if="!graphNodes.length" x="200" y="230" text-anchor="middle" class="graph-empty">
              还没有关系数据
            </text>
          </svg>
        </div>
      </div>
    </transition>

    <!-- ═══════════════ 新建 / 重命名 通用弹窗 ═══════════════ -->
    <div v-if="prompt.open" class="dialog-mask" @click.self="prompt.open = false">
      <div class="dialog">
        <h2 class="dialog-title">{{ prompt.title }}</h2>
        <input
          ref="promptInput"
          v-model="prompt.value"
          class="field-input"
          :placeholder="prompt.placeholder"
          @keyup.enter="submitPrompt"
        />
        <div class="dialog-actions">
          <button class="ghost-btn" type="button" @click="prompt.open = false">取消</button>
          <button class="primary-btn" type="button" :disabled="!prompt.value.trim()" @click="submitPrompt">
            确定
          </button>
        </div>
      </div>
    </div>

    <!-- 案件信息编辑 -->
    <div v-if="bookDialog.open" class="dialog-mask" @click.self="bookDialog.open = false">
      <div class="dialog">
        <h2 class="dialog-title">案件信息</h2>
        <label class="field">
          <span class="field-label">案件名称</span>
          <input v-model="bookDialog.name" class="field-input" placeholder="案件名称" />
        </label>
        <label class="field">
          <span class="field-label">封面题字</span>
          <input v-model="bookDialog.coverText" class="field-input" placeholder="封面短标签" />
        </label>
        <div class="dialog-actions">
          <button class="ghost-btn" type="button" @click="bookDialog.open = false">取消</button>
          <button class="primary-btn" type="button" @click="submitBook">保存</button>
        </div>
      </div>
    </div>

    <!-- ═══════════════ 对象资料（人物 / 事件 / 事物） ═══════════════ -->
    <div v-if="nodeDialog.open" class="dialog-mask" @click.self="nodeDialog.open = false">
      <div class="dialog">
        <h2 class="dialog-title">
          {{ nodeDialog.mode === 'create' ? '新增' : '编辑' }}{{ KIND_LABEL[nodeDialog.kind] }}资料
        </h2>

        <label class="field">
          <span class="field-label">{{ nodeDialog.kind === 'person' ? '姓名' : '名称' }}</span>
          <input
            ref="nodeNameInput"
            v-model="nodeDialog.name"
            class="field-input"
            :placeholder="NODE_PLACEHOLDER[nodeDialog.kind]"
            @keyup.enter="submitNodeDialog"
          />
        </label>

        <!-- 人物 -->
        <template v-if="nodeDialog.kind === 'person'">
          <label class="field">
            <span class="field-label">年龄</span>
            <input v-model="nodeDialog.age" class="field-input" placeholder="选填，如 35" />
          </label>
          <label class="field">
            <span class="field-label">职业 / 身份</span>
            <input v-model="nodeDialog.occupation" class="field-input" placeholder="选填，如 法医" />
          </label>
        </template>

        <!-- 事件 -->
        <template v-else-if="nodeDialog.kind === 'event'">
          <label class="field">
            <span class="field-label">时间</span>
            <input v-model="nodeDialog.time" class="field-input" placeholder="选填，如 3月14日 20:00" />
          </label>
          <label class="field">
            <span class="field-label">地点</span>
            <input v-model="nodeDialog.place" class="field-input" placeholder="选填，如 城东公寓" />
          </label>
        </template>

        <!-- 事物 -->
        <template v-else>
          <label class="field">
            <span class="field-label">类别</span>
            <input v-model="nodeDialog.category" class="field-input" placeholder="选填，如 凶器 / 线索" />
          </label>
        </template>

        <label class="field">
          <span class="field-label">备注</span>
          <textarea
            v-model="nodeDialog.description"
            class="field-input field-area"
            placeholder="选填，补充说明"
            rows="3"
          />
        </label>

        <p class="dialog-tip">所有字段都可以留空，之后双击对象即可再次编辑。</p>

        <div class="dialog-actions">
          <button class="ghost-btn" type="button" @click="nodeDialog.open = false">取消</button>
          <button class="primary-btn" type="button" @click="submitNodeDialog">
            {{ nodeDialog.mode === 'create' ? '创建' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ═══════════════ 关系设置（命名 + 线型） ═══════════════ -->
    <div v-if="relDialog.open" class="dialog-mask" @click.self="closeRelationDialog">
      <div class="dialog">
        <h2 class="dialog-title">{{ relDialog.id ? '关系设置' : '命名这条关系' }}</h2>

        <label class="field">
          <span class="field-label">关系说明</span>
          <input
            ref="relLabelInput"
            v-model="relDialog.label"
            class="field-input"
            placeholder="如 仇人 / 同事 / 持有"
            @keyup.enter="submitRelationDialog"
          />
        </label>

        <div class="field">
          <span class="field-label">线型</span>
          <div class="rel-kind-row">
            <button
              v-for="k in REL_KINDS"
              :key="k.key"
              type="button"
              class="rel-kind-btn"
              :class="{ active: relDialog.type === k.key }"
              @click="relDialog.type = k.key"
            >
              <svg viewBox="0 0 44 14" fill="none">
                <line
                  x1="3"
                  y1="7"
                  x2="41"
                  y2="7"
                  stroke="currentColor"
                  stroke-width="1.4"
                  :stroke-dasharray="k.key === 'dashed' ? '5 4' : undefined"
                />
                <polygon points="34,3.4 41,7 34,10.6" fill="currentColor" />
                <polygon
                  v-if="k.key === 'bidirectional'"
                  points="10,3.4 3,7 10,10.6"
                  fill="currentColor"
                />
              </svg>
              <span>{{ k.label }}</span>
            </button>
          </div>
        </div>

        <div class="dialog-actions">
          <button
            v-if="relDialog.id"
            class="ghost-btn danger-btn"
            type="button"
            @click="deleteRelation"
          >
            删除关系
          </button>
          <span class="actions-spacer" />
          <button class="ghost-btn" type="button" @click="closeRelationDialog">取消</button>
          <button class="primary-btn" type="button" @click="submitRelationDialog">
            {{ relDialog.id ? '保存' : '确定' }}
          </button>
        </div>
      </div>
    </div>

    <!-- event 右键菜单 -->
    <div
      v-if="ctxMenu.open"
      class="ctx-menu"
      :style="{ left: `${ctxMenu.x}px`, top: `${ctxMenu.y}px` }"
      @click.stop
    >
      <button type="button" @click="ctxRename">重命名</button>
      <button type="button" class="danger" @click="ctxDelete">删除事件</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { useWorkspaceStore, localId } from '@/stores/workspaceStore'
import { useBookStore } from '@/stores/bookStore'
import { relationGraphApi } from '@/api/relationGraph'
import type {
  Annotation,
  CanvasObject,
  CanvasTool,
  EventResponse,
  GraphEdge,
  GraphNode,
  ObjectShape,
  Relationship,
  RelationshipType,
  Timeline,
  TimelinePoint,
  WorkspacePage
} from '@/api/types'
import CanvasNode from '@/components/canvas/CanvasNode.vue'
import CanvasRelation from '@/components/canvas/CanvasRelation.vue'
import CanvasAnnotation from '@/components/canvas/CanvasAnnotation.vue'
import TimelinePanel from '@/components/canvas/TimelinePanel.vue'
import {
  BG_OPTIONS,
  getBg,
  setBg,
  type CanvasBg
} from '@/utils/canvasBackground'

const route = useRoute()
const router = useRouter()
const workspaceStore = useWorkspaceStore()
const bookStore = useBookStore()

const bookId = computed(() => Number(route.params.bookId))

const canvasRef = ref<HTMLElement | null>(null)
const stageW = ref(1600)
const stageH = ref(1400)

const book = computed(() => workspaceStore.book)
const events = computed(() => workspaceStore.events)
const pages = computed(() => workspaceStore.pages)
const pagesByEvent = computed(() => workspaceStore.pagesByEvent)
const currentEventId = computed(() => workspaceStore.currentEventId)
const currentPageId = computed(() => workspaceStore.currentPageId)
const currentEvent = computed(() => workspaceStore.currentEvent)
const currentPage = computed(() => workspaceStore.currentPage)
const canvas = computed(() => workspaceStore.canvas)
const eventCount = computed(() => workspaceStore.eventCount)
const totalPageCount = computed(() => workspaceStore.totalPageCount)

/* ---------------- 初始化 ---------------- */
onMounted(async () => {
  await workspaceStore.loadWorkspace(bookId.value)
  measureStage()
  window.addEventListener('resize', measureStage)
  document.addEventListener('click', closeCtxMenu)
})

onUnmounted(() => {
  window.removeEventListener('resize', measureStage)
  document.removeEventListener('click', closeCtxMenu)
})

function measureStage() {
  const el = canvasRef.value
  if (!el) return
  stageW.value = Math.max(el.clientWidth, 1600)
  stageH.value = Math.max(el.clientHeight, 1400)
}

function goBack() {
  workspaceStore.clearWorkspace()
  router.push('/bookshelf')
}

/* ---------------- 保存状态 ---------------- */
const saveText = computed(() => {
  switch (workspaceStore.saveState) {
    case 'saving':
      return '正在保存…'
    case 'saved':
      return `已自动保存 · ${workspaceStore.lastSavedAt}`
    case 'error':
      return '保存失败，点击重试'
    default:
      return '已同步'
  }
})

/**
 * 防抖自动保存。
 * 快照与计时都放在 store 里：切换 page / event 时会先把队列落盘再加载新画布，
 * 因此这里只负责「登记一次待保存」，不需要自己管定时器。
 */
function scheduleSave() {
  workspaceStore.scheduleSave()
}

/** 立即保存（新增 / 删除这类必须马上落盘的操作用它） */
async function saveNow() {
  try {
    await workspaceStore.saveCanvas()
  } catch {
    /* 拦截器已提示 */
  }
}

/* ---------------- 拖拽排序：event / page ---------------- */
const dragEventIndex = ref<number | null>(null)
const dragOverEventIndex = ref<number | null>(null)
const dragPageIndex = ref<number | null>(null)
const dragOverPageIndex = ref<number | null>(null)

function onEventDragStart(e: DragEvent, index: number) {
  dragEventIndex.value = index
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    // Firefox 需要 setData 才会真正启动拖拽
    e.dataTransfer.setData('text/plain', String(index))
  }
}

function onEventDragLeave(index: number) {
  if (dragOverEventIndex.value === index) dragOverEventIndex.value = null
}

async function onEventDrop(index: number) {
  const from = dragEventIndex.value
  dragOverEventIndex.value = null
  dragEventIndex.value = null
  if (from == null || from === index) return

  const next = [...events.value]
  const [moved] = next.splice(from, 1)
  next.splice(index, 0, moved)

  try {
    await workspaceStore.reorderEvents(next)
  } catch {
    message.error('排序保存失败')
    await workspaceStore.loadWorkspace(bookId.value)
  }
}

function onEventDragEnd() {
  dragEventIndex.value = null
  dragOverEventIndex.value = null
}

function onPageDragStart(e: DragEvent, index: number) {
  dragPageIndex.value = index
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(index))
  }
}

function onPageDragLeave(index: number) {
  if (dragOverPageIndex.value === index) dragOverPageIndex.value = null
}

async function onPageDrop(index: number) {
  const from = dragPageIndex.value
  dragOverPageIndex.value = null
  dragPageIndex.value = null
  if (from == null || from === index) return

  const next = [...pages.value]
  const [moved] = next.splice(from, 1)
  next.splice(index, 0, moved)

  try {
    await workspaceStore.reorderPages(next)
  } catch {
    message.error('排序保存失败')
    if (currentEventId.value != null) {
      await workspaceStore.selectEvent(currentEventId.value)
    }
  }
}

function onPageDragEnd() {
  dragPageIndex.value = null
  dragOverPageIndex.value = null
}

/* ---------------- 通用弹窗 prompt ---------------- */
const promptInput = ref<HTMLInputElement | null>(null)
const prompt = reactive({
  open: false,
  title: '',
  value: '',
  placeholder: '',
  onOk: null as ((value: string) => void | Promise<void>) | null
})

async function openPrompt(opts: {
  title: string
  placeholder: string
  value?: string
  onOk: (v: string) => void | Promise<void>
}) {
  prompt.open = true
  prompt.title = opts.title
  prompt.placeholder = opts.placeholder
  prompt.value = opts.value ?? ''
  prompt.onOk = (v: string) => opts.onOk(v)
  await nextTick()
  promptInput.value?.focus()
  promptInput.value?.select()
}

async function submitPrompt() {
  const v = prompt.value.trim()
  if (!v || !prompt.onOk) return
  const fn = prompt.onOk
  prompt.open = false
  await fn(v)
}

/* ---------------- Event ---------------- */
const renamingEventId = ref<number | null>(null)
const eventInputEl = ref<HTMLInputElement | null>(null)

/** v-for 里的 ref 回调：只在当前正在重命名的那个 input 上挂载 */
function setEventInput(el: Element | ComponentPublicInstance | null) {
  eventInputEl.value = (el as HTMLInputElement) ?? null
}

function openCreateEvent() {
  openPrompt({
    title: '新增 event',
    placeholder: '如：第一幕 · 案发当日',
    async onOk(name) {
      try {
        await workspaceStore.createEvent(name)
        message.success('事件已创建')
      } catch {
        /* 拦截器已提示 */
      }
    }
  })
}

async function startRenameEvent(ev: EventResponse) {
  renamingEventId.value = ev.id
  await nextTick()
  eventInputEl.value?.focus()
  eventInputEl.value?.select()
}

async function commitRenameEvent(ev: EventResponse) {
  if (renamingEventId.value !== ev.id) return
  const name = eventInputEl.value?.value.trim()
  renamingEventId.value = null
  if (!name || name === ev.name) return
  try {
    await workspaceStore.renameEvent(ev.id, name)
  } catch {
    /* 拦截器已提示 */
  }
}

/* ---------------- Page ---------------- */
const renamingPageId = ref<number | null>(null)
const pageInputEl = ref<HTMLInputElement | null>(null)

function setPageInput(el: Element | ComponentPublicInstance | null) {
  pageInputEl.value = (el as HTMLInputElement) ?? null
}

function openCreatePage() {
  if (currentEventId.value == null) {
    message.warning('先在左侧选择一个 event')
    return
  }
  openPrompt({
    title: '新增 page',
    placeholder: '如：现场勘查记录',
    async onOk(name) {
      try {
        await workspaceStore.createPage(name)
        message.success('页面已创建')
      } catch {
        /* 拦截器已提示 */
      }
    }
  })
}

async function startRenamePage(p: WorkspacePage) {
  renamingPageId.value = p.id
  await nextTick()
  pageInputEl.value?.focus()
  pageInputEl.value?.select()
}

async function commitRenamePage(p: WorkspacePage) {
  if (renamingPageId.value !== p.id) return
  const name = pageInputEl.value?.value.trim()
  renamingPageId.value = null
  if (!name || name === p.name) return
  try {
    await workspaceStore.renamePage(p.id, name)
  } catch {
    /* 拦截器已提示 */
  }
}

/* ---------------- event 右键菜单 ---------------- */
const ctxMenu = reactive({
  open: false,
  x: 0,
  y: 0,
  event: null as EventResponse | null
})

function openEventMenu(e: MouseEvent, ev: EventResponse) {
  ctxMenu.open = true
  ctxMenu.x = e.clientX
  ctxMenu.y = e.clientY
  ctxMenu.event = ev
}

function closeCtxMenu() {
  ctxMenu.open = false
}

function ctxRename() {
  const ev = ctxMenu.event
  ctxMenu.open = false
  if (ev) startRenameEvent(ev)
}

function ctxDelete() {
  const ev = ctxMenu.event
  ctxMenu.open = false
  if (!ev) return
  Modal.confirm({
    title: '删除事件',
    content: `「${ev.name}」及其下的所有页面与画布都会删除，此操作不可撤销。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await workspaceStore.removeEvent(ev.id)
      message.success('事件已删除')
    }
  })
}

/* ---------------- 案件信息 ---------------- */
const bookDialog = reactive({
  open: false,
  name: '',
  coverText: ''
})

function openBookDialog() {
  bookDialog.open = true
  bookDialog.name = book.value?.name ?? ''
  bookDialog.coverText = book.value?.coverText ?? ''
}

async function submitBook() {
  const name = bookDialog.name.trim()
  if (!name) return
  try {
    await bookStore.updateBook(bookId.value, {
      name,
      coverText: bookDialog.coverText.trim() || null
    })
    if (book.value) {
      book.value.name = name
      book.value.coverText = bookDialog.coverText.trim() || null
    }
    bookDialog.open = false
    message.success('已保存')
  } catch {
    /* 拦截器已提示 */
  }
}

/* ---------------- 画布：平移 / 缩放 ---------------- */
const pan = ref({ x: 0, y: 0 })
const zoom = ref(1)
const panning = ref(false)
let panStart = { x: 0, y: 0 }
let panOrigin = { x: 0, y: 0 }

/* ---------------- 画布背景（本地偏好，不入后端） ---------------- */
const bgMode = ref<CanvasBg>(getBg(currentPageId.value))
const bgOpen = ref(false)

/* ---------------- 对象资料弹窗（人物 / 事件 / 事物） ---------------- */
type NodeKind = 'person' | 'event' | 'thing'

const KIND_LABEL: Record<NodeKind, string> = {
  person: '人物',
  event: '事件',
  thing: '事物'
}

const NODE_PLACEHOLDER: Record<NodeKind, string> = {
  person: '选填，如 张远山',
  event: '选填，如 第一次出现',
  thing: '选填，如 老式怀表'
}

const NODE_SIZE: Record<NodeKind, { width: number; height: number; shape: ObjectShape }> = {
  person: { width: 162, height: 72, shape: 'ellipse' },
  event: { width: 172, height: 72, shape: 'rect' },
  thing: { width: 160, height: 100, shape: 'diamond' }
}

const nodeNameInput = ref<HTMLInputElement | null>(null)

const nodeDialog = reactive({
  open: false,
  mode: 'create' as 'create' | 'edit',
  kind: 'person' as NodeKind,
  id: '',
  name: '',
  age: '',
  occupation: '',
  time: '',
  place: '',
  category: '',
  description: ''
})

function kindOf(o: CanvasObject): NodeKind {
  const t = (o.type || '').toLowerCase()
  if (t === 'person' || t === '人物') return 'person'
  if (t === 'event' || t === '事件' || t === 'time') return 'event'
  return 'thing'
}

function resetNodeFields() {
  nodeDialog.name = ''
  nodeDialog.age = ''
  nodeDialog.occupation = ''
  nodeDialog.time = ''
  nodeDialog.place = ''
  nodeDialog.category = ''
  nodeDialog.description = ''
}

/** 添加对象：先弹窗收集资料，而不是丢一个「新人物」占位 */
async function openNodeCreator(kind: NodeKind) {
  if (currentPageId.value == null) {
    message.warning('先选择一个 page')
    return
  }
  nodeDialog.open = true
  nodeDialog.mode = 'create'
  nodeDialog.kind = kind
  nodeDialog.id = ''
  resetNodeFields()
  await nextTick()
  nodeNameInput.value?.focus()
}

/** 双击对象 → 弹出资料面板并回显已有信息 */
async function openNodeEditor(object: CanvasObject) {
  const cf = (object.customFields ?? {}) as Record<string, unknown>
  const str = (v: unknown) => (typeof v === 'string' ? v : typeof v === 'number' ? String(v) : '')

  nodeDialog.open = true
  nodeDialog.mode = 'edit'
  nodeDialog.kind = kindOf(object)
  nodeDialog.id = object.id
  nodeDialog.name = object.name
  nodeDialog.age = str(cf.age)
  nodeDialog.occupation = str(cf.occupation)
  nodeDialog.time = str(cf.time)
  nodeDialog.place = str(cf.place)
  nodeDialog.category = str(cf.category)
  nodeDialog.description = str(cf.description) || object.description || ''
  await nextTick()
  nodeNameInput.value?.focus()
  nodeNameInput.value?.select()
}

/** 收集面板里所有非空字段（空字段不落库，保持数据干净） */
function collectCustomFields(): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  const put = (k: string, v: string) => {
    const s = v.trim()
    if (s) out[k] = s
  }
  put('age', nodeDialog.age)
  put('occupation', nodeDialog.occupation)
  put('time', nodeDialog.time)
  put('place', nodeDialog.place)
  put('category', nodeDialog.category)
  put('description', nodeDialog.description)
  return out
}

async function submitNodeDialog() {
  const kind = nodeDialog.kind
  const fallback = kind === 'person' ? '新人物' : kind === 'event' ? '新事件' : '新事物'
  const name = nodeDialog.name.trim() || fallback
  const customFields = collectCustomFields()
  const desc = nodeDialog.description.trim()

  if (nodeDialog.mode === 'edit') {
    const obj = objectById(nodeDialog.id)
    if (!obj) {
      nodeDialog.open = false
      return
    }
    obj.name = name
    obj.description = desc || null
    obj.customFields = customFields
    nodeDialog.open = false
    await saveNow()
    message.success('资料已更新')
    return
  }

  const size = NODE_SIZE[kind]
  // 落在当前视口中心；多个节点之间做阶梯错位，避免完全重叠
  const rect = canvasRef.value?.getBoundingClientRect()
  const n = canvas.value.objects.length
  const cx = rect ? (rect.width / 2 - pan.value.x) / zoom.value : 420
  const cy = rect ? (rect.height / 2 - pan.value.y) / zoom.value : 300

  const obj: CanvasObject = {
    id: localId(kind === 'person' ? 'obj' : kind === 'event' ? 'evt' : 'thg'),
    type: kind,
    shape: size.shape,
    name,
    description: desc || null,
    x: Math.round((cx - size.width / 2 + (n % 4) * 28) / 8) * 8,
    y: Math.round((cy - size.height / 2 + (n % 4) * 24) / 8) * 8,
    width: size.width,
    height: size.height,
    style: {},
    customFields
  }

  canvas.value.objects.push(obj)
  selectedId.value = obj.id
  nodeDialog.open = false
  await saveNow()
  message.success(`${KIND_LABEL[kind]}已添加`)
}

// 切 page 时读取该 page 自己的背景设置
watch(currentPageId, (id) => {
  bgMode.value = getBg(id)
})

function chooseBg(bg: CanvasBg) {
  bgMode.value = bg
  if (currentPageId.value != null) setBg(currentPageId.value, bg)
}

const zoomLabel = computed(() => `${Math.round(zoom.value * 100)}%`)

function onCanvasMouseDown(e: MouseEvent) {
  const target = e.target as HTMLElement
  // 只在空白 / 网格层上平移
  if (!target.classList.contains('canvas-area') && !target.classList.contains('grid-layer')) return
  panning.value = true
  panStart = { x: e.clientX, y: e.clientY }
  panOrigin = { ...pan.value }
  selectedId.value = null
  window.addEventListener('mousemove', onCanvasMove)
  window.addEventListener('mouseup', onCanvasUp)
}

function onCanvasMove(e: MouseEvent) {
  if (!panning.value) return
  pan.value = {
    x: panOrigin.x + (e.clientX - panStart.x),
    y: panOrigin.y + (e.clientY - panStart.y)
  }
}

function onCanvasUp() {
  panning.value = false
  window.removeEventListener('mousemove', onCanvasMove)
  window.removeEventListener('mouseup', onCanvasUp)
}

function onWheel(e: WheelEvent) {
  const factor = e.deltaY > 0 ? 0.9 : 1.1
  zoom.value = Math.min(Math.max(zoom.value * factor, 0.25), 3)
}

function zoomIn() {
  zoom.value = Math.min(zoom.value * 1.1, 3)
}

function zoomOut() {
  zoom.value = Math.max(zoom.value * 0.9, 0.25)
}

function fitCanvas() {
  zoom.value = 1
  pan.value = { x: 0, y: 0 }
}

/* ---------------- 工具条 ---------------- */
const activeTool = ref<CanvasTool>('select')

const TOOLS: { key: CanvasTool; label: string; dividerBefore?: boolean; icon: string }[] = [
  {
    key: 'select',
    label: '选择',
    icon: `<svg viewBox="0 0 18 18" fill="none"><path d="M4 2.6 13.4 9.2l-4.1.7 2.2 4.6-1.9.9-2.2-4.6-2.4 2.3z" stroke="#6B665E" stroke-width="1.5" stroke-linejoin="round"/></svg>`
  },
  {
    key: 'person',
    label: '人物',
    dividerBefore: true,
    icon: `<svg viewBox="0 0 18 18" fill="none"><circle cx="9" cy="6.2" r="2.9" stroke="#6B665E" stroke-width="1.5"/><path d="M3.6 15.2c0-2.7 2.4-4.4 5.4-4.4s5.4 1.7 5.4 4.4" stroke="#6B665E" stroke-width="1.5" stroke-linecap="round"/></svg>`
  },
  {
    key: 'event',
    label: '事件',
    icon: `<svg viewBox="0 0 18 18" fill="none"><rect x="2.6" y="3.6" width="12.8" height="11.4" rx="2" stroke="#6B665E" stroke-width="1.5"/><path d="M2.6 7.4h12.8M6.2 2.2v2.8M11.8 2.2v2.8" stroke="#6B665E" stroke-width="1.5" stroke-linecap="round"/></svg>`
  },
  {
    key: 'thing',
    label: '事物',
    icon: `<svg viewBox="0 0 18 18" fill="none"><path d="M9 2.4 15.6 9 9 15.6 2.4 9z" stroke="#6B665E" stroke-width="1.5" stroke-linejoin="round"/></svg>`
  },
  {
    key: 'relation',
    label: '关系',
    dividerBefore: true,
    icon: `<svg viewBox="0 0 18 18" fill="none"><circle cx="4.2" cy="12.4" r="1.9" stroke="#6B665E" stroke-width="1.5"/><circle cx="13.8" cy="5.6" r="1.9" stroke="#6B665E" stroke-width="1.5"/><path d="M5.7 11.1 12.3 6.9" stroke="#6B665E" stroke-width="1.5" stroke-linecap="round"/></svg>`
  },
  {
    key: 'timeline',
    label: '时间线',
    icon: `<svg viewBox="0 0 18 18" fill="none"><path d="M2 9h14" stroke="#6B665E" stroke-width="1.5" stroke-linecap="round"/><circle cx="5.6" cy="9" r="2" fill="#6B665E"/><circle cx="12.6" cy="9" r="2" stroke="#6B665E" stroke-width="1.5"/></svg>`
  },
  {
    key: 'annotation',
    label: '注解',
    icon: `<svg viewBox="0 0 18 18" fill="none"><path d="M3.4 3.4h11.2v8.4H8.2L5 15.2v-3.4H3.4z" stroke="#6B665E" stroke-width="1.5" stroke-linejoin="round"/></svg>`
  },
  {
    key: 'relationGraph',
    label: '关系图',
    dividerBefore: true,
    icon: `<svg viewBox="0 0 18 18" fill="none"><circle cx="4.2" cy="4.2" r="2.1" stroke="#6B665E" stroke-width="1.5"/><circle cx="13.8" cy="4.2" r="2.1" stroke="#6B665E" stroke-width="1.5"/><circle cx="9" cy="13.8" r="2.1" stroke="#6B665E" stroke-width="1.5"/><path d="M5.7 5.6 7.7 11.8M12.3 5.6 10.3 11.8" stroke="#6B665E" stroke-width="1.5" stroke-linecap="round"/></svg>`
  }
]

function onToolClick(tool: CanvasTool) {
  switch (tool) {
    case 'relationGraph':
      openRelationGraphs()
      return
    case 'timeline':
      addTimeline()
      return
    case 'relation':
      // 先弹线型选择，选完再进入点选连线模式
      relPopOpen.value = !relPopOpen.value
      return
    case 'annotation':
      addAnnotation()
      return
    case 'person':
    case 'event':
    case 'thing':
      openNodeCreator(tool)
      return
    default:
      activeTool.value = tool
  }
}

/* ---------------- 对象操作 ---------------- */
const selectedId = ref<string | null>(null)
const draggingId = ref<string | null>(null)

function objectById(id: string): CanvasObject | undefined {
  return canvas.value.objects.find((o) => o.id === id)
}

let dragStartPos = { x: 0, y: 0 }
let dragOriginPos = { x: 0, y: 0 }
let draggedObj: CanvasObject | null = null

function onNodeDragStart(e: MouseEvent, obj: CanvasObject) {
  // 关系工具下：第一次点选起点，第二次点选终点后弹窗命名
  if (activeTool.value === 'relation') {
    const current = linkSourceId.value
    if (!current) {
      linkSourceId.value = obj.id
      selectedId.value = obj.id
      return
    }
    if (current === obj.id) {
      linkSourceId.value = null
      return
    }
    const source = objectById(current)
    linkSourceId.value = null
    linkHoverId.value = null
    if (source) void createRelation(source, obj)
    return
  }

  selectedId.value = obj.id
  draggingId.value = obj.id
  draggedObj = obj
  dragStartPos = { x: e.clientX, y: e.clientY }
  dragOriginPos = { x: obj.x, y: obj.y }

  window.addEventListener('mousemove', onNodeMove)
  window.addEventListener('mouseup', onNodeUp)
}

function onNodeMove(e: MouseEvent) {
  if (!draggedObj) return
  const dx = (e.clientX - dragStartPos.x) / zoom.value
  const dy = (e.clientY - dragStartPos.y) / zoom.value
  const grid = 8
  // 网格吸附
  draggedObj.x = Math.round((dragOriginPos.x + dx) / grid) * grid
  draggedObj.y = Math.round((dragOriginPos.y + dy) / grid) * grid
}

function onNodeUp() {
  draggingId.value = null
  draggedObj = null
  window.removeEventListener('mousemove', onNodeMove)
  window.removeEventListener('mouseup', onNodeUp)
  saveNow()
}

/* ---------------- 关系连线 ----------------
 * 交互链路：点工具条图标 → 弹出三种线型（选完自动收起）
 *   → 鼠标移过对象时该对象轻微放大 → 点第一个作为起点（锁定放大）
 *   → 再点第二个 → 弹窗命名并确认线型
 * ------------------------------------------ */
const relPopOpen = ref(false)
const relKind = ref<RelationshipType>('unidirectional')
const linkSourceId = ref<string | null>(null)
const linkHoverId = ref<string | null>(null)

const REL_KINDS: { key: RelationshipType; label: string }[] = [
  { key: 'unidirectional', label: '单向箭头' },
  { key: 'bidirectional', label: '双向箭头' },
  { key: 'dashed', label: '虚线箭头' }
]

const REL_KIND_LABEL = computed(
  () => REL_KINDS.find((k) => k.key === relKind.value)?.label ?? '单向箭头'
)

const linkHint = computed(() => {
  if (activeTool.value !== 'relation') return ''
  if (!linkSourceId.value) return `线型：${REL_KIND_LABEL.value} · 点一个对象作为起点`
  return '再点另一个对象完成连线 · Esc 退出'
})

/** 起点 → 当前悬停对象 的预览线 */
const linkPreview = computed(() => {
  const from = linkSourceId.value ? objectById(linkSourceId.value) : undefined
  const to = linkHoverId.value ? objectById(linkHoverId.value) : undefined
  if (!from || !to || from.id === to.id) return null
  return {
    x1: from.x + from.width / 2,
    y1: from.y + from.height / 2,
    x2: to.x + to.width / 2,
    y2: to.y + to.height / 2
  }
})

function chooseRelKind(kind: RelationshipType) {
  relKind.value = kind
  relPopOpen.value = false
  activeTool.value = 'relation'
  linkSourceId.value = null
  linkHoverId.value = null
}

function onNodeHover(id: string | null) {
  if (activeTool.value !== 'relation') return
  linkHoverId.value = id
}

function cancelLinking() {
  relPopOpen.value = false
  linkSourceId.value = null
  linkHoverId.value = null
  activeTool.value = 'select'
}

/* ---------------- 关系弹窗：命名 + 线型 ---------------- */
const relLabelInput = ref<HTMLInputElement | null>(null)

const relDialog = reactive({
  open: false,
  id: '',
  label: '',
  type: 'unidirectional' as RelationshipType,
  sourceId: '',
  targetId: ''
})

/** 两个对象都点完了 → 弹窗命名 */
async function createRelation(source: CanvasObject, target: CanvasObject) {
  relDialog.open = true
  relDialog.id = ''
  relDialog.label = ''
  relDialog.type = relKind.value
  relDialog.sourceId = source.id
  relDialog.targetId = target.id
  await nextTick()
  relLabelInput.value?.focus()
}

/** 双击关系标签 → 改名 / 改线型 */
async function openRelationDialog(rel: Relationship) {
  relDialog.open = true
  relDialog.id = rel.id
  relDialog.label = rel.label
  relDialog.type = rel.type
  relDialog.sourceId = rel.source
  relDialog.targetId = rel.target
  await nextTick()
  relLabelInput.value?.focus()
  relLabelInput.value?.select()
}

function closeRelationDialog() {
  relDialog.open = false
  linkSourceId.value = null
  linkHoverId.value = null
  activeTool.value = 'select'
}

async function submitRelationDialog() {
  const label = relDialog.label.trim() || '关系'
  const editing = !!relDialog.id

  if (editing) {
    const rel = canvas.value.relationships.find((r) => r.id === relDialog.id)
    if (rel) {
      rel.label = label
      rel.type = relDialog.type
    }
  } else {
    canvas.value.relationships.push({
      id: localId('rel'),
      source: relDialog.sourceId,
      target: relDialog.targetId,
      type: relDialog.type,
      label,
      style: {}
    })
  }

  relDialog.open = false
  linkSourceId.value = null
  linkHoverId.value = null
  activeTool.value = 'select'
  await saveNow()
  message.success(editing ? '关系已更新' : '关系已建立')
}

async function deleteRelation() {
  const id = relDialog.id
  relDialog.open = false
  linkSourceId.value = null
  linkHoverId.value = null
  activeTool.value = 'select'
  if (!id) return
  canvas.value.relationships = canvas.value.relationships.filter((r) => r.id !== id)
  await saveNow()
  message.success('关系已删除')
}

/* ---------------- 注解 ---------------- */
async function addAnnotation() {
  if (currentPageId.value == null) {
    message.warning('先选择一个 page')
    return
  }
  const ann: Annotation = {
    id: `ann_${Date.now().toString(36)}`,
    x: Math.round((560 + canvas.value.annotations.length * 30) / 8) * 8,
    y: Math.round((150 + canvas.value.annotations.length * 24) / 8) * 8,
    width: 240,
    height: 130,
    content: '<p></p>',
    style: {}
  }
  canvas.value.annotations.push(ann)
  activeTool.value = 'select'
  await saveNow()
}

const annotationObstacles = computed(() =>
  canvas.value.objects.map((o) => ({ x: o.x, y: o.y, width: o.width, height: o.height }))
)

function onAnnotationUpdate(id: string, patch: Partial<Annotation>) {
  const ann = canvas.value.annotations.find((a) => a.id === id)
  if (ann) Object.assign(ann, patch)
}

/* ---------------- 时间线 ---------------- */
async function addTimeline() {
  if (currentPageId.value == null) {
    message.warning('先选择一个 page')
    return
  }
  const tl: Timeline = {
    id: `tl_${Date.now().toString(36)}`,
    name: `时间线 ${canvas.value.timelines.length + 1}`,
    direction: 'horizontal',
    points: []
  }
  canvas.value.timelines.push(tl)
  await saveNow()
  message.success('已创建自适应时间线，可切换横向 / 纵向')
}

function onTimelineRename(id: string, name: string) {
  workspaceStore.renameTimeline(id, name)
  scheduleSave()
}

function onTimelineDirection(id: string, direction: Timeline['direction']) {
  workspaceStore.setTimelineDirection(id, direction)
  scheduleSave()
}

function onAddTimelinePoint(timelineId: string, point: TimelinePoint) {
  workspaceStore.addTimelinePoint(timelineId, point)
  scheduleSave()
}

function onUpdateTimelinePoint(
  timelineId: string,
  pointId: string,
  patch: Partial<TimelinePoint>
) {
  workspaceStore.updateTimelinePoint(timelineId, pointId, patch)
  scheduleSave()
}

function onRemoveTimelinePoint(timelineId: string, pointId: string) {
  workspaceStore.removeTimelinePoint(timelineId, pointId)
  scheduleSave()
}

/** 拖动时间线面板：面板位置属于画布数据，随画布一起存 */
function onTimelineMove(timelineId: string, x: number, y: number) {
  workspaceStore.setTimelinePosition(timelineId, x, y)
  scheduleSave()
}

/** 关闭 = 移除这条时间线 */
function onTimelineClose(timelineId: string) {
  const tl = canvas.value.timelines.find((t) => t.id === timelineId)
  Modal.confirm({
    title: '移除时间线',
    content: `「${tl?.name ?? '时间线'}」及其上已录入的时间点会被删除，此操作不可撤销。`,
    okText: '移除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      workspaceStore.removeTimeline(timelineId)
      await saveNow()
      message.success('时间线已移除')
    }
  })
}

/* ---------------- 关系图抽屉 ---------------- */
const graphDrawer = ref(false)
const graphSource = ref<'auto' | 'custom'>('auto')
const graphFilter = ref<'all' | 'person' | 'thing'>('all')
const graphView = ref<'list' | 'graph'>('list')

const GRAPH_FILTERS = [
  { value: 'all' as const, label: '全部' },
  { value: 'person' as const, label: '人物↔人物' },
  { value: 'thing' as const, label: '事物↔事物' }
]

/** 从画布推导关系图（自动提取 = 直接读当前页的 objects + relationships） */
const graphNodes = computed<GraphNode[]>(() =>
  canvas.value.objects.map((o) => ({ id: o.id, name: o.name, type: o.type }))
)

const graphEdges = computed<GraphEdge[]>(() =>
  canvas.value.relationships.map((r) => ({
    id: r.id,
    source: r.source,
    target: r.target,
    label: r.label,
    type: r.type
  }))
)

const graphRows = computed(() => {
  const rows = graphEdges.value
    .map((e) => {
      const from = graphNodes.value.find((n) => n.id === e.source)
      const to = graphNodes.value.find((n) => n.id === e.target)
      if (!from || !to) return null
      // 类型过滤
      if (graphFilter.value === 'person') {
        if (from.type !== 'person' && to.type !== 'person') return null
      } else if (graphFilter.value === 'thing') {
        if (from.type === 'person' && to.type === 'person') return null
      }
      return { id: e.id, from: from.name, to: to.name, label: e.label }
    })
    .filter((x): x is { id: string; from: string; to: string; label: string } => x !== null)
  return rows
})

function openRelationGraphs() {
  graphDrawer.value = true
}

/** 圆形布局，稳定可预期 */
function pos(id: string) {
  const nodes = graphNodes.value
  const i = nodes.findIndex((n) => n.id === id)
  const n = Math.max(nodes.length, 1)
  const cx = 200
  const cy = 230
  const r = Math.min(150, 40 + n * 14)
  const angle = (i / n) * Math.PI * 2 - Math.PI / 2
  return { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle) }
}

async function extractFromCanvas() {
  try {
    const data = await relationGraphApi.extractRelationGraph(bookId.value, {
      objectTypes: ['person', 'thing'],
      relationTypes: ['unidirectional', 'bidirectional', 'dashed']
    })
    message.success(`提取完成：${data.nodes.length} 个节点 · ${data.edges.length} 条关系`)
  } catch {
    /* 拦截器已提示 */
  }
}

/* ---------------- 族谱图 ---------------- */
function openFamilyTree() {
  message.info('族谱图基于关系图数据生成，先在关系图里建立「父母 / 子女」关系')
  graphDrawer.value = true
}

/* ---------------- 键盘 ---------------- */
function onKeyUp(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    if (relDialog.open) {
      closeRelationDialog()
      return
    }
    if (nodeDialog.open) {
      nodeDialog.open = false
      return
    }
    cancelLinking()
    return
  }

  // 正在输入时不触发删除
  const el = e.target as HTMLElement | null
  if (el && (el.isContentEditable || el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) return

  if ((e.key === 'Delete' || e.key === 'Backspace') && selectedId.value) {
    const id = selectedId.value
    canvas.value.objects = canvas.value.objects.filter((o) => o.id !== id)
    canvas.value.relationships = canvas.value.relationships.filter(
      (r) => r.source !== id && r.target !== id
    )
    selectedId.value = null
    saveNow()
  }
}

onMounted(() => {
  window.addEventListener('keyup', onKeyUp)
})

onUnmounted(() => {
  window.removeEventListener('keyup', onKeyUp)
})
</script>

<style scoped>
.workspace {
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: var(--bg-app);
}

/* ═══════════ 侧栏 ═══════════ */
.sidebar {
  display: flex;
  flex-direction: column;
  flex: none;
  width: 372px;
  background: var(--bg-bar);
}

.side-head {
  padding: 14px 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.back-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ink-3);
  font-family: inherit;
  font-size: 11px;
  cursor: pointer;
  transition: color 140ms var(--ease);
}

.back-row svg {
  width: 14px;
  height: 14px;
}

.back-row:hover {
  color: var(--ink-1);
}

.side-title {
  margin: 0;
  font-family: var(--font-serif);
  font-weight: 700;
  font-size: 18px;
  line-height: 1.35;
  color: var(--ink-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.side-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin: 0;
  font-size: 10.5px;
  color: var(--ink-4);
}

.side-edit {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ink-3);
  font-family: inherit;
  font-size: 10.5px;
  cursor: pointer;
}

.side-edit:hover {
  color: var(--green-ink);
}

.side-divider {
  height: 1px;
  background: var(--line-4);
}

/* ═══ 两列容器：180 + 192 = 372 ═══ */
.cols {
  display: flex;
  flex: 1;
  min-height: 0;
}

.col-event {
  display: flex;
  flex-direction: column;
  flex: none;
  width: 180px;
  background: var(--bg-canvas);
  padding: 12px 6px;
  gap: 2px;
}

.col-page {
  display: flex;
  flex-direction: column;
  flex: none;
  width: 192px;
  background: var(--bg-column-page);
  padding: 12px 6px;
  gap: 2px;
}

.col-head {
  flex: none;
  height: 22px;
  padding: 0 6px;
  display: flex;
  align-items: center;
  font-size: 9.5px;
  font-weight: 500;
  color: var(--ink-5);
  letter-spacing: 0.02em;
}

.col-rule {
  flex: none;
  height: 1px;
  margin: 0 6px 2px;
  background: var(--line-4);
}

.col-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.col-hint {
  margin: 8px 6px 0;
  font-size: 10.5px;
  color: var(--ink-5);
}

/* event 项 34 高 */
.ev-item {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: none;
  height: 34px;
  padding: 0 8px;
  border-radius: 7px;
  cursor: pointer;
  transition: background 120ms var(--ease);
}

.ev-item:hover {
  background: rgba(0, 0, 0, 0.025);
}

.ev-item.active {
  background: var(--green-tint-2);
}

/* ── 拖拽排序反馈 ── */
.ev-item.dragging,
.pg-item.dragging {
  opacity: 0.4;
}

.ev-item.drag-over {
  box-shadow: inset 0 2px 0 0 #7C9A88;
}

.pg-item.drag-over {
  box-shadow: inset 0 2px 0 0 #7C9A88;
}

.ev-caret {
  width: 11px;
  height: 11px;
  flex: none;
  color: var(--ink-4);
  transition: transform 140ms var(--ease);
}

.ev-item.active .ev-caret {
  color: var(--green-ink);
  transform: rotate(90deg);
}

.ev-item .item-name {
  flex: 1;
  min-width: 0;
  font-size: 11.5px;
  color: var(--ink-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ev-item.active .item-name {
  font-weight: 500;
  color: var(--green-ink-2);
}

.item-count {
  flex: none;
  font-size: 10px;
  color: var(--ink-3);
}

.ev-item.active .item-count {
  display: none;
}

/* page 项 32 高 */
.pg-item {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: none;
  height: 32px;
  padding: 0 8px;
  border-radius: 7px;
  cursor: pointer;
  transition: background 120ms var(--ease);
}

.pg-item:hover {
  background: rgba(0, 0, 0, 0.025);
}

.pg-item.active {
  background: var(--green-tint);
}

.pg-icon {
  width: 13px;
  height: 13px;
  flex: none;
  color: var(--ink-4);
}

.pg-item.active .pg-icon {
  color: var(--green-ink);
}

.pg-item .item-name {
  flex: 1;
  min-width: 0;
  font-size: 11.5px;
  color: var(--ink-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pg-item.active .item-name {
  font-weight: 500;
  color: var(--green-ink-2);
}

/* 新增行 */
.add-row {
  display: flex;
  align-items: center;
  gap: 5px;
  flex: none;
  height: 30px;
  margin-top: 2px;
  padding: 0 8px;
  border: 0;
  border-radius: var(--r-md);
  background: transparent;
  color: var(--ink-3);
  font-family: inherit;
  font-size: 10.5px;
  font-weight: 500;
  cursor: pointer;
  transition: color 140ms var(--ease), background 140ms var(--ease);
}

.add-row svg {
  width: 11px;
  height: 11px;
  transition: transform 140ms var(--ease);
}

.add-row:hover:not(:disabled) {
  color: var(--ink-1);
  background: rgba(0, 0, 0, 0.025);
}

.add-row:hover:not(:disabled) svg {
  transform: rotate(90deg);
}

.add-row:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* inline 重命名输入 */
.inline-input {
  flex: 1;
  min-width: 0;
  height: 22px;
  padding: 0 6px;
  border: 1px solid var(--green-line);
  border-radius: 5px;
  background: #fff;
  font-family: inherit;
  font-size: 11.5px;
  color: var(--ink-1);
  outline: 0;
}

/* 扩展功能入口 */
.ext-rail {
  flex: none;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 12px 12px;
  border-top: 1px solid var(--line-4);
}

.ext-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 8px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--ink-3);
  font-family: inherit;
  font-size: 11.5px;
  cursor: pointer;
  transition: color 140ms var(--ease), background 140ms var(--ease);
}

.ext-btn svg {
  width: 14px;
  height: 14px;
  flex: none;
}

.ext-btn:hover {
  color: var(--green-ink);
  background: var(--green-soft);
}

/* ═══════════ 主工作区 ═══════════ */
.main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.canvas-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex: none;
  height: 60px;
  padding: 0 20px 0 24px;
  background: var(--bg-bar);
}

.crumbs {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.crumb {
  font-size: 12.5px;
  color: var(--ink-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 220px;
}

.crumb.current {
  font-weight: 500;
  color: var(--green-ink);
}

.crumb-sep {
  width: 12px;
  height: 12px;
  flex: none;
  color: var(--ink-5);
}

.save-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: none;
  height: 32px;
  padding: 0 13px 0 11px;
  border-radius: 20px;
  background: var(--green-soft);
  color: var(--green-deep);
  font-size: 11.5px;
  font-weight: 500;
  cursor: default;
}

.save-pill svg {
  width: 12px;
  height: 12px;
}

.save-pill.error {
  background: #F4EFEA;
  color: #A0574F;
}

.spin {
  width: 11px;
  height: 11px;
  border: 1.5px solid var(--green-line);
  border-top-color: var(--green-deep);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ═══════════ 画布区 ═══════════ */
.canvas-area {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: var(--bg-canvas);
  cursor: default;
}

/* 选中 page 后，画布底色跟随笔记背景（默认纯白） */
.canvas-area.has-bg {
  background: #FFFFFF;
}

.canvas-area.bg-line {
  background: #FFFFFF;
  background-image: repeating-linear-gradient(
    to bottom,
    transparent 0,
    transparent 23px,
    #E8E9EA 23px,
    #E8E9EA 24px
  );
  background-size: 100% 24px;
}

.canvas-area.grabbing {
  cursor: grabbing;
}

.grid-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.stage {
  position: absolute;
  top: 0;
  left: 0;
  transform-origin: 0 0;
}

.edge-layer,
.node-layer {
  position: absolute;
  top: 0;
  left: 0;
  overflow: visible;
}

.edge-layer {
  pointer-events: none;
}

.tl-host {
  position: absolute;
}

/* 空态 */
.canvas-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.canvas-empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 32px 40px;
  background: var(--bg-card);
  border: 1px solid var(--line-1);
  border-radius: var(--r-2xl);
  box-shadow: var(--sh-card);
}

.canvas-empty-card svg {
  width: 40px;
  height: 40px;
}

.canvas-empty-card p {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-3);
}

.primary-btn {
  height: 32px;
  padding: 0 16px;
  border: 0;
  border-radius: var(--r-lg);
  background: var(--green);
  color: #fff;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: var(--sh-btn);
}

.primary-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

/* ═══════════ 浮动工具条 ═══════════ */
.toolbar {
  position: absolute;
  left: 20px;
  top: 150px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 52px;
  padding: 8px 6px;
  background: #fff;
  border: 1px solid #E8E3DB;
  border-radius: var(--r-2xl);
  box-shadow: var(--sh-float);
}

.tb-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: var(--r-lg);
  background: transparent;
  cursor: pointer;
  transition: background 140ms var(--ease);
}

.tb-btn :deep(svg) {
  width: 18px;
  height: 18px;
}

.tb-btn:hover {
  background: rgba(0, 0, 0, 0.025);
}

.tb-btn.active {
  background: var(--green-tint);
}

.tb-sep {
  width: 24px;
  height: 1px;
  margin: 0 auto;
  background: var(--line-3);
}

/* ═══════════ 缩放条 ═══════════ */
.zoombar {
  position: absolute;
  right: 20px;
  bottom: 20px;
  display: flex;
  align-items: center;
  gap: 2px;
  height: 34px;
  padding: 0 6px;
  background: #fff;
  border: 1px solid #E8E3DB;
  border-radius: var(--r-lg);
  box-shadow: var(--sh-zoom);
}

.zb-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: var(--r-sm);
  background: transparent;
  color: var(--ink-2);
  cursor: pointer;
  transition: background 140ms var(--ease);
}

.zb-btn svg {
  width: 12px;
  height: 12px;
}

.zb-btn:hover {
  background: rgba(0, 0, 0, 0.04);
}

.zb-value {
  min-width: 42px;
  text-align: center;
  font-size: 11.5px;
  font-weight: 500;
  color: var(--ink-2);
}

.zb-sep {
  width: 1px;
  height: 16px;
  margin: 0 2px;
  background: var(--line-3);
}

/* ═══════════ 工具按钮插槽 & 关系线型弹层 ═══════════ */
.tb-slot {
  position: relative;
  display: flex;
}

.rel-pop {
  position: absolute;
  left: calc(100% + 12px);
  top: 50%;
  transform: translateY(-50%);
  z-index: 40;
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 138px;
  padding: 7px;
  background: #fff;
  border: 1px solid #e8e3db;
  border-radius: var(--r-lg);
  box-shadow: 0 10px 26px -6px rgba(89, 84, 74, 0.2);
}

.rel-pop-title {
  padding: 2px 6px 5px;
  font-size: 9.5px;
  letter-spacing: 0.03em;
  color: var(--ink-5);
}

.rel-opt {
  display: flex;
  align-items: center;
  gap: 7px;
  height: 28px;
  padding: 0 7px;
  border: 0;
  border-radius: var(--r-sm);
  background: transparent;
  color: var(--ink-2);
  font-family: inherit;
  font-size: 11px;
  text-align: left;
  cursor: pointer;
  transition: background 120ms var(--ease), color 120ms var(--ease);
}

.rel-opt:hover {
  background: #f5f3ef;
}

.rel-opt.active {
  background: #edf2ee;
  color: #3f5b4c;
}

.rel-prev {
  width: 44px;
  height: 14px;
  flex: none;
}

/* ═══════════ 连线提示条 ═══════════ */
.link-hint {
  position: absolute;
  left: 50%;
  top: 16px;
  transform: translateX(-50%);
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 6px 0 12px;
  border-radius: 16px;
  background: rgba(253, 252, 250, 0.97);
  border: 1px solid #e8e3db;
  box-shadow: 0 6px 18px -8px rgba(72, 66, 55, 0.28);
  font-size: 11.5px;
  color: var(--ink-2);
}

.link-hint svg {
  width: 14px;
  height: 14px;
  flex: none;
  color: #5c7f6b;
}

.link-cancel {
  height: 22px;
  padding: 0 9px;
  border: 0;
  border-radius: 11px;
  background: #edeae4;
  color: var(--ink-2);
  font-family: inherit;
  font-size: 10.5px;
  cursor: pointer;
  transition: background 140ms var(--ease), color 140ms var(--ease);
}

.link-cancel:hover {
  background: #e2eae5;
  color: #3f5b4c;
}

/* ═══════════ 背景选择器 ═══════════ */
.bg-picker {
  position: relative;
  display: flex;
  align-items: center;
}

.zb-btn.active {
  background: #E9EFEB;
  color: #3F5B4C;
}

.bg-pop {
  position: absolute;
  right: 0;
  bottom: calc(100% + 10px);
  width: 168px;
  padding: 8px;
  border-radius: var(--r-lg);
  background: #fff;
  border: 1px solid #E8E3DB;
  box-shadow: 0 10px 26px -6px rgba(89, 84, 74, 0.20);
  z-index: 40;
}

.bg-pop-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 6px;
  padding: 2px 6px 7px;
  font-size: 11px;
  font-weight: 500;
  color: var(--ink-2);
}

.bg-pop-note {
  font-size: 9.5px;
  font-weight: 400;
  color: var(--ink-5);
}

.bg-item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  height: 30px;
  padding: 0 6px;
  border: 0;
  border-radius: var(--r-sm);
  background: transparent;
  font-family: inherit;
  font-size: 11.5px;
  color: var(--ink-2);
  cursor: pointer;
  text-align: left;
  transition: background 140ms var(--ease);
}

.bg-item:hover {
  background: #F5F3EF;
}

.bg-item.active {
  background: #EDF2EE;
  color: #3F5B4C;
}

.bg-swatch {
  flex: none;
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid #E4DFD6;
}

.bg-label {
  flex: 1;
}

.bg-check {
  flex: none;
  width: 12px;
  height: 12px;
  color: #5C7F6B;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 140ms var(--ease), transform 140ms var(--ease);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}

/* ═══════════ 关系图抽屉 ═══════════ */
.drawer {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 30;
  display: flex;
  flex-direction: column;
  width: 420px;
  background: var(--bg-bar);
  border-left: 1px solid var(--line-1);
  box-shadow: -8px 0 24px rgba(89, 84, 74, 0.10);
}

.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex: none;
  height: 56px;
  padding: 0 16px 0 20px;
  border-bottom: 1px solid var(--line-4);
}

.drawer-title {
  margin: 0;
  font-family: var(--font-serif);
  font-weight: 700;
  font-size: 15px;
  color: var(--ink-1);
}

.drawer-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: var(--r-md);
  background: transparent;
  color: var(--ink-3);
  cursor: pointer;
}

.drawer-close svg {
  width: 12px;
  height: 12px;
}

.drawer-close:hover {
  background: rgba(0, 0, 0, 0.04);
}

.drawer-tools {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: none;
  padding: 14px 20px;
  border-bottom: 1px solid var(--line-4);
}

.tool-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-label {
  flex: none;
  width: 32px;
  font-size: 10.5px;
  color: var(--ink-4);
}

.mini-select {
  flex: 1;
  height: 30px;
  padding: 0 8px;
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
  flex-wrap: wrap;
  gap: 6px;
}

.pill {
  height: 28px;
  padding: 0 10px;
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
  border-radius: 6px;
  background: #F0EEEA;
  align-self: flex-end;
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

.drawer-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.drawer-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 24px 0;
  font-size: 12.5px;
  color: var(--ink-4);
}

.link-btn {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--green-ink);
  font-family: inherit;
  font-size: 12.5px;
  text-decoration: underline;
  cursor: pointer;
}

.rel-row {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 12px;
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

.rel-row .rel-name:last-child {
  text-align: right;
}

.rel-word {
  flex: none;
  padding: 3px 9px;
  border-radius: 5px;
  background: var(--event-fill);
  color: #6B665E;
  font-size: 10.5px;
}

.graph-body {
  align-items: center;
}

.graph-svg {
  display: block;
}

.graph-node-label {
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 500;
  fill: #2E3D34;
  pointer-events: none;
}

.graph-empty {
  font-family: var(--font-sans);
  font-size: 12.5px;
  fill: var(--ink-5);
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 200ms var(--ease);
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}

/* ═══════════ 弹窗 ═══════════ */
.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(46, 44, 41, 0.24);
}

.dialog {
  width: 400px;
  padding: 22px;
  background: var(--bg-card);
  border-radius: var(--r-2xl);
  box-shadow: var(--sh-panel);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dialog-title {
  margin: 0;
  font-family: var(--font-serif);
  font-weight: 700;
  font-size: 18px;
  color: var(--ink-1);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 11.5px;
  font-weight: 500;
  color: var(--ink-3);
}

.field-input {
  height: 38px;
  padding: 0 12px;
  border: 1px solid var(--line-1);
  border-radius: var(--r-lg);
  background: #fff;
  font-family: inherit;
  font-size: 13px;
  color: var(--ink-2);
  outline: 0;
  transition: border-color 160ms var(--ease);
}

.field-input:focus {
  border-color: var(--green-line);
}

.field-input::placeholder {
  color: var(--ink-5);
}

/* 多行备注 */
.field-area {
  height: auto;
  min-height: 66px;
  padding: 9px 12px;
  line-height: 1.55;
  resize: vertical;
}

.dialog-tip {
  margin: -2px 0 0;
  font-size: 10.5px;
  color: var(--ink-5);
}

/* 关系线型三选一 */
.rel-kind-row {
  display: flex;
  gap: 8px;
}

.rel-kind-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 9px 4px 7px;
  border: 1px solid var(--line-1);
  border-radius: var(--r-md);
  background: #fff;
  color: var(--ink-3);
  font-family: inherit;
  font-size: 10.5px;
  cursor: pointer;
  transition: border-color 140ms var(--ease), background 140ms var(--ease), color 140ms var(--ease);
}

.rel-kind-btn svg {
  width: 40px;
  height: 13px;
}

.rel-kind-btn:hover {
  border-color: #c8d4cc;
}

.rel-kind-btn.active {
  border-color: #7c9a88;
  background: #edf2ee;
  color: #3f5b4c;
}

.actions-spacer {
  flex: 1;
}

.ghost-btn.danger-btn {
  color: #a8756b;
}

.ghost-btn.danger-btn:hover {
  background: #f5ebe8;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.ghost-btn,
.dialog .primary-btn {
  height: 36px;
  padding: 0 18px;
  border-radius: var(--r-lg);
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
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

/* ═══════════ 右键菜单 ═══════════ */
.ctx-menu {
  position: fixed;
  z-index: 300;
  min-width: 132px;
  padding: 4px;
  background: var(--bg-card);
  border: 1px solid var(--line-1);
  border-radius: var(--r-lg);
  box-shadow: var(--sh-card);
}

.ctx-menu button {
  display: block;
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: var(--r-md);
  background: transparent;
  color: var(--ink-2);
  font-family: inherit;
  font-size: 12.5px;
  text-align: left;
  cursor: pointer;
}

.ctx-menu button:hover {
  background: rgba(0, 0, 0, 0.03);
}

.ctx-menu button.danger {
  color: #A0574F;
}
</style>
