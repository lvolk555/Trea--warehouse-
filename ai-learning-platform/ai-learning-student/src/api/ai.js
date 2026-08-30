import request from '../utils/request'

// AI 答疑接口
export const aiSessions = () => request.get('/student/ai/sessions')
export const aiMessages = (sessionId) => request.get(`/student/ai/sessions/${sessionId}/messages`)
export const deleteAiSession = (sessionId) => request.delete(`/student/ai/sessions/${sessionId}`)
