<template>
  <div class="relation-graph-view">
    <a-layout>
      <a-layout-header>
        <div class="header-content">
          <h1 class="title">关系图谱</h1>
          <a-button type="primary" @click="showCreateGraphModal">
            <template #icon><PlusOutlined /></template>
            新建关系图
          </a-button>
        </div>
      </a-layout-header>

      <a-layout-content>
        <div class="content-area">
          <a-row :gutter="16">
            <a-col :span="6">
              <a-card title="案件书列表" size="small">
                <a-list
                  :data-source="bookStore.books"
                  :loading="bookStore.loading"
                  size="small"
                  @click="handleBookSelect"
                >
                  <template #renderItem="{ item }">
                    <a-list-item
                      :class="{ 'active-book': selectedBook?.id === item.id }"
                      class="book-item"
                    >
                      {{ item.title }}
                    </a-list-item>
                  </template>
                </a-list>
              </a-card>

              <a-card title="关系图列表" size="small" style="margin-top: 16px">
                <a-list
                  :data-source="relationGraphs"
                  :loading="loading"
                  size="small"
                  @click="handleGraphSelect"
                >
                  <template #renderItem="{ item }">
                    <a-list-item
                      :class="{ 'active-graph': selectedGraph?.id === item.id }"
                      class="graph-item"
                    >
                      <div class="graph-item-content">
                        <div class="graph-title">{{ item.title }}</div>
                        <div class="graph-desc">{{ item.description || '暂无描述' }}</div>
                      </div>
                      <a-button
                        type="text"
                        size="small"
                        danger
                        @click.stop="deleteGraph(item.id)"
                      >
                        <DeleteOutlined />
                      </a-button>
                    </a-list-item>
                  </template>
                </a-list>

                <a-empty
                  v-if="!loading && relationGraphs.length === 0"
                  description="暂无关系图"
                />
              </a-card>
            </a-col>

            <a-col :span="18">
              <a-card v-if="selectedGraph" title="关系图编辑器" size="small">
                <template #extra>
                  <a-space>
                    <a-button size="small" @click="extractRelations">自动提取关系</a-button>
                    <a-button type="primary" size="small" @click="saveGraph">保存</a-button>
                  </a-space>
                </template>

                <div class="graph-editor">
                  <RelationGraphCanvas
                    :graph-data="graphData"
                    @update="handleGraphUpdate"
                  />
                </div>
              </a-card>

              <a-empty v-else description="请选择一个关系图或新建" />
            </a-col>
          </a-row>
        </div>
      </a-layout-content>
    </a-layout>

    <!-- 新建/编辑关系图对话框 -->
    <a-modal
      v-model:open="graphModalVisible"
      :title="isEditGraph ? '编辑关系图' : '新建关系图'"
      @ok="handleGraphSubmit"
    >
      <a-form :model="graphFormData" layout="vertical">
        <a-form-item label="标题" required>
          <a-input v-model:value="graphFormData.title" placeholder="请输入关系图标题" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="graphFormData.description"
            placeholder="请输入关系图描述"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { useBookStore } from '@/stores/bookStore'
import { relationGraphApi, type RelationGraphCreateDTO, type RelationGraphUpdateDTO, type RelationGraphVO, type RelationGraphExtractDTO } from '@/api/relationGraph'
import RelationGraphCanvas from '@/components/relation/RelationGraphCanvas.vue'

const bookStore = useBookStore()

const loading = ref(false)
const selectedBook = ref<any>(null)
const selectedGraph = ref<RelationGraphVO | null>(null)
const relationGraphs = ref<RelationGraphVO[]>([])
const graphData = ref('')

const graphModalVisible = ref(false)
const isEditGraph = ref(false)
const editingGraph = ref<RelationGraphVO | null>(null)
const graphFormData = reactive({
  title: '',
  description: ''
})

onMounted(async () => {
  await bookStore.fetchAllBooks()
  if (bookStore.books.length > 0) {
    handleBookSelect(bookStore.books[0])
  }
})

async function handleBookSelect(book: any) {
  selectedBook.value = book
  selectedGraph.value = null
  graphData.value = ''

  await loadRelationGraphs(book.id)
}

async function loadRelationGraphs(bookId: number) {
  loading.value = true
  try {
    const response = await relationGraphApi.getRelationGraphsByBookId(bookId)
    relationGraphs.value = response.data
  } catch (error) {
    console.error('加载关系图列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleGraphSelect(graph: RelationGraphVO) {
  selectedGraph.value = graph
  graphData.value = graph.graphData || ''
}

function showCreateGraphModal() {
  if (!selectedBook.value) {
    message.warning('请先选择一个案件书')
    return
  }

  isEditGraph.value = false
  editingGraph.value = null
  graphFormData.title = ''
  graphFormData.description = ''
  graphModalVisible.value = true
}

async function handleGraphSubmit() {
  if (!graphFormData.title.trim()) {
    message.error('请输入关系图标题')
    return
  }

  if (!selectedBook.value) return

  try {
    if (isEditGraph.value && editingGraph.value) {
      const data: RelationGraphUpdateDTO = {
        title: graphFormData.title,
        description: graphFormData.description
      }
      await relationGraphApi.updateRelationGraph(editingGraph.value.id, data)
      message.success('关系图更新成功')
    } else {
      const data: RelationGraphCreateDTO = {
        bookId: selectedBook.value.id,
        title: graphFormData.title,
        description: graphFormData.description
      }
      await relationGraphApi.createRelationGraph(data)
      message.success('关系图创建成功')
    }

    graphModalVisible.value = false
    await loadRelationGraphs(selectedBook.value.id)
  } catch (error) {
    // 错误已在API中处理
  }
}

async function deleteGraph(id: number) {
  const confirmed = await new Promise(resolve => {
    const modal = message.confirm('确定要删除这个关系图吗？')
    modal.then(() => resolve(true)).catch(() => resolve(false))
  })

  if (!confirmed) return

  if (!selectedBook.value) return

  try {
    await relationGraphApi.deleteRelationGraph(id)
    message.success('关系图删除成功')

    if (selectedGraph.value?.id === id) {
      selectedGraph.value = null
      graphData.value = ''
    }

    await loadRelationGraphs(selectedBook.value.id)
  } catch (error) {
    // 错误已在API中处理
  }
}

function handleGraphUpdate(data: string) {
  graphData.value = data
}

async function saveGraph() {
  if (!selectedGraph.value) return

  try {
    await relationGraphApi.updateRelationGraph(selectedGraph.value.id, {
      title: selectedGraph.value.title,
      description: selectedGraph.value.description,
      graphData: graphData.value
    })
    message.success('关系图保存成功')
  } catch (error) {
    // 错误已在API中处理
  }
}

async function extractRelations() {
  if (!selectedBook.value) {
    message.warning('请先选择一个案件书')
    return
  }

  try {
    const data: RelationGraphExtractDTO = {
      bookId: selectedBook.value.id
    }
    const response = await relationGraphApi.extractRelationGraph(data)

    message.success('关系提取成功')
    await loadRelationGraphs(selectedBook.value.id)

    // 选中新生成的图
    if (response.data) {
      handleGraphSelect(response.data)
    }
  } catch (error) {
    // 错误已在API中处理
  }
}
</script>

<style scoped>
.relation-graph-view {
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

.title {
  margin: 0;
  color: #fff;
  font-size: 20px;
}

.content-area {
  padding: 20px;
  overflow-y: auto;
  max-height: calc(100vh - 64px);
}

.book-item,
.graph-item {
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
}

.book-item:hover,
.graph-item:hover {
  background-color: #fafafa;
}

.active-book,
.active-graph {
  background-color: #e6f7ff;
}

.graph-item-content {
  flex: 1;
}

.graph-title {
  font-weight: 500;
  margin-bottom: 4px;
}

.graph-desc {
  font-size: 12px;
  color: #999;
}

.graph-editor {
  min-height: 600px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}
</style>