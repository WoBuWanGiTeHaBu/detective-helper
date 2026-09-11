<template>
  <div class="workspace-view">
    <a-layout>
      <a-layout-header>
        <div class="header-content">
          <div class="left-content">
            <a-button type="text" @click="goBack">
              <ArrowLeftOutlined /> 返回书架
            </a-button>
            <h1 class="title">{{ currentBook?.title }}</h1>
          </div>
          <a-button type="primary" @click="showCreatePageModal">
            <template #icon><PlusOutlined /></template>
            新建页面
          </a-button>
        </div>
      </a-layout-header>

      <a-layout>
        <a-layout-sider width="300" theme="light">
          <div class="sidebar-content">
            <div class="section-title">页面列表</div>
            <a-list
              :data-source="workspaceStore.pages"
              :loading="workspaceStore.pageLoading"
              size="small"
              @click="handlePageClick"
            >
              <template #renderItem="{ item }">
                <a-list-item
                  :class="{ 'active-page': workspaceStore.currentPage?.id === item.id }"
                  class="page-item"
                >
                  <div class="page-item-content">
                    <div class="page-title">{{ item.title || '未命名页面' }}</div>
                    <div class="page-meta">
                      <span>{{ item.eventCount }} 个事件</span>
                      <span>{{ formatDate(item.updatedAt) }}</span>
                    </div>
                  </div>
                  <div class="page-actions" @click.stop>
                    <a-button type="text" size="small" @click="editPage(item)">
                      <EditOutlined />
                    </a-button>
                    <a-button type="text" size="small" danger @click="deletePage(item.id)">
                      <DeleteOutlined />
                    </a-button>
                  </div>
                </a-list-item>
              </template>
            </a-list>
          </div>
        </a-layout-sider>

        <a-layout-content>
          <div v-if="workspaceStore.currentPage" class="content-area">
            <div class="content-header">
              <h2>{{ workspaceStore.currentPage.title }}</h2>
              <a-space>
                <a-button @click="showEventModal">添加事件</a-button>
                <a-button @click="toggleCanvasView">切换画布</a-button>
              </a-space>
            </div>

            <!-- 编辑器视图 -->
            <div v-if="!showCanvas" class="editor-view">
              <a-textarea
                v-model:value="content"
                placeholder="开始记录案件信息..."
                :rows="20"
                @change="handleContentChange"
              />
            </div>

            <!-- 画布视图 -->
            <div v-else class="canvas-view">
              <CanvasEditor
                :page-id="workspaceStore.currentPage.id"
                :canvas-data="canvasStore.canvasData"
                @save="handleCanvasSave"
              />
            </div>

            <!-- 事件列表 -->
            <div class="events-section">
              <h3>事件列表</h3>
              <a-timeline>
                <a-timeline-item
                  v-for="event in workspaceStore.events"
                  :key="event.id"
                  :color="event.eventTime ? 'blue' : 'gray'"
                >
                  <div class="event-item">
                    <div class="event-header">
                      <h4>{{ event.title || '未命名事件' }}</h4>
                      <a-space>
                        <a-button type="text" size="small" @click="editEvent(event)">
                          <EditOutlined />
                        </a-button>
                        <a-button type="text" size="small" danger @click="deleteEvent(event.id)">
                          <DeleteOutlined />
                        </a-button>
                      </a-space>
                    </div>
                    <p>{{ event.description }}</p>
                    <small class="event-time">
                      {{ event.eventTime ? formatDate(event.eventTime) : '无时间记录' }}
                    </small>
                  </div>
                </a-timeline-item>
              </a-timeline>

              <a-empty v-if="workspaceStore.events.length === 0" description="暂无事件" />
            </div>
          </div>

          <a-empty v-else description="请选择一个页面开始工作" />
        </a-layout-content>
      </a-layout>
    </a-layout>

    <!-- 新建/编辑页面对话框 -->
    <a-modal
      v-model:open="pageModalVisible"
      :title="isEditPage ? '编辑页面' : '新建页面'"
      @ok="handlePageSubmit"
    >
      <a-form :model="pageFormData" layout="vertical">
        <a-form-item label="页面标题" required>
          <a-input v-model:value="pageFormData.title" placeholder="请输入页面标题" />
        </a-form-item>
        <a-form-item label="初始内容">
          <a-textarea
            v-model:value="pageFormData.content"
            placeholder="请输入初始内容"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 新建/编辑事件对话框 -->
    <a-modal
      v-model:open="eventModalVisible"
      :title="isEditEvent ? '编辑事件' : '添加事件'"
      @ok="handleEventSubmit"
    >
      <a-form :model="eventFormData" layout="vertical">
        <a-form-item label="事件标题">
          <a-input v-model:value="eventFormData.title" placeholder="请输入事件标题" />
        </a-form-item>
        <a-form-item label="事件描述">
          <a-textarea
            v-model:value="eventFormData.description"
            placeholder="请输入事件描述"
            :rows="4"
          />
        </a-form-item>
        <a-form-item label="事件时间">
          <a-date-picker
            v-model:value="eventFormData.eventTime"
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs, { Dayjs } from 'dayjs'
import {
  ArrowLeftOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { useBookStore } from '@/stores/bookStore'
import { useWorkspaceStore } from '@/stores/workspaceStore'
import { useCanvasStore } from '@/stores/canvasStore'
import CanvasEditor from '@/components/canvas/CanvasEditor.vue'
import type { PageCreateDTO, PageUpdateDTO, PageVO } from '@/api/page'
import type { EventCreateDTO, EventUpdateDTO, EventVO } from '@/api/event'

const router = useRouter()
const route = useRoute()
const bookStore = useBookStore()
const workspaceStore = useWorkspaceStore()
const canvasStore = useCanvasStore()

const bookId = computed(() => Number(route.params.bookId))
const currentBook = computed(() => bookStore.currentBook)

const showCanvas = ref(false)
const content = ref('')

// 页面相关
const pageModalVisible = ref(false)
const isEditPage = ref(false)
const editingPage = ref<PageVO | null>(null)
const pageFormData = reactive({
  title: '',
  content: ''
})

// 事件相关
const eventModalVisible = ref(false)
const isEditEvent = ref(false)
const editingEvent = ref<EventVO | null>(null)
const eventFormData = reactive({
  title: '',
  description: '',
  eventTime: null as Dayjs | null
})

onMounted(async () => {
  await bookStore.getBookById(bookId.value)
  await workspaceStore.fetchPagesByBookId(bookId.value)

  // 如果有页面，加载第一个
  if (workspaceStore.pages.length > 0) {
    handlePageClick(workspaceStore.pages[0])
  }
})

function goBack() {
  router.push('/bookshelf')
}

function handlePageClick(page: PageVO) {
  workspaceStore.setCurrentPage(page)
  content.value = page.content || ''
  showCanvas.value = false
}

function showCreatePageModal() {
  isEditPage.value = false
  editingPage.value = null
  pageFormData.title = ''
  pageFormData.content = ''
  pageModalVisible.value = true
}

function editPage(page: PageVO) {
  isEditPage.value = true
  editingPage.value = page
  pageFormData.title = page.title
  pageFormData.content = page.content || ''
  pageModalVisible.value = true
}

async function handlePageSubmit() {
  if (!pageFormData.title.trim()) {
    message.error('请输入页面标题')
    return
  }

  try {
    if (isEditPage.value && editingPage.value) {
      const data: PageUpdateDTO = {
        title: pageFormData.title,
        content: pageFormData.content
      }
      await workspaceStore.updatePage(editingPage.value.id, data)
      message.success('页面更新成功')
    } else {
      const data: PageCreateDTO = {
        bookId: bookId.value,
        title: pageFormData.title,
        content: pageFormData.content
      }
      const newPage = await workspaceStore.createPage(data)
      message.success('页面创建成功')
      workspaceStore.setCurrentPage(newPage)
    }

    pageModalVisible.value = false
  } catch (error) {
    // 错误已在store中处理
  }
}

async function deletePage(id: number) {
  const confirmed = await new Promise(resolve => {
    const modal = message.confirm('确定要删除这个页面吗？')
    modal.then(() => resolve(true)).catch(() => resolve(false))
  })

  if (!confirmed) return

  try {
    await workspaceStore.deletePage(id)
    message.success('页面删除成功')
  } catch (error) {
    // 错误已在store中处理
  }
}

function showEventModal() {
  isEditEvent.value = false
  editingEvent.value = null
  eventFormData.title = ''
  eventFormData.description = ''
  eventFormData.eventTime = null
  eventModalVisible.value = true
}

function editEvent(event: EventVO) {
  isEditEvent.value = true
  editingEvent.value = event
  eventFormData.title = event.title
  eventFormData.description = event.description
  eventFormData.eventTime = event.eventTime ? dayjs(event.eventTime) : null
  eventModalVisible.value = true
}

async function handleEventSubmit() {
  if (!workspaceStore.currentPage) return

  try {
    const eventTime = eventFormData.eventTime
      ? eventFormData.eventTime.toISOString()
      : null

    if (isEditEvent.value && editingEvent.value) {
      const data: EventUpdateDTO = {
        title: eventFormData.title,
        description: eventFormData.description,
        eventTime: eventTime
      }
      await workspaceStore.updateEvent(editingEvent.value.id, data)
      message.success('事件更新成功')
    } else {
      const data: EventCreateDTO = {
        pageId: workspaceStore.currentPage.id,
        title: eventFormData.title,
        description: eventFormData.description,
        eventTime: eventTime
      }
      await workspaceStore.createEvent(data)
      message.success('事件创建成功')
    }

    eventModalVisible.value = false
  } catch (error) {
    // 错误已在store中处理
  }
}

async function deleteEvent(id: number) {
  const confirmed = await new Promise(resolve => {
    const modal = message.confirm('确定要删除这个事件吗？')
    modal.then(() => resolve(true)).catch(() => resolve(false))
  })

  if (!confirmed) return

  try {
    await workspaceStore.deleteEvent(id)
    message.success('事件删除成功')
  } catch (error) {
    // 错误已在store中处理
  }
}

async function handleContentChange() {
  if (!workspaceStore.currentPage) return

  try {
    await workspaceStore.updatePage(workspaceStore.currentPage.id, {
      content: content.value
    })
  } catch (error) {
    // 错误已在store中处理
  }
}

async function toggleCanvasView() {
  if (!workspaceStore.currentPage) return

  showCanvas.value = !showCanvas.value

  if (showCanvas.value) {
    await canvasStore.loadCanvas(workspaceStore.currentPage.id)
  }
}

async function handleCanvasSave(canvasData: string) {
  if (!workspaceStore.currentPage) return

  try {
    await canvasStore.saveCanvas(workspaceStore.currentPage.id, canvasData)
    message.success('画布保存成功')
  } catch (error) {
    // 错误已在store中处理
  }
}

function formatDate(dateString: string) {
  return dayjs(dateString).format('YYYY-MM-DD HH:mm:ss')
}
</script>

<style scoped>
.workspace-view {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.left-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title {
  margin: 0;
  color: #fff;
  font-size: 20px;
}

.sidebar-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.section-title {
  padding: 16px;
  font-weight: 600;
  border-bottom: 1px solid #f0f0f0;
}

.page-item {
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.page-item:hover {
  background-color: #fafafa;
}

.active-page {
  background-color: #e6f7ff;
  border-left: 3px solid #1890ff;
}

.page-item-content {
  flex: 1;
}

.page-title {
  font-weight: 500;
  margin-bottom: 4px;
}

.page-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  justify-content: space-between;
}

.content-area {
  padding: 20px;
  overflow-y: auto;
  max-height: calc(100vh - 64px);
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.editor-view {
  margin-bottom: 20px;
}

.canvas-view {
  margin-bottom: 20px;
  min-height: 500px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 16px;
}

.events-section {
  background: #fafafa;
  padding: 16px;
  border-radius: 4px;
}

.events-section h3 {
  margin-top: 0;
  margin-bottom: 16px;
}

.event-item {
  width: 100%;
}

.event-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.event-header h4 {
  margin: 0 0 8px 0;
}

.event-time {
  color: #999;
  font-size: 12px;
}
</style>