import axios from 'axios'
import { useUserStore } from '../stores/user'

// axios 实例：统一携带 token、统一处理响应
// 参数序列化：对参数值二次编码（% → %25），兼容反向代理对 query string
// 预先解码一次的行为（后端 QueryParamDecodeFilter 会做对应容错解码）
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  paramsSerializer: {
    serialize: (params) => {
      const parts = []
      Object.keys(params || {}).forEach((key) => {
        const value = params[key]
        if (value === undefined || value === null || value === '') return
        parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value)).replace(/%/g, '%25')}`)
      })
      return parts.join('&')
    }
  }
})

// 请求拦截：附加 Authorization 头（Sa-Token 从请求头读取，前缀 Bearer）
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
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
