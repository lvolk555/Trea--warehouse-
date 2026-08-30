package com.ailearning.module.stats.service;

import com.ailearning.common.UserContext;
import com.ailearning.module.course.entity.Chapter;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.entity.CourseEnrollment;
import com.ailearning.module.course.entity.Video;
import com.ailearning.module.course.mapper.ChapterMapper;
import com.ailearning.module.course.mapper.CourseEnrollmentMapper;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.course.mapper.VideoMapper;
import com.ailearning.module.exam.entity.ExamRecord;
import com.ailearning.module.exam.entity.PracticeRecord;
import com.ailearning.module.exam.entity.Question;
import com.ailearning.module.exam.mapper.ExamRecordMapper;
import com.ailearning.module.exam.mapper.PracticeRecordMapper;
import com.ailearning.module.exam.mapper.QuestionMapper;
import com.ailearning.module.points.entity.PointsAccount;
import com.ailearning.module.points.mapper.PointsAccountMapper;
import com.ailearning.module.stats.mapper.StatsMapper;
import com.ailearning.module.study.entity.LearningRecord;
import com.ailearning.module.study.mapper.LearningRecordMapper;
import com.ailearning.module.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计服务：为学生/教师/管理员三端看板组装统计数据
 *
 * 设计要点：
 * 1. 趋势类数据（积分/用户增长/AI 调用）在后端补齐缺失日期为 0，前端折线图无需处理空洞；
 * 2. 聚合查询下沉到 StatsMapper 原生 SQL，服务层只做组装与补充维度信息（课程名、题干等）；
 * 3. 所有接口按角色鉴权，教师只能看自己课程的统计。
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final VideoMapper videoMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final ExamRecordMapper examRecordMapper;
    private final QuestionMapper questionMapper;
    private final PointsAccountMapper pointsAccountMapper;

    /** 趋势图回看天数 */
    private static final int TREND_DAYS = 14;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== 学生看板 ====================

    /**
     * 学生看板：学习概况 + 积分趋势 + 练习/考试概况
     */
    public Map<String, Object> studentDashboard() {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        long studentId = UserContext.userId();
        Map<String, Object> result = new HashMap<>();

        // 学习概况
        List<CourseEnrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<CourseEnrollment>().eq(CourseEnrollment::getStudentId, studentId));
        result.put("courseCount", enrollments.size());
        BigDecimal avgProgress = enrollments.stream()
                .map(e -> e.getProgress() != null ? e.getProgress() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("avgProgress", enrollments.isEmpty() ? BigDecimal.ZERO
                : avgProgress.divide(BigDecimal.valueOf(enrollments.size()), 1, RoundingMode.HALF_UP));

        List<LearningRecord> learningRecords = learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecord>().eq(LearningRecord::getStudentId, studentId));
        long finishedVideos = learningRecords.stream().filter(r -> r.getFinished() == 1).count();
        result.put("finishedVideos", finishedVideos);
        result.put("studySeconds", statsMapper.studentStudySeconds(studentId));

        // 积分概况与趋势
        PointsAccount account = pointsAccountMapper.selectOne(new LambdaQueryWrapper<PointsAccount>()
                .eq(PointsAccount::getUserId, studentId).last("LIMIT 1"));
        result.put("pointsBalance", account != null ? account.getBalance() : 0);
        result.put("pointsTrend", fillTrend(statsMapper.pointsDaily(studentId, sinceDate()), "delta"));

        // 练习概况
        List<PracticeRecord> practices = practiceRecordMapper.selectList(
                new LambdaQueryWrapper<PracticeRecord>().eq(PracticeRecord::getStudentId, studentId));
        long practiceCorrect = practices.stream().filter(p -> p.getCorrect() == 1).count();
        result.put("practiceTotal", practices.size());
        result.put("practiceAccuracy", practices.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(practiceCorrect * 100.0 / practices.size()).setScale(1, RoundingMode.HALF_UP));

        // 考试概况
        List<ExamRecord> examRecords = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStudentId, studentId)
                .isNotNull(ExamRecord::getSubmitTime));
        result.put("examCount", examRecords.size());
        BigDecimal scoreSum = examRecords.stream()
                .map(r -> r.getScore() != null ? r.getScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("examAvgScore", examRecords.isEmpty() ? BigDecimal.ZERO
                : scoreSum.divide(BigDecimal.valueOf(examRecords.size()), 1, RoundingMode.HALF_UP));

        return result;
    }

    // ==================== 教师看板 ====================

    /**
     * 教师看板：课程概况 + 各课程选课人数 + 章节完课率 + 易错题 TOP
     */
    public Map<String, Object> teacherDashboard() {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        long teacherId = UserContext.userId();
        Map<String, Object> result = new HashMap<>();

        List<Course> myCourses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getTeacherId, teacherId));
        result.put("courseCount", myCourses.size());
        if (myCourses.isEmpty()) {
            result.put("totalStudents", 0);
            result.put("enrollmentByCourse", List.of());
            result.put("courseProgress", List.of());
            result.put("chapterCompletion", List.of());
            result.put("topWrongQuestions", List.of());
            return result;
        }
        List<Long> courseIds = myCourses.stream().map(Course::getId).toList();
        Map<Long, String> courseTitleMap = new HashMap<>();
        myCourses.forEach(c -> courseTitleMap.put(c.getId(), c.getTitle()));

        // 选课人数（总 + 按课程）
        List<CourseEnrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<CourseEnrollment>().in(CourseEnrollment::getCourseId, courseIds));
        result.put("totalStudents", enrollments.size());

        Map<Long, Long> countByCourse = new HashMap<>();
        Map<Long, List<BigDecimal>> progressByCourse = new HashMap<>();
        for (CourseEnrollment e : enrollments) {
            countByCourse.merge(e.getCourseId(), 1L, Long::sum);
            progressByCourse.computeIfAbsent(e.getCourseId(), k -> new ArrayList<>())
                    .add(e.getProgress() != null ? e.getProgress() : BigDecimal.ZERO);
        }
        List<Map<String, Object>> enrollmentByCourse = new ArrayList<>();
        List<Map<String, Object>> courseProgress = new ArrayList<>();
        for (Long courseId : courseIds) {
            String title = courseTitleMap.get(courseId);
            Map<String, Object> item = new HashMap<>();
            item.put("courseId", courseId);
            item.put("title", title);
            item.put("count", countByCourse.getOrDefault(courseId, 0L));
            enrollmentByCourse.add(item);

            List<BigDecimal> progresses = progressByCourse.getOrDefault(courseId, List.of());
            BigDecimal avg = progresses.isEmpty() ? BigDecimal.ZERO
                    : progresses.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(progresses.size()), 1, RoundingMode.HALF_UP);
            Map<String, Object> p = new HashMap<>();
            p.put("courseId", courseId);
            p.put("title", title);
            p.put("avgProgress", avg);
            courseProgress.add(p);
        }
        result.put("enrollmentByCourse", enrollmentByCourse);
        result.put("courseProgress", courseProgress);

        // 章节完课率：完成章节内全部视频的学生数 / 课程选课人数
        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .in(Chapter::getCourseId, courseIds)
                .orderByAsc(Chapter::getCourseId).orderByAsc(Chapter::getSortOrder));
        List<Map<String, Object>> chapterCompletion = new ArrayList<>();
        if (!chapters.isEmpty()) {
            List<Long> chapterIds = chapters.stream().map(Chapter::getId).toList();
            List<Video> videos = videoMapper.selectList(
                    new LambdaQueryWrapper<Video>().in(Video::getChapterId, chapterIds));
            // 每章节视频总数
            Map<Long, Long> videoCountByChapter = new HashMap<>();
            for (Video v : videos) {
                videoCountByChapter.merge(v.getChapterId(), 1L, Long::sum);
            }
            // 每章节每个学生已完成视频数
            Map<Long, Map<Long, Long>> finishedByChapter = new HashMap<>();
            for (Map<String, Object> row : statsMapper.chapterStudentFinished(chapterIds)) {
                Long chapterId = ((Number) row.get("chapterId")).longValue();
                Long studentId = ((Number) row.get("studentId")).longValue();
                Long cnt = ((Number) row.get("cnt")).longValue();
                finishedByChapter.computeIfAbsent(chapterId, k -> new HashMap<>()).put(studentId, cnt);
            }
            for (Chapter chapter : chapters) {
                long videoCount = videoCountByChapter.getOrDefault(chapter.getId(), 0L);
                long enrolled = countByCourse.getOrDefault(chapter.getCourseId(), 0L);
                if (videoCount == 0 || enrolled == 0) {
                    continue;
                }
                long completedStudents = finishedByChapter.getOrDefault(chapter.getId(), Map.of())
                        .values().stream().filter(cnt -> cnt >= videoCount).count();
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("chapterId", chapter.getId());
                item.put("title", chapter.getTitle());
                item.put("courseTitle", courseTitleMap.get(chapter.getCourseId()));
                item.put("completionRate", BigDecimal.valueOf(completedStudents * 100.0 / enrolled)
                        .setScale(1, RoundingMode.HALF_UP));
                chapterCompletion.add(item);
            }
        }
        result.put("chapterCompletion", chapterCompletion);

        // 易错题 TOP10（限定本人课程的题目）
        List<Map<String, Object>> topWrong = statsMapper.topWrongQuestions(50);
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : topWrong) {
            Long questionId = ((Number) row.get("questionId")).longValue();
            Question q = questionMapper.selectById(questionId);
            if (q == null || !courseIds.contains(q.getCourseId())) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("questionId", questionId);
            item.put("content", q.getContent());
            item.put("courseTitle", courseTitleMap.get(q.getCourseId()));
            item.put("wrongCount", row.get("wrongCount"));
            filtered.add(item);
            if (filtered.size() >= 10) {
                break;
            }
        }
        result.put("topWrongQuestions", filtered);
        return result;
    }

    // ==================== 管理看板 ====================

    /**
     * 管理看板：总量指标 + 用户增长趋势 + 课程热度 + AI 调用趋势 + 积分统计
     */
    public Map<String, Object> adminDashboard() {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        Map<String, Object> result = new HashMap<>();

        // 总量指标
        result.put("userTotal", userMapper.selectCount(null));
        result.put("studentTotal", userMapper.selectCount(new LambdaQueryWrapper<com.ailearning.module.user.entity.User>()
                .eq(com.ailearning.module.user.entity.User::getRole, 1)));
        result.put("courseTotal", courseMapper.selectCount(null));
        result.put("enrollmentTotal", enrollmentMapper.selectCount(null));
        result.put("questionTotal", questionMapper.selectCount(null));

        String since = sinceDate();
        // 用户增长趋势（补零）
        result.put("userGrowth", fillTrend(statsMapper.userGrowthDaily(since), "cnt"));
        // AI 调用趋势（补零）
        result.put("aiCallTrend", fillTrend(statsMapper.aiCallDaily(since), "cnt"));

        // 课程热度 TOP8（补充课程名，统一字段名为 count 供前端使用）
        List<Map<String, Object>> topCourses = statsMapper.topCourses(8);
        for (Map<String, Object> row : topCourses) {
            Course course = courseMapper.selectById(((Number) row.get("courseId")).longValue());
            row.put("title", course != null ? course.getTitle() : "课程#" + row.get("courseId"));
            row.put("count", row.get("cnt"));
        }
        result.put("topCourses", topCourses);

        // 积分发放/消耗（按类型）
        Map<Integer, String> typeNames = Map.of(
                1, "完课", 2, "签到", 3, "考试", 4, "AI提问", 5, "兑换扣减", 6, "注册赠送");
        List<Map<String, Object>> pointsStats = statsMapper.pointsByType();
        for (Map<String, Object> row : pointsStats) {
            int type = ((Number) row.get("type")).intValue();
            row.put("typeName", typeNames.getOrDefault(type, "其他"));
        }
        result.put("pointsStats", pointsStats);
        return result;
    }

    // ==================== 工具方法 ====================

    /** 趋势起点日期（TREND_DAYS 天前的 00:00） */
    private String sinceDate() {
        return LocalDate.now().minusDays(TREND_DAYS - 1L).atStartOfDay().format(DAY_FMT) + " 00:00:00";
    }

    /**
     * 趋势补零：把按天聚合的结果补齐为连续 TREND_DAYS 天，缺失日期 value=0
     *
     * @param rows      SQL 结果（含 day 与 value 列）
     * @param valueKey  数值列名（delta / cnt）
     */
    private List<Map<String, Object>> fillTrend(List<Map<String, Object>> rows, String valueKey) {
        Map<String, Object> byDay = new HashMap<>();
        for (Map<String, Object> row : rows) {
            byDay.put(String.valueOf(row.get("day")), row.get(valueKey));
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(TREND_DAYS - 1L);
        for (int i = 0; i < TREND_DAYS; i++) {
            String day = start.plusDays(i).format(DAY_FMT);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", day.substring(5)); // MM-DD 更紧凑
            item.put("value", byDay.getOrDefault(day, 0));
            trend.add(item);
        }
        return trend;
    }
}
