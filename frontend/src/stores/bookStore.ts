import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { bookApi } from '@/api/book'
import type { BookResponse, CreateBookRequest, UpdateBookRequest } from '@/api/types'

/** 书架排序方式 */
export type SortMode = 'recent' | 'created' | 'name'

export const useBookStore = defineStore('book', () => {
  const books = ref<BookResponse[]>([])
  const loading = ref(false)
  const sortMode = ref<SortMode>('recent')

  const hasBooks = computed(() => books.value.length > 0)
  const bookCount = computed(() => books.value.length)

  /** 排序后的书籍列表 */
  const sortedBooks = computed(() => {
    const list = [...books.value]
    switch (sortMode.value) {
      case 'name':
        return list.sort((a, b) => a.name.localeCompare(b.name, 'zh-Hans-CN'))
      case 'created':
        return list.sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt))
      case 'recent':
      default:
        return list.sort((a, b) => +new Date(b.updatedAt) - +new Date(a.updatedAt))
    }
  })

  /** 最近一次更新时间（用于副标题） */
  const latestUpdatedAt = computed(() => {
    if (!books.value.length) return null
    return books.value.reduce(
      (acc, b) => (+new Date(b.updatedAt) > +new Date(acc) ? b.updatedAt : acc),
      books.value[0].updatedAt
    )
  })

  async function fetchBooks() {
    loading.value = true
    try {
      const list = await bookApi.listBooks()
      // 防御：后端异常时可能返回非数组，避免把页面搞崩
      books.value = Array.isArray(list) ? list : []
    } catch (e) {
      // 不要把列表清空 —— 保留上一次数据，否则接口一抖动整屏就空了，
      // 表现为「明明建过书却看不到」。错误信息由拦截器统一提示。
      console.error('[bookStore] fetchBooks failed:', e)
    } finally {
      loading.value = false
    }
  }

  async function createBook(data: CreateBookRequest): Promise<BookResponse> {
    const created = await bookApi.createBook(data)
    books.value.push(created)
    return created
  }

  async function updateBook(id: number, data: UpdateBookRequest): Promise<BookResponse> {
    const updated = await bookApi.updateBook(id, data)
    const i = books.value.findIndex((b) => b.id === id)
    if (i !== -1) books.value[i] = updated
    return updated
  }

  async function deleteBook(id: number) {
    await bookApi.deleteBook(id)
    books.value = books.value.filter((b) => b.id !== id)
  }

  async function fetchBook(id: number): Promise<BookResponse> {
    return await bookApi.getBook(id)
  }

  function setSortMode(mode: SortMode) {
    sortMode.value = mode
  }

  return {
    books,
    loading,
    sortMode,
    hasBooks,
    bookCount,
    sortedBooks,
    latestUpdatedAt,
    fetchBooks,
    createBook,
    updateBook,
    deleteBook,
    fetchBook,
    setSortMode
  }
})
