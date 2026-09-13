import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],

  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },

  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      // target 必须与后端 server.port（backend/src/main/resources/application.yml）一致。
      // 旧配置指向 8080，但后端实际是随机端口，且 8080 被本机 nginx 占用，故改为 8081。
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // 封面等静态资源。后端静态映射路径确定后，改这里 + src/utils/coverUrl.ts 两处即可
      '/files': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },

  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    },
    rollupOptions: {
      output: {
        manualChunks: {
          // 注意：axios / dayjs / lodash-es 与 ant-design-vue 之间存在互相引用，
          // 单独拆成 utils 会产生循环 chunk，这里只做稳定的三方拆分。
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'ant-design': ['ant-design-vue', '@ant-design/icons-vue']
        }
      }
    }
  }
})