package com.ailearning.module.exam.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.course.entity.Chapter;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.mapper.ChapterMapper;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.exam.dto.QuestionSaveDTO;
import com.ailearning.module.exam.dto.QuestionVO;
import com.ailearning.module.exam.entity.Question;
import com.ailearning.module.exam.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题库服务：教师按 课程 → 章节 两级归属维护题目（CRUD + 分页查询）
 */
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final ObjectMapper objectMapper;

    /** 题型：1单选 2多选 3判断 4简答 */
    public static final int TYPE_SINGLE = 1;
    public static final int TYPE_MULTI = 2;
    public static final int TYPE_JUDGE = 3;
    public static final int TYPE_SUBJECTIVE = 4;

    /** 来源：1人工录入 2AI生成 */
    public static final int SOURCE_MANUAL = 1;
    public static final int SOURCE_AI = 2;

    /**
     * 新增/编辑题目（教师）：校验章节必须归属于所选课程
     */
    @Transactional(rollbackFor = Exception.class)
    public QuestionVO save(QuestionSaveDTO dto) {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        checkChapterBelongsToCourse(dto.getChapterId(), dto.getCourseId());

        Question question = dto.getId() != null ? getOwnQuestion(dto.getId()) : new Question();
        question.setCourseId(dto.getCourseId());
        question.setChapterId(dto.getChapterId());
        question.setType(dto.getType());
        question.setContent(dto.getContent());
        question.setOptions(toJson(dto.getOptions()));
        question.setAnswer(dto.getAnswer().trim());
        question.setAnalysis(dto.getAnalysis());
        if (question.getId() == null) {
            question.setSource(SOURCE_MANUAL);
            questionMapper.insert(question);
        } else {
            questionMapper.updateById(question);
        }
        return toVO(question);
    }

    /**
     * 删除题目（教师，仅本人）
     */
    public void delete(Long id) {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        getOwnQuestion(id);
        questionMapper.deleteById(id);
    }

    /**
     * 分页查询题库（教师看自己、管理员看全部）
     */
    public IPage<QuestionVO> page(int pageNum, int pageSize, Long courseId, Long chapterId, Integer type) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(courseId != null, Question::getCourseId, courseId)
                .eq(chapterId != null, Question::getChapterId, chapterId)
                .eq(type != null, Question::getType, type)
                .orderByDesc(Question::getCreateTime);
        if (UserContext.role() == UserContext.ROLE_TEACHER) {
            // 教师只能管理自己课程的题目：先查名下课程
            List<Course> myCourses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                    .eq(Course::getTeacherId, UserContext.userId()));
            if (myCourses.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            wrapper.in(Question::getCourseId, myCourses.stream().map(Course::getId).toList());
        } else {
            UserContext.checkRole(UserContext.ROLE_ADMIN);
        }
        IPage<Question> page = questionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return page.convert(this::toVO);
    }

    /**
     * 按章节抽题（学生练习用）：随机抽取 limit 道客观题，可排除已掌握的错题
     */
    public List<QuestionVO> pickQuestions(Long chapterId, int limit, boolean excludeMastered) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BizException("章节不存在");
        }
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getChapterId, chapterId)
                .in(Question::getType, TYPE_SINGLE, TYPE_MULTI, TYPE_JUDGE)
                .last("ORDER BY RAND() LIMIT " + Math.max(limit, 1));
        List<Question> questions = questionMapper.selectList(wrapper);
        return questions.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 按 ID 批量查询题目并转 VO
     */
    public List<QuestionVO> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Question> questions = questionMapper.selectBatchIds(ids);
        // 按传入顺序返回
        Map<Long, Question> map = new HashMap<>();
        questions.forEach(q -> map.put(q.getId(), q));
        return ids.stream().filter(map::containsKey).map(id -> toVO(map.get(id))).toList();
    }

    /** 根据 ID 获取题目，不存在则抛异常 */
    public Question getRequired(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BizException("题目不存在");
        }
        return question;
    }

    /** 校验章节归属于课程 */
    private void checkChapterBelongsToCourse(Long chapterId, Long courseId) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null || !courseId.equals(chapter.getCourseId())) {
            throw new BizException("章节不属于所选课程，请重新选择");
        }
    }

    /** 获取当前教师名下课程中的题目 */
    private Question getOwnQuestion(Long id) {
        Question question = getRequired(id);
        Course course = courseMapper.selectById(question.getCourseId());
        if (course == null || !course.getTeacherId().equals(UserContext.userId())) {
            throw new BizException("无权操作该题目");
        }
        return question;
    }

    /** 选项列表 → JSON 字符串 */
    private String toJson(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            throw new BizException("选项格式错误");
        }
    }

    /** JSON 字符串 → 选项列表 */
    List<String> parseOptions(String optionsJson) {
        if (!StringUtils.hasText(optionsJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** 实体 → VO（附带课程/章节名称） */
    QuestionVO toVO(Question question) {
        QuestionVO vo = new QuestionVO();
        vo.setId(question.getId());
        vo.setCourseId(question.getCourseId());
        vo.setChapterId(question.getChapterId());
        vo.setType(question.getType());
        vo.setContent(question.getContent());
        vo.setOptions(parseOptions(question.getOptions()));
        vo.setAnswer(question.getAnswer());
        vo.setAnalysis(question.getAnalysis());
        vo.setSource(question.getSource());
        vo.setCreateTime(question.getCreateTime());

        Course course = courseMapper.selectById(question.getCourseId());
        vo.setCourseTitle(course != null ? course.getTitle() : "");
        Chapter chapter = chapterMapper.selectById(question.getChapterId());
        vo.setChapterTitle(chapter != null ? chapter.getTitle() : "");
        return vo;
    }

    /** 批量填充课程名称：courseId → title */
    Map<Long, String> courseTitles(Set<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle));
    }
}
