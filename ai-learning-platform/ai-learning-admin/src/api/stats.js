import request from '../utils/request'

// 看板统计数据
export const teacherStats = () => request.get('/stats/teacher')
export const adminStats = () => request.get('/stats/admin')
