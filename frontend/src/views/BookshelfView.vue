<template>
  <div class="bookshelf">
    <!-- ═══════════ 顶栏 (h 64) ═══════════ -->
    <header class="topbar">
      <div class="brand">
        <span class="brand-logo">
          <svg width="30" height="30" viewBox="0 0 30 30" fill="none">
            <path
              d="M7 5.6h9.8a4.6 4.6 0 0 1 4.6 4.6v14.2H11.6A4.6 4.6 0 0 0 7 29V5.6Z"
              stroke="#3F5B4C"
              stroke-width="1.5"
              stroke-linejoin="round"
            />
            <circle cx="21.4" cy="18.6" r="2.5" fill="#7C9A88" />
          </svg>
        </span>
        <div class="brand-text">
          <div class="brand-name">推演录</div>
          <div class="brand-sub">推理笔记 · 线索推演</div>
        </div>
      </div>

      <div class="topbar-spacer" />

      <label class="search">
        <svg class="search-icon" viewBox="0 0 16 16" fill="none">
          <circle cx="7" cy="7" r="4.6" stroke="currentColor" stroke-width="1.4" />
          <path d="m10.6 10.6 3.2 3.2" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
        </svg>
        <input v-model="keyword" type="text" placeholder="搜索案件、人物或线索…" />
      </label>

      <button class="btn-new" type="button" @click="openCreate">
        <svg viewBox="0 0 14 14" fill="none">
          <path d="M7 1.6v10.8M1.6 7h10.8" stroke="#fff" stroke-width="1.6" stroke-linecap="round" />
        </svg>
        <span>新建书籍</span>
      </button>

      <div class="avatar-wrap">
        <button class="avatar" type="button" @click.stop="profileOpen = !profileOpen">
          {{ userStore.avatarChar() }}
        </button>

        <!-- 用户资料面板：展示名 + 主题（深色 / 羊皮纸为预留位） -->
        <div v-if="profileOpen" class="profile-pop" @click.stop>
          <div class="profile-head">
            <span class="profile-avatar">{{ profileChar }}</span>
            <div class="profile-head-text">
              <div class="profile-name">{{ profileDraft.name.trim() || '用户' }}</div>
              <div class="profile-sub">单机应用 · 资料仅本机可见</div>
            </div>
          </div>

          <label class="p-field">
            <span class="p-label">展示名</span>
            <input
              v-model="profileDraft.name"
              class="p-input"
              placeholder="用户"
              maxlength="24"
              @keyup.enter="saveProfile"
            />
          </label>

          <div class="p-field">
            <span class="p-label">主题<span class="p-opt">深色 / 羊皮纸为预留位</span></span>
            <div class="theme-chips">
              <button
                v-for="t in THEME_OPTIONS"
                :key="t.value"
                type="button"
                class="theme-chip"
                :class="{ active: profileDraft.theme === t.value, disabled: !t.ready }"
                :disabled="!t.ready"
                @click="profileDraft.theme = t.value"
              >
                {{ t.label }}
              </button>
            </div>
          </div>

          <div class="profile-actions">
            <button class="p-ghost" type="button" @click="profileOpen = false">取消</button>
            <button class="p-primary" type="button" @click="saveProfile">保存</button>
          </div>
        </div>
      </div>
    </header>

    <!-- ═══════════ 内容区 ═══════════ -->
    <main class="content">
      <div class="title-row">
        <div class="title-left">
          <h1 class="title">我的案件</h1>
          <p class="subtitle">{{ subtitle }}</p>
        </div>

        <div class="sort-wrap">
          <button class="sort-ctrl" type="button" @click.stop="sortMenuOpen = !sortMenuOpen">
            <span>{{ sortLabel }}</span>
            <svg viewBox="0 0 10 10" fill="none" class="caret">
              <path d="m2 3.6 3 3 3-3" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </button>

          <div v-if="sortMenuOpen" class="sort-menu" @click.stop>
            <button
              v-for="opt in SORT_OPTIONS"
              :key="opt.value"
              type="button"
              class="sort-option"
              :class="{ active: bookStore.sortMode === opt.value }"
              @click="pickSort(opt.value)"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>
      </div>

      <!-- 加载态 -->
      <div v-if="bookStore.loading" class="skeleton-grid">
        <div v-for="i in 6" :key="i" class="skeleton-card" />
      </div>

      <!-- 空状态 -->
      <div v-else-if="!filteredBooks.length" class="empty-state">
        <button class="empty-ghost" type="button" @click="openCreate">
          <svg viewBox="0 0 44 44" fill="none">
            <path d="M22 10v24M10 22h24" stroke="#8C877E" stroke-width="2" stroke-linecap="round" />
          </svg>
          <span>新建案件</span>
        </button>
        <p class="empty-hint">创建你的第一个案件</p>
      </div>

      <!-- 书架网格：每页 11 格（10 本 + 新建位） -->
      <template v-else>
        <div class="shelf">
          <article
            v-for="book in pagedBooks"
            :key="book.id"
            class="book-card"
            @click="openBook(book)"
          >
            <div class="cover" :style="coverStyle(book)">
              <!-- 图片封面 -->
              <img
                v-if="bookImage(book)"
                class="cover-img"
                :src="bookImage(book)!"
                :alt="book.name"
                draggable="false"
              />

              <div class="cover-tag">{{ coverTag(book) }}</div>
              <div class="cover-push" />
              <div class="cover-foot">
                <h3 class="cover-name">{{ book.name }}</h3>
                <p class="cover-sub">{{ book.coverText || '推演录 · 案件卷宗' }}</p>
              </div>

              <div class="cover-hover">
                <button type="button" class="hover-btn" @click.stop="triggerCoverUpload(book)">
                  {{ book.coverType === 'image' ? '换封面' : '传封面' }}
                </button>
                <button type="button" class="hover-btn" @click.stop="renameBook(book)">重命名</button>
                <button type="button" class="hover-btn danger" @click.stop="removeBook(book)">删除</button>
              </div>
            </div>
            <p class="book-meta">
              <span>{{ book.coverText || '案件' }}</span>
              <span>{{ relativeTime(bookContentTime(book)) }}</span>
            </p>
          </article>

          <!-- 末尾：虚化新建空位 -->
          <button class="create-slot" type="button" @click="openCreate">
            <span class="create-ghost">
              <svg viewBox="0 0 44 44" fill="none">
                <path d="M22 10v24M10 22h24" stroke="#8C877E" stroke-width="2" stroke-linecap="round" />
              </svg>
              <span class="create-label">新建案件</span>
            </span>
            <span class="create-meta">为下一个案子留个位置</span>
          </button>
        </div>

        <!-- 翻页 -->
        <nav v-if="totalPages > 1" class="pager">
          <button class="pager-btn" :disabled="page === 1" @click="page--">
            <svg viewBox="0 0 12 12" fill="none">
              <path d="M7.5 2.5 4 6l3.5 3.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </button>
          <button
            v-for="p in totalPages"
            :key="p"
            class="pager-dot"
            :class="{ active: p === page }"
            @click="page = p"
          >
            {{ p }}
          </button>
          <button class="pager-btn" :disabled="page === totalPages" @click="page++">
            <svg viewBox="0 0 12 12" fill="none">
              <path d="M4.5 2.5 8 6l-3.5 3.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </button>
        </nav>
      </template>
    </main>

    <!-- ═══════════ 新建 / 重命名 ═══════════ -->
    <div v-if="dialog.open" class="dialog-mask" @click.self="dialog.open = false">
      <div class="dialog">
        <h2 class="dialog-title">{{ dialog.mode === 'create' ? '新建案件' : '重命名案件' }}</h2>

        <label class="field">
          <span class="field-label">案件名称</span>
          <input
            v-model="dialog.name"
            class="field-input"
            placeholder="如：王某某失踪案"
            @keyup.enter="submitDialog"
          />
        </label>

        <label class="field">
          <span class="field-label">封面题字</span>
          <input v-model="dialog.coverText" class="field-input" placeholder="封面上的短标签，可留空" />
        </label>

        <!-- 创建时可先选一张封面图；重命名模式不提供（走卡片 hover 的「换封面」） -->
        <div v-if="dialog.mode === 'create'" class="field">
          <span class="field-label">封面图片<span class="field-opt">可选 · ≤2MB</span></span>
          <div class="cover-pick">
            <div class="cover-preview" :style="{ background: dialogPreviewBg }">
              <img v-if="dialog.previewUrl" :src="dialog.previewUrl" alt="" />
              <span v-else class="cover-preview-hint">未选择</span>
            </div>
            <div class="cover-pick-actions">
              <button type="button" class="ghost-btn sm" @click="pickDialogCover">
                {{ dialog.file ? '重新选择' : '选择图片' }}
              </button>
              <button
                v-if="dialog.file"
                type="button"
                class="ghost-btn sm"
                @click="clearDialogCover"
              >
                移除
              </button>
            </div>
          </div>
        </div>

        <div class="field">
          <span class="field-label">封面色调</span>
          <div class="swatches" :class="{ dim: !!dialog.file }">
            <button
              v-for="(t, i) in COVER_TOKENS"
              :key="i"
              type="button"
              class="swatch"
              :class="{ active: dialog.tokenIndex === i }"
              :style="{ background: coverGradient(t) }"
              @click="dialog.tokenIndex = i"
            />
          </div>
        </div>

        <div class="dialog-actions">
          <button class="ghost-btn" type="button" @click="dialog.open = false">取消</button>
          <button
            class="primary-btn"
            type="button"
            :disabled="!dialog.name.trim() || dialog.submitting"
            @click="submitDialog"
          >
            {{ dialog.mode === 'create' ? '创建案件' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- 封面上传用的隐藏 input（卡片 / 对话框共用，由 pickTarget 区分去向） -->
  <input
    ref="coverInput"
    type="file"
    accept="image/jpeg,image/png,image/webp"
    class="hidden-file"
    @change="onCoverFileChange"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { useBookStore, bookContentTime } from '@/stores/bookStore'
import type { SortMode } from '@/stores/bookStore'
import { useUserStore } from '@/stores/userStore'
import { bookApi } from '@/api/book'
import type { BookResponse, ThemeId } from '@/api/types'
import { coverUrl } from '@/utils/coverUrl'
import {
  COVER_TOKENS,
  caseLabel,
  coverGradient,
  coverShadow,
  coverTokenFor,
  type CoverToken
} from '@/utils/coverTokens'

const router = useRouter()
const bookStore = useBookStore()
const userStore = useUserStore()

/** 每页 11 格 = 10 本书 + 1 个新建位 */
const PER_PAGE = 11

const keyword = ref('')
const page = ref(1)
const sortMenuOpen = ref(false)

const SORT_OPTIONS: { value: SortMode; label: string }[] = [
  { value: 'recent', label: '按最近编辑' },
  { value: 'created', label: '按创建时间' },
  { value: 'name', label: '按名称' }
]

/* ---------------- 用户资料（展示名 + 主题预留） ---------------- */
const profileOpen = ref(false)
const profileDraft = reactive({ name: '', theme: 'light' as ThemeId })

const THEME_OPTIONS: { value: ThemeId; label: string; ready: boolean }[] = [
  { value: 'light', label: '浅色', ready: true },
  { value: 'dark', label: '深色', ready: false },
  { value: 'sepia', label: '羊皮纸', ready: false }
]

const profileChar = computed(() => {
  const n = profileDraft.name.trim()
  return n ? n[0] : '用'
})

watch(profileOpen, (open) => {
  if (open) {
    profileDraft.name = userStore.displayName
    profileDraft.theme = userStore.theme
  }
})

async function saveProfile() {
  await userStore.save(profileDraft.name, profileDraft.theme)
  profileOpen.value = false
  message.success('已保存')
}

/* ---------------- 计算 ---------------- */
const filteredBooks = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return bookStore.sortedBooks
  return bookStore.sortedBooks.filter(
    (b) =>
      b.name.toLowerCase().includes(kw) ||
      (b.coverText ?? '').toLowerCase().includes(kw)
  )
})

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredBooks.value.length / (PER_PAGE - 1)))
)

const pagedBooks = computed(() => {
  const size = PER_PAGE - 1
  const start = (page.value - 1) * size
  return filteredBooks.value.slice(start, start + size)
})

const subtitle = computed(() => {
  const n = filteredBooks.value.length
  if (!n) return '还没有案件'
  const latest = bookStore.latestUpdatedAt
  return `共 ${n} 个案件 · 最近更新 ${latest ? relativeTime(latest) : '—'}`
})

const sortLabel = computed(
  () => SORT_OPTIONS.find((o) => o.value === bookStore.sortMode)?.label ?? '按最近打开'
)

/* ---------------- 生命周期 ---------------- */
onMounted(() => {
  bookStore.fetchBooks()
  userStore.load()
  document.addEventListener('click', closeSortMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeSortMenu)
})

watch([keyword, () => bookStore.sortMode], () => {
  page.value = 1
})

watch(totalPages, (n) => {
  if (page.value > n) page.value = n
})

/* ---------------- 展示辅助 ---------------- */
function coverToken(book: BookResponse): CoverToken {
  const token = coverTokenFor(book.id)
  if (
    book.coverType === 'color' &&
    book.coverValue &&
    /^#[0-9a-fA-F]{6}$/.test(book.coverValue)
  ) {
    return { ...token, from: book.coverValue }
  }
  return token
}

function coverStyle(book: BookResponse) {
  const token = coverToken(book)
  const style: Record<string, string> = {
    background: coverGradient(token),
    boxShadow: coverShadow(token)
  }
  if (book.coverType === 'image' && book.coverValue) {
    style.backgroundImage =
      `linear-gradient(135deg, rgba(46,44,41,0.20), rgba(46,44,41,0.44)), url("${book.coverValue}")`
    style.backgroundSize = 'cover'
    style.backgroundPosition = 'center'
  }
  return style
}

function coverTag(book: BookResponse): string {
  const idx = filteredBooks.value.findIndex((b) => b.id === book.id)
  return caseLabel(idx === -1 ? 0 : idx)
}

function relativeTime(iso: string): string {
  const t = +new Date(iso)
  if (Number.isNaN(t)) return '—'
  const diff = Date.now() - t
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min} 分钟前`
  const hour = Math.floor(min / 60)
  if (hour < 24) return `${hour} 小时前`
  const day = Math.floor(hour / 24)
  if (day < 30) return `${day} 天前`
  return new Date(iso).toLocaleDateString('zh-CN')
}

/* ---------------- 交互 ---------------- */
function openBook(book: BookResponse) {
  router.push(`/workspace/${book.id}`)
}

function closeSortMenu() {
  sortMenuOpen.value = false
  profileOpen.value = false
}

function pickSort(mode: SortMode) {
  bookStore.setSortMode(mode)
  sortMenuOpen.value = false
}

/* ---------------- 新建 / 重命名 ---------------- */
const dialog = reactive({
  open: false,
  mode: 'create' as 'create' | 'rename',
  id: 0,
  name: '',
  coverText: '',
  tokenIndex: 0,
  submitting: false,
  /** 新建时预选的封面图 */
  file: null as File | null,
  /** objectURL 预览 */
  previewUrl: ''
})

const dialogPreviewBg = computed(() => {
  const t = COVER_TOKENS[dialog.tokenIndex] ?? COVER_TOKENS[0]
  return coverGradient(t)
})

function openCreate() {
  dialog.open = true
  dialog.mode = 'create'
  dialog.id = 0
  dialog.name = ''
  dialog.coverText = ''
  dialog.tokenIndex = bookStore.books.length % COVER_TOKENS.length
  clearDialogCover()
}

function renameBook(book: BookResponse) {
  dialog.open = true
  dialog.mode = 'rename'
  dialog.id = book.id
  dialog.name = book.name
  dialog.coverText = book.coverText ?? ''
  dialog.tokenIndex = Math.max(0, COVER_TOKENS.indexOf(coverTokenFor(book.id)))
  clearDialogCover()
}

async function submitDialog() {
  const name = dialog.name.trim()
  if (!name || dialog.submitting) return
  dialog.submitting = true
  const token = COVER_TOKENS[dialog.tokenIndex] ?? COVER_TOKENS[0]
  try {
    if (dialog.mode === 'create') {
      // 有选图就先建书、再传封面；无图则用色板
      const created = await bookStore.createBook({
        name,
        coverType: dialog.file ? 'image' : 'color',
        coverValue: dialog.file ? null : token.from,
        coverText: dialog.coverText.trim() || null
      })

      if (dialog.file) {
        try {
          const res = await bookApi.uploadCover(created.id, dialog.file)
          await bookStore.updateBook(created.id, {
            coverType: 'image',
            coverValue: res.coverValue
          })
        } catch {
          // 建书已成功，封面失败不阻断流程
          message.warning('案件已创建，但封面图片上传失败')
        }
      }

      clearDialogCover()
      dialog.open = false
      message.success('案件已创建')
      router.push(`/workspace/${created.id}`)
    } else {
      await bookStore.updateBook(dialog.id, {
        name,
        coverType: 'color',
        coverValue: token.from,
        coverText: dialog.coverText.trim()
      })
      dialog.open = false
      message.success('已保存')
    }
  } catch {
    /* 拦截器已提示 */
  } finally {
    dialog.submitting = false
  }
}

function removeBook(book: BookResponse) {
  Modal.confirm({
    title: '删除案件',
    content: `「${book.name}」下的事件、页面与画布会一并删除，此操作不可撤销。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await bookStore.deleteBook(book.id)
      message.success('案件已删除')
    }
  })
}

/* ---------------- 封面上传 ----------------
 * 上传走 POST /api/books/{id}/cover，返回 { coverValue }，
 * 再 PUT /api/books/{id} 把 coverType 置为 image、coverValue 存该路径。
 * 图片访问地址的拼接集中在 @/utils/coverUrl.ts，后端确定静态映射后改一处即可。
 */
const MAX_COVER_BYTES = 2 * 1024 * 1024

const coverInput = ref<HTMLInputElement | null>(null)
/** 'card' 表示卡片 hover 直接换封面；'dialog' 表示在新建对话框里预选 */
let pickTarget: 'card' | 'dialog' = 'card'
/** 卡片模式下待上传的目标书 */
let pendingBook: BookResponse | null = null

function bookImage(book: BookResponse): string | null {
  if (book.coverType !== 'image') return null
  return coverUrl(book.coverValue)
}

function triggerCoverUpload(book: BookResponse) {
  pickTarget = 'card'
  pendingBook = book
  coverInput.value?.click()
}

function pickDialogCover() {
  pickTarget = 'dialog'
  pendingBook = null
  coverInput.value?.click()
}

function clearDialogCover() {
  if (dialog.previewUrl) URL.revokeObjectURL(dialog.previewUrl)
  dialog.file = null
  dialog.previewUrl = ''
}

async function onCoverFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  // 允许重复选择同一个文件
  input.value = ''
  if (!file) return

  if (!/^image\/(jpeg|png|webp)$/i.test(file.type)) {
    message.warning('只支持 JPEG / PNG / WebP')
    return
  }
  if (file.size > MAX_COVER_BYTES) {
    message.warning('图片不能超过 2MB')
    return
  }

  if (pickTarget === 'dialog') {
    if (dialog.previewUrl) URL.revokeObjectURL(dialog.previewUrl)
    dialog.file = file
    dialog.previewUrl = URL.createObjectURL(file)
    return
  }

  const book = pendingBook
  pendingBook = null
  if (!book) return

  // 乐观预览，失败再回滚
  const optimistic = URL.createObjectURL(file)
  const prevType = book.coverType
  const prevValue = book.coverValue
  book.coverType = 'image'
  book.coverValue = optimistic

  try {
    const res = await bookApi.uploadCover(book.id, file)
    // 拿真实路径覆盖本地预览
    book.coverValue = res.coverValue
    await bookStore.updateBook(book.id, {
      coverType: 'image',
      coverValue: res.coverValue
    })
    message.success('封面已更新')
    await bookStore.fetchBooks()
  } catch {
    book.coverType = prevType
    book.coverValue = prevValue
  } finally {
    URL.revokeObjectURL(optimistic)
  }
}
</script>

<style scoped>
.bookshelf {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background: var(--bg-app);
}

/* ═══════════ 顶栏 ═══════════ */
.topbar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: none;
  height: 64px;
  padding: 0 32px;
  background: var(--bg-bar);
  box-shadow: var(--sh-bar);
  z-index: 10;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-logo {
  display: flex;
  flex: none;
}

.brand-name {
  font-family: var(--font-serif);
  font-weight: 700;
  font-size: 17px;
  line-height: 1.3;
  color: var(--ink-1);
}

.brand-sub {
  font-size: 12px;
  line-height: 1.3;
  color: var(--ink-4);
}

.topbar-spacer {
  flex: 1;
}

.search {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 340px;
  height: 38px;
  padding: 0 16px;
  background: #fff;
  border: 1px solid var(--line-1);
  border-radius: 19px;
  transition: border-color 160ms var(--ease);
}

.search:focus-within {
  border-color: var(--green-line);
}

.search-icon {
  width: 15px;
  height: 15px;
  flex: none;
  color: var(--ink-5);
}

.search input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  color: var(--ink-2);
}

.search input::placeholder {
  color: var(--ink-5);
}

.btn-new {
  display: flex;
  align-items: center;
  gap: 7px;
  flex: none;
  height: 36px;
  padding: 0 16px;
  border: 0;
  border-radius: var(--r-lg);
  background: var(--green);
  color: #fff;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: var(--sh-btn);
  transition: background 160ms var(--ease), transform 160ms var(--ease);
}

.btn-new svg {
  width: 13px;
  height: 13px;
}

.btn-new:hover {
  background: var(--green-deep);
}

.btn-new:active {
  transform: translateY(1px);
}

.avatar-wrap {
  position: relative;
  flex: none;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  background: var(--green-tint);
  color: var(--green-deep);
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 160ms var(--ease);
}

.avatar:hover {
  background: #DFE9E1;
}

/* ═══════════ 用户资料面板 ═══════════ */
.profile-pop {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 60;
  display: flex;
  flex-direction: column;
  gap: 14px;
  width: 268px;
  padding: 18px;
  background: var(--bg-card);
  border: 1px solid var(--line-1);
  border-radius: var(--r-2xl);
  box-shadow: var(--sh-panel);
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--green-tint);
  color: var(--green-deep);
  font-size: 15px;
  font-weight: 500;
}

.profile-name {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.3;
  color: var(--ink-1);
  max-width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.profile-sub {
  font-size: 11px;
  line-height: 1.4;
  color: var(--ink-5);
}

.p-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.p-label {
  font-size: 11.5px;
  font-weight: 500;
  color: var(--ink-3);
}

.p-opt {
  margin-left: 6px;
  font-size: 10px;
  font-weight: 400;
  color: var(--ink-5);
}

.p-input {
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--line-1);
  border-radius: var(--r-lg);
  background: #fff;
  font-family: inherit;
  font-size: 13px;
  color: var(--ink-2);
  outline: 0;
  transition: border-color 160ms var(--ease);
}

.p-input:focus {
  border-color: var(--green-line);
}

.theme-chips {
  display: flex;
  gap: 8px;
}

.theme-chip {
  flex: 1;
  height: 30px;
  border: 1px solid var(--line-1);
  border-radius: var(--r-md);
  background: #fff;
  color: var(--ink-3);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: all 160ms var(--ease);
}

.theme-chip.active {
  background: var(--green-tint);
  border-color: var(--green-line);
  color: var(--green-ink);
  font-weight: 500;
}

.theme-chip.disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.profile-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 2px;
}

.p-ghost,
.p-primary {
  height: 32px;
  padding: 0 14px;
  border-radius: var(--r-lg);
  font-family: inherit;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 160ms var(--ease);
}

.p-ghost {
  border: 1px solid var(--line-1);
  background: #fff;
  color: var(--ink-3);
}

.p-ghost:hover {
  color: var(--ink-2);
  border-color: var(--green-line);
}

.p-primary {
  border: 0;
  background: var(--green);
  color: #fff;
  box-shadow: var(--sh-btn);
}

.p-primary:hover {
  background: var(--green-deep);
}

/* ═══════════ 内容区 ═══════════ */
.content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 40px 56px 56px;
}

.title-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}

.title {
  margin: 0;
  font-family: var(--font-serif);
  font-weight: 700;
  font-size: 26px;
  line-height: 1.3;
  color: var(--ink-1);
}

.subtitle {
  margin: 6px 0 0;
  font-size: 12.5px;
  line-height: 1.4;
  color: var(--ink-3);
}

.sort-wrap {
  position: relative;
}

.sort-ctrl {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 12px;
  background: #fff;
  border: 1px solid var(--line-1);
  border-radius: var(--r-lg);
  color: var(--ink-3);
  font-family: inherit;
  font-size: 12.5px;
  cursor: pointer;
  transition: border-color 160ms var(--ease), color 160ms var(--ease);
}

.sort-ctrl:hover {
  border-color: var(--green-line);
  color: var(--ink-2);
}

.caret {
  width: 10px;
  height: 10px;
}

.sort-menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  z-index: 20;
  min-width: 148px;
  padding: 4px;
  background: var(--bg-card);
  border: 1px solid var(--line-1);
  border-radius: var(--r-lg);
  box-shadow: var(--sh-card);
}

.sort-option {
  display: block;
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: var(--r-md);
  background: transparent;
  color: var(--ink-2);
  font-family: inherit;
  font-size: 12.5px;
  text-align: left;
  cursor: pointer;
}

.sort-option:hover {
  background: rgba(0, 0, 0, 0.025);
}

.sort-option.active {
  background: var(--green-tint);
  color: var(--green-ink);
  font-weight: 500;
}

/* ═══════════ 书架网格 ═══════════ */
.shelf {
  display: grid;
  grid-template-columns: repeat(auto-fill, 201px);
  column-gap: 22px;
  row-gap: 24px;
  justify-content: start;
}

.book-card {
  display: flex;
  flex-direction: column;
  gap: 11px;
  cursor: pointer;
}

.cover {
  position: relative;
  height: 252px;
  padding: 18px;
  border-radius: var(--r-xl);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: transform 180ms var(--ease), box-shadow 180ms var(--ease);
}

.book-card:hover .cover {
  transform: translateY(-2px);
}

.cover-tag {
  align-self: flex-start;
  padding: 4px 8px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.28);
  color: #fff;
  font-size: 10px;
  font-weight: 500;
  line-height: 1.2;
  letter-spacing: 0.02em;
}

.cover-push {
  flex: 1;
}

.cover-foot {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cover-name {
  margin: 0;
  font-family: var(--font-serif);
  font-weight: 700;
  font-size: 18px;
  line-height: 1.35;
  color: #fff;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.cover-sub {
  margin: 0;
  font-size: 10.5px;
  line-height: 1.4;
  color: rgba(255, 255, 255, 0.78);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cover-hover {
  position: absolute;
  top: 14px;
  right: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  opacity: 0;
  transform: translateY(-4px);
  transition: opacity 180ms var(--ease), transform 180ms var(--ease);
}

.book-card:hover .cover-hover {
  opacity: 1;
  transform: translateY(0);
}

.hover-btn {
  padding: 5px 9px;
  border: 0;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--ink-2);
  font-family: inherit;
  font-size: 10.5px;
  cursor: pointer;
}

.hover-btn:hover {
  background: #fff;
}

.hover-btn.danger {
  color: #A0574F;
}

.book-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin: 0;
  padding: 0 2px;
  font-size: 11.5px;
  line-height: 1.4;
  color: var(--ink-4);
}

.book-meta span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-meta span:last-child {
  flex: none;
}

/* 末尾新建空位 */
.create-slot {
  display: flex;
  flex-direction: column;
  gap: 11px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.create-ghost {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 252px;
  border-radius: var(--r-xl);
  background: #F0EDEA;
  border: 1px solid var(--line-1);
  filter: blur(0.5px);
  transition: background 180ms var(--ease), border-color 180ms var(--ease), filter 180ms var(--ease);
}

.create-slot:hover .create-ghost {
  background: #ECE8E4;
  border-color: var(--green-line);
  filter: blur(0);
}

.create-ghost svg {
  width: 44px;
  height: 44px;
}

.create-label {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink-3);
}

.create-meta {
  padding: 0 2px;
  font-size: 11.5px;
  line-height: 1.4;
  color: var(--ink-4);
}

/* ═══════════ 空 / 加载态 ═══════════ */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 201px);
  column-gap: 22px;
  row-gap: 24px;
}

.skeleton-card {
  height: 252px;
  border-radius: var(--r-xl);
  background: linear-gradient(100deg, #EFECE6 25%, #F6F4F0 50%, #EFECE6 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}

@keyframes shimmer {
  from { background-position: 200% 0; }
  to   { background-position: -200% 0; }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  min-height: 460px;
}

.empty-ghost {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 201px;
  height: 252px;
  border: 1px solid var(--line-1);
  border-radius: var(--r-xl);
  background: #F0EDEA;
  color: var(--ink-3);
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  filter: blur(0.5px);
  transition: filter 180ms var(--ease), border-color 180ms var(--ease);
}

.empty-ghost:hover {
  filter: blur(0);
  border-color: var(--green-line);
}

.empty-ghost svg {
  width: 44px;
  height: 44px;
}

.empty-hint {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-4);
}

/* ═══════════ 翻页 ═══════════ */
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding-top: 28px;
}

.pager-btn,
.pager-dot {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  border: 1px solid var(--line-1);
  border-radius: var(--r-md);
  background: #fff;
  color: var(--ink-3);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: all 160ms var(--ease);
}

.pager-btn svg {
  width: 12px;
  height: 12px;
}

.pager-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pager-dot.active {
  background: var(--green);
  border-color: var(--green);
  color: #fff;
  font-weight: 500;
}

.pager-btn:not(:disabled):hover,
.pager-dot:not(.active):hover {
  border-color: var(--green-line);
  color: var(--ink-2);
}

/* ═══════════ 封面上传 ═══════════ */
.hidden-file {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: inherit;
}

/* 图片封面时压暗底部，保证标题可读 */
.cover:has(.cover-img) .cover-foot {
  position: relative;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.45);
}

.cover:has(.cover-img)::after {
  content: '';
  position: absolute;
  inset: auto 0 0 0;
  height: 58%;
  background: linear-gradient(to top, rgba(24, 22, 20, 0.52), transparent);
  pointer-events: none;
}

.field-opt {
  margin-left: 6px;
  font-size: 10px;
  font-weight: 400;
  color: var(--ink-5);
}

.cover-pick {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cover-preview {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 86px;
  border-radius: 8px;
  border: 1px solid #E4DFD6;
  overflow: hidden;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-preview-hint {
  font-size: 10px;
  color: var(--ink-5);
}

.cover-pick-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ghost-btn.sm {
  height: 26px;
  padding: 0 10px;
  font-size: 11px;
}

.swatches.dim {
  opacity: 0.4;
  pointer-events: none;
}

/* ═══════════ 对话框 ═══════════ */
.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(46, 44, 41, 0.24);
}

.dialog {
  width: 420px;
  padding: 24px;
  background: var(--bg-card);
  border-radius: var(--r-2xl);
  box-shadow: var(--sh-panel);
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.dialog-title {
  margin: 0;
  font-family: var(--font-serif);
  font-weight: 700;
  font-size: 18px;
  color: var(--ink-1);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 11.5px;
  font-weight: 500;
  color: var(--ink-3);
}

.field-input {
  height: 38px;
  padding: 0 12px;
  border: 1px solid var(--line-1);
  border-radius: var(--r-lg);
  background: #fff;
  font-family: inherit;
  font-size: 13px;
  color: var(--ink-2);
  outline: 0;
  transition: border-color 160ms var(--ease);
}

.field-input:focus {
  border-color: var(--green-line);
}

.field-input::placeholder {
  color: var(--ink-5);
}

.swatches {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.swatch {
  width: 30px;
  height: 30px;
  border: 2px solid transparent;
  border-radius: var(--r-md);
  cursor: pointer;
  transition: transform 140ms var(--ease);
}

.swatch:hover {
  transform: translateY(-2px);
}

.swatch.active {
  border-color: var(--green-ink);
  box-shadow: var(--sh-card);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 4px;
}

.ghost-btn,
.primary-btn {
  height: 36px;
  padding: 0 18px;
  border-radius: var(--r-lg);
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 160ms var(--ease);
}

.ghost-btn {
  border: 1px solid var(--line-1);
  background: #fff;
  color: var(--ink-3);
}

.ghost-btn:hover {
  color: var(--ink-2);
  border-color: var(--green-line);
}

.primary-btn {
  border: 0;
  background: var(--green);
  color: #fff;
  box-shadow: var(--sh-btn);
}

.primary-btn:hover:not(:disabled) {
  background: var(--green-deep);
}

.primary-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}
</style>
