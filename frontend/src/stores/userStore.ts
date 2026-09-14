import { defineStore } from 'pinia'
import { ref } from 'vue'
import { profileApi } from '@/api/profile'
import type { ThemeId } from '@/api/types'

/**
 * 用户资料 store —— 展示名 + 主题
 *
 * 存储策略：**后端 /api/profile 为唯一权威存储**（第十一节）。
 * localStorage 只是降级缓存：后端接口尚未实现（404）时保功能可用，
 * 接口一就绪，读取和保存都以后端返回为准。
 */

const CACHE_KEY = 'wb.profile'
export const DEFAULT_DISPLAY_NAME = '用户'
export const DEFAULT_THEME: ThemeId = 'light'

interface ProfileCache {
  displayName: string
  theme: string
}

function readCache(): ProfileCache | null {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    if (!raw) return null
    const d = JSON.parse(raw) as ProfileCache
    if (typeof d.displayName !== 'string') return null
    return { displayName: d.displayName, theme: d.theme || DEFAULT_THEME }
  } catch {
    return null
  }
}

function writeCache(c: ProfileCache) {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify(c))
  } catch {
    /* 隐私模式等场景写入失败无所谓，内存里还有一份 */
  }
}

export const useUserStore = defineStore('user', () => {
  const cached = readCache()
  const displayName = ref(cached?.displayName || DEFAULT_DISPLAY_NAME)
  const theme = ref<ThemeId>((cached?.theme as ThemeId) || DEFAULT_THEME)

  /** 展示名的首字，头像圆里显示用 */
  function avatarChar(): string {
    const n = displayName.value.trim()
    return n ? n[0] : DEFAULT_DISPLAY_NAME[0]
  }

  /**
   * 主题挂载点：现在只有 light 一套样式，先只写 data-theme 属性占位；
   * 深色 / 羊皮纸主题上线后，在 tokens.css 里按 [data-theme='dark'] 出变量即可。
   */
  function applyTheme() {
    document.documentElement.dataset.theme = theme.value
  }

  /** 启动时调用：后端有 profile 就以它为准，否则静默沿用本地缓存 */
  async function load() {
    const p = await profileApi.getProfile()
    if (p) {
      displayName.value = p.displayName.trim() || DEFAULT_DISPLAY_NAME
      theme.value = (p.theme as ThemeId) || DEFAULT_THEME
      writeCache({ displayName: displayName.value, theme: theme.value })
    }
    applyTheme()
  }

  /** 保存：以后端为准——PUT 成功用返回值回填；接口未就绪才落到本地缓存，功能不中断 */
  async function save(name: string, nextTheme: ThemeId) {
    const nextName = name.trim() || DEFAULT_DISPLAY_NAME
    const saved = await profileApi.updateProfile({ displayName: nextName, theme: nextTheme })
    displayName.value = saved?.displayName?.trim() || nextName
    theme.value = (saved?.theme as ThemeId) || nextTheme
    writeCache({ displayName: displayName.value, theme: theme.value })
    applyTheme()
  }

  return { displayName, theme, avatarChar, load, save }
})
