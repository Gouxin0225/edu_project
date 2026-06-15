import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

const backendTarget = 'http://127.0.0.1:8080'

function normalizePackageChunkName(packageName: string) {
  return packageName.replace('@', '').replace('/', '-')
}

function getPackageName(id: string) {
  const packagePath = id.split('/node_modules/')[1]
  if (!packagePath) return ''

  const parts = packagePath.split('/')
  return parts[0].startsWith('@') ? `${parts[0]}/${parts[1]}` : parts[0]
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3001,
    host: true,
    proxy: {
      '/auth': { target: backendTarget, changeOrigin: true },
      '/user': { target: backendTarget, changeOrigin: true },
      '/admin/user': { target: backendTarget, changeOrigin: true },
      '/admin/class': { target: backendTarget, changeOrigin: true },
      '/teacher/my-classes': { target: backendTarget, changeOrigin: true },
      '/teacher/student': { target: backendTarget, changeOrigin: true },
      '/teacher/students': { target: backendTarget, changeOrigin: true },
      '/api': { target: backendTarget, changeOrigin: true }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }
          if (id.includes('/node_modules/vue/') || id.includes('/node_modules/vue-router/') || id.includes('/node_modules/pinia/')) {
            return 'vue-vendor'
          }
          if (id.includes('/node_modules/@element-plus/icons-vue/')) {
            return 'element-plus-icons'
          }
          if (id.includes('/node_modules/element-plus/') || id.includes('/node_modules/@element-plus/')) {
            return 'element-plus'
          }
          if (id.includes('/node_modules/echarts/') || id.includes('/node_modules/zrender/')) {
            return 'echarts'
          }
          if (id.includes('/node_modules/xlsx/') || id.includes('/node_modules/cfb/') || id.includes('/node_modules/codepage/') || id.includes('/node_modules/ssf/')) {
            return 'xlsx'
          }
          if (id.includes('/node_modules/axios/')) {
            return 'axios'
          }
          if (id.includes('/node_modules/@codemirror/') || id.includes('/node_modules/@lezer/')) {
            return 'codemirror'
          }
          if (id.includes('/node_modules/markdown-it/') || id.includes('/node_modules/linkify-it/') || id.includes('/node_modules/mdurl/') || id.includes('/node_modules/entities/') || id.includes('/node_modules/uc.micro/')) {
            return 'markdown'
          }

          const packageName = getPackageName(id)
          if (['vue-demi', '@vue/devtools-api', 'lodash-unified'].includes(packageName)) {
            return
          }
          return packageName ? `npm-${normalizePackageChunkName(packageName)}` : 'vendor'
        }
      }
    }
  }
})
