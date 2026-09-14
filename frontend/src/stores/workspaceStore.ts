import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { bookApi } from '@/api/book'
import { eventApi } from '@/api/event'
import { pageApi } from '@/api/page'
import type {
  BookResponse,
  CanvasResponse,
  EventResponse,
  PageResponse,
  Timeline,
  TimelinePoint,
  WorkspacePage
} from '@/api/types'

/** 保存状态机：idle → saving → saved | error */
export type SaveState = 'idle' | 'saving' | 'saved' | 'error'

export function emptyCanvas(): CanvasResponse {
  return {
    background: 'plain',
    objects: [],
    relationships: [],
    annotations: [],
    timelines: []
  }
}

/** 本地临时 id（后端保存时会分配正式 id） */
export function localId(prefix: string): string {
  return `${prefix}_${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`
}

/**
 * 工作区 store —— 承载 Book → Event → Page → Canvas 四级结构
 * 数据形状严格跟随 `GET /api/books/{id}/workspace`
 */
export const useWorkspaceStore = defineStore('workspace', () => {
  /* ---------------- Book ---------------- */
  const book = ref<BookResponse | null>(null)

/* ---------------- Event 树 ---------------- */
/**
 * 工作区返回的 event 是精简结构（无 bookId / 时间戳），
 * 这里补全成完整的 EventResponse，避免下游到处做非空判断。
 */
const events = ref<EventResponse[]>([])
/** eventId → 该 event 下的页面（workspace 一次性带回，之后由增删改维护） */
const pagesByEvent = ref<Record<number, WorkspacePage[]>>({})

  /* ---------------- 当前选中 ---------------- */
  const currentEventId = ref<number | null>(null)
  const currentPageId = ref<number | null>(null)

  /* ---------------- 画布 ---------------- */
  const canvas = ref<CanvasResponse>(emptyCanvas())
  const canvasLoaded = ref(false)

  /* ---------------- 加载态 ---------------- */
  const loading = ref(false)
  const canvasLoading = ref(false)
  const saving = ref(false)
  const saveState = ref<SaveState>('idle')
  const lastSavedAt = ref<string>('')

  /* ---------------- 派生 ---------------- */
  const currentEvent = computed(
    () => events.value.find((e) => e.id === currentEventId.value) ?? null
  )

  const pages = computed<WorkspacePage[]>(() => {
    if (currentEventId.value == null) return []
    return pagesByEvent.value[currentEventId.value] ?? []
  })

  const currentPage = computed<PageResponse | null>(() => {
    const p = pages.value.find((x) => x.id === currentPageId.value)
    if (!p || currentEventId.value == null) return null
    return {
      id: p.id,
      eventId: currentEventId.value,
      name: p.name,
      sortOrder: p.sortOrder,
      createdAt: '',
      updatedAt: ''
    }
  })

  const eventCount = computed(() => events.value.length)

  const totalPageCount = computed(() =>
    events.value.reduce((sum, e) => sum + (pagesByEvent.value[e.id]?.length ?? 0), 0)
  )

  const timelines = computed(() => canvas.value.timelines)

  /* ---------------- 加载工作区 ---------------- */
  async function loadWorkspace(bookId: number) {
    loading.value = true
    try {
      const ws = await bookApi.getWorkspace(bookId)
      book.value = ws.book

      events.value = [...ws.events]
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map((e) => ({
          id: e.id,
          name: e.name,
          sortOrder: e.sortOrder,
          bookId: ws.book.id,
          createdAt: ws.book.createdAt,
          updatedAt: ws.book.updatedAt
        }))

      const map: Record<number, WorkspacePage[]> = {}
      for (const e of ws.events) {
        map[e.id] = [...e.pages].sort((a, b) => a.sortOrder - b.sortOrder)
      }
      pagesByEvent.value = map

      const firstEvent = events.value[0]
      if (firstEvent) {
        currentEventId.value = firstEvent.id
        const firstPage = map[firstEvent.id]?.[0]
        currentPageId.value = firstPage ? firstPage.id : null
        if (firstPage) await loadCanvas(firstPage.id)
        else {
          canvas.value = emptyCanvas()
          canvasLoaded.value = false
        }
      } else {
        currentEventId.value = null
        currentPageId.value = null
        canvas.value = emptyCanvas()
        canvasLoaded.value = false
      }
    } catch (e) {
      console.error('[workspaceStore] loadWorkspace failed:', e)
      book.value = null
      events.value = []
      pagesByEvent.value = {}
      currentEventId.value = null
      currentPageId.value = null
      canvas.value = emptyCanvas()
      canvasLoaded.value = false
    } finally {
      loading.value = false
    }
  }

  /* ---------------- Event CRUD ---------------- */
  async function selectEvent(eventId: number) {
    if (eventId === currentEventId.value) return
    // 切换前先落盘，否则未保存的画布会随画布替换一起丢失
    await safeFlush()
    currentEventId.value = eventId
    const first = pagesByEvent.value[eventId]?.[0]
    if (first) {
      await selectPage(first.id)
    } else {
      currentPageId.value = null
      canvas.value = emptyCanvas()
      canvasLoaded.value = false
    }
  }

  async function createEvent(name: string): Promise<EventResponse> {
    if (!book.value) throw new Error('未加载书籍')
    const created = await eventApi.createEvent(book.value.id, { name })
    events.value.push(created)
    events.value.sort((a, b) => a.sortOrder - b.sortOrder)
    pagesByEvent.value[created.id] = pagesByEvent.value[created.id] ?? []
    currentEventId.value = created.id
    currentPageId.value = null
    canvas.value = emptyCanvas()
    canvasLoaded.value = false
    return created
  }

  async function renameEvent(id: number, name: string) {
    const updated = await eventApi.updateEvent(id, { name })
    const i = events.value.findIndex((e) => e.id === id)
    if (i !== -1) events.value[i] = { ...events.value[i], ...updated }
  }

  async function removeEvent(id: number) {
    await eventApi.deleteEvent(id)
    events.value = events.value.filter((e) => e.id !== id)
    delete pagesByEvent.value[id]
    if (currentEventId.value === id) {
      const next = events.value[0]
      currentEventId.value = next ? next.id : null
      const firstPage = next ? pagesByEvent.value[next.id]?.[0] : undefined
      if (firstPage) await selectPage(firstPage.id)
      else {
        currentPageId.value = null
        canvas.value = emptyCanvas()
        canvasLoaded.value = false
      }
    }
  }

  async function reorderEvents(ordered: EventResponse[]) {
    if (!book.value) return
    events.value = ordered.map((e, i) => ({ ...e, sortOrder: i }))
    await eventApi.sortEvents(
      book.value.id,
      events.value.map((e) => ({ id: e.id, sortOrder: e.sortOrder }))
    )
  }

  /* ---------------- Page CRUD ---------------- */
  async function selectPage(pageId: number) {
    if (pageId === currentPageId.value) return
    // 切换前先落盘：这是「切 page 后数据消失」的修复点
    await safeFlush()
    currentPageId.value = pageId
    await loadCanvas(pageId)
  }

  async function createPage(name: string): Promise<WorkspacePage> {
    if (currentEventId.value == null) throw new Error('未选择事件')
    const created = await pageApi.createPage(currentEventId.value, { name })
    const list = pagesByEvent.value[currentEventId.value] ?? []
    const item: WorkspacePage = {
      id: created.id,
      name: created.name,
      sortOrder: created.sortOrder
    }
    list.push(item)
    list.sort((a, b) => a.sortOrder - b.sortOrder)
    pagesByEvent.value[currentEventId.value] = list
    await selectPage(created.id)
    return item
  }

  async function renamePage(id: number, name: string) {
    const updated = await pageApi.updatePage(id, { name })
    for (const key of Object.keys(pagesByEvent.value)) {
      const list = pagesByEvent.value[Number(key)]
      const i = list.findIndex((p) => p.id === id)
      if (i !== -1) list[i] = { ...list[i], name: updated.name }
    }
  }

  async function removePage(id: number) {
    const eventId = currentEventId.value
    // 该页马上要没了，先丢弃它尚未落盘的作业，避免 PUT 打到已删除的 page
    dropPendingSave(id)
    await pageApi.deletePage(id)
    if (eventId == null) return
    const list = pagesByEvent.value[eventId] ?? []
    pagesByEvent.value[eventId] = list.filter((p) => p.id !== id)

    if (currentPageId.value === id) {
      const next = pagesByEvent.value[eventId]?.[0]
      if (next) {
        // 目标页与当前页不同，正常走强制落盘 + 加载
        await selectPage(next.id)
      } else {
        currentPageId.value = null
        canvas.value = emptyCanvas()
        canvasLoaded.value = false
      }
    }
  }

  async function reorderPages(ordered: WorkspacePage[]) {
    const eventId = currentEventId.value
    if (eventId == null) return
    const next = ordered.map((p, i) => ({ ...p, sortOrder: i }))
    pagesByEvent.value[eventId] = next
    await pageApi.sortPages(
      eventId,
      next.map((p) => ({ id: p.id, sortOrder: p.sortOrder }))
    )
  }

  /* ---------------- 画布 ---------------- */

  /**
   * 待保存队列。
   *
   * 关键：入队时就对画布做**深拷贝快照**，并记住当时的 pageId。
   * 这样即使用户在防抖窗口内切走了 page，旧页的数据依然会被正确写回旧页，
   * 而不会被「新页已加载的 canvas」冒名顶替（这是切页丢数据的根因）。
   */
  interface SaveJob {
    pageId: number
    data: CanvasResponse
    at: number
  }

  let saveQueue: SaveJob[] = []
  let saveTimer: number | undefined

  function snapshot(src: CanvasResponse): CanvasResponse {
    return JSON.parse(JSON.stringify(src)) as CanvasResponse
  }

  /** 把当前画布状态压入待保存队列（同一 page 只保留最新一份） */
  function stashCurrent() {
    const pageId = currentPageId.value
    if (pageId == null || !canvasLoaded.value) return
    saveQueue = saveQueue.filter((j) => j.pageId !== pageId)
    saveQueue.push({ pageId, data: snapshot(canvas.value), at: Date.now() })
  }

  /** 防抖入队：停止操作 delay 毫秒后落盘 */
  function scheduleSave(delay = 800) {
    stashCurrent()
    if (saveTimer) window.clearTimeout(saveTimer)
    saveTimer = window.setTimeout(() => {
      void flushSave()
    }, delay)
  }

  /** 立即落盘：把队列里所有待保存的画布写回各自所属的 page */
  async function flushSave(): Promise<void> {
    if (saveTimer) {
      window.clearTimeout(saveTimer)
      saveTimer = undefined
    }
    if (!saveQueue.length) return

    saving.value = true
    saveState.value = 'saving'
    const jobs = saveQueue
    saveQueue = []

    try {
      for (const job of jobs) {
        await pageApi.saveCanvas(job.pageId, job.data)
      }
      saveState.value = 'saved'
      lastSavedAt.value = new Date().toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
      })
    } catch (e) {
      // 失败的作业放回队首，避免静默丢数据
      saveQueue = [...jobs, ...saveQueue]
      saveState.value = 'error'
      throw e
    } finally {
      saving.value = false
    }
  }

  /** 立即保存当前画布（先入队再落盘） */
  async function saveCanvas(): Promise<void> {
    stashCurrent()
    await flushSave()
  }

  /** 丢弃某个 page 的待保存作业（该 page 已被删除时调用） */
  function dropPendingSave(pageId: number) {
    saveQueue = saveQueue.filter((j) => j.pageId !== pageId)
    if (!saveQueue.length && saveTimer) {
      window.clearTimeout(saveTimer)
      saveTimer = undefined
    }
  }

  /**
   * 切换前的尽力落盘：即使保存失败也不阻塞导航。
   * 失败的作业已回到队列，会在下一次切换 / 保存时重试，不会静默丢失。
   */
  async function safeFlush() {
    try {
      await flushSave()
    } catch {
      /* 保持队列，等下次重试 */
    }
  }

  async function loadCanvas(pageId: number) {
    canvasLoading.value = true
    try {
      const data = await pageApi.getCanvas(pageId)
      canvas.value = {
        // background 由后端随画布一起下发，缺省回落纯白
        background: (data.background as CanvasResponse['background']) ?? 'plain',
        // 画布尺寸随画布一起下发；后端还没这个字段时留 null，由页面用本地兜底
        canvasWidth: data.canvasWidth ?? null,
        canvasHeight: data.canvasHeight ?? null,
        objects: data.objects ?? [],
        relationships: data.relationships ?? [],
        annotations: data.annotations ?? [],
        timelines: data.timelines ?? []
      }
      canvasLoaded.value = true
      saveState.value = saveQueue.length ? saveState.value : 'idle'
    } catch (e) {
      console.error('[workspaceStore] loadCanvas failed:', pageId, e)
      canvas.value = emptyCanvas()
      // 置为 false：加载失败时不允许入队保存，否则会把空画布写回覆盖真实数据
      canvasLoaded.value = false
    } finally {
      canvasLoading.value = false
    }
  }

  /* ---------------- 画布局部操作 ---------------- */
  function setTimelineDirection(timelineId: string, direction: Timeline['direction']) {
    const t = canvas.value.timelines.find((x) => x.id === timelineId)
    if (t) t.direction = direction
  }

  function renameTimeline(timelineId: string, name: string) {
    const t = canvas.value.timelines.find((x) => x.id === timelineId)
    if (t) t.name = name
  }

  function addTimelinePoint(timelineId: string, point: TimelinePoint) {
    const t = canvas.value.timelines.find((x) => x.id === timelineId)
    if (t) t.points.push(point)
  }

  function updateTimelinePoint(timelineId: string, pointId: string, patch: Partial<TimelinePoint>) {
    const t = canvas.value.timelines.find((x) => x.id === timelineId)
    if (!t) return
    const i = t.points.findIndex((p) => p.id === pointId)
    if (i >= 0) t.points[i] = { ...t.points[i], ...patch }
  }

  function removeTimelinePoint(timelineId: string, pointId: string) {
    const t = canvas.value.timelines.find((x) => x.id === timelineId)
    if (!t) return
    t.points = t.points.filter((p) => p.id !== pointId)
  }

  function addTimeline(name: string, direction: Timeline['direction'] = 'horizontal'): Timeline {
    const n = canvas.value.timelines.length
    const tl: Timeline = {
      id: localId('tl'),
      name,
      direction,
      points: [],
      x: 44 + n * 24,
      y: 44 + n * 24
    }
    canvas.value.timelines.push(tl)
    return tl
  }

  function setTimelinePosition(timelineId: string, x: number, y: number) {
    const t = canvas.value.timelines.find((x2) => x2.id === timelineId)
    if (!t) return
    t.x = x
    t.y = y
  }

  /** 时间线面板尺寸（横向存 width，纵向存 height） */
  function setTimelineSize(timelineId: string, size: { width?: number; height?: number }) {
    const t = canvas.value.timelines.find((x) => x.id === timelineId)
    if (!t) return
    if (size.width != null) t.width = size.width
    if (size.height != null) t.height = size.height
  }

  function removeTimeline(timelineId: string) {
    canvas.value.timelines = canvas.value.timelines.filter((t) => t.id !== timelineId)
  }

  /**
   * 设置画布背景。
   * 背景随画布一起存到后端（canvas.background），localStorage 只作为离线兜底。
   */
  function setCanvasBackground(bg: string) {
    canvas.value.background = bg
    scheduleSave()
  }

  /* ---------------- 批量重排 ---------------- */
  function applyEventOrder(ordered: EventResponse[]) {
    events.value = ordered.map((e, i) => ({ ...e, sortOrder: i }))
  }

  function applyPageOrder(eventId: number, ordered: WorkspacePage[]) {
    pagesByEvent.value[eventId] = ordered.map((p, i) => ({
      ...p,
      sortOrder: i
    })) as WorkspacePage[]
  }

  function clearWorkspace() {
    if (saveTimer) {
      window.clearTimeout(saveTimer)
      saveTimer = undefined
    }
    saveQueue = []
    book.value = null
    events.value = []
    pagesByEvent.value = {}
    currentEventId.value = null
    currentPageId.value = null
    canvas.value = emptyCanvas()
    canvasLoaded.value = false
    saveState.value = 'idle'
  }

  return {
    // state
    book,
    events,
    pagesByEvent,
    currentEventId,
    currentPageId,
    canvas,
    canvasLoaded,
    loading,
    canvasLoading,
    saving,
    saveState,
    lastSavedAt,

    // getters
    currentEvent,
    currentPage,
    pages,
    eventCount,
    totalPageCount,
    timelines,

    // event actions
    loadWorkspace,
    selectEvent,
    createEvent,
    renameEvent,
    removeEvent,
    reorderEvents,

    // page actions
    selectPage,
    createPage,
    renamePage,
    removePage,
    reorderPages,

    // canvas actions
    loadCanvas,
    saveCanvas,
    scheduleSave,
    flushSave,
    setTimelineDirection,
    renameTimeline,
    addTimelinePoint,
    updateTimelinePoint,
    removeTimelinePoint,
    addTimeline,
    setTimelinePosition,
    setTimelineSize,
    removeTimeline,
    setCanvasBackground,

    // 重排
    applyEventOrder,
    applyPageOrder,

    clearWorkspace
  }
})
