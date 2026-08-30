import request from '../utils/request'

// 学生端课程接口
export const courseSquare = (params) => request.get('/course/square', { params })
export const courseDetail = (courseId) => request.get(`/course/${courseId}`)
export const myCourses = () => request.get('/course/my')
export const enrollCourse = (courseId) => request.post(`/course/enroll/${courseId}`)

// 学习进度接口
export const reportProgress = (data) => request.post('/study/progress', data)
export const resumePosition = (videoId) => request.get(`/study/resume/${videoId}`)
