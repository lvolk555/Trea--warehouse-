package com.ailearning.module.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目实体：强制绑定课程 + 章节（课程 → 章节 两级归属）
 * 题型：1单选 2多选 3判断 4简答；来源：1人工录入 2AI生成
 */
@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long chapterId;

    private Integer type;

    private String content;

    /** 选项 JSON 字符串（客观题），如 ["A选项","B选项"] */
    private String options;

    private String answer;

    private String analysis;

    /** 来源：1人工录入 2AI生成 */
    private Integer source;

    private LocalDateTime createTime;
}
