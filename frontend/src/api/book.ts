import request from './request'
import type {
  BookResponse,
  BookWorkspaceVO,
  CoverResponse,
  CreateBookRequest,
  SortItem,
  UpdateBookRequest
} from './types'

/**
 * 书籍 API —— 对应 tag「书籍」「工作区」
 *
 * GET    /api/books                获取书籍列表
 * POST   /api/books                创建书籍
 * GET    /api/books/{id}           获取单本书籍
 * PUT    /api/books/{id}           更新书籍
 * DELETE /api/books/{id}           删除书籍
 * POST   /api/books/{id}/cover     上传封面图片（multipart/form-data，字段名 file）
 * PUT    /api/books/sort           批量排序书籍
 * GET    /api/books/{id}/workspace 获取书籍完整工作区
 */
export const bookApi = {
  listBooks(): Promise<BookResponse[]> {
    return request.get('/books')
  },

  createBook(data: CreateBookRequest): Promise<BookResponse> {
    return request.post('/books', data)
  },

  getBook(id: number): Promise<BookResponse> {
    return request.get(`/books/${id}`)
  },

  updateBook(id: number, data: UpdateBookRequest): Promise<BookResponse> {
    return request.put(`/books/${id}`, data)
  },

  deleteBook(id: number): Promise<void> {
    return request.delete(`/books/${id}`)
  },

  uploadCover(id: number, file: File): Promise<CoverResponse> {
    const form = new FormData()
    form.append('file', file)
    return request.post(`/books/${id}/cover`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  sortBooks(items: SortItem[]): Promise<void> {
    return request.put('/books/sort', items)
  },

  getWorkspace(id: number): Promise<BookWorkspaceVO> {
    return request.get(`/books/${id}/workspace`)
  }
}
