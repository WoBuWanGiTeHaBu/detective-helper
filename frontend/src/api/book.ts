import request from './request'
import type { ApiResponse } from './request'

// 书籍相关类型
export interface BookVO {
  id: number
  title: string
  description: string
  coverImage: string
  sortOrder: number
  pageCount: number
  createdAt: string
  updatedAt: string
}

export interface BookCreateDTO {
  title: string
  description?: string
  coverImage?: string
}

export interface BookUpdateDTO {
  title: string
  description?: string
  coverImage?: string
}

export interface BookSortDTO {
  bookIds: number[]
}

export interface BookWorkspaceVO {
  id: number
  title: string
  description: string
  coverImage: string
  sortOrder: number
  pages: any[]
  relationGraphs: any[]
  createdAt: string
  updatedAt: string
}

// 书籍API
export const bookApi = {
  // 创建书籍
  createBook(data: BookCreateDTO): Promise<ApiResponse<BookVO>> {
    return request.post('/books', data)
  },

  // 更新书籍
  updateBook(id: number, data: BookUpdateDTO): Promise<ApiResponse<BookVO>> {
    return request.put(`/books/${id}`, data)
  },

  // 删除书籍
  deleteBook(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/books/${id}`)
  },

  // 获取书籍详情
  getBookById(id: number): Promise<ApiResponse<BookVO>> {
    return request.get(`/books/${id}`)
  },

  // 获取所有书籍
  getAllBooks(): Promise<ApiResponse<BookVO[]>> {
    return request.get('/books')
  },

  // 获取书籍工作空间
  getBookWorkspace(id: number): Promise<ApiResponse<BookWorkspaceVO>> {
    return request.get(`/books/${id}/workspace`)
  },

  // 批量排序书籍
  sortBooks(data: BookSortDTO): Promise<ApiResponse<void>> {
    return request.post('/books/sort', data)
  }
}