import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { pageApi, type PageVO, type PageCreateDTO, type PageUpdateDTO } from '@/api/page'
import { eventApi, type EventVO, type EventCreateDTO, type EventUpdateDTO } from '@/api/event'

export const useWorkspaceStore = defineStore('workspace', () => {
  // 页面状态
  const pages = ref<PageVO[]>([])
  const currentPage = ref<PageVO | null>(null)
  const pageLoading = ref(false)

  // 事件状态
  const events = ref<EventVO[]>([])
  const currentEvent = ref<EventVO | null>(null)
  const eventLoading = ref(false)

  // 计算属性
  const pageCount = computed(() => pages.value.length)
  const eventCount = computed(() => events.value.length)

  // 页面操作方法
  async function fetchPagesByBookId(bookId: number) {
    pageLoading.value = true
    try {
      const response = await pageApi.getPagesByBookId(bookId)
      pages.value = response.data
    } catch (error) {
      console.error('获取页面列表失败:', error)
    } finally {
      pageLoading.value = false
    }
  }

  async function createPage(data: PageCreateDTO) {
    pageLoading.value = true
    try {
      const response = await pageApi.createPage(data)
      pages.value.push(response.data)
      return response.data
    } catch (error) {
      console.error('创建页面失败:', error)
      throw error
    } finally {
      pageLoading.value = false
    }
  }

  async function updatePage(id: number, data: PageUpdateDTO) {
    pageLoading.value = true
    try {
      const response = await pageApi.updatePage(id, data)
      const index = pages.value.findIndex(p => p.id === id)
      if (index !== -1) {
        pages.value[index] = response.data
      }
      if (currentPage.value?.id === id) {
        currentPage.value = response.data
      }
      return response.data
    } catch (error) {
      console.error('更新页面失败:', error)
      throw error
    } finally {
      pageLoading.value = false
    }
  }

  async function deletePage(id: number) {
    pageLoading.value = true
    try {
      await pageApi.deletePage(id)
      pages.value = pages.value.filter(p => p.id !== id)
      if (currentPage.value?.id === id) {
        currentPage.value = null
      }
    } catch (error) {
      console.error('删除页面失败:', error)
      throw error
    } finally {
      pageLoading.value = false
    }
  }

  async function savePageCanvas(id: number, canvasData: string) {
    pageLoading.value = true
    try {
      const response = await pageApi.savePageCanvas(id, canvasData)
      const index = pages.value.findIndex(p => p.id === id)
      if (index !== -1) {
        pages.value[index] = response.data
      }
      if (currentPage.value?.id === id) {
        currentPage.value = response.data
      }
      return response.data
    } catch (error) {
      console.error('保存页面画布失败:', error)
      throw error
    } finally {
      pageLoading.value = false
    }
  }

  // 事件操作方法
  async function fetchEventsByPageId(pageId: number) {
    eventLoading.value = true
    try {
      const response = await eventApi.getEventsByPageId(pageId)
      events.value = response.data
    } catch (error) {
      console.error('获取事件列表失败:', error)
    } finally {
      eventLoading.value = false
    }
  }

  async function createEvent(data: EventCreateDTO) {
    eventLoading.value = true
    try {
      const response = await eventApi.createEvent(data)
      events.value.push(response.data)
      return response.data
    } catch (error) {
      console.error('创建事件失败:', error)
      throw error
    } finally {
      eventLoading.value = false
    }
  }

  async function updateEvent(id: number, data: EventUpdateDTO) {
    eventLoading.value = true
    try {
      const response = await eventApi.updateEvent(id, data)
      const index = events.value.findIndex(e => e.id === id)
      if (index !== -1) {
        events.value[index] = response.data
      }
      if (currentEvent.value?.id === id) {
        currentEvent.value = response.data
      }
      return response.data
    } catch (error) {
      console.error('更新事件失败:', error)
      throw error
    } finally {
      eventLoading.value = false
    }
  }

  async function deleteEvent(id: number) {
    eventLoading.value = true
    try {
      await eventApi.deleteEvent(id)
      events.value = events.value.filter(e => e.id !== id)
      if (currentEvent.value?.id === id) {
        currentEvent.value = null
      }
    } catch (error) {
      console.error('删除事件失败:', error)
      throw error
    } finally {
      eventLoading.value = false
    }
  }

  // 设置当前页面和事件
  function setCurrentPage(page: PageVO | null) {
    currentPage.value = page
    if (page) {
      fetchEventsByPageId(page.id)
    } else {
      events.value = []
    }
  }

  function setCurrentEvent(event: EventVO | null) {
    currentEvent.value = event
  }

  // 清空工作空间
  function clearWorkspace() {
    pages.value = []
    currentPage.value = null
    events.value = []
    currentEvent.value = null
  }

  return {
    // 页面状态
    pages,
    currentPage,
    pageLoading,

    // 事件状态
    events,
    currentEvent,
    eventLoading,

    // 计算属性
    pageCount,
    eventCount,

    // 页面方法
    fetchPagesByBookId,
    createPage,
    updatePage,
    deletePage,
    savePageCanvas,

    // 事件方法
    fetchEventsByPageId,
    createEvent,
    updateEvent,
    deleteEvent,

    // 工具方法
    setCurrentPage,
    setCurrentEvent,
    clearWorkspace
  }
})