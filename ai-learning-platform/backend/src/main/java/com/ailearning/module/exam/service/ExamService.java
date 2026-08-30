package com.ailearning.module.exam.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.entity.CourseEnrollment;
import com.ailearning.module.course.mapper.CourseEnrollmentMapper;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.exam.dto.ExamResultVO;
import com.ailearning.module.exam.dto.ExamSaveDTO;
import com.ailearning.module.exam.dto.ExamSubmitDTO;
import com.ailearning.module.exam.dto.ExamVO;
import com.ailearning.module.exam.dto.QuestionVO;
import com.ailearning.module.exam.entity.Exam;
import com.ailearning.module.exam.entity.ExamAnswer;
import com.ailearning.module.exam.entity.ExamRecord;
import com.ailearning.module.exam.entity.Question;
import com.ailearning.module.exam.mapper.ExamAnswerMapper;
import com.ailearning.module.exam.mapper.ExamMapper;
import com.ailearning.module.exam.mapper.ExamRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考试服务：教师组卷/发布；学生参加考试（限时由前端按 duration 倒计时控制）、交卷自动判分、成绩查询
 * 判分规则：客观题自动判分；简答题暂记 0 分，阶段四接入 AI 批改后重算
 */
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final QuestionService questionService;
    private final ObjectMapper objectMapper;

    /** 试卷状态：0草稿 1已发布 */
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;

    /**
     * 教师组卷：创建/更新试卷，题目必须全部属于所选课程
     */
    @Transactional(rollbackFor = Exception.class)
    public ExamVO save(ExamSaveDTO dto) {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        checkOwnCourse(dto.getCourseId());
        checkQuestionsBelongToCourse(dto.getQuestionIds(), dto.getCourseId());

        Exam exam = dto.getId() != null ? getOwnExam(dto.getId()) : new Exam();
        exam.setCourseId(dto.getCourseId());
        exam.setTitle(dto.getTitle());
        exam.setDuration(dto.getDuration());
        exam.setQuestionIds(toJson(dto.getQuestionIds()));
        exam.setStatus(dto.getStatus() != null && dto.getStatus() == STATUS_PUBLISHED
                ? STATUS_PUBLISHED : STATUS_DRAFT);
        if (exam.getId() == null) {
            examMapper.insert(exam);
        } else {
            examMapper.updateById(exam);
        }
        return toVO(exam, false);
    }

    /**
     * 发布试卷
     */
    public ExamVO publish(Long examId) {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        Exam exam = getOwnExam(examId);
        exam.setStatus(STATUS_PUBLISHED);
        examMapper.updateById(exam);
        return toVO(exam, false);
    }

    /**
     * 删除试卷（教师，仅本人）
     */
    public void delete(Long examId) {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        getOwnExam(examId);
        examMapper.deleteById(examId);
    }

    /**
     * 教师查看自己课程的试卷列表
     */
    public List<ExamVO> teacherExams(Long courseId) {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        List<Course> myCourses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getTeacherId, UserContext.userId()));
        if (myCourses.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .in(Exam::getCourseId, myCourses.stream().map(Course::getId).toList())
                .eq(courseId != null, Exam::getCourseId, courseId)
                .orderByDesc(Exam::getCreateTime);
        return examMapper.selectList(wrapper).stream().map(e -> toVO(e, false)).toList();
    }

    /**
     * 学生可参加的考试：已选课程下已发布的试卷
     */
    public List<ExamVO> studentExams() {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        List<CourseEnrollment> enrollments = enrollmentMapper.selectList(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getStudentId, UserContext.userId()));
        if (enrollments.isEmpty()) {
            return List.of();
        }
        List<Exam> exams = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .in(Exam::getCourseId, enrollments.stream().map(CourseEnrollment::getCourseId).toList())
                .eq(Exam::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Exam::getCreateTime));
        return exams.stream().map(e -> toVO(e, false)).toList();
    }

    /**
     * 学生进入考试：返回试卷题目（隐藏答案与解析）；已交卷则拒绝重复进入
     */
    public ExamVO startExam(Long examId) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        Exam exam = getPublishedExam(examId);
        Long submitted = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStudentId, UserContext.userId())
                .isNotNull(ExamRecord::getSubmitTime));
        if (submitted > 0) {
            throw new BizException("你已完成该考试，不能重复参加");
        }
        return toVO(exam, true);
    }

    /**
     * 交卷：自动判分（客观题），生成考试记录与答题明细，返回成绩
     */
    @Transactional(rollbackFor = Exception.class)
    public ExamResultVO submit(ExamSubmitDTO dto) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        Exam exam = getPublishedExam(dto.getExamId());

        Long submitted = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, dto.getExamId())
                .eq(ExamRecord::getStudentId, UserContext.userId())
                .isNotNull(ExamRecord::getSubmitTime));
        if (submitted > 0) {
            throw new BizException("你已完成该考试，不能重复交卷");
        }

        List<Long> questionIds = parseQuestionIds(exam.getQuestionIds());
        Map<Long, Question> questionMap = new HashMap<>();
        questionService.listByIds(questionIds).forEach(vo -> {
            Question q = questionService.getRequired(vo.getId());
            questionMap.put(q.getId(), q);
        });

        // 创建考试记录
        ExamRecord record = new ExamRecord();
        record.setExamId(exam.getId());
        record.setStudentId(UserContext.userId());
        record.setSubmitTime(LocalDateTime.now());
        examRecordMapper.insert(record);

        // 逐题判分
        int correctCount = 0;
        List<ExamResultVO.AnswerDetail> details = new ArrayList<>();
        for (Long questionId : questionIds) {
            Question question = questionMap.get(questionId);
            if (question == null) {
                continue;
            }
            String studentAnswer = dto.getAnswers() != null
                    ? dto.getAnswers().getOrDefault(questionId, "") : "";

            ExamAnswer answer = new ExamAnswer();
            answer.setRecordId(record.getId());
            answer.setQuestionId(questionId);
            answer.setStudentAnswer(studentAnswer);

            ExamResultVO.AnswerDetail detail = new ExamResultVO.AnswerDetail();
            detail.setQuestionId(questionId);
            detail.setType(question.getType());
            detail.setContent(question.getContent());
            detail.setOptions(questionService.parseOptions(question.getOptions()));
            detail.setStudentAnswer(studentAnswer);
            detail.setAnswer(question.getAnswer());
            detail.setAnalysis(question.getAnalysis());

            if (question.getType() == QuestionService.TYPE_SUBJECTIVE) {
                // 简答题：等待阶段四 AI 批改，暂记错
                answer.setCorrect(0);
                detail.setCorrect(0);
            } else {
                boolean correct = judge(question, studentAnswer);
                answer.setCorrect(correct ? 1 : 0);
                detail.setCorrect(correct ? 1 : 0);
                if (correct) {
                    correctCount++;
                }
            }
            examAnswerMapper.insert(answer);
            details.add(detail);
        }

        // 总分：答对题数 / 总题数 × 100
        int total = details.size();
        BigDecimal score = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(correctCount * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
        record.setScore(score);
        examRecordMapper.updateById(record);

        ExamResultVO result = new ExamResultVO();
        result.setRecordId(record.getId());
        result.setExamId(exam.getId());
        result.setScore(score);
        result.setCorrectCount(correctCount);
        result.setTotalCount(total);
        result.setDetails(details);
        return result;
    }

    /**
     * 我的考试成绩列表
     */
    public List<Map<String, Object>> myScores() {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStudentId, UserContext.userId())
                .isNotNull(ExamRecord::getSubmitTime)
                .orderByDesc(ExamRecord::getSubmitTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecord r : records) {
            Exam exam = examMapper.selectById(r.getExamId());
            Map<String, Object> item = new HashMap<>();
            item.put("recordId", r.getId());
            item.put("examId", r.getExamId());
            item.put("examTitle", exam != null ? exam.getTitle() : "");
            item.put("courseId", exam != null ? exam.getCourseId() : null);
            item.put("score", r.getScore());
            item.put("submitTime", r.getSubmitTime());
            Course course = exam != null ? courseMapper.selectById(exam.getCourseId()) : null;
            item.put("courseTitle", course != null ? course.getTitle() : "");
            result.add(item);
        }
        return result;
    }

    /** 获取已发布试卷，否则抛异常 */
    private Exam getPublishedExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null || exam.getStatus() != STATUS_PUBLISHED) {
            throw new BizException("考试不存在或未发布");
        }
        return exam;
    }

    /** 获取当前教师名下试卷 */
    private Exam getOwnExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BizException("试卷不存在");
        }
        checkOwnCourse(exam.getCourseId());
        return exam;
    }

    /** 校验课程属于当前教师 */
    private void checkOwnCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || !course.getTeacherId().equals(UserContext.userId())) {
            throw new BizException("课程不存在或无权操作");
        }
    }

    /** 校验所有题目均属于指定课程 */
    private void checkQuestionsBelongToCourse(List<Long> questionIds, Long courseId) {
        for (Long id : questionIds) {
            Question q = questionService.getRequired(id);
            if (!courseId.equals(q.getCourseId())) {
                throw new BizException("题目【" + q.getId() + "】不属于所选课程");
            }
        }
    }

    /** 客观题判分（与练习同一规则） */
    private boolean judge(Question question, String studentAnswer) {
        String expected = question.getAnswer().trim();
        String actual = studentAnswer == null ? "" : studentAnswer.trim();
        if (question.getType() == QuestionService.TYPE_MULTI) {
            return normalizeMulti(expected).equals(normalizeMulti(actual));
        }
        return expected.equalsIgnoreCase(actual);
    }

    private String normalizeMulti(String answer) {
        return answer.replaceAll("[,，、\\s]", "").toUpperCase().chars()
                .sorted()
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private String toJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            throw new BizException("题目列表格式错误");
        }
    }

    List<Long> parseQuestionIds(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 实体 → VO
     *
     * @param forStudent 学生视角时隐藏答案与解析
     */
    private ExamVO toVO(Exam exam, boolean forStudent) {
        ExamVO vo = new ExamVO();
        vo.setId(exam.getId());
        vo.setCourseId(exam.getCourseId());
        vo.setTitle(exam.getTitle());
        vo.setDuration(exam.getDuration());
        vo.setStatus(exam.getStatus());
        vo.setCreateTime(exam.getCreateTime());
        Course course = courseMapper.selectById(exam.getCourseId());
        vo.setCourseTitle(course != null ? course.getTitle() : "");

        List<Long> ids = parseQuestionIds(exam.getQuestionIds());
        vo.setQuestionIds(ids);
        if (!forStudent) {
            vo.setQuestions(questionService.listByIds(ids));
        } else {
            // 学生考试页：隐藏答案与解析
            List<QuestionVO> hidden = new ArrayList<>();
            for (QuestionVO q : questionService.listByIds(ids)) {
                q.setAnswer(null);
                q.setAnalysis(null);
                hidden.add(q);
            }
            vo.setQuestions(hidden);
        }
        return vo;
    }
}
