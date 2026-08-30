package com.ailearning.module.exam.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷视图：附带课程名称与题目详情
 */
@Data
public class ExamVO {

    private Long id;

    private Long courseId;

    /** 课程名称 */
    private String courseTitle;

    private String title;

    /** 考试时长（分钟） */
    private Integer duration;

    /** 题目 ID 列表 */
    private List<Long> questionIds;

    /** 题目详情（学生考试页/教师预览时返回） */
    private List<QuestionVO> questions;

    /** 0草稿 1已发布 */
    private Integer status;

    private LocalDateTime createTime;
}
