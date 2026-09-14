<template>
  <div class="workspace">
    <!-- ═══════════════ 左侧栏 ═══════════════ -->
    <aside class="sidebar" :style="{ width: `${sideW}px` }">
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

      <!-- ═══ 两列容器：event 列 + page 列，中间分隔可拖宽 ═══ -->
      <div class="cols">
        <!-- ── event 列 ── -->
        <section class="col-event" :style="{ width: `${eventColW}px` }">
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

        <!-- ── 列宽拖柄 ── -->
        <div class="col-resize" title="拖动调整列宽" @mousedown.stop.prevent="startColResize" />

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
        <button class="ext-btn" type="button" @click="openExt('relation')">
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
        <button class="ext-btn" type="button" @click="openExt('family')">
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

    <!-- 侧栏整体宽度拖柄：拖动时 event + page 两列一起变宽 / 变窄 -->
    <div
      class="side-resize"
      :class="{ active: sideResizing }"
      title="拖动调整侧栏宽度"
      @mousedown.stop.prevent="startSideResize"
    />

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

      <!-- 画布区：外框固定，内容在 .canvas-viewport 里滚 -->
      <div
        ref="canvasRef"
        class="canvas-area"
        :class="{
          grabbing: contentMoving || viewPanning,
          'mode-content': activeTool === 'moveContent'
        }"
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
          <!-- 滚动视口：画布尺寸大于可视区域时，超出部分在这里滚（滚轮 / 滚动条） -->
          <div ref="canvasScrollRef" class="canvas-viewport">
            <!-- 占位层：尺寸 = 纸张 ∪ 内容包围盒（含负坐标的历史内容），滚动条按它算 -->
            <div
              class="canvas-scroll"
              :style="{ width: `${scrollBox.w}px`, height: `${scrollBox.h}px` }"
            >
              <!-- 原点层：画布 (0,0) 在滚动盒子里的落点。纸张 / 网格 / 内容都挂在这里，
                   内容坐标一个不用改，负坐标的老对象也能滚到 -->
              <div
                class="canvas-origin"
                :style="{
                  left: `${scrollBox.offsetX}px`,
                  top: `${scrollBox.offsetY}px`
                }"
              >
              <!-- 纸张边界：这张「纸」多大，内容的活动范围就是多大 -->
              <div
                class="canvas-paper"
                :style="{ width: `${stageScaledW}px`, height: `${stageScaledH}px` }"
              />

              <!-- 背景层：纯色 / 点阵 / 方格 / 横线 -->
              <svg
                v-if="bgMode !== 'plain'"
                class="grid-layer"
                :width="stageScaledW"
                :height="stageScaledH"
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
                :width="stageScaledW"
                :height="24 * zoom"
                patternUnits="userSpaceOnUse"
              >
                <line
                  x1="0"
                  :y1="24 * zoom"
                  :x2="stageScaledW"
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

          <!-- 内容层（缩放，位置由 .canvas-scroll 承载滚动） -->
          <div
            class="stage"
            :style="{
              transform: `scale(${zoom})`
            }"
          >
            <!-- 连线 -->
            <svg
              class="edge-layer"
              :width="stageW"
              :height="stageH"
            >
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
                :offset="relOffsets[rel.id] ?? 0"
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
            <svg
              class="node-layer"
              :width="stageW"
              :height="stageH"
            >
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
                @resize="onTimelineResize"
                @close="onTimelineClose"
              />
            </div>
              </div>
            </div>
          </div>
          </div>

          <!-- 浮动工具条：最上面的箭头就是握把，按住可拖动整条工具栏 -->
          <div
            ref="toolbarRef"
            class="toolbar"
            :class="{ 'tb-dragging': tbDragging, 'tb-pinned': toolbarPinned }"
            :style="{ left: `${toolbarPos.x}px`, top: `${toolbarPos.y}px` }"
          >
            <template v-for="tool in TOOLS" :key="tool.key">
              <div v-if="tool.dividerBefore" class="tb-sep" />
              <div class="tb-slot">
                <button
                  class="tb-btn"
                  type="button"
                  :class="{
                    active:
                      activeTool === tool.key ||
                      (tool.key === 'relation' && relPopOpen) ||
                      (tool.key === 'canvasSize' && sizePopOpen),
                    grip: tool.key === 'select'
                  }"
                  :title="tool.key === 'select' ? GRIP_TITLE : tool.label"
                  @mousedown.stop="onToolMouseDown($event, tool.key)"
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

                <!-- 画布尺寸：改这张「纸」多大；内容坐标不动，超出部分用滚轮看 -->
                <transition name="fade">
                  <div
                    v-if="tool.key === 'canvasSize' && sizePopOpen"
                    class="size-pop"
                    @mousedown.stop
                  >
                    <div class="size-pop-title">画布尺寸</div>
                    <div class="size-fields">
                      <label class="size-field">
                        <span class="size-field-label">宽</span>
                        <input
                          v-model.number="sizeDraft.w"
                          class="size-input"
                          type="number"
                          :min="MIN_CANVAS_W"
                          :max="MAX_CANVAS_W"
                          @keyup.enter="applyCanvasSize"
                        />
                      </label>
                      <span class="size-x">×</span>
                      <label class="size-field">
                        <span class="size-field-label">高</span>
                        <input
                          v-model.number="sizeDraft.h"
                          class="size-input"
                          type="number"
                          :min="MIN_CANVAS_H"
                          :max="MAX_CANVAS_H"
                          @keyup.enter="applyCanvasSize"
                        />
                      </label>
                    </div>

                    <div class="size-pop-sub">常用尺寸</div>
                    <button
                      v-for="p in CANVAS_PRESETS"
                      :key="p.key"
                      type="button"
                      class="rel-opt size-opt"
                      :class="{ active: sizeDraft.w === p.w && sizeDraft.h === p.h }"
                      @click="pickSizePreset(p)"
                    >
                      <span>{{ p.label }}</span>
                      <span class="size-opt-value">{{ p.w }} × {{ p.h }}</span>
                    </button>

                    <div class="size-pop-foot">
                      <button type="button" class="size-btn ghost" @click="resetCanvasSize">
                        恢复默认
                      </button>
                      <button
                        type="button"
                        class="size-btn primary"
                        :disabled="!sizeDirty"
                        @click="applyCanvasSize"
                      >
                        应用
                      </button>
                    </div>
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
                    <span class="bg-pop-note">随笔记保存</span>
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

    <!-- ═══════════════ 右侧：关系图 / 族谱图 抽屉 ═══════════════ -->
    <transition name="slide">
      <div
        v-if="extDrawer"
        ref="drawerRef"
        class="drawer"
        :class="{ 'dr-resizing': drawerResizing }"
        :style="{
          width: `${drawerW}px`,
          height: drawerH == null ? undefined : `${drawerH}px`,
          bottom: drawerH == null ? '0' : 'auto'
        }"
      >
        <!-- 尺寸拖柄：左缘调宽、下缘调高、左下角两者一起 -->
        <div class="dr-grip dr-grip-x" title="拖动调整宽度" @mousedown="onDrawerResizeStart($event, 'x')" />
        <div class="dr-grip dr-grip-y" title="拖动调整高度" @mousedown="onDrawerResizeStart($event, 'y')" />
        <div
          class="dr-grip dr-grip-xy"
          title="拖动同时调整宽度与高度"
          @mousedown="onDrawerResizeStart($event, 'both')"
        />

        <div class="drawer-head">
          <button
            v-if="extMode === 'detail'"
            class="drawer-back"
            type="button"
            title="返回列表"
            @click="backToExtList"
          >
            <svg viewBox="0 0 14 14" fill="none">
              <path d="M8.6 3 4.6 7l4 4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </button>
          <h2 class="drawer-title">{{ extTitle }}</h2>
          <button class="drawer-size-reset" type="button" title="恢复默认尺寸" @click="resetDrawerSize">
            <svg viewBox="0 0 14 14" fill="none">
              <path d="M2.6 5.2v-2.6h2.6M11.4 8.8v2.6H8.8" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M11 5.4 8.2 8.2M3 8.6l2.8-2.8" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" opacity="0.5" />
            </svg>
          </button>
          <button class="drawer-close" type="button" @click="extDrawer = false">
            <svg viewBox="0 0 12 12" fill="none">
              <path d="m3 3 6 6M9 3l-6 6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
            </svg>
          </button>
        </div>

        <!-- ══ 列表层：已有图 + 新增 ══ -->
        <template v-if="extMode === 'list'">
          <div class="drawer-actions">
            <button class="primary-btn" type="button" :disabled="extLoading" @click="createExt">
              <svg viewBox="0 0 12 12" fill="none">
                <path d="M6 2.2v7.6M2.2 6h7.6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
              </svg>
              新增{{ extKind === 'relation' ? '关系图' : '族谱图' }}
            </button>
          </div>

          <div class="drawer-body">
            <p v-if="!extList.length && !extLoading" class="drawer-empty">
              <template v-if="extKind === 'relation'">
                还没有关系图，点上方「新增关系图」创建，
                <br />会自动提取画布里已有的对象与关系。
              </template>
              <template v-else>
                还没有族谱图，点上方「新增族谱图」创建。
                <br />族谱不做自动提取，成员全部手工添加。
              </template>
            </p>
            <button
              v-for="g in extList"
              :key="g.id"
              type="button"
              class="graph-row"
              @click="openExtDetail(g)"
            >
              <svg v-if="extKind === 'relation'" viewBox="0 0 18 18" fill="none">
                <circle cx="5.4" cy="5.4" r="2.6" stroke="currentColor" stroke-width="1.3" />
                <circle cx="12.6" cy="5.4" r="2.6" stroke="currentColor" stroke-width="1.3" />
                <circle cx="9" cy="13.4" r="2.6" stroke="currentColor" stroke-width="1.3" />
                <path d="M6.6 7.3 8.1 10.9M11.4 7.3 9.9 10.9" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
              </svg>
              <svg v-else viewBox="0 0 18 18" fill="none">
                <rect x="6.6" y="1.6" width="4.8" height="4" rx="1.2" stroke="currentColor" stroke-width="1.3" />
                <rect x="1.6" y="12.4" width="4.8" height="4" rx="1.2" stroke="currentColor" stroke-width="1.3" />
                <rect x="11.6" y="12.4" width="4.8" height="4" rx="1.2" stroke="currentColor" stroke-width="1.3" />
                <path d="M9 5.6v3.2M4 12.4V8.8h10v3.6" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <span class="graph-row-name">{{ g.name }}</span>
              <span class="graph-row-time">{{ fmtGraphTime(g.updatedAt) }}</span>
            </button>
          </div>
        </template>

        <!-- ══ 详情层：查看 / 编辑某一张图（不影响画布） ══ -->
        <template v-else>
          <div class="drawer-tools">
            <div class="tool-group">
              <span class="tool-label">{{ extKind === 'relation' ? '类型' : '规模' }}</span>
              <div v-if="extKind === 'relation'" class="pill-group">
                <button
                  v-for="f in GRAPH_FILTERS"
                  :key="f.value"
                  type="button"
                  class="pill"
                  :class="{ active: graphFilter === f.value }"
                  @click="setGraphFilter(f.value)"
                >
                  {{ f.label }}
                </button>
              </div>
              <span v-else class="graph-meta">
                共 {{ familyLayout.generations }} 代 · {{ extNodes.length }} 位成员
              </span>
            </div>

            <button
              v-if="extKind === 'relation'"
              class="ghost-btn xs extract-btn"
              type="button"
              :disabled="extLoading"
              @click="reextractActiveGraph"
            >
              <svg viewBox="0 0 12 12" fill="none">
                <path d="M9.6 4.2A3.6 3.6 0 1 0 9.9 7.2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
                <path d="M9.9 1.6v2.8H7.1" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              从画布重新提取
            </button>

            <div class="dir-switch">
              <button
                type="button"
                class="dir-seg"
                :class="{ active: extView === 'list' }"
                @click="extView = 'list'"
              >
                列表
              </button>
              <button
                type="button"
                class="dir-seg"
                :class="{ active: extView === 'graph' }"
                @click="extView = 'graph'"
              >
                图形
              </button>
            </div>
          </div>

          <!-- 列表视图（可删） -->
          <div v-if="extView === 'list'" class="drawer-body">
            <div v-if="!extRows.length" class="drawer-empty">
              {{
                extKind === 'relation'
                  ? relFilterEmpty
                    ? '这个筛选下没有关系'
                    : '这张图还没有关系'
                  : '还没有亲属关系'
              }}
            </div>
            <div v-for="row in extRows" :key="row.id" class="rel-row">
              <span class="rel-name" :title="row.from">{{ row.from }}</span>
              <span class="rel-word">{{ row.label }}</span>
              <span class="rel-name" :title="row.to">{{ row.to }}</span>
              <button class="row-del" type="button" title="删除这条关系" @click="removeExtRelation(row.id)">
                <svg viewBox="0 0 12 12" fill="none">
                  <path d="m3.2 3.2 5.6 5.6M8.8 3.2 3.2 8.8" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
                </svg>
              </button>
            </div>

            <!-- ① 对象 / 成员：先有人 / 物，才谈得上关系 -->
            <div class="edge-add">
              <div class="edge-add-title">
                {{ extKind === 'relation' ? '对象' : '成员' }}
                <span class="edge-add-note">{{ extNodes.length }} 个</span>
              </div>
              <div class="edge-add-row">
                <input
                  v-model="nodeDraft.name"
                  class="mini-input"
                  :placeholder="extKind === 'relation' ? '名称，如 张远山' : '姓名，如 沈砚清'"
                  @keyup.enter="addExtNode"
                />
                <a-select
                  v-if="extKind === 'relation'"
                  v-model:value="nodeDraft.type"
                  class="mini-select mini-select-s"
                  size="small"
                  :dropdown-match-select-width="false"
                >
                  <a-select-option value="person">人物</a-select-option>
                  <a-select-option value="thing">事物</a-select-option>
                  <a-select-option value="event">事件</a-select-option>
                </a-select>
                <a-select
                  v-else
                  v-model:value="nodeDraft.gender"
                  class="mini-select mini-select-s"
                  size="small"
                  :dropdown-match-select-width="false"
                >
                  <a-select-option value="male">男</a-select-option>
                  <a-select-option value="female">女</a-select-option>
                  <a-select-option value="unknown">不详</a-select-option>
                </a-select>
                <button
                  class="primary-btn sm"
                  type="button"
                  :disabled="!nodeDraft.name.trim()"
                  @click="addExtNode"
                >
                  添加
                </button>
              </div>
              <!-- 族谱额外记生卒年，便于按年代核对辈分 -->
              <div v-if="extKind === 'family'" class="edge-add-row">
                <input v-model="nodeDraft.birth" class="mini-input" placeholder="生年，如 1901" />
                <input v-model="nodeDraft.death" class="mini-input" placeholder="卒年，可留空" />
              </div>
              <div v-if="extNodes.length" class="node-chips">
                <span
                  v-for="n in extNodes"
                  :key="n.id"
                  class="node-chip"
                  :class="extKind === 'family' ? `g-${n.gender ?? 'unknown'}` : `k-${n.kind}`"
                >
                  {{ n.name }}
                  <button type="button" title="删除该对象及其关系" @click="removeExtNode(n.id)">
                    <svg viewBox="0 0 10 10" fill="none">
                      <path d="m2.4 2.4 5.2 5.2M7.6 2.4 2.4 7.6" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
                    </svg>
                  </button>
                </span>
              </div>
              <p v-else class="edge-add-empty">
                {{ extKind === 'relation' ? '还没有对象，先在上面加一个' : '还没有成员，先在上面加一位' }}
              </p>
            </div>

            <!-- ② 关系 -->
            <div class="edge-add">
              <div class="edge-add-title">
                {{ extKind === 'relation' ? '关系' : '亲属关系' }}
                <span class="edge-add-note">{{ extEdges.length }} 条</span>
              </div>
              <div class="edge-add-row">
                <a-select
                  v-model:value="edgeSourceModel"
                  class="mini-select"
                  size="small"
                  :placeholder="extKind === 'relation' ? '起点' : '父 / 母'"
                  :dropdown-match-select-width="false"
                >
                  <a-select-option v-for="n in extNodes" :key="n.id" :value="n.id">
                    {{ n.name }}
                  </a-select-option>
                </a-select>
                <span class="edge-arr">→</span>
                <a-select
                  v-model:value="edgeTargetModel"
                  class="mini-select"
                  size="small"
                  :placeholder="extKind === 'relation' ? '终点' : '子女 / 配偶'"
                  :dropdown-match-select-width="false"
                >
                  <a-select-option v-for="n in extNodes" :key="n.id" :value="n.id">
                    {{ n.name }}
                  </a-select-option>
                </a-select>
              </div>
              <div class="edge-add-row">
                <template v-if="extKind === 'relation'">
                  <input v-model="edgeDraft.label" class="mini-input" placeholder="关系说明，如 父子" />
                  <a-select
                    v-model:value="edgeDraft.type"
                    class="mini-select mini-select-s"
                    size="small"
                    :dropdown-match-select-width="false"
                  >
                    <a-select-option value="unidirectional">单向</a-select-option>
                    <a-select-option value="bidirectional">双向</a-select-option>
                    <a-select-option value="dashed">虚线</a-select-option>
                  </a-select>
                </template>
                <a-select
                  v-else
                  v-model:value="familyRelType"
                  class="mini-select"
                  size="small"
                  :dropdown-match-select-width="false"
                >
                  <a-select-option value="parent-child">父母 → 子女</a-select-option>
                  <a-select-option value="spouse">配偶</a-select-option>
                </a-select>
                <button
                  class="primary-btn sm"
                  type="button"
                  :disabled="!canAddExtRelation"
                  @click="addExtRelation"
                >
                  添加
                </button>
              </div>
              <p class="edge-add-hint">
                {{
                  extKind === 'relation'
                    ? '同一对对象可以有多条关系，图形里会自动平行排开'
                    : '族谱按「一代一行」排布：父母在上、子女在下，配偶并排'
                }}
              </p>
            </div>
          </div>

          <!-- 图形视图 -->
          <div v-else class="drawer-body graph-body">
            <!-- ══ 关系图：中心切换式排布 ══ -->
            <template v-if="extKind === 'relation'">
              <div class="graph-hint">
                <span>点任一对象，把它切成中心</span>
                <button v-if="focusStack.length" class="link-btn" type="button" @click="backFocus">
                  返回上一个中心
                </button>
                <button
                  v-if="relFilterEmpty"
                  class="link-btn"
                  type="button"
                  @click="graphFilter = 'all'"
                >
                  显示全部
                </button>
              </div>
              <div :ref="bindGraphCanvas" class="graph-canvas">
                <svg :width="graphBox.w" :height="graphBox.h" class="graph-svg">
                  <defs>
                    <marker id="drawer-arrow" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto">
                      <polygon points="0 0, 9 4.5, 0 9" fill="#8C877E" />
                    </marker>
                  </defs>
                  <g v-for="e in drawerEdgeGeoms" :key="e.id">
                    <line
                      :x1="e.x1"
                      :y1="e.y1"
                      :x2="e.x2"
                      :y2="e.y2"
                      stroke="#8C877E"
                      stroke-width="1"
                      :stroke-dasharray="e.dash ? '5 4' : undefined"
                      marker-end="url(#drawer-arrow)"
                    />
                    <text :x="e.lx" :y="e.ly" text-anchor="middle" class="edge-label">
                      {{ e.label }}
                    </text>
                  </g>

                  <!-- 人物：圆形 + 名字 / 事件：方框 / 事物：菱形 —— 三态在所有视图里保持一致 -->
                  <g
                    v-for="n in drawerNodes"
                    :key="n.id"
                    class="graph-node"
                    :class="{ focus: n.focus }"
                    @click="setFocus(n.id)"
                  >
                    <template v-if="n.kind === 'person'">
                      <circle :cx="n.x" :cy="n.y" :r="n.r" class="gn-shape k-person" />
                      <text :x="n.x" :y="n.y + 4" text-anchor="middle" class="node-name">
                        {{ n.name }}
                      </text>
                    </template>
                    <template v-else-if="n.kind === 'event'">
                      <rect
                        :x="n.x - 52"
                        :y="n.y - 17"
                        width="104"
                        height="34"
                        rx="8"
                        class="gn-shape k-event"
                      />
                      <text :x="n.x" :y="n.y + 4" text-anchor="middle" class="node-name dark">
                        {{ n.name }}
                      </text>
                    </template>
                    <template v-else>
                      <path :d="diamondPath(n.x, n.y, 62, 30)" class="gn-shape k-thing" />
                      <text :x="n.x" :y="n.y + 4" text-anchor="middle" class="node-name dark">
                        {{ n.name }}
                      </text>
                    </template>
                  </g>

                  <text
                    v-if="!drawerNodes.length"
                    :x="graphBox.w / 2"
                    :y="graphBox.h / 2"
                    text-anchor="middle"
                    class="graph-empty"
                  >
                    {{
                      !extNodes.length
                        ? '还没有对象'
                        : graphFilter === 'all'
                          ? '这张图还没有关系'
                          : '这个筛选下没有关系'
                    }}
                  </text>
                </svg>
              </div>

              <div class="gn-legend">
                <span class="gn-key k-person" />人物
                <span class="gn-key k-thing" />事物
                <span class="gn-key k-event" />事件
                <span class="gn-sep" />
                实心环 = 当前中心
              </div>
            </template>

            <!-- ══ 族谱图：一代一行 ══ -->
            <template v-else>
              <div class="graph-hint">
                <span>共 {{ familyLayout.generations }} 代 · 父母在上、子女在下</span>
                <span v-if="familyScale < 1" class="graph-scale-note">
                  已缩放 {{ Math.round(familyScale * 100) }}%
                </span>
              </div>
              <div :ref="bindGraphCanvas" class="graph-canvas graph-canvas-scroll">
                <svg
                  :width="familyLayout.width * familyScale"
                  :height="familyLayout.height * familyScale"
                  :viewBox="`0 0 ${familyLayout.width} ${familyLayout.height}`"
                  class="graph-svg"
                >
                  <g v-for="e in familyLayout.edges" :key="e.id">
                    <path :d="e.path" class="fam-edge" :class="e.kind" />
                  </g>
                  <circle
                    v-for="s in familyLayout.spouses"
                    :key="s.id"
                    :cx="s.x"
                    :cy="s.y"
                    r="3.2"
                    class="fam-spouse"
                  />
                  <g v-for="n in familyLayout.nodes" :key="n.id">
                    <rect
                      :x="n.x"
                      :y="n.y"
                      :width="n.w"
                      :height="n.h"
                      rx="10"
                      class="fam-node"
                      :class="`g-${n.gender}`"
                    />
                    <text
                      :x="n.x + n.w / 2"
                      :y="n.y + (n.meta ? 16 : 21)"
                      text-anchor="middle"
                      class="fam-name"
                    >
                      {{ n.name }}
                    </text>
                    <text
                      v-if="n.meta"
                      :x="n.x + n.w / 2"
                      :y="n.y + 28"
                      text-anchor="middle"
                      class="fam-meta"
                    >
                      {{ n.meta }}
                    </text>
                  </g>
                  <text
                    v-if="!familyLayout.nodes.length"
                    x="190"
                    y="120"
                    text-anchor="middle"
                    class="graph-empty"
                  >
                    还没有成员，切到「列表」添加
                  </text>
                </svg>
              </div>

              <div class="gn-legend">
                <span class="gn-key g-male" />男
                <span class="gn-key g-female" />女
                <span class="gn-key g-unknown" />不详
                <span class="gn-sep" />
                实线 = 亲子 · 圆点 = 配偶
              </div>
            </template>
          </div>

          <div class="drawer-foot">
            <button class="ghost-btn danger-btn" type="button" @click="removeExt">
              删除这张{{ extKind === 'relation' ? '关系图' : '族谱图' }}
            </button>
            <span class="drawer-foot-note">
              {{ extKind === 'relation' ? '这里的内容不会改动画布' : '族谱已存到云端，换设备也能打开' }}
            </span>
          </div>
        </template>
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
import { familyTreeApi } from '@/api/familyTree'
import type {
  Annotation,
  CanvasObject,
  CanvasTool,
  EventResponse,
  FamilyTreeResponse,
  ObjectShape,
  RelationGraphData,
  RelationGraphDetailResponse,
  RelationGraphResponse,
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
import {
  CANVAS_PRESETS,
  DEFAULT_CANVAS_H,
  DEFAULT_CANVAS_W,
  MAX_CANVAS_H,
  MAX_CANVAS_W,
  MIN_CANVAS_H,
  MIN_CANVAS_W,
  clampSize,
  getLocalSize,
  setLocalSize,
  type CanvasPreset
} from '@/utils/canvasSize'
import { bundleOffsets, normalFlip } from '@/utils/edgeBundle'
import { neighborsOf, nodeShapeOf, personRadius, radialLayout, ringRadius } from '@/utils/graphLayout'
import {
  layoutFamilyTree,
  newMemberId,
  newRelationId,
  type FamilyGender,
  type FamilyMember,
  type FamilyRelation,
  type FamilyRelType,
  type FamilyTree
} from '@/utils/familyTree'

const route = useRoute()
const router = useRouter()
const workspaceStore = useWorkspaceStore()
const bookStore = useBookStore()

const bookId = computed(() => Number(route.params.bookId))

const canvasRef = ref<HTMLElement | null>(null)
/** 画布滚动容器：滚动条挂在这里，尺寸 = 画布尺寸 × 缩放 */
const canvasScrollRef = ref<HTMLElement | null>(null)

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

/* ---------------- 画布尺寸 ----------------
 * 画布是一张有明确尺寸的「纸」，尺寸随画布存后端（canvasWidth / canvasHeight）；
 * 后端还没这个字段时（首屏 / 老数据）回落到按 page 隔离的本机兜底值。
 * 尺寸之外的内容不再挤压边界，而是由 .canvas-scroll 容器滚。
 * ------------------------------------------ */
const stageW = computed(
  () => canvas.value.canvasWidth ?? getLocalSize(currentPageId.value).w
)
const stageH = computed(
  () => canvas.value.canvasHeight ?? getLocalSize(currentPageId.value).h
)
/** 缩放后的纸张占位尺寸 */
const stageScaledW = computed(() => Math.round(stageW.value * zoom.value))
const stageScaledH = computed(() => Math.round(stageH.value * zoom.value))

/**
 * 滚动范围 = 纸张 ∪ 内容包围盒（各留一圈余量）。
 *
 * 为什么要连带内容一起算：历史数据里可能有落在纸张之外、甚至坐标为负的对象
 * （上一版无限画布是允许往左上拖的）。固定原点的话负坐标永远滚不到，对象就"消失"了。
 * 所以这里给滚动盒子一个原点偏移：画布 (0,0) 落在盒子里的 offsetX / offsetY 处，
 * 纸张与内容层都按这个偏移定位，坐标本身一个都不用改。
 */
const SCROLL_PAD = 140

const scrollBox = computed(() => {
  let minX = 0
  let minY = 0
  let maxX = stageW.value
  let maxY = stageH.value
  const grow = (x: number, y: number, w: number, h: number) => {
    if (!Number.isFinite(x) || !Number.isFinite(y)) return
    minX = Math.min(minX, x)
    minY = Math.min(minY, y)
    maxX = Math.max(maxX, x + (Number.isFinite(w) ? w : 0))
    maxY = Math.max(maxY, y + (Number.isFinite(h) ? h : 0))
  }
  for (const o of canvas.value.objects) grow(o.x, o.y, o.width, o.height)
  for (const a of canvas.value.annotations) grow(a.x, a.y, a.width, a.height)
  for (const t of canvas.value.timelines) {
    grow(t.x ?? 0, t.y ?? 0, t.width ?? 760, t.height ?? 300)
  }

  const ox = Math.min(0, minX) - SCROLL_PAD
  const oy = Math.min(0, minY) - SCROLL_PAD
  const z = zoom.value || 1
  return {
    /** 画布 (0,0) 在滚动盒子里的位置（屏幕 px） */
    offsetX: Math.round(-ox * z),
    offsetY: Math.round(-oy * z),
    w: Math.max(1, Math.round((maxX + SCROLL_PAD - ox) * z)),
    h: Math.max(1, Math.round((maxY + SCROLL_PAD - oy) * z))
  }
})

/** 把视口滚到纸张左上角（切页 / 改尺寸 / 适配时用） */
function scrollToPaper() {
  requestAnimationFrame(() => {
    const v = canvasScrollRef.value
    if (!v) return
    const b = scrollBox.value
    v.scrollLeft = b.offsetX
    v.scrollTop = b.offsetY
  })
}

/** 当前可视区域中心对应的画布坐标（新对象落位用） */
function viewportCenterInCanvas(): { x: number; y: number } {
  const v = canvasScrollRef.value
  const z = zoom.value || 1
  if (!v) return { x: stageW.value / 2, y: stageH.value / 2 }
  const b = scrollBox.value
  return {
    x: (v.scrollLeft + v.clientWidth / 2 - b.offsetX) / z,
    y: (v.scrollTop + v.clientHeight / 2 - b.offsetY) / z
  }
}

/* ---------------- 初始化 ---------------- */
onMounted(async () => {
  await workspaceStore.loadWorkspace(bookId.value)
  scrollToPaper()
  document.addEventListener('click', closeCtxMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeCtxMenu)
})

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

/* ---------------- 侧栏宽度调整 ----------------
 * 两个拖柄各管一段：
 *  ① 侧栏右缘 → 改整条侧栏宽度（event + page 两列一起变）
 *  ② event / page 中间 → 只改 event 列宽，page 列自动占满剩下的
 * ------------------------------------------ */
const COL_W_KEY = 'wb.colW.event'
const SIDE_W_KEY = 'wb.colW.side'
const eventColW = ref(Number(localStorage.getItem(COL_W_KEY)) || 180)
const sideW = ref(Number(localStorage.getItem(SIDE_W_KEY)) || 372)
const sideResizing = ref(false)
const SIDE_MIN = 260
const SIDE_MAX = 760
/** page 列保留的最小宽度（列头 + 一个图标 + 一行字） */
const PAGE_MIN = 190

function startColResize(e: MouseEvent) {
  const sx = e.clientX
  const ow = eventColW.value
  const maxEvent = Math.max(130, sideW.value - PAGE_MIN)

  const onMove = (ev: MouseEvent) => {
    eventColW.value = Math.min(maxEvent, Math.max(130, Math.round(ow + ev.clientX - sx)))
  }
  const onUp = () => {
    localStorage.setItem(COL_W_KEY, String(eventColW.value))
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }

  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

function startSideResize(e: MouseEvent) {
  const sx = e.clientX
  const ow = sideW.value
  sideResizing.value = true

  const onMove = (ev: MouseEvent) => {
    const next = Math.min(SIDE_MAX, Math.max(SIDE_MIN, Math.round(ow + ev.clientX - sx)))
    sideW.value = next
    // 侧栏变窄时顺带把过宽的 event 列收回来，别把 page 列挤没
    if (eventColW.value > next - PAGE_MIN) {
      eventColW.value = Math.max(130, next - PAGE_MIN)
    }
  }
  const onUp = () => {
    sideResizing.value = false
    localStorage.setItem(SIDE_W_KEY, String(sideW.value))
    localStorage.setItem(COL_W_KEY, String(eventColW.value))
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }

  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
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
      coverText: bookDialog.coverText.trim()
    })
    if (book.value) {
      book.value.name = name
      book.value.coverText = bookDialog.coverText.trim()
    }
    bookDialog.open = false
    message.success('已保存')
  } catch {
    /* 拦截器已提示 */
  }
}

/* ---------------- 画布：缩放 + 滚动 ----------------
 * 画布尺寸是「纸」的尺寸（见上），视口不再无限生长：
 *   滚轮（非 ctrl）→ 原生滚动；ctrl + 滚轮 / 缩放条 → 改 zoom；
 *   缩放靠内层 .canvas-scroll 的占位尺寸承载（stage × zoom），
 *   所以滚动条长度、可滚范围都跟着缩放一起变。
 * ------------------------------------------ */
const zoom = ref(1)

/* ---------------- 画布背景（随画布存进后端 canvas.background） ---------------- */
/** 优先用后端下发的 background，未设置时回落到本地偏好 */
const bgMode = computed<CanvasBg>(() => {
  const v = canvas.value.background
  if (v && BG_OPTIONS.some((o) => o.key === v)) return v as CanvasBg
  return getBg(currentPageId.value)
})
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
  // 落在当前可视区域中心；多个节点之间做阶梯错位，避免完全重叠
  const n = canvas.value.objects.length
  const center = viewportCenterInCanvas()
  const cx = center.x
  const cy = center.y

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

function chooseBg(bg: CanvasBg) {
  if (currentPageId.value == null) return
  // 随画布一起存到后端（切页 / 换设备都能跟上）
  workspaceStore.setCanvasBackground(bg)
  // 本地仍留一份，作为离线或后端不可用时的兜底
  setBg(currentPageId.value, bg)
  bgOpen.value = false
}

const zoomLabel = computed(() => `${Math.round(zoom.value * 100)}%`)

/** 画布上这些区域有自己的交互，不能被「移动内容」抢走 */
const BLOCKING_SELECTOR = '.anno, .tl-host, .node, .toolbar, button, input, textarea, select'

/** 画布的空白落点：只有点在这些地方才算「点了空白」 */
function isBlankTarget(target: HTMLElement): boolean {
  return (
    target.classList.contains('canvas-area') ||
    target.classList.contains('canvas-viewport') ||
    target.classList.contains('canvas-scroll') ||
    target.classList.contains('canvas-origin') ||
    target.classList.contains('canvas-paper') ||
    target.classList.contains('stage') ||
    target.classList.contains('grid-layer')
  )
}

function onCanvasMouseDown(e: MouseEvent) {
  const target = e.target as HTMLElement

  // ①「移动内容」工具：整体平移当前页的对象 / 注解 / 时间线（坐标真的会改）
  if (activeTool.value === 'moveContent') {
    if (target.closest(BLOCKING_SELECTOR)) return
    startContentMove(e)
    return
  }

  // ②箭头工具：空白处按住拖动 = 平移视野；没拖动（纯单击）= 取消选中
  if (isBlankTarget(target)) startViewPan(e)
}

/* ---------------- 移动视野（箭头工具的默认能力） ----------------
 * 箭头 = 「选择」，同时保留原本的视野平移：在空白处按住拖动，
 * 只动滚动条（视口），内容坐标一个不改。纯单击（位移 < 4px）
 * 才算「点了空白」，取消选中。
 * ------------------------------------------ */
const viewPanning = ref(false)
let vpStart = { x: 0, y: 0, sl: 0, st: 0 }
let vpMoved = false

function startViewPan(e: MouseEvent) {
  const v = canvasScrollRef.value
  if (!v) return
  vpStart = { x: e.clientX, y: e.clientY, sl: v.scrollLeft, st: v.scrollTop }
  vpMoved = false
  viewPanning.value = true
  window.addEventListener('mousemove', onCanvasMove)
  window.addEventListener('mouseup', onCanvasUp)
}

function onCanvasMove(e: MouseEvent) {
  if (viewPanning.value) {
    const v = canvasScrollRef.value
    if (!v) return
    const dx = e.clientX - vpStart.x
    const dy = e.clientY - vpStart.y
    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) vpMoved = true
    v.scrollLeft = vpStart.sl - dx
    v.scrollTop = vpStart.st - dy
    return
  }
  if (contentMoving.value) applyContentMove(e)
}

function onCanvasUp() {
  window.removeEventListener('mousemove', onCanvasMove)
  window.removeEventListener('mouseup', onCanvasUp)
  if (viewPanning.value) {
    viewPanning.value = false
    // 没真正拖动 = 单击空白，取消选中
    if (!vpMoved) selectedId.value = null
    return
  }
  if (contentMoving.value) {
    contentMoving.value = false
    cmSnapshot = null
    // 真的挪动了才落盘，纯点一下不产生多余的 PUT
    if (cmMoved) {
      cmMoved = false
      void saveNow()
    }
  }
}

/* ---------------- 移动内容（独立工具，坐标会真的改） ----------------
 * 把当前页的对象 / 注解 / 时间线整体平移，拖动期间只改内存，抬手才存，
 * 避免每帧一次请求。
 * ------------------------------------------ */
const contentMoving = ref(false)
let cmStart = { x: 0, y: 0 }
let cmMoved = false
let cmSnapshot: {
  objects: { ref: CanvasObject; x: number; y: number }[]
  annotations: { ref: Annotation; x: number; y: number }[]
  timelines: { ref: Timeline; x: number; y: number }[]
} | null = null

function startContentMove(e: MouseEvent) {
  cmStart = { x: e.clientX, y: e.clientY }
  cmMoved = false
  cmSnapshot = {
    objects: canvas.value.objects.map((o) => ({ ref: o, x: o.x, y: o.y })),
    annotations: canvas.value.annotations.map((a) => ({ ref: a, x: a.x, y: a.y })),
    timelines: canvas.value.timelines.map((t) => ({ ref: t, x: t.x ?? 0, y: t.y ?? 0 }))
  }
  contentMoving.value = true
  selectedId.value = null
  window.addEventListener('mousemove', onCanvasMove)
  window.addEventListener('mouseup', onCanvasUp)
}

function applyContentMove(e: MouseEvent) {
  const snap = cmSnapshot
  if (!snap) return
  const z = zoom.value || 1
  const dx = Math.round((e.clientX - cmStart.x) / z)
  const dy = Math.round((e.clientY - cmStart.y) / z)
  if (dx === 0 && dy === 0) return
  cmMoved = true
  snap.objects.forEach((s) => {
    s.ref.x = s.x + dx
    s.ref.y = s.y + dy
  })
  snap.annotations.forEach((s) => {
    s.ref.x = s.x + dx
    s.ref.y = s.y + dy
  })
  snap.timelines.forEach((s) => {
    s.ref.x = s.x + dx
    s.ref.y = s.y + dy
  })
}

/* ---------------- 缩放 ----------------
 * ctrl + 滚轮（在模板上 .prevent 掉了浏览器缩放）或缩放条按钮。
 * 缩放只改 zoom，滚动范围由 .canvas-scroll 的占位尺寸自动跟随。
 * ------------------------------------------ */
const ZOOM_MIN = 0.25
const ZOOM_MAX = 3

/** 缩放时保持「视口中心那张纸的位置不变」，不然会跳到左上角 */
function setZoom(next: number) {
  const view = canvasScrollRef.value
  const prev = zoom.value
  const z = Math.min(Math.max(next, ZOOM_MIN), ZOOM_MAX)
  if (z === prev) return
  if (view) {
    const cx = view.clientWidth / 2
    const cy = view.clientHeight / 2
    const rx = (view.scrollLeft + cx) / prev
    const ry = (view.scrollTop + cy) / prev
    zoom.value = z
    requestAnimationFrame(() => {
      view.scrollLeft = rx * z - cx
      view.scrollTop = ry * z - cy
    })
  } else {
    zoom.value = z
  }
}

function onWheel(e: WheelEvent) {
  setZoom(zoom.value * (e.deltaY > 0 ? 0.9 : 1.1))
}

function zoomIn() {
  setZoom(zoom.value * 1.1)
}

function zoomOut() {
  setZoom(zoom.value * 0.9)
}

/** 适配画布：按视口算一个刚好放得下整张纸的缩放，并滚回左上角 */
function fitCanvas() {
  const el = canvasRef.value
  if (!el) return
  const pad = 48
  const fit = Math.min(
    (el.clientWidth - pad) / stageW.value,
    (el.clientHeight - pad) / stageH.value
  )
  zoom.value = Math.min(Math.max(fit, ZOOM_MIN), 1)
  requestAnimationFrame(() => {
    const view = canvasScrollRef.value
    if (view) {
      view.scrollLeft = 0
      view.scrollTop = 0
    }
  })
}

/* ---------------- 工具条 ---------------- */
const activeTool = ref<CanvasTool>('select')

/* ---------------- 画布尺寸面板 ----------------
 * 和「移动内容」同级的功能栏工具：改的是画布这张「纸」的尺寸，
 * 内容坐标不动；超出可视区域的部分由容器滚动查看。
 * 写进 canvas（随画布存后端）+ 本机兜底，然后落盘。
 * ------------------------------------------ */
const sizePopOpen = ref(false)
/** 面板草稿值：打开面板时用当前画布尺寸填充 */
const sizeDraft = ref({ w: stageW.value, h: stageH.value })

const sizeDirty = computed(
  () => sizeDraft.value.w !== stageW.value || sizeDraft.value.h !== stageH.value
)

function toggleSizePanel() {
  const next = !sizePopOpen.value
  if (next) {
    sizeDraft.value = { w: stageW.value, h: stageH.value }
    relPopOpen.value = false
  }
  sizePopOpen.value = next
}

function pickSizePreset(p: CanvasPreset) {
  sizeDraft.value = { w: p.w, h: p.h }
}

/** 应用尺寸：写进画布 + 本机兜底并立即落盘 */
function applyCanvasSize() {
  const size = clampSize(Number(sizeDraft.value.w), Number(sizeDraft.value.h))
  sizeDraft.value = { ...size }
  canvas.value.canvasWidth = size.w
  canvas.value.canvasHeight = size.h
  setLocalSize(currentPageId.value, size)
  sizePopOpen.value = false
  scrollToPaper()
  void saveNow()
  message.success(`画布已改为 ${size.w} × ${size.h}`)
}

/** 恢复缺省尺寸 */
function resetCanvasSize() {
  sizeDraft.value = { w: DEFAULT_CANVAS_W, h: DEFAULT_CANVAS_H }
  applyCanvasSize()
}

/* ---------------- 工具栏拖动 ----------------
 * 最上面那个箭头既是「选择」工具，也是整条工具栏的握把：
 *   · 按住拖动（> 5px）→ 拖动整条工具栏；靠近左侧原位吸附，上下不吸附
 *   · 原地单击         → 切回「选择」工具（不再承担移动画布）
 * ------------------------------------------ */
const TOOLBAR_POS_KEY = 'wb.toolbar.pos'
const TOOLBAR_HOME_X = 20
const TOOLBAR_SNAP = 26

const toolbarRef = ref<HTMLElement | null>(null)
const tbDragging = ref(false)
const toolbarPinned = ref(false)
const toolbarPos = ref(loadToolbarPos())

/** 拖动结束后紧跟着的那次 click 要吞掉，否则会误触发「移动画布」 */
let suppressToolClick = false

const GRIP_TITLE = '按住拖动整条工具栏 · 单击回到「选择」'

function loadToolbarPos(): { x: number; y: number } {
  try {
    const raw = localStorage.getItem(TOOLBAR_POS_KEY)
    if (raw) {
      const p = JSON.parse(raw) as { x?: number; y?: number }
      if (typeof p.x === 'number' && typeof p.y === 'number') return { x: p.x, y: p.y }
    }
  } catch {
    /* 坏数据就回默认位 */
  }
  return { x: TOOLBAR_HOME_X, y: 150 }
}

/** 靠近左侧原位就吸附过去；上下完全不吸附 */
function snapToolbarX(x: number): number {
  const snapped = Math.abs(x - TOOLBAR_HOME_X) <= TOOLBAR_SNAP
  toolbarPinned.value = snapped
  if (snapped) return TOOLBAR_HOME_X
  const el = canvasRef.value
  const w = toolbarRef.value?.offsetWidth ?? 52
  const max = Math.max(8, (el?.clientWidth ?? 1200) - w - 8)
  return Math.min(Math.max(8, x), max)
}

/** 纵向只做视口内约束，不吸附 */
function clampToolbarY(y: number): number {
  const el = canvasRef.value
  const h = toolbarRef.value?.offsetHeight ?? 420
  const max = Math.max(8, (el?.clientHeight ?? 800) - h - 8)
  return Math.min(Math.max(8, y), max)
}

function onToolMouseDown(e: MouseEvent, tool: CanvasTool) {
  if (tool !== 'select' || e.button !== 0) return
  const startX = e.clientX
  const startY = e.clientY
  const ox = toolbarPos.value.x
  const oy = toolbarPos.value.y
  let moved = false

  const onMove = (ev: MouseEvent) => {
    const dx = ev.clientX - startX
    const dy = ev.clientY - startY
    if (!moved && Math.hypot(dx, dy) < 5) return
    moved = true
    tbDragging.value = true
    toolbarPos.value = { x: snapToolbarX(ox + dx), y: clampToolbarY(oy + dy) }
    document.body.style.userSelect = 'none'
  }
  const onUp = () => {
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
    document.body.style.userSelect = ''
    if (!moved) return
    tbDragging.value = false
    suppressToolClick = true
    toolbarPos.value = {
      x: snapToolbarX(toolbarPos.value.x),
      y: clampToolbarY(toolbarPos.value.y)
    }
    localStorage.setItem(TOOLBAR_POS_KEY, JSON.stringify(toolbarPos.value))
    // click 紧跟在 mouseup 之后同步派发，下个 tick 再放开即可
    window.setTimeout(() => {
      suppressToolClick = false
    }, 0)
  }

  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

const TOOLS: { key: CanvasTool; label: string; dividerBefore?: boolean; icon: string }[] = [
  {
    key: 'select',
    label: '选择',
    icon: `<svg viewBox="0 0 18 18" fill="none"><path d="M4 2.6 13.4 9.2l-4.1.7 2.2 4.6-1.9.9-2.2-4.6-2.4 2.3z" stroke="#6B665E" stroke-width="1.5" stroke-linejoin="round"/></svg>`
  },
  {
    key: 'moveContent',
    label: '移动内容',
    dividerBefore: true,
    icon: `<svg viewBox="0 0 18 18" fill="none"><path d="M9 2.4v13.2M2.4 9h13.2" stroke="#6B665E" stroke-width="1.5" stroke-linecap="round"/><path d="M9 2.4 6.8 4.9M9 2.4l2.2 2.5M9 15.6l-2.2-2.5M9 15.6l2.2-2.5M2.4 9l2.5-2.2M2.4 9l2.5 2.2M15.6 9l-2.5-2.2M15.6 9l-2.5 2.2" stroke="#6B665E" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>`
  },
  {
    key: 'canvasSize',
    label: '画布尺寸',
    icon: `<svg viewBox="0 0 18 18" fill="none"><rect x="2.8" y="2.8" width="12.4" height="12.4" rx="1.8" stroke="#6B665E" stroke-width="1.5" stroke-dasharray="3.4 2.4"/><path d="M7.4 10.6 10.6 7.4" stroke="#6B665E" stroke-width="1.5" stroke-linecap="round"/><path d="M11.2 7.4H9.4v1.8M6.8 10.6h1.8V8.8" stroke="#6B665E" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg>`
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
  // 刚拖完工具栏产生的那次 click 不算数
  if (suppressToolClick) {
    suppressToolClick = false
    return
  }
  switch (tool) {
    case 'select':
      // 箭头恢复为原始功能：单纯的「选择」
      activeTool.value = 'select'
      return
    case 'moveContent':
      // 移动整页内容（坐标会真的改）
      activeTool.value = activeTool.value === 'moveContent' ? 'select' : 'moveContent'
      sizePopOpen.value = false
      return
    case 'canvasSize':
      // 弹出画布尺寸面板；与「移动内容」互斥
      activeTool.value = 'select'
      toggleSizePanel()
      return
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

/**
 * 同一对对象之间存在多条关系时（师徒 / 怀疑 / 血缘…），
 * 给每条算一个法线偏移，让它们平行并排，而不是叠成一条线。
 */
const relOffsets = computed(() => bundleOffsets(canvas.value.relationships))

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

/** 拖缘调整时间线面板大小：横向存 width，纵向存 height */
function onTimelineResize(timelineId: string, size: { width?: number; height?: number }) {
  workspaceStore.setTimelineSize(timelineId, size)
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

/* ---------------- 关系图 / 族谱图 抽屉 ----------------
 * 两种「图」共用一套抽屉外壳：
 *   列表层：本案件下已有的 + 新增
 *   详情层：查看 / 编辑某一张图（增删对象与关系），只改这张图，不动画布
 * 差别只有三处：
 *   ① 数据来源  关系图走后端（支持从画布 / 全书自动提取）
 *               族谱图全部手工建立，存在本机浏览器
 *   ② 对象字段  族谱成员多出性别 / 生卒年
 *   ③ 图形排布  关系图 = 中心切换的环状，族谱图 = 一代一行
 * ------------------------------------------ */
type ExtKind = 'relation' | 'family'

const extDrawer = ref(false)
const extKind = ref<ExtKind>('relation')
const extMode = ref<'list' | 'detail'>('list')
const extView = ref<'list' | 'graph'>('list')
const extLoading = ref(false)

const graphList = ref<RelationGraphResponse[]>([])
const activeGraph = ref<RelationGraphDetailResponse | null>(null)
const graphFilter = ref<'all' | 'person' | 'thing'>('all')

const familyTrees = ref<FamilyTreeResponse[]>([])
const activeFamily = ref<FamilyTree | null>(null)
const familyRelType = ref<FamilyRelType>('parent-child')

const GRAPH_FILTERS = [
  { value: 'all' as const, label: '全部' },
  { value: 'person' as const, label: '人物↔人物' },
  { value: 'thing' as const, label: '事物↔事物' }
]

const edgeDraft = reactive({
  source: '',
  target: '',
  label: '',
  type: 'unidirectional' as RelationshipType
})

/*
  antd 的 Select 只有在值为 undefined / null 时才显示 placeholder，
  空串会被当作"确实选中了一个空值"，于是起点/终点位置看起来是空白而不是提示。
  草稿本身继续用空串（addExtRelation / canAddExtRelation 的判空逻辑不动），
  这里只垫一层转换。
*/
const edgeSourceModel = computed<string | undefined>({
  get: () => edgeDraft.source || undefined,
  set: (v) => {
    edgeDraft.source = v ?? ''
  }
})

const edgeTargetModel = computed<string | undefined>({
  get: () => edgeDraft.target || undefined,
  set: (v) => {
    edgeDraft.target = v ?? ''
  }
})

/** 详情层「新增对象 / 成员」的草稿 */
const nodeDraft = reactive({
  name: '',
  type: 'person' as string,
  gender: 'male' as FamilyGender,
  birth: '',
  death: ''
})

/* ---- 统一视图模型：把两种图抹平成同一套 节点 / 关系 ---- */

interface ExtNode {
  id: string
  name: string
  type: string
  kind: NodeKind
  gender?: FamilyGender
}

interface ExtEdge {
  id: string
  source: string
  target: string
  label: string
  type: string
}

const extNodes = computed<ExtNode[]>(() => {
  if (extKind.value === 'relation') {
    return (activeGraph.value?.data.nodes ?? []).map((n) => ({
      id: n.id,
      name: n.name,
      type: n.type,
      kind: nodeShapeOf(n.type)
    }))
  }
  return (activeFamily.value?.members ?? []).map((m) => ({
    id: m.id,
    name: m.name,
    type: 'person',
    kind: 'person' as NodeKind,
    gender: m.gender
  }))
})

const extEdges = computed<ExtEdge[]>(() => {
  if (extKind.value === 'relation') {
    // 自环（自己连自己）没有意义，直接丢掉
    return (activeGraph.value?.data.edges ?? [])
      .filter((e) => e.source !== e.target)
      .map((e) => ({
        id: e.id,
        source: e.source,
        target: e.target,
        label: e.label,
        type: e.type
      }))
  }
  return (activeFamily.value?.relations ?? []).map((r) => ({
    id: r.id,
    source: r.from,
    target: r.to,
    label: r.type === 'spouse' ? '配偶' : '亲子',
    type: r.type === 'spouse' ? 'bidirectional' : 'unidirectional'
  }))
})

/**
 * 类型筛选：这一版**真正作用到节点与关系上**，
 * 所以图形视图会随筛选重新排布，而不是只过滤列表。
 *   人物↔人物 → 两端都是人物
 *   事物↔事物 → 两端都不是人物（事物 / 事件）
 */
const extFilteredEdges = computed<ExtEdge[]>(() => {
  if (extKind.value !== 'relation' || graphFilter.value === 'all') return extEdges.value
  const kindOf = (id: string) => extNodes.value.find((n) => n.id === id)?.kind
  const want = (k?: NodeKind) =>
    graphFilter.value === 'person' ? k === 'person' : !!k && k !== 'person'
  return extEdges.value.filter((e) => want(kindOf(e.source)) && want(kindOf(e.target)))
})

const extRows = computed(() =>
  extFilteredEdges.value
    .map((e) => {
      const from = extNodes.value.find((n) => n.id === e.source)?.name
      const to = extNodes.value.find((n) => n.id === e.target)?.name
      if (!from || !to) return null
      return { id: e.id, from, to, label: e.label }
    })
    .filter((x): x is { id: string; from: string; to: string; label: string } => x !== null)
)

const extList = computed(() =>
  extKind.value === 'relation'
    ? graphList.value.map((g) => ({ id: String(g.id), name: g.name, updatedAt: g.updatedAt }))
    : familyTrees.value.map((t) => ({ id: String(t.id), name: t.name, updatedAt: t.updatedAt }))
)

const extTitle = computed(() => {
  if (extMode.value === 'detail') {
    return extKind.value === 'relation'
      ? activeGraph.value?.name ?? '关系图'
      : activeFamily.value?.name ?? '族谱图'
  }
  return extKind.value === 'relation' ? '人物关系图' : '族谱图'
})

const canAddExtRelation = computed(
  () => !!edgeDraft.source && !!edgeDraft.target && edgeDraft.source !== edgeDraft.target
)

/** 类型筛选下一条关系都没有：空态里给个一键回到「全部」 */
const relFilterEmpty = computed(
  () =>
    extKind.value === 'relation' &&
    graphFilter.value !== 'all' &&
    extFilteredEdges.value.length === 0
)

/* ---- 抽屉尺寸：可拖拽调整，关系图 / 族谱图共用一套 ---- */

const DRAWER_W_MIN = 320
const DRAWER_W_MAX = 900
const DRAWER_H_MIN = 320
const DRAWER_W_DEFAULT = 420

const drawerW = ref(DRAWER_W_DEFAULT)
/** null = 高度由上下撑满（还没手动调过）；拖动下边缘后才有确定值 */
const drawerH = ref<number | null>(null)

const drawerRef = ref<HTMLElement | null>(null)
const drawerResizing = ref(false)

type DrawerResizeAxis = 'x' | 'y' | 'both'

/**
 * 拖抽屉边缘改尺寸。
 * 抽屉贴着右侧，所以「左边缘」往左拖 = 变宽；「下边缘」往下拖 = 变高。
 * 高度第一次拖动时以当前渲染高度为起点，避免尺寸突跳。
 */
function onDrawerResizeStart(e: MouseEvent, axis: DrawerResizeAxis) {
  if (e.button !== 0) return
  e.preventDefault()
  e.stopPropagation()
  const startX = e.clientX
  const startY = e.clientY
  const w0 = drawerW.value
  const h0 = drawerH.value ?? drawerRef.value?.offsetHeight ?? DRAWER_H_MIN
  // 高度不能超过容器：否则底部的「删除」等操作会被顶到屏幕外够不着
  const maxH = drawerRef.value?.parentElement?.clientHeight ?? Number.POSITIVE_INFINITY
  drawerResizing.value = true
  document.body.style.userSelect = 'none'
  document.body.style.cursor =
    axis === 'x' ? 'col-resize' : axis === 'y' ? 'row-resize' : 'nwse-resize'

  const onMove = (ev: MouseEvent) => {
    if (axis !== 'y') {
      drawerW.value = Math.min(
        DRAWER_W_MAX,
        Math.max(DRAWER_W_MIN, w0 - (ev.clientX - startX))
      )
    }
    if (axis !== 'x') {
      drawerH.value = Math.min(
        maxH,
        Math.max(DRAWER_H_MIN, h0 + (ev.clientY - startY))
      )
    }
  }
  const onUp = () => {
    drawerResizing.value = false
    document.body.style.userSelect = ''
    document.body.style.cursor = ''
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

function resetDrawerSize() {
  drawerW.value = DRAWER_W_DEFAULT
  drawerH.value = null
}

/* ---- 图形区尺寸：随抽屉一起变，图跟着重排而不是被裁掉 ---- */

const graphCanvasRef = ref<HTMLElement | null>(null)
const graphBox = reactive({ w: 360, h: 380 })
let graphRO: ResizeObserver | null = null

function bindGraphCanvas(el: unknown) {
  const node = el instanceof HTMLElement ? el : null
  graphCanvasRef.value = node
  if (graphRO) {
    graphRO.disconnect()
    graphRO = null
  }
  if (!node || typeof ResizeObserver === 'undefined') return
  const measure = () => {
    graphBox.w = Math.max(160, node.clientWidth)
    graphBox.h = Math.max(160, node.clientHeight)
  }
  measure()
  graphRO = new ResizeObserver(measure)
  graphRO.observe(node)
}

onUnmounted(() => {
  graphRO?.disconnect()
  graphRO = null
})

/* ---- 关系图图形视图：中心切换式排布 ---- */

/** 当前中心对象 */
const focusId = ref('')
/** 走过的中心，用来「返回上一个中心」 */
const focusStack = ref<string[]>([])

/**
 * 筛选后仍出现在关系里的对象集合。
 * 「全部」且这张图还没有任何关系时，退化成全部对象——有对象可看，好过一片空白；
 * 但类型筛选下不退化，否则会出现「选了事物↔事物却孤零零站着一个人物」的怪象。
 */
const allowedIds = computed(() => {
  const set = new Set<string>()
  extFilteredEdges.value.forEach((e) => {
    set.add(e.source)
    set.add(e.target)
  })
  if (!set.size && graphFilter.value === 'all') extNodes.value.forEach((n) => set.add(n.id))
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

interface DrawerNode {
  id: string
  name: string
  kind: NodeKind
  x: number
  y: number
  /** 人物圆形半径；其余形态为 0 */
  r: number
  focus: boolean
}

/** 环形半径上限：随图形区大小走，别贴边 */
const ringCap = computed(() => Math.max(70, Math.min(graphBox.w, graphBox.h) / 2 - 52))

const drawerNodes = computed<DrawerNode[]>(() => {
  const cx = graphBox.w / 2
  const cy = graphBox.h / 2
  const rOf = (id: string) => {
    const meta = extNodes.value.find((x) => x.id === id)
    return meta && meta.kind === 'person' ? personRadius(meta.name) : 0
  }
  const make = (id: string, x: number, y: number, focus: boolean): DrawerNode | null => {
    const meta = extNodes.value.find((n) => n.id === id)
    if (!meta) return null
    return {
      id,
      name: meta.name,
      kind: meta.kind,
      x,
      y,
      r: meta.kind === 'person' ? personRadius(meta.name) : 0,
      focus
    }
  }

  // 没有任何关系可画时：全部筛选下把对象平铺出来，类型筛选下交给空态文案
  if (!extFilteredEdges.value.length) {
    if (graphFilter.value !== 'all') return []
    const all = [...allowedIds.value]
    if (!all.length) return []
    const n = all.length
    const radius = n === 1 ? 0 : Math.min(ringCap.value, 44 + n * 8)
    const out: DrawerNode[] = []
    all.forEach((id, i) => {
      const a = -Math.PI / 2 + (i * 2 * Math.PI) / n
      const d = make(id, cx + Math.cos(a) * radius, cy + Math.sin(a) * radius, false)
      if (d) out.push(d)
    })
    return out
  }

  const center = focusId.value
  if (!center) return []
  const nb = neighborsOf(center, extFilteredEdges.value, (id) => allowedIds.value.has(id))
  // 环形半径：既不被中心圆压住，圆周也够放下所有邻居（间距放宽，别挤成一团），再受可用区域限制
  const radius = Math.min(
    ringCap.value,
    Math.max(62 + nb.length * 9, ringRadius(rOf(center), nb.map(rOf), 42))
  )
  const posMap = radialLayout(center, nb, { cx, cy, radius })
  const out: DrawerNode[] = []
  for (const id of [center, ...nb]) {
    const p = posMap[id]
    if (!p) continue
    const d = make(id, p.x, p.y, id === center)
    if (d) out.push(d)
  }
  return out
})

const drawerEdgeGeoms = computed(() => {
  const ids = new Set(drawerNodes.value.map((n) => n.id))
  // 只画「中心 ↔ 邻居」的辐条：邻居之间的连线横穿圆心，看着杂乱且遮住中心，
  // 等点它成为中心时自然会展开（中心切换式排布的约定）
  const rel = extFilteredEdges.value.filter(
    (e) =>
      ids.has(e.source) &&
      ids.has(e.target) &&
      (e.source === focusId.value || e.target === focusId.value)
  )
  const offsets = bundleOffsets(rel, 15)
  const at = (id: string) => drawerNodes.value.find((n) => n.id === id)
  return rel
    .map((e) => {
      const a = at(e.source)
      const b = at(e.target)
      if (!a || !b) return null
      const dx = b.x - a.x
      const dy = b.y - a.y
      const len = Math.hypot(dx, dy) || 1
      // 法线按无向基准方向算，A→B 与 B→A 才不会把偏移抵消掉
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

function diamondPath(cx: number, cy: number, halfW: number, halfH: number): string {
  return `M${cx} ${cy - halfH} L${cx + halfW} ${cy} L${cx} ${cy + halfH} L${cx - halfW} ${cy} Z`
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

function setGraphFilter(v: 'all' | 'person' | 'thing') {
  graphFilter.value = v
  focusId.value = ''
  focusStack.value = []
}

/* ---- 族谱图排布（一代一行） ---- */

const familyLayout = computed(() =>
  activeFamily.value
    ? layoutFamilyTree(activeFamily.value)
    : { nodes: [], edges: [], spouses: [], width: 320, height: 220, generations: 0 }
)

/**
 * 族谱可能比抽屉宽（同一代人多时）。按宽度等比缩一下好让整棵树一眼看全，
 * 但不放大（放大只会糊），缩到 42% 是下限，再宽就交给容器滚动。
 */
const familyScale = computed(() => {
  const w = familyLayout.value.width || 1
  return Math.min(1, Math.max(0.42, (graphBox.w - 4) / w))
})

/* ---- 打开 / 关闭 ---- */

async function openExt(kind: ExtKind) {
  extKind.value = kind
  extDrawer.value = true
  extMode.value = 'list'
  extView.value = 'list'
  activeGraph.value = null
  activeFamily.value = null
  graphFilter.value = 'all'
  focusId.value = ''
  focusStack.value = []
  resetExtDrafts()
  if (kind === 'relation') await refreshGraphList()
  else await refreshFamilyList()
}

/** 顶栏「关系图」工具 = 直接打开人物关系图 */
function openRelationGraphs() {
  return openExt('relation')
}

function resetExtDrafts() {
  nodeDraft.name = ''
  nodeDraft.birth = ''
  nodeDraft.death = ''
  edgeDraft.source = ''
  edgeDraft.target = ''
  edgeDraft.label = ''
}

async function refreshGraphList() {
  extLoading.value = true
  try {
    graphList.value = await relationGraphApi.listRelationGraphs(bookId.value)
  } catch {
    /* 拦截器已提示 */
  } finally {
    extLoading.value = false
  }
}

async function refreshFamilyList() {
  extLoading.value = true
  try {
    familyTrees.value = await familyTreeApi.listFamilyTrees(bookId.value)
  } catch {
    /* 拦截器已提示 */
  } finally {
    extLoading.value = false
  }
}

function fmtGraphTime(v?: string): string {
  if (!v) return ''
  return v.slice(0, 10)
}

/** 当前画布能直接给出的关系图数据（不依赖后端） */
function canvasGraphData(): RelationGraphData {
  const cv = canvas.value
  const ids = new Set(cv.objects.map((o) => o.id))
  return {
    nodes: cv.objects.map((o) => ({
      id: o.id,
      name: o.name || '未命名',
      type: o.type || 'thing'
    })),
    edges: cv.relationships
      .filter((r) => ids.has(r.source) && ids.has(r.target))
      .map((r) => ({
        id: r.id,
        source: r.source,
        target: r.target,
        label: r.label,
        type: r.type
      }))
  }
}

/**
 * 取一份关系图数据。
 *
 * 后端 extract 覆盖整本书、理应更全，但实测存在「接口成功返回、
 * 里面却一条关系都没有」的情况——于是定了一条兜底规则：
 * 只要 extract 拿不出可用结果（空节点，或者画布明明有关系而它一条都没提取到），
 * 就退回当前画布推导。绝不出现「提示提取成功、点开图里却空空如也」。
 */
async function extractGraphData(): Promise<{
  data: RelationGraphData
  from: 'server' | 'canvas'
}> {
  const local = canvasGraphData()
  let remote: RelationGraphData | null = null
  try {
    remote = await relationGraphApi.extractRelationGraph(bookId.value, {
      objectTypes: ['person', 'thing'],
      relationTypes: ['unidirectional', 'bidirectional', 'dashed']
    })
  } catch {
    remote = null
  }

  const remoteNodes = remote?.nodes?.length ?? 0
  const remoteEdges = remote?.edges?.length ?? 0
  const serverUsable = remoteNodes > 0 && (remoteEdges > 0 || local.edges.length === 0)

  if (remote && serverUsable) {
    const nodeIds = new Set(remote.nodes.map((n) => n.id))
    return {
      data: {
        nodes: remote.nodes,
        edges: (remote.edges ?? []).filter(
          (e) => nodeIds.has(e.source) && nodeIds.has(e.target)
        )
      },
      from: 'server'
    }
  }
  return { data: local, from: 'canvas' }
}

function describeExtract(data: RelationGraphData, from: 'server' | 'canvas'): string {
  const src = from === 'server' ? '全书' : '当前画布'
  return `已从${src}提取 ${data.nodes.length} 个对象 · ${data.edges.length} 条关系`
}

/** 新增：建图 → 自动提取 → 存为初始数据 → 进入详情 */
async function createGraphFromCanvas() {
  openPrompt({
    title: '新增关系图',
    placeholder: '如：主要人物关系',
    async onOk(name) {
      try {
        extLoading.value = true
        const created = await relationGraphApi.createRelationGraph(bookId.value, { name })
        const { data, from } = await extractGraphData()
        await relationGraphApi.saveRelationGraphData(created.id, data)

        await refreshGraphList()
        await openExtDetail({ id: String(created.id), name: created.name, updatedAt: '' })
        message.success(`${describeExtract(data, from)}，可继续编辑`)
      } catch {
        /* 拦截器已提示 */
      } finally {
        extLoading.value = false
      }
    }
  })
}

/** 新增族谱图：不做任何提取，建一张空白的，成员全部手工加 */
function createFamilyFlow() {
  openPrompt({
    title: '新增族谱图',
    placeholder: '如：沈氏家族',
    async onOk(name) {
      try {
        extLoading.value = true
        const created = await familyTreeApi.createFamilyTree(bookId.value, { name })
        await refreshFamilyList()
        const detail = await familyTreeApi.getFamilyTree(created.id)
        openFamilyDetail(detail)
        message.success('已创建，接下来手工添加成员与亲属关系')
      } catch {
        /* 拦截器已提示 */
      } finally {
        extLoading.value = false
      }
    }
  })
}

function createExt() {
  if (extKind.value === 'relation') void createGraphFromCanvas()
  else createFamilyFlow()
}

/** 详情层：重新提取并覆盖这张图的数据（只有关系图有这一步） */
function reextractActiveGraph() {
  const g = activeGraph.value
  if (!g) return
  Modal.confirm({
    title: '从画布重新提取',
    content: '会用提取结果整体覆盖这张关系图的对象与关系，手工补充的内容会被替换。',
    okText: '覆盖提取',
    cancelText: '取消',
    async onOk() {
      extLoading.value = true
      try {
        const { data, from } = await extractGraphData()
        g.data.nodes = data.nodes
        g.data.edges = data.edges
        await persistGraph()
        message.success(describeExtract(data, from))
      } catch {
        /* 拦截器已提示 */
      } finally {
        extLoading.value = false
      }
    }
  })
}

/** 详情层入口：按当前 kind 拉取对应的数据 */
async function openExtDetail(item: { id: string; name: string; updatedAt: string }) {
  if (extKind.value === 'relation') {
    extLoading.value = true
    try {
      const detail = await relationGraphApi.getRelationGraph(Number(item.id))
      // 后端可能给 null，先归一成空数组，后面所有 push 才安全
      activeGraph.value = {
        ...detail,
        data: {
          nodes: detail.data?.nodes ?? [],
          edges: detail.data?.edges ?? []
        }
      }
      extMode.value = 'detail'
      extView.value = 'list'
      graphFilter.value = 'all'
      focusId.value = ''
      focusStack.value = []
      resetExtDrafts()
    } catch {
      /* 拦截器已提示 */
    } finally {
      extLoading.value = false
    }
    return
  }

  extLoading.value = true
  try {
    const detail = await familyTreeApi.getFamilyTree(Number(item.id))
    openFamilyDetail(detail)
  } catch {
    /* 拦截器已提示 */
  } finally {
    extLoading.value = false
  }
}

function openFamilyDetail(d: {
  id: number
  name: string
  createdAt: string
  updatedAt: string
  data: { members?: FamilyMember[] | null; relations?: FamilyRelation[] | null }
}) {
  // 拷一份出来编辑，改完再整体写回
  activeFamily.value = {
    id: d.id,
    name: d.name,
    members: [...(d.data.members ?? [])],
    relations: [...(d.data.relations ?? [])],
    createdAt: d.createdAt,
    updatedAt: d.updatedAt
  }
  extMode.value = 'detail'
  extView.value = 'list'
  resetExtDrafts()
}

function backToExtList() {
  extMode.value = 'list'
  activeGraph.value = null
  activeFamily.value = null
  focusId.value = ''
  focusStack.value = []
}

/** 把详情层的数据写回后端（只影响这张关系图，不碰画布） */
async function persistGraph() {
  const g = activeGraph.value
  if (!g) return
  try {
    await relationGraphApi.saveRelationGraphData(g.id, g.data)
  } catch {
    /* 拦截器已提示 */
  }
}

/** 族谱写回后端（只影响这张族谱图，不碰画布） */
async function persistFamily() {
  const t = activeFamily.value
  if (!t) return
  try {
    await familyTreeApi.saveFamilyTreeData(t.id, {
      members: t.members,
      relations: t.relations
    })
  } catch {
    /* 拦截器已提示 */
  }
}

async function removeExtRelation(relationId: string) {
  if (extKind.value === 'relation') {
    const g = activeGraph.value
    if (!g) return
    g.data.edges = g.data.edges.filter((e) => e.id !== relationId)
    await persistGraph()
    return
  }
  const t = activeFamily.value
  if (!t) return
  t.relations = t.relations.filter((r) => r.id !== relationId)
  await persistFamily()
}

async function removeExtNode(nodeId: string) {
  if (extKind.value === 'relation') {
    const g = activeGraph.value
    if (!g) return
    g.data.nodes = g.data.nodes.filter((n) => n.id !== nodeId)
    g.data.edges = g.data.edges.filter((e) => e.source !== nodeId && e.target !== nodeId)
    await persistGraph()
    return
  }
  const t = activeFamily.value
  if (!t) return
  t.members = t.members.filter((m) => m.id !== nodeId)
  t.relations = t.relations.filter((r) => r.from !== nodeId && r.to !== nodeId)
  await persistFamily()
}

async function addExtRelation() {
  if (!canAddExtRelation.value) return
  if (extKind.value === 'relation') {
    const g = activeGraph.value
    if (!g) return
    if (!g.data.edges) g.data.edges = []
    g.data.edges.push({
      id: localId('ge'),
      source: edgeDraft.source,
      target: edgeDraft.target,
      label: edgeDraft.label.trim() || '关系',
      type: edgeDraft.type
    })
    edgeDraft.source = ''
    edgeDraft.target = ''
    edgeDraft.label = ''
    await persistGraph()
    return
  }
  const t = activeFamily.value
  if (!t) return
  t.relations.push({
    id: newRelationId(),
    type: familyRelType.value,
    from: edgeDraft.source,
    to: edgeDraft.target
  })
  edgeDraft.source = ''
  edgeDraft.target = ''
  await persistFamily()
}

/**
 * 手动新增一个对象 / 成员。
 * 之前的详情层只有「新增关系」却没有地方建对象，
 * 一旦提取失败就完全没法往下走。
 */
async function addExtNode() {
  const name = nodeDraft.name.trim()
  if (!name) return

  if (extKind.value === 'relation') {
    const g = activeGraph.value
    if (!g) return
    if (!g.data.nodes) g.data.nodes = []
    g.data.nodes.push({ id: localId('gn'), name, type: nodeDraft.type })
    nodeDraft.name = ''
    await persistGraph()
  } else {
    const t = activeFamily.value
    if (!t) return
    t.members.push({
      id: newMemberId(),
      name,
      gender: nodeDraft.gender,
      birth: nodeDraft.birth.trim() || undefined,
      death: nodeDraft.death.trim() || undefined
    })
    nodeDraft.name = ''
    nodeDraft.birth = ''
    nodeDraft.death = ''
    await persistFamily()
  }
  message.success(`已添加「${name}」`)
}

function removeExt() {
  if (extKind.value === 'relation') {
    const g = activeGraph.value
    if (!g) return
    Modal.confirm({
      title: '删除关系图',
      content: `「${g.name}」会被删除，画布不受影响。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      async onOk() {
        await relationGraphApi.deleteRelationGraph(g.id)
        backToExtList()
        await refreshGraphList()
        message.success('关系图已删除')
      }
    })
    return
  }

  const t = activeFamily.value
  if (!t) return
  Modal.confirm({
    title: '删除族谱图',
    content: `「${t.name}」会被删除，画布不受影响。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await familyTreeApi.deleteFamilyTree(t.id)
      backToExtList()
      await refreshFamilyList()
      message.success('族谱图已删除')
    }
  })
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
    if (sizePopOpen.value) {
      sizePopOpen.value = false
      return
    }
    if (activeTool.value === 'moveContent') activeTool.value = 'select'
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

/* ═══ 两列容器：event 列 + page 列（可拖宽） ═══ */
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

/* 列宽拖柄：贴在两列之间 */
.col-resize {
  flex: none;
  width: 4px;
  margin: 0 -2px;
  z-index: 3;
  cursor: col-resize;
  background: transparent;
  transition: background 140ms var(--ease);
}

.col-resize:hover,
.col-resize:active {
  background: rgba(124, 154, 136, 0.35);
}

.col-page {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
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
  /* 箭头工具：空白处按住可拖动平移视野 */
  cursor: grab;
}

/* 选中 page 后，画布周围留一圈中性底色，白色「纸」才是主角 */
.canvas-area.has-bg {
  background: var(--bg-canvas);
}

/* 移动内容模式：四向箭头光标（整体平移内容，不是平移视口） */
.canvas-area.mode-content {
  cursor: move;
}

.canvas-area.grabbing {
  cursor: grabbing;
}

/* ---------------- 固定尺寸画布：滚动视口 + 纸张 ----------------
 * 画布是一张有明确尺寸的纸（见 canvasSize.ts），
 * 尺寸比可视区域大时，超出部分由 .canvas-viewport 滚（滚轮 / 滚动条）。
 * ------------------------------------------------------------ */
.canvas-viewport {
  position: absolute;
  inset: 0;
  overflow: auto;
  overscroll-behavior: contain;
}

/* 占位层：尺寸 = 纸张 ∪ 内容包围盒（× 缩放），滚动条长度按它算 */
.canvas-scroll {
  position: relative;
}

/* 原点层：画布 (0,0) 在滚动盒子里的落点（内容有负坐标时 > 0） */
.canvas-origin {
  position: absolute;
  pointer-events: none;
}

/* 原点层里的东西各自恢复交互（节点在 CanvasNode 里单独 auto） */
.canvas-origin > .stage {
  pointer-events: auto;
}

/* 纸张：把画布边界显式画出来（内容坐标 0,0 就在它的左上角） */
.canvas-paper {
  position: absolute;
  left: 0;
  top: 0;
  background: #FFFFFF;
  box-shadow:
    0 0 0 1px #E8E3DB,
    0 10px 26px -18px rgba(89, 84, 74, 0.4);
  pointer-events: none;
}

.grid-layer {
  position: absolute;
  top: 0;
  left: 0;
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
  /* 关键：整张 SVG 必须放行鼠标事件，否则它会盖住整个画布吃掉平移点击；
     节点本体在 CanvasNode 里用 pointer-events: auto 单独恢复 */
  pointer-events: none;
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

/* ═══════════ 画布尺寸面板 ═══════════ */
.size-pop {
  position: absolute;
  left: calc(100% + 12px);
  top: 50%;
  transform: translateY(-50%);
  z-index: 40;
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 196px;
  padding: 8px;
  background: #fff;
  border: 1px solid #e8e3db;
  border-radius: var(--r-lg);
  box-shadow: 0 10px 26px -6px rgba(89, 84, 74, 0.2);
}

.size-pop-title {
  padding: 2px 4px 7px;
  font-size: 9.5px;
  letter-spacing: 0.03em;
  color: var(--ink-5);
}

.size-fields {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 2px 8px;
}

.size-field {
  display: flex;
  align-items: center;
  gap: 5px;
  flex: 1;
  min-width: 0;
}

.size-field-label {
  flex: none;
  font-size: 10px;
  color: var(--ink-5);
}

.size-input {
  width: 100%;
  min-width: 0;
  height: 26px;
  padding: 0 7px;
  border: 1px solid #e8e3db;
  border-radius: var(--r-sm);
  background: #fbfaf8;
  color: var(--ink-1);
  font-family: inherit;
  font-size: 11px;
  outline: none;
}

.size-input:focus {
  border-color: #c9c2b6;
  background: #fff;
}

.size-x {
  flex: none;
  font-size: 10px;
  color: var(--ink-5);
}

.size-pop-sub {
  padding: 2px 6px 5px;
  font-size: 9.5px;
  letter-spacing: 0.03em;
  color: var(--ink-5);
}

.size-opt {
  justify-content: space-between;
}

.size-opt-value {
  color: var(--ink-5);
  font-size: 10px;
}

.size-pop-foot {
  display: flex;
  gap: 6px;
  margin-top: 7px;
  padding-top: 8px;
  border-top: 1px solid #f1ede6;
}

.size-btn {
  flex: 1;
  height: 27px;
  border: 1px solid #e8e3db;
  border-radius: var(--r-sm);
  background: #fff;
  color: var(--ink-2);
  font-family: inherit;
  font-size: 11px;
  cursor: pointer;
  transition:
    background 120ms var(--ease),
    border-color 120ms var(--ease),
    color 120ms var(--ease);
}

.size-btn:hover {
  background: #f5f3ef;
}

.size-btn.primary {
  border-color: #cfdcd3;
  background: #edf2ee;
  color: #3f5b4c;
}

.size-btn.primary:hover {
  background: #e3ece6;
}

.size-btn.primary:disabled {
  border-color: #ece8e1;
  background: #f7f5f2;
  color: var(--ink-5);
  cursor: default;
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
  flex: 1;
  min-width: 0;
  font-family: var(--font-serif);
  font-weight: 700;
  font-size: 15px;
  color: var(--ink-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 抽屉尺寸拖柄：左缘调宽 / 下缘调高 / 左下角一起调 */
.dr-grip {
  position: absolute;
  z-index: 2;
}

.dr-grip-x {
  top: 0;
  bottom: 14px;
  left: -3px;
  width: 7px;
  cursor: col-resize;
}

.dr-grip-y {
  left: 14px;
  right: 0;
  bottom: -3px;
  height: 7px;
  cursor: row-resize;
}

.dr-grip-xy {
  left: -3px;
  bottom: -3px;
  width: 18px;
  height: 18px;
  cursor: nwse-resize;
}

/* 左下角画一小段斜纹，暗示这里能拖 */
.dr-grip-xy::after {
  content: '';
  position: absolute;
  left: 4px;
  bottom: 4px;
  width: 8px;
  height: 8px;
  border-left: 1px solid var(--line-3);
  border-bottom: 1px solid var(--line-3);
  opacity: 0.7;
}

.dr-grip:hover {
  background: rgba(124, 154, 136, 0.22);
}

.dr-resizing,
.dr-resizing * {
  user-select: none;
}

.drawer-size-reset {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: var(--r-md);
  background: transparent;
  color: var(--ink-4);
  cursor: pointer;
}

.drawer-size-reset svg {
  width: 14px;
  height: 14px;
}

.drawer-size-reset:hover {
  background: rgba(0, 0, 0, 0.04);
  color: var(--ink-2);
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

/* .mini-select 现在挂在 antd 的 <a-select> 上（原来是原生 <select>）。
   原生控件那套 border / padding / height 直接加在 antd 外层容器上会变成"框套框"，
   所以这里只负责宽度分配，外观统一交给 styles/global.css 的 .mini-select.ant-select。 */
.mini-select {
  flex: 1 1 0;
  min-width: 0;
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
  overflow: hidden;
}

/* 图形画布：占满抽屉剩余高度，尺寸由 ResizeObserver 量出来给布局用 */
.graph-canvas {
  flex: 1;
  min-height: 0;
  align-self: stretch;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

/* 族谱可能比抽屉大，这一条允许滚动看全 */
.graph-canvas-scroll {
  align-items: flex-start;
  overflow: auto;
}

.graph-scale-note {
  margin-left: auto;
  font-size: 10px;
  color: var(--ink-5);
}

.graph-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: none;
  align-self: stretch;
  font-size: 10.5px;
  color: var(--ink-4);
}

.graph-meta {
  font-size: 11px;
  color: var(--ink-4);
}

.graph-svg {
  display: block;
  flex: none;
}

/* ── 关系图：人物 / 事物 / 事件各长各的样，一眼分得开 ── */
.graph-node {
  cursor: pointer;
}

.graph-node .gn-shape {
  stroke-width: 1;
  filter: drop-shadow(0 4px 10px rgba(89, 107, 94, 0.1));
  transition: filter 140ms var(--ease), stroke 140ms var(--ease), fill 140ms var(--ease);
}

.graph-node:hover .gn-shape {
  filter: drop-shadow(0 6px 14px rgba(89, 107, 94, 0.2));
}

/* 当前中心：描边加重、底色加深 */
.graph-node.focus .gn-shape {
  stroke-width: 2;
  filter: drop-shadow(0 6px 16px rgba(93, 122, 106, 0.26));
}

.gn-shape.k-person {
  fill: #e8efea;
  stroke: #a6bfb0;
}

.gn-shape.k-thing {
  fill: #edeaf0;
  stroke: #b8b0c6;
}

.gn-shape.k-event {
  fill: #efede7;
  stroke: #d1ccc2;
}

.graph-node.focus .gn-shape.k-person {
  fill: #dde5df;
  stroke: #5d7a6a;
}

.graph-node.focus .gn-shape.k-thing {
  fill: #e4e0ea;
  stroke: #8f86a0;
}

.graph-node.focus .gn-shape.k-event {
  fill: #e9e5dc;
  stroke: #a0917c;
}

.graph-node .node-name {
  font-family: var(--font-sans);
  font-size: 12.5px;
  font-weight: 500;
  fill: #2e3d34;
  pointer-events: none;
  user-select: none;
}

.graph-node .node-name.dark {
  fill: #3b3740;
}

/* 图例 */
.gn-legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
  flex: none;
  align-self: stretch;
  padding-top: 2px;
  font-size: 10px;
  color: var(--ink-5);
}

.gn-key {
  width: 14px;
  height: 9px;
  margin-left: 7px;
  border-radius: 5px;
  border: 1px solid #a6bfb0;
  background: #e8efea;
}

.gn-key:first-child {
  margin-left: 0;
}

/* 人物 = 圆形 */
.gn-key.k-person {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.gn-key.k-thing {
  width: 9px;
  height: 9px;
  border-radius: 1px;
  border-color: #b8b0c6;
  background: #edeaf0;
  transform: rotate(45deg);
}

.gn-key.k-event {
  border-radius: 2px;
  border-color: #d1ccc2;
  background: #efede7;
}

.gn-key.g-male {
  border-color: #a9bcca;
  background: #e6edf2;
}

.gn-key.g-female {
  border-color: #cbb0b4;
  background: #f2e9ea;
}

.gn-key.g-unknown {
  border-color: #d1ccc2;
  background: #efede7;
}

.gn-sep {
  width: 1px;
  height: 10px;
  margin: 0 5px;
  background: var(--line-3);
}

/* ── 族谱图：一代一行 ── */
.fam-edge {
  fill: none;
  stroke: #bdb6a9;
  stroke-width: 1.2;
  stroke-linejoin: round;
  stroke-linecap: round;
}

.fam-edge.spouse {
  stroke: #a6bfb0;
  stroke-width: 1.6;
}

.fam-spouse {
  fill: #7c9a88;
}

.fam-node {
  stroke-width: 1;
}

.fam-node.g-male {
  fill: #e6edf2;
  stroke: #a9bcca;
}

.fam-node.g-female {
  fill: #f2e9ea;
  stroke: #cbb0b4;
}

.fam-node.g-unknown {
  fill: #efede7;
  stroke: #d1ccc2;
}

.fam-name {
  font-family: var(--font-sans);
  font-size: 12.5px;
  font-weight: 500;
  fill: #2e3d34;
  pointer-events: none;
  user-select: none;
}

.fam-meta {
  font-family: var(--font-mono);
  font-size: 9.5px;
  fill: #9c978e;
  pointer-events: none;
  user-select: none;
}

.graph-empty {
  font-family: var(--font-sans);
  font-size: 12.5px;
  fill: var(--ink-5);
}

/* ═══ 关系图：列表层 ═══ */
.drawer-back {
  flex: none;
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  padding: 0;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--ink-3);
  cursor: pointer;
}

.drawer-back:hover {
  background: var(--hover-fill, #edeae4);
  color: var(--ink-1);
}

.drawer-back svg {
  width: 14px;
  height: 14px;
}

.drawer-actions {
  flex: none;
  padding: 12px 14px 4px;
}

.drawer-actions .primary-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.drawer-actions svg {
  width: 11px;
  height: 11px;
}

.graph-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 12px;
  border: 0;
  border-radius: var(--r-lg, 10px);
  background: transparent;
  color: var(--ink-1);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 120ms var(--ease);
}

.graph-row:hover {
  background: var(--event-fill);
}

.graph-row svg {
  flex: none;
  width: 15px;
  height: 15px;
  color: #7C9A88;
}

.graph-row-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12.5px;
  font-weight: 500;
}

.graph-row-time {
  flex: none;
  font-size: 10px;
  color: var(--ink-5);
}

/* ═══ 关系图：详情层编辑 ═══ */
.rel-row .row-del {
  flex: none;
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  padding: 0;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #b3aca2;
  cursor: pointer;
  opacity: 0;
  transition: opacity 120ms var(--ease), background 120ms var(--ease), color 120ms var(--ease);
}

.rel-row:hover .row-del {
  opacity: 1;
}

.rel-row .row-del:hover {
  background: #f6e6e3;
  color: #9c5a50;
}

.row-del svg {
  width: 10px;
  height: 10px;
}

.edge-add {
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px dashed var(--line-3);
  border-radius: var(--r-lg, 10px);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.edge-add-title {
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.04em;
  color: var(--ink-5);
}

.edge-add-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.edge-arr {
  flex: none;
  color: var(--ink-5);
  font-size: 11px;
}

.mini-input {
  flex: 1;
  min-width: 0;
  height: 26px;
  padding: 0 8px;
  border: 1px solid var(--line-3);
  border-radius: 7px;
  background: #fff;
  font-family: inherit;
  font-size: 11.5px;
  color: var(--ink-1);
  outline: 0;
}

.mini-input:focus {
  border-color: #a6bfb0;
}

.primary-btn.sm {
  flex: none;
  height: 26px;
  padding: 0 12px;
  font-size: 11.5px;
}

.nodes-block {
  margin-top: 12px;
  padding: 0 2px;
}

.node-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.node-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 6px 3px 9px;
  border-radius: 20px;
  border: 1px solid transparent;
  background: #e8efea;
  font-size: 11px;
  color: #2e3d34;
}

/* 关系图对象：按类型上色，和图形视图里的形状颜色对齐 */
.node-chip.k-person {
  border-color: #a6bfb0;
  background: #e8efea;
  color: #2e3d34;
}

.node-chip.k-thing {
  border-color: #b8b0c6;
  background: #edeaf0;
  color: #3b3740;
}

.node-chip.k-event {
  border-color: #d1ccc2;
  background: #efede7;
  color: #3e3a33;
}

/* 族谱成员：按性别上色 */
.node-chip.g-male {
  border-color: #a9bcca;
  background: #e6edf2;
  color: #34424d;
}

.node-chip.g-female {
  border-color: #cbb0b4;
  background: #f2e9ea;
  color: #4a3a3d;
}

.node-chip.g-unknown {
  border-color: #d1ccc2;
  background: #efede7;
  color: #4a4640;
}

.node-chip button {
  display: grid;
  place-items: center;
  width: 14px;
  height: 14px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #8fa898;
  cursor: pointer;
}

.node-chip button:hover {
  background: rgba(0, 0, 0, 0.08);
  color: #9c5a50;
}

.node-chip button svg {
  width: 8px;
  height: 8px;
}

.drawer-foot {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 14px;
  border-top: 1px solid var(--line-4);
}

.drawer-foot-note {
  font-size: 10px;
  color: var(--ink-5);
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

/* ═══════════ 侧栏整体宽度拖柄 ═══════════ */
.side-resize {
  position: relative;
  z-index: 5;
  flex: none;
  width: 5px;
  margin: 0 -2px;
  cursor: col-resize;
  background: transparent;
  transition: background 140ms var(--ease);
}

.side-resize:hover,
.side-resize.active {
  background: rgba(124, 154, 136, 0.35);
}

/* ═══════════ 工具栏握把 / 拖动态 ═══════════ */
.tb-btn.grip {
  cursor: grab;
}

.toolbar.tb-dragging,
.toolbar.tb-dragging .tb-btn.grip {
  cursor: grabbing;
}

.toolbar.tb-dragging {
  box-shadow: 0 12px 30px -6px rgba(89, 84, 74, 0.24);
}

/* 吸附回左侧原位时给一圈很淡的提示，不做大动作 */
.toolbar.tb-pinned {
  box-shadow: var(--sh-float), 0 0 0 1px rgba(124, 154, 136, 0.32);
}

/* ═══════════ 关系图详情层：新增对象 / 关系 ═══════════ */
.edge-add-note {
  margin-left: 6px;
  font-size: 9.5px;
  font-weight: 400;
  color: var(--ink-5);
}

.edge-add-empty {
  margin: 2px 0 0;
  font-size: 10.5px;
  color: var(--ink-5);
}
.edge-add-hint {
  margin: 2px 0 0;
  font-size: 10px;
  line-height: 1.55;
  color: var(--ink-5);
}

.mini-select-s {
  flex: none;
  width: 74px;
}

/* 图形视图里的关系标签：白色光晕，压在连线上也读得清 */
.edge-label {
  font-family: var(--font-sans);
  font-size: 10px;
  fill: #6B665E;
  pointer-events: none;
  paint-order: stroke;
  stroke: rgba(255, 255, 255, 0.92);
  stroke-width: 3px;
  stroke-linejoin: round;
}

/* 更小一号的幽灵按钮（「从画布重新提取」） */
.ghost-btn.xs {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  align-self: flex-start;
  height: 26px;
  padding: 0 10px;
  border-radius: 8px;
  font-size: 11px;
}

.ghost-btn.xs svg {
  width: 12px;
  height: 12px;
}

.ghost-btn.xs:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
