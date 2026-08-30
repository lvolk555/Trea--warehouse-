package com.ailearning.module.exam.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.course.entity.Chapter;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.mapper.ChapterMapper;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.exam.dto.ErrorBookVO;
import com.ailearning.module.exam.dto.PracticeResultVO;
import com.ailearning.module.exam.dto.PracticeSubmitDTO;
import com.ailearning.module.exam.entity.PracticeRecord;
import com.ailearning.module.exam.entity.Question;
import com.ailearning.module.exam.mapper.PracticeRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 章节练习服务：单题即时判分 + 错题本（按课程归类、标记掌握）
 * 判分规则：客观题（单选/多选/判断）按答案精确比对；简答题练习模式不做判分，仅记录
 */
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final PracticeRecordMapper practiceRecordMapper;
    private final QuestionService questionService;
    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;

    /**
     * 提交单题练习：即时判分并写入练习记录（错题自动进入错题本）
     */
    @Transactional(rollbackFor = Exception.class)
    public PracticeResultVO submit(PracticeSubmitDTO dto) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        Question question = questionService.getRequired(dto.getQuestionId());
        if (question.getType() == QuestionService.TYPE_SUBJECTIVE) {
            throw new BizException("简答题请在考试中作答，练习仅支持客观题");
        }

        boolean correct = judge(question, dto.getStudentAnswer());

        PracticeRecord record = new PracticeRecord();
        record.setStudentId(UserContext.userId());
        record.setQuestionId(question.getId());
        record.setStudentAnswer(dto.getStudentAnswer().trim());
        record.setCorrect(correct ? 1 : 0);
        record.setMastered(0);
        practiceRecordMapper.insert(record);

        PracticeResultVO vo = new PracticeResultVO();
        vo.setCorrect(correct);
        vo.setAnswer(question.getAnswer());
        vo.setAnalysis(question.getAnalysis());
        return vo;
    }

    /**
     * 错题本列表：当前学生未标记掌握的错题，按课程归类返回
     */
    public Map<String, Object> errorBook(Long courseId) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        long studentId = UserContext.userId();

        // 取每个学生最近一次作答错误且未掌握的记录
        List<PracticeRecord> records = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getStudentId, studentId)
                .eq(PracticeRecord::getCorrect, 0)
                .eq(PracticeRecord::getMastered, 0)
                .orderByDesc(PracticeRecord::getCreateTime));

        List<ErrorBookVO> items = new ArrayList<>();
        Map<Long, String> courseNameCache = new HashMap<>();
        Map<Long, String> chapterNameCache = new HashMap<>();
        for (PracticeRecord r : records) {
            Question question = questionService.getRequired(r.getQuestionId());
            if (courseId != null && !courseId.equals(question.getCourseId())) {
                continue;
            }
            ErrorBookVO vo = new ErrorBookVO();
            vo.setRecordId(r.getId());
            vo.setQuestionId(question.getId());
            vo.setType(question.getType());
            vo.setContent(question.getContent());
            vo.setOptions(questionService.parseOptions(question.getOptions()));
            vo.setAnswer(question.getAnswer());
            vo.setAnalysis(question.getAnalysis());
            vo.setStudentAnswer(r.getStudentAnswer());
            vo.setCreateTime(r.getCreateTime());
            vo.setCourseTitle(courseNameCache.computeIfAbsent(question.getCourseId(), id -> {
                Course c = courseMapper.selectById(id);
                return c != null ? c.getTitle() : "";
            }));
            vo.setChapterTitle(chapterNameCache.computeIfAbsent(question.getChapterId(), id -> {
                Chapter c = chapterMapper.selectById(id);
                return c != null ? c.getTitle() : "";
            }));
            items.add(vo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", items.size());
        return result;
    }

    /**
     * 标记错题已掌握：答对一次或手动标记后从错题本移除
     */
    public void markMastered(Long recordId) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        PracticeRecord record = practiceRecordMapper.selectById(recordId);
        if (record == null || !record.getStudentId().equals(UserContext.userId())) {
            throw new BizException("记录不存在");
        }
        record.setMastered(1);
        practiceRecordMapper.updateById(record);
    }

    /**
     * 客观题判分：单选/判断去空格比对；多选答案排序后比对（兼容 "AB"/"A,B" 写法）
     */
    private boolean judge(Question question, String studentAnswer) {
        String expected = question.getAnswer().trim();
        String actual = studentAnswer.trim();
        if (question.getType() == QuestionService.TYPE_MULTI) {
            return normalizeMulti(expected).equals(normalizeMulti(actual));
        }
        return expected.equalsIgnoreCase(actual);
    }

    /** 多选答案归一化：去掉分隔符、转大写、排序 */
    private String normalizeMulti(String answer) {
        return answer.replaceAll("[,，、\\s]", "").toUpperCase().chars()
                .sorted()
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
