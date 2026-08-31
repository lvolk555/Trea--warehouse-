import request from '../utils/request'

// 管理端积分接口
export const pointsRules = () => request.get('/admin/points/rules')
export const updatePointsRule = (id, params) => request.post(`/admin/points/rules/${id}`, null, { params })
export const exchangeRecords = (params) => request.get('/admin/points/exchanges', { params })

// 管理端积分活动接口
export const activityList = () => request.get('/admin/points/activities')
export const createActivity = (data) => request.post('/admin/points/activities', data)
export const updateActivity = (id, data) => request.post(`/admin/points/activities/${id}`, data)
export const toggleActivity = (id, enabled) => request.post(`/admin/points/activities/${id}/status`, null, { params: { enabled } })
export const deleteActivity = (id) => request.delete(`/admin/points/activities/${id}`)

// 管理端公告接口
export const noticePage = (params) => request.get('/admin/ops/notices', { params })
export const saveNotice = (data) => request.post('/admin/ops/notices', data)
export const noticeStatus = (id, publish) => request.post(`/admin/ops/notices/${id}/status`, null, { params: { publish } })
export const noticeTop = (id, top) => request.post(`/admin/ops/notices/${id}/top`, null, { params: { top } })
export const deleteNotice = (id) => request.delete(`/admin/ops/notices/${id}`)

// 管理端评论接口
export const commentPage = (params) => request.get('/admin/ops/comments', { params })
export const reviewComment = (id, visible) => request.post(`/admin/ops/comments/${id}/review`, null, { params: { visible } })
export const deleteComment = (id) => request.delete(`/admin/ops/comments/${id}`)

// 管理端用户接口
export const userPage = (params) => request.get('/admin/ops/users', { params })
export const userStatus = (userId, enable) => request.post(`/admin/ops/users/${userId}/status`, null, { params: { enable } })
export const userRole = (userId, role) => request.post(`/admin/ops/users/${userId}/role`, null, { params: { role } })
