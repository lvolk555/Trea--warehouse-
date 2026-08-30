import request from '../utils/request'

// AI 出题
export const aiGenerate = (data) => request.post('/teacher/ai/generate', data)
export const aiSaveDrafts = (data) => request.post('/teacher/ai/save-drafts', data)

// AI 批改
export const pendingGrades = () => request.get('/teacher/ai/pending-grades')
export const aiGrade = (answerId) => request.post(`/teacher/ai/grade/${answerId}`)
export const adjustScore = (answerId, score, comment) =>
  request.post(`/teacher/ai/grade/${answerId}/adjust`, null, { params: { score, comment } })
