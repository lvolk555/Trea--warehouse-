package com.ailearning.module.exam.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目视图：附带课程/章节名称，便于前端展示归属
 */
@Data
public class QuestionVO {

    private Long id;

    private Long courseId;

    private Long chapterId;

    /** 课程名称 */
    private String courseTitle;

    /** 章节名称 */
    private String chapterTitle;

    /** 题型：1单选 2多选 3判断 4简答 */
    private Integer type;

    private String content;

    /** 选项列表（客观题） */
    private List<String> options;

    private String answer;

    private String analysis;

    /** 来源：1人工录入 2AI生成 */
    private Integer source;

    private LocalDateTime createTime;
}
