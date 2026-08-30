package com.ailearning.module.stats.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 统计聚合 Mapper：三端看板的 GROUP BY 查询
 * 说明：统计类查询涉及跨表聚合与按日期分组，LambdaQueryWrapper 表达力不足，
 * 故使用 @Select 原生 SQL；所有查询均带时间或数量限制，避免全表扫描拖慢看板。
 */
@Mapper
public interface StatsMapper {

    // ---------- 学生看板 ----------

    /** 学生累计学习时长（秒）：所有视频播放位置之和 */
    @Select("SELECT COALESCE(SUM(position), 0) FROM learning_record WHERE student_id = #{studentId}")
    long studentStudySeconds(@Param("studentId") long studentId);

    /** 学生积分每日净变动（最近 N 天）：按天分组 */
    @Select("SELECT DATE(create_time) AS day, SUM(change_value) AS delta " +
            "FROM points_record WHERE user_id = #{userId} AND create_time >= #{since} " +
            "GROUP BY DATE(create_time) ORDER BY day")
    List<Map<String, Object>> pointsDaily(@Param("userId") long userId, @Param("since") String since);

    // ---------- 教师看板 ----------

    /** 教师课程选课人数（按课程分组） */
    @Select("SELECT e.course_id AS courseId, COUNT(*) AS cnt " +
            "FROM course_enrollment e JOIN course c ON e.course_id = c.id " +
            "WHERE c.teacher_id = #{teacherId} GROUP BY e.course_id")
    List<Map<String, Object>> enrollmentCountByCourse(@Param("teacherId") long teacherId);

    /** 各章节下每个学生已完成的视频数（用于计算章节完课率：完成全部视频的学生占比） */
    @Select("<script>" +
            "SELECT v.chapter_id AS chapterId, lr.student_id AS studentId, COUNT(DISTINCT lr.video_id) AS cnt " +
            "FROM learning_record lr JOIN video v ON lr.video_id = v.id " +
            "WHERE lr.finished = 1 AND v.chapter_id IN " +
            "<foreach collection='chapterIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY v.chapter_id, lr.student_id" +
            "</script>")
    List<Map<String, Object>> chapterStudentFinished(@Param("chapterIds") List<Long> chapterIds);

    /** 易错题 TOP：练习 + 考试作答的错题数合并统计 */
    @Select("SELECT question_id AS questionId, SUM(wrong) AS wrongCount FROM (" +
            "  SELECT question_id, COUNT(*) AS wrong FROM practice_record WHERE correct = 0 GROUP BY question_id " +
            "  UNION ALL " +
            "  SELECT question_id, COUNT(*) AS wrong FROM exam_answer WHERE correct = 0 GROUP BY question_id" +
            ") t GROUP BY question_id ORDER BY wrongCount DESC LIMIT #{limit}")
    List<Map<String, Object>> topWrongQuestions(@Param("limit") int limit);

    // ---------- 管理看板 ----------

    /** 用户增长：最近 N 天每日新增用户数 */
    @Select("SELECT DATE(create_time) AS day, COUNT(*) AS cnt " +
            "FROM user WHERE create_time >= #{since} GROUP BY DATE(create_time) ORDER BY day")
    List<Map<String, Object>> userGrowthDaily(@Param("since") String since);

    /** 课程热度：选课数 TOP */
    @Select("SELECT e.course_id AS courseId, COUNT(*) AS cnt " +
            "FROM course_enrollment e GROUP BY e.course_id ORDER BY cnt DESC LIMIT #{limit}")
    List<Map<String, Object>> topCourses(@Param("limit") int limit);

    /** AI 调用每日趋势：最近 N 天 assistant 消息数（≈大模型调用次数） */
    @Select("SELECT DATE(create_time) AS day, COUNT(*) AS cnt " +
            "FROM ai_chat_message WHERE role = 'assistant' AND create_time >= #{since} " +
            "GROUP BY DATE(create_time) ORDER BY day")
    List<Map<String, Object>> aiCallDaily(@Param("since") String since);

    /** 积分发放/消耗统计：按类型汇总 */
    @Select("SELECT type, " +
            "COALESCE(SUM(CASE WHEN change_value > 0 THEN change_value ELSE 0 END), 0) AS earned, " +
            "COALESCE(SUM(CASE WHEN change_value < 0 THEN -change_value ELSE 0 END), 0) AS spent " +
            "FROM points_record GROUP BY type")
    List<Map<String, Object>> pointsByType();
}
