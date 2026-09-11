import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { bookApi, type BookVO, type BookCreateDTO, type BookUpdateDTO } from '@/api/book'

export const useBookStore = defineStore('book', () => {
  // 状态
  const books = ref<BookVO[]>([])
  const currentBook = ref<BookVO | null>(null)
  const loading = ref(false)

  // 计算属性
  const bookCount = computed(() => books.value.length)
  const hasBooks = computed(() => books.value.length > 0)

  // 操作方法
  async function fetchAllBooks() {
    loading.value = true
    try {
      const response = await bookApi.getAllBooks()
      books.value = response.data
    } catch (error) {
      console.error('获取案件书列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  async function createBook(data: BookCreateDTO) {
    loading.value = true
    try {
      const response = await bookApi.createBook(data)
      books.value.push(response.data)
      return response.data
    } catch (error) {
      console.error('创建案件书失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function updateBook(id: number, data: BookUpdateDTO) {
    loading.value = true
    try {
      const response = await bookApi.updateBook(id, data)
      const index = books.value.findIndex(b => b.id === id)
      if (index !== -1) {
        books.value[index] = response.data
      }
      if (currentBook.value?.id === id) {
        currentBook.value = response.data
      }
      return response.data
    } catch (error) {
      console.error('更新案件书失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function deleteBook(id: number) {
    loading.value = true
    try {
      await bookApi.deleteBook(id)
      books.value = books.value.filter(b => b.id !== id)
      if (currentBook.value?.id === id) {
        currentBook.value = null
      }
    } catch (error) {
      console.error('删除案件书失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function getBookById(id: number) {
    loading.value = true
    try {
      const response = await bookApi.getBookById(id)
      currentBook.value = response.data
      return response.data
    } catch (error) {
      console.error('获取案件书详情失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  function setCurrentBook(book: BookVO | null) {
    currentBook.value = book
  }

  return {
    // 状态
    books,
    currentBook,
    loading,

    // 计算属性
    bookCount,
    hasBooks,

    // 方法
    fetchAllBooks,
    createBook,
    updateBook,
    deleteBook,
    getBookById,
    setCurrentBook
  }
})