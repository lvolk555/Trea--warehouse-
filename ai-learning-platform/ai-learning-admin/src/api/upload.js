import request from '../utils/request'

// 文件上传：图片/视频统一走 /upload，返回 { url, name }
// 视频文件较大，覆盖默认 15s 超时，避免大文件上传被中断
export const uploadFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload', formData, { timeout: 600000 })
}