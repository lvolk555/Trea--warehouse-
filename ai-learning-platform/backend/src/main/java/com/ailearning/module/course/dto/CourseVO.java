package com.ailearning.module.course.dto;

import com.ailearning.module.course.entity.Chapter;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.entity.Video;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 课程详情 VO：课程信息 + 章节视频树 + 教师昵称 + 学习进度
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseVO extends Course {

    /** 授课教师昵称 */
    private String teacherName;

    /** 视频总数 */
    private Integer videoCount;

    /** 当前学生是否已选课 */
    private Boolean enrolled;

    /** 当前学生完成度（已选课时返回） */
    private BigDecimal progress;

    /** 章节与视频结构 */
    private List<ChapterVO> chapters;

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ChapterVO extends Chapter {
        private List<VideoVO> videos;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class VideoVO extends Video {
        /** 当前学生是否已完成该视频 */
        private Boolean finished;
    }
}
