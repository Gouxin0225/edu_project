import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

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
      '/auth': { target: 'http://localhost:8080', changeOrigin: true },
      '/user': { target: 'http://localhost:8080', changeOrigin: true },
      '/admin/user': { target: 'http://localhost:8080', changeOrigin: true },
      '/admin/class': { target: 'http://localhost:8080', changeOrigin: true },
      '/teacher/my-classes': { target: 'http://localhost:8080', changeOrigin: true },
      '/teacher/student': { target: 'http://localhost:8080', changeOrigin: true },
      '/teacher/students': { target: 'http://localhost:8080', changeOrigin: true },
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
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
          if (id.includes('/node_modules/element-plus/') || id.includes('/node_modules/@element-plus/')) {
            return 'element-plus'
          }
          if (id.includes('/node_modules/echarts/')) {
            return 'echarts'
          }
          if (id.includes('/node_modules/xlsx/')) {
            return 'xlsx'
          }
          if (id.includes('/node_modules/axios/')) {
            return 'axios'
          }
          return 'vendor'
        }
      }
    }
  }
})
