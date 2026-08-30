import request from '../utils/request'

// 学生看板数据
export const studentStats = () => request.get('/stats/student')
