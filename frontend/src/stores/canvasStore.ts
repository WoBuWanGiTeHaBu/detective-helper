import { defineStore } from 'pinia'
import { ref } from 'vue'
import { canvasApi } from '@/api/canvas'

export const useCanvasStore = defineStore('canvas', () => {
  const canvasData = ref('')
  const loading = ref(false)
  const dirty = ref(false)

  async function loadCanvas(pageId: number) {
    loading.value = true
    try {
      const response = await canvasApi.getPageCanvas(pageId)
      canvasData.value = response.data.canvasData
      dirty.value = false
    } catch (error) {
      console.error('加载画布失败:', error)
      canvasData.value = ''
    } finally {
      loading.value = false
    }
  }

  async function saveCanvas(pageId: number, data: string) {
    loading.value = true
    try {
      const response = await canvasApi.savePageCanvas(pageId, data)
      canvasData.value = response.data.canvasData
      dirty.value = false
    } catch (error) {
      console.error('保存画布失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  function updateCanvas(data: string) {
    canvasData.value = data
    dirty.value = true
  }

  function clearCanvas() {
    canvasData.value = ''
    dirty.value = false
  }

  function markDirty() {
    dirty.value = true
  }

  function markClean() {
    dirty.value = false
  }

  return {
    canvasData,
    loading,
    dirty,
    loadCanvas,
    saveCanvas,
    updateCanvas,
    clearCanvas,
    markDirty,
    markClean
  }
})