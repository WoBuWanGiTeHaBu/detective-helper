'use strict'

/*
  渲染进程与外壳之间的桥。

  contextIsolation 是开着的，渲染进程拿不到 Node，只能通过这里暴露的白名单方法。
  只暴露「网页自己做不到的原生能力」，不暴露任何文件系统或命令执行入口。

  前端可以这样判断自己跑在桌面外壳里：

      if (window.detectiveHelper) {
        // 桌面版：可以调原生能力
      }

  浏览器里 window.detectiveHelper 是 undefined，前端原有逻辑不受影响。
*/

const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('detectiveHelper', {
  /** 标记：让前端能识别「我在外壳里」。值里带上版本便于排查。 */
  desktop: true,
  platform: process.platform,
  versions: {
    electron: process.versions.electron,
    chrome: process.versions.chrome,
    node: process.versions.node
  },

  /** 应用与运行环境信息（版本、数据目录、后端状态） */
  getInfo: () => ipcRenderer.invoke('shell:get-info'),

  /** 拉取当前启动状态（启动页加载完主动调一次，避免错过早于页面就绪的推送） */
  getStatus: () => ipcRenderer.invoke('shell:get-status'),

  /** 启动失败后重试：重新走完整启动流程 */
  retryBoot: () => ipcRenderer.invoke('shell:retry-boot'),

  /** 用系统文件管理器打开数据目录（备份案件就是把这个目录拷走） */
  openDataDir: () => ipcRenderer.invoke('shell:open-data-dir'),

  /** 打开日志目录，出问题时用 */
  openLogs: () => ipcRenderer.invoke('shell:open-logs'),

  /** 关于对话框 */
  about: () => ipcRenderer.invoke('shell:about'),

  /** 刷新页面 */
  reload: () => ipcRenderer.invoke('shell:reload'),

  /** 设置缩放（1.0 = 100%） */
  setZoom: (factor) => ipcRenderer.invoke('shell:set-zoom', factor),

  /** 读取当前缩放 */
  getZoom: () => ipcRenderer.invoke('shell:get-zoom'),

  /** 切换全屏，返回切换后的状态 */
  toggleFullscreen: () => ipcRenderer.invoke('shell:toggle-fullscreen'),

  /**
   * 订阅启动进度（供启动页使用）。
   * 返回取消订阅的函数，避免热更新时累积监听器。
   */
  onStatus: (callback) => {
    const listener = (_event, payload) => callback(payload)
    ipcRenderer.on('shell:status', listener)
    return () => ipcRenderer.removeListener('shell:status', listener)
  }
})
