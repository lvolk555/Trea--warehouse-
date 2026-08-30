package com.ailearning.module.exam.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 错题本条目：错题 + 归属课程/章节 + 最近作答信息
 */
@Data
public class ErrorBookVO {

    private Long recordId;

    private Long questionId;

    /** 课程名称（按课程归类展示） */
    private String courseTitle;

    /** 章节名称 */
    private String chapterTitle;

    private Integer type;

    private String content;

    private List<String> options;

    private String answer;

    private String analysis;

    /** 学生最近一次错误答案 */
    private String studentAnswer;

    /** 最近作答时间 */
    private LocalDateTime createTime;
}
