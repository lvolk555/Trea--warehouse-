import request from '../utils/request'

// 学生端积分接口
export const pointsAccount = () => request.get('/points/account')
export const pointsRecords = (params) => request.get('/points/records', { params })
export const dailySign = () => request.post('/points/sign')
export const signMonth = () => request.get('/points/sign/month')
export const exchangeCourse = (courseId) => request.post(`/points/exchange/${courseId}`)
export const myExchanges = () => request.get('/points/exchange/my')

// 运营接口：公告、评论
export const notices = () => request.get('/ops/notices')
export const courseComments = (courseId) => request.get(`/ops/comments/${courseId}`)
export const publishComment = (courseId, content) => request.post(`/ops/comments/${courseId}`, { content })
