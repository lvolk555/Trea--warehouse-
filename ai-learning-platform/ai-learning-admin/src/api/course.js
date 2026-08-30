import request from '../utils/request'

// 教师课程接口
export const teacherCourseList = () => request.get('/teacher/course/list')
export const saveCourse = (data) => request.post('/teacher/course/save', data)
export const submitCourse = (courseId) => request.post(`/teacher/course/submit/${courseId}`)
export const deleteCourse = (courseId) => request.delete(`/teacher/course/${courseId}`)

// 管理端课程接口
export const pendingCourses = () => request.get('/admin/course/pending')
export const reviewCourse = (data) => request.post('/admin/course/review', data)
export const changeCourseStatus = (courseId, online) =>
  request.post(`/admin/course/status/${courseId}`, null, { params: { online } })
export const adminCoursePage = (params) => request.get('/admin/course/page', { params })

// 课程详情（含章节视频树）
export const courseDetail = (courseId) => request.get(`/course/${courseId}`)
