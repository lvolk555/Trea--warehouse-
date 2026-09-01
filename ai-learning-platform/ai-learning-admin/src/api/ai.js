import request from '../utils/request'

// AI 出题
export const aiGenerate = (data) => request.post('/teacher/ai/generate', data)
export const aiSaveDrafts = (data) => request.post('/teacher/ai/save-drafts', data)

// AI 生成教程文章（返回 Markdown 内容，生成较慢，放宽超时）
export const aiGenerateArticle = (data) => request.post('/teacher/ai/generate-article', data, { timeout: 120000 })

// AI 批改
export const pendingGrades = () => request.get('/teacher/ai/pending-grades')
export const aiGrade = (answerId) => request.post(`/teacher/ai/grade/${answerId}`)
export const adjustScore = (answerId, score, comment) =>
  request.post(`/teacher/ai/grade/${answerId}/adjust`, null, { params: { score, comment } })
