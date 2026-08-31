import request from '../utils/request'

// 学生端积分接口
export const pointsAccount = () => request.get('/points/account')
export const pointsRecords = (params) => request.get('/points/records', { params })
export const dailySign = () => request.post('/points/sign')
export const signMonth = () => request.get('/points/sign/month')
export const exchangeCourse = (courseId, couponId) => request.post(`/points/exchange/${courseId}`, null, { params: { couponId } })
export const myExchanges = () => request.get('/points/exchange/my')

// 积分活动
export const pointsActivities = () => request.get('/points/activities')
export const claimActivity = (id) => request.post(`/points/activities/${id}/claim`)

// 我的优惠券
export const myCoupons = () => request.get('/points/coupons')

// 运营接口：公告、评论
export const notices = () => request.get('/ops/notices')
export const courseComments = (courseId) => request.get(`/ops/comments/${courseId}`)
export const publishComment = (courseId, content) => request.post(`/ops/comments/${courseId}`, { content })
