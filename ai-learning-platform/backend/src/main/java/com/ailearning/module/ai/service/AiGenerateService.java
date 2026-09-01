package com.ailearning.module.ai.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.ai.dto.AiArticleDTO;
import com.ailearning.module.ai.dto.AiGenerateDTO;
import com.ailearning.module.ai.dto.AiGradeVO;
import com.ailearning.module.ai.dto.AiQuestionSaveDTO;
import com.ailearning.module.course.entity.Chapter;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.mapper.ChapterMapper;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.exam.entity.Exam;
import com.ailearning.module.exam.entity.ExamAnswer;
import com.ailearning.module.exam.entity.ExamRecord;
import com.ailearning.module.exam.entity.Question;
import com.ailearning.module.exam.mapper.ExamAnswerMapper;
import com.ailearning.module.exam.mapper.ExamMapper;
import com.ailearning.module.exam.mapper.ExamRecordMapper;
import com.ailearning.module.exam.mapper.QuestionMapper;
import com.ailearning.module.exam.service.ExamService;
import com.ailearning.module.points.service.PointsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 出题与批改服务
 * 出题：大模型按 JSON 格式输出 → 解析为题目草稿 → 教师审核后入库（source=2）
 * 批改：简答题由大模型评分（0-10）+ 建议 → 回写答题明细 → 重算考试总分
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerateService {

    private final ZhipuAiClient zhipuAiClient;
    private final ObjectMapper objectMapper;
    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final QuestionMapper questionMapper;
    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final PointsService pointsService;

    /** 来源：2 AI 生成 */
    private static final int SOURCE_AI = 2;

    /**
     * AI 出题：返回题目草稿列表（不入库，待教师审核）
     */
    public List<AiQuestionSaveDTO.Draft> generate(AiGenerateDTO dto) {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        if (!zhipuAiClient.isConfigured()) {
            throw new BizException("AI 服务暂未配置（缺少 ZHIPU_API_KEY）");
        }
        Course course = courseMapper.selectById(dto.getCourseId());
        Chapter chapter = chapterMapper.selectById(dto.getChapterId());
        if (course == null || chapter == null || !dto.getCourseId().equals(chapter.getCourseId())) {
            throw new BizException("课程或章节不存在");
        }

        String typeName = switch (dto.getType()) {
            case 1 -> "单选题（答案为单个大写字母，如 A）";
            case 2 -> "多选题（答案为多个大写字母连写，如 AB）";
            case 3 -> "判断题（答案为 对 或 错）";
            default -> throw new BizException("AI 出题仅支持单选/多选/判断题");
        };

        String systemPrompt = """
                你是一名专业的出题专家，请严格按照用户要求出题。
                输出必须是合法的 JSON 数组，不要输出任何解释、前后缀或 markdown 代码块标记。
                每道题的 JSON 结构为：
                {"type":题型数字,"content":"题干","options":["选项1","选项2","选项3","选项4"],"answer":"正确答案","analysis":"解析"}
                判断题的 options 固定为 ["对","错"]。
                """;
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("请为课程《").append(course.getTitle())
                .append("》的章节「").append(chapter.getTitle()).append("」出 ")
                .append(dto.getCount()).append(" 道").append(typeName).append("。");
        if (dto.getKnowledgePoint() != null && !dto.getKnowledgePoint().isBlank()) {
            userPrompt.append("考察知识点：").append(dto.getKnowledgePoint()).append("。");
        }
        userPrompt.append("题目难度适中，面向初学者，每题必须附带解析。");

        String raw = zhipuAiClient.chat(systemPrompt,
                List.of(Map.of("role", "user", "content", userPrompt.toString()))).block();
        return parseDrafts(raw, dto.getType());
    }

    /**
     * AI 生成教程文章：返回 Markdown 格式的教程内容（教师/管理员可用）
     */
    public String generateArticle(AiArticleDTO dto) {
        UserContext.checkRole(UserContext.ROLE_TEACHER, UserContext.ROLE_ADMIN);
        if (!zhipuAiClient.isConfigured()) {
            throw new BizException("AI 服务暂未配置（缺少 ZHIPU_API_KEY）");
        }
        String systemPrompt = """
                你是一名专业的教程作者，请根据用户提供的主题撰写一篇结构清晰、通俗易懂的教程文章。
                使用 Markdown 格式输出，包含标题、分节标题、列表、代码块等，便于前端渲染。
                直接输出文章正文，不要输出任何解释、前后缀或用代码块包裹整篇文章。
                """;
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("请撰写一篇教程，主题为：「").append(dto.getTitle()).append("」。");
        if (dto.getKeywords() != null && !dto.getKeywords().isBlank()) {
            userPrompt.append("需覆盖的关键词/知识点：").append(dto.getKeywords()).append("。");
        }
        if (dto.getRequirements() != null && !dto.getRequirements().isBlank()) {
            userPrompt.append("补充要求：").append(dto.getRequirements()).append("。");
        }
        userPrompt.append("内容结构完整，面向初学者。");
        return zhipuAiClient.chat(systemPrompt,
                List.of(Map.of("role", "user", "content", userPrompt.toString()))).block();
    }

    /**
     * 审核入库：教师勾选的 AI 题目草稿批量入库（source=2）
     */
    @Transactional(rollbackFor = Exception.class)
    public int saveDrafts(AiQuestionSaveDTO dto) {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        Chapter chapter = chapterMapper.selectById(dto.getChapterId());
        if (chapter == null || !dto.getCourseId().equals(chapter.getCourseId())) {
            throw new BizException("章节不属于所选课程");
        }
        int count = 0;
        for (AiQuestionSaveDTO.Draft draft : dto.getQuestions()) {
            Question question = new Question();
            question.setCourseId(dto.getCourseId());
            question.setChapterId(dto.getChapterId());
            question.setType(draft.getType());
            question.setContent(draft.getContent());
            question.setOptions(toJson(draft.getOptions()));
            question.setAnswer(draft.getAnswer().trim());
            question.setAnalysis(draft.getAnalysis());
            question.setSource(SOURCE_AI);
            questionMapper.insert(question);
            count++;
        }
        return count;
    }

    /**
     * 待批改简答题列表：当前教师课程下所有考试的未批改简答题
     */
    public List<Map<String, Object>> pendingGrades() {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        long teacherId = UserContext.userId();

        // 教师名下课程 → 试卷
        List<Course> myCourses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getTeacherId, teacherId));
        if (myCourses.isEmpty()) {
            return List.of();
        }
        List<Exam> exams = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .in(Exam::getCourseId, myCourses.stream().map(Course::getId).toList()));
        if (exams.isEmpty()) {
            return List.of();
        }
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .in(ExamRecord::getExamId, exams.stream().map(Exam::getId).toList()));
        if (records.isEmpty()) {
            return List.of();
        }

        // 未批改的简答题作答：correct=0 且 aiScore 为空
        List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .in(ExamAnswer::getRecordId, records.stream().map(ExamRecord::getId).toList())
                .isNull(ExamAnswer::getAiScore));
        List<Map<String, Object>> result = new ArrayList<>();
        Map<Long, Exam> examMap = new HashMap<>();
        exams.forEach(e -> examMap.put(e.getId(), e));
        for (ExamAnswer answer : answers) {
            Question question = questionMapper.selectById(answer.getQuestionId());
            if (question == null || question.getType() != 4) {
                continue; // 仅简答题需要 AI 批改
            }
            ExamRecord record = records.stream()
                    .filter(r -> r.getId().equals(answer.getRecordId())).findFirst().orElse(null);
            Exam exam = record != null ? examMap.get(record.getExamId()) : null;
            Map<String, Object> item = new HashMap<>();
            item.put("answerId", answer.getId());
            item.put("recordId", answer.getRecordId());
            item.put("examTitle", exam != null ? exam.getTitle() : "");
            item.put("questionContent", question.getContent());
            item.put("referenceAnswer", question.getAnswer());
            item.put("studentAnswer", answer.getStudentAnswer());
            item.put("studentId", record != null ? record.getStudentId() : null);
            result.add(item);
        }
        return result;
    }

    /**
     * AI 批改单道简答题：评分 0-10 + 建议，回写并重算考试总分
     */
    @Transactional(rollbackFor = Exception.class)
    public AiGradeVO grade(Long answerId) {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        ExamAnswer answer = examAnswerMapper.selectById(answerId);
        if (answer == null) {
            throw new BizException("答题记录不存在");
        }
        Question question = questionMapper.selectById(answer.getQuestionId());
        if (question == null || question.getType() != 4) {
            throw new BizException("仅简答题支持 AI 批改");
        }
        if (!zhipuAiClient.isConfigured()) {
            throw new BizException("AI 服务暂未配置（缺少 ZHIPU_API_KEY）");
        }

        String systemPrompt = """
                你是一名严谨的阅卷老师，请对学生的简答题答案进行评分。
                评分范围 0 到 10 分（可为小数），并给出简短批改建议。
                输出必须是合法 JSON，不要输出任何解释或 markdown 标记，格式为：
                {"score":8.5,"comment":"批改建议"}
                """;
        String userPrompt = "题目：" + question.getContent()
                + "\n参考答案：" + question.getAnswer()
                + "\n学生答案：" + (answer.getStudentAnswer() != null ? answer.getStudentAnswer() : "（未作答）")
                + "\n请评分并给出建议。";

        String raw = zhipuAiClient.chat(systemPrompt,
                List.of(Map.of("role", "user", "content", userPrompt))).block();
        GradeResult parsed = parseGrade(raw);

        // 回写批改结果
        answer.setAiScore(parsed.score);
        answer.setAiComment(parsed.comment);
        examAnswerMapper.updateById(answer);

        // 重算考试总分：(客观题答对数 + Σ主观题得分/10) / 总题数 × 100
        BigDecimal newTotal = recalcRecordScore(answer.getRecordId());

        AiGradeVO vo = new AiGradeVO();
        vo.setAnswerId(answerId);
        vo.setAiScore(parsed.score);
        vo.setAiComment(parsed.comment);
        vo.setExamScore(newTotal);
        vo.setReferenceAnswer(question.getAnswer());
        vo.setStudentAnswer(answer.getStudentAnswer());
        return vo;
    }

    /** 教师采纳/改分：手动修正 AI 评分并重算总分 */
    @Transactional(rollbackFor = Exception.class)
    public AiGradeVO adjustScore(Long answerId, BigDecimal score, String comment) {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        ExamAnswer answer = examAnswerMapper.selectById(answerId);
        if (answer == null) {
            throw new BizException("答题记录不存在");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.TEN) > 0) {
            throw new BizException("评分必须在 0-10 之间");
        }
        answer.setAiScore(score);
        if (comment != null) {
            answer.setAiComment(comment);
        }
        examAnswerMapper.updateById(answer);
        BigDecimal newTotal = recalcRecordScore(answer.getRecordId());

        AiGradeVO vo = new AiGradeVO();
        vo.setAnswerId(answerId);
        vo.setAiScore(score);
        vo.setAiComment(answer.getAiComment());
        vo.setExamScore(newTotal);
        return vo;
    }

    /**
     * 重算考试记录总分：客观题按对错计 1/0，主观题按 aiScore/10 折算
     */
    private BigDecimal recalcRecordScore(Long recordId) {
        List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId));
        if (answers.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double earned = 0;
        for (ExamAnswer a : answers) {
            Question q = questionMapper.selectById(a.getQuestionId());
            if (q == null) {
                continue;
            }
            if (q.getType() == 4) {
                // 主观题：AI 分折算（未批改按 0）
                earned += a.getAiScore() != null ? a.getAiScore().doubleValue() / 10.0 : 0;
            } else {
                earned += a.getCorrect() != null && a.getCorrect() == 1 ? 1 : 0;
            }
        }
        BigDecimal score = BigDecimal.valueOf(earned * 100.0 / answers.size())
                .setScale(1, RoundingMode.HALF_UP);
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record != null) {
            record.setScore(score);
            examRecordMapper.updateById(record);
            // AI 批改后分数可能跨过及格线：补发及格奖励（按 exam#id 幂等，已发过则跳过）
            if (score.compareTo(BigDecimal.valueOf(ExamService.PASS_SCORE)) >= 0) {
                pointsService.grantOnceByRule(record.getStudentId(), "exam_pass", "exam#" + record.getExamId(),
                        "考试及格奖励（exam#" + record.getExamId() + "）");
            }
        }
        return score;
    }

    /** 解析大模型返回的题目 JSON 数组 */
    private List<AiQuestionSaveDTO.Draft> parseDrafts(String raw, Integer type) {
        try {
            JsonNode node = objectMapper.readTree(stripCodeFence(raw));
            if (!node.isArray()) {
                throw new BizException("AI 返回格式异常");
            }
            List<AiQuestionSaveDTO.Draft> drafts = new ArrayList<>();
            for (JsonNode item : node) {
                AiQuestionSaveDTO.Draft draft = new AiQuestionSaveDTO.Draft();
                draft.setType(type);
                draft.setContent(item.path("content").asText(""));
                draft.setAnswer(item.path("answer").asText(""));
                draft.setAnalysis(item.path("analysis").asText(""));
                List<String> options = objectMapper.convertValue(
                        item.path("options"), new TypeReference<List<String>>() {});
                draft.setOptions(options);
                if (!draft.getContent().isBlank() && !draft.getAnswer().isBlank()) {
                    drafts.add(draft);
                }
            }
            if (drafts.isEmpty()) {
                throw new BizException("AI 未生成有效题目，请重试");
            }
            return drafts;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析 AI 出题结果失败: {}", raw, e);
            throw new BizException("AI 返回格式异常，请重试");
        }
    }

    /** 解析批改结果 JSON */
    private GradeResult parseGrade(String raw) {
        try {
            JsonNode node = objectMapper.readTree(stripCodeFence(raw));
            GradeResult result = new GradeResult();
            double score = node.path("score").asDouble(-1);
            if (score < 0 || score > 10) {
                throw new BizException("AI 评分超出范围");
            }
            result.score = BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP);
            result.comment = node.path("comment").asText("");
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析 AI 批改结果失败: {}", raw, e);
            throw new BizException("AI 返回格式异常，请重试");
        }
    }

    /** 去除大模型可能输出的 markdown 代码块标记 */
    private String stripCodeFence(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```[a-zA-Z]*", "").replaceFirst("```$", "").trim();
        }
        return s;
    }

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

    /** 批改解析结果内部载体 */
    private static class GradeResult {
        BigDecimal score;
        String comment;
    }
}
