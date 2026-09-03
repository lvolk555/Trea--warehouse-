import request from '../utils/request'

// 章节练习
export const pickQuestions = (chapterId, limit = 10) =>
  request.get('/student/practice/questions', { params: { chapterId, limit } })
export const submitPractice = (data) => request.post('/student/practice/submit', data)
export const errorBook = (courseId) => request.get('/student/practice/error-book', { params: { courseId } })
export const markMastered = (recordId) => request.post(`/student/practice/mastered/${recordId}`)

// 考试
export const studentExamList = () => request.get('/student/exam/list')
export const startExam = (examId) => request.get(`/student/exam/start/${examId}`)
export const submitExam = (data) => request.post('/student/exam/submit', data)
export const myScores = () => request.get('/student/exam/scores')
export const examRecordDetail = (recordId) => request.get(`/student/exam/record/${recordId}`)
