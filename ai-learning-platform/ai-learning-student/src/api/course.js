import request from '../utils/request'

// 学生端课程接口
export const courseSquare = (params) => request.post('/course/square', params)
export const courseDetail = (courseId) => request.get(`/course/${courseId}`)
export const myCourses = () => request.get('/course/my')
export const enrollCourse = (courseId) => request.post(`/course/enroll/${courseId}`)

// 学习进度接口
export const reportProgress = (data) => request.post('/study/progress', data)
export const resumePosition = (videoId) => request.get(`/study/resume/${videoId}`)

// 学习笔记接口
export const getNote = (videoId) => request.get(`/study/note/${videoId}`)
export const saveNote = (data) => request.post('/study/note', data)
export const deleteNote = (id) => request.delete(`/study/note/${id}`)
export const myNotes = () => request.get('/study/notes')
