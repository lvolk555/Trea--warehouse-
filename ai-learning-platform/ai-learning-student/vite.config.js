import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 学生端开发服务器：端口 5173，接口代理到后端 8080
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
