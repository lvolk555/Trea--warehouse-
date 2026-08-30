import request from '../utils/request'

// 教师题库接口
export const questionPage = (params) => request.get('/teacher/question/page', { params })
export const saveQuestion = (data) => request.post('/teacher/question/save', data)
export const deleteQuestion = (id) => request.delete(`/teacher/question/${id}`)

// 教师组卷接口
export const teacherExamList = (courseId) => request.get('/teacher/exam/list', { params: { courseId } })
export const saveExam = (data) => request.post('/teacher/exam/save', data)
export const publishExam = (examId) => request.post(`/teacher/exam/publish/${examId}`)
export const deleteExam = (examId) => request.delete(`/teacher/exam/${examId}`)
