import axios from 'axios'
import { useUserStore } from '../stores/user'

// axios 实例：统一携带 token、统一处理响应
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：附加 Authorization 头（Sa-Token 从请求头读取，前缀 Bearer）
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('student_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：解包统一响应结构 {code, message, data}
request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body.code === 200) {
      return body.data
    }
    if (body.code === 401) {
      const userStore = useUserStore()
      userStore.logout()
    }
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (err) => Promise.reject(err)
)

export default request
