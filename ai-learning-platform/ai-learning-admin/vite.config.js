import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 管理端开发服务器：端口 5174，接口代理到后端 8080
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
