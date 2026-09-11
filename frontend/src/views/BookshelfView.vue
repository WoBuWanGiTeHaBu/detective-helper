<template>
  <div class="bookshelf-view">
    <a-layout>
      <a-layout-header>
        <div class="header-content">
          <h1 class="title">案件书架</h1>
          <a-button type="primary" @click="showCreateModal">
            <template #icon><PlusOutlined /></template>
            新建案件书
          </a-button>
        </div>
      </a-layout-header>

      <a-layout-content>
        <div v-if="bookStore.loading" class="loading">
          <a-spin size="large" />
        </div>

        <div v-else-if="bookStore.hasBooks" class="books-grid">
          <div
            v-for="book in bookStore.books"
            :key="book.id"
            class="book-card"
            @click="openBook(book.id)"
          >
            <div class="book-cover">
              <img v-if="book.coverImage" :src="book.coverImage" :alt="book.title" />
              <div v-else class="default-cover">
                <BookOutlined />
              </div>
            </div>
            <div class="book-info">
              <h3 class="book-title">{{ book.title }}</h3>
              <p class="book-description">{{ book.description || '暂无描述' }}</p>
              <div class="book-meta">
                <span>{{ book.pageCount }} 页</span>
                <span>{{ formatDate(book.updatedAt) }}</span>
              </div>
            </div>
            <div class="book-actions">
              <a-button type="text" size="small" @click.stop="editBook(book)">
                <EditOutlined />
              </a-button>
              <a-button type="text" size="small" danger @click.stop="deleteBook(book.id)">
                <DeleteOutlined />
              </a-button>
            </div>
          </div>
        </div>

        <a-empty v-else description="暂无案件书，点击右上角新建" />

        <a-modal
          v-model:open="modalVisible"
          :title="isEdit ? '编辑案件书' : '新建案件书'"
          @ok="handleSubmit"
        >
          <a-form :model="formData" layout="vertical">
            <a-form-item label="标题" required>
              <a-input v-model:value="formData.title" placeholder="请输入案件书标题" />
            </a-form-item>
            <a-form-item label="描述">
              <a-textarea
                v-model:value="formData.description"
                placeholder="请输入案件书描述"
                :rows="4"
              />
            </a-form-item>
            <a-form-item label="封面图片">
              <a-upload
                :before-upload="handleUpload"
                :show-upload-list="false"
                accept="image/*"
              >
                <a-button>
                  <UploadOutlined /> 上传封面
                </a-button>
              </a-upload>
              <div v-if="formData.coverImage" class="cover-preview">
                <img :src="formData.coverImage" alt="封面预览" />
              </div>
            </a-form-item>
          </a-form>
        </a-modal>
      </a-layout-content>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  BookOutlined,
  EditOutlined,
  DeleteOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import { useBookStore } from '@/stores/bookStore'
import type { BookCreateDTO, BookUpdateDTO, BookVO } from '@/api/book'

const router = useRouter()
const bookStore = useBookStore()

const modalVisible = ref(false)
const isEdit = ref(false)
const editingBook = ref<BookVO | null>(null)
const formData = reactive({
  title: '',
  description: '',
  coverImage: ''
})

onMounted(() => {
  bookStore.fetchAllBooks()
})

function showCreateModal() {
  isEdit.value = false
  editingBook.value = null
  formData.title = ''
  formData.description = ''
  formData.coverImage = ''
  modalVisible.value = true
}

function editBook(book: BookVO) {
  isEdit.value = true
  editingBook.value = book
  formData.title = book.title
  formData.description = book.description
  formData.coverImage = book.coverImage
  modalVisible.value = true
}

async function handleSubmit() {
  if (!formData.title.trim()) {
    message.error('请输入案件书标题')
    return
  }

  try {
    if (isEdit.value && editingBook.value) {
      const data: BookUpdateDTO = {
        title: formData.title,
        description: formData.description,
        coverImage: formData.coverImage
      }
      await bookStore.updateBook(editingBook.value.id, data)
      message.success('案件书更新成功')
    } else {
      const data: BookCreateDTO = {
        title: formData.title,
        description: formData.description,
        coverImage: formData.coverImage
      }
      await bookStore.createBook(data)
      message.success('案件书创建成功')
    }

    modalVisible.value = false
  } catch (error) {
    // 错误已在store中处理
  }
}

async function deleteBook(id: number) {
  const confirmed = await new Promise(resolve => {
    const modal = message.confirm('确定要删除这个案件书吗？')
    modal.then(() => resolve(true)).catch(() => resolve(false))
  })

  if (!confirmed) return

  try {
    await bookStore.deleteBook(id)
    message.success('案件书删除成功')
  } catch (error) {
    // 错误已在store中处理
  }
}

function openBook(id: number) {
  router.push(`/workspace/${id}`)
}

function handleUpload(file: File) {
  const reader = new FileReader()
  reader.onload = (e) => {
    formData.coverImage = e.target?.result as string
  }
  reader.readAsDataURL(file)
  return false // 阻止默认上传
}

function formatDate(dateString: string) {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.bookshelf-view {
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

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  padding: 20px;
  overflow-y: auto;
  max-height: calc(100vh - 64px);
}

.book-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid #e8e8e8;
}

.book-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.book-cover {
  width: 100%;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  overflow: hidden;
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-cover {
  font-size: 64px;
  color: #d9d9d9;
}

.book-info {
  padding: 12px;
}

.book-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-description {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #666;
  height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.book-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}

.book-actions {
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
}

.cover-preview {
  margin-top: 12px;
  width: 100%;
  height: 200px;
  overflow: hidden;
  border-radius: 4px;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>