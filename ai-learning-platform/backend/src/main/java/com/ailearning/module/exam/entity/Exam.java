package com.ailearning.module.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 试卷实体：绑定课程，题目以 ID 列表 JSON 存储
 */
@Data
@TableName("exam")
public class Exam {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private String title;

    /** 考试时长（分钟） */
    private Integer duration;

    /** 题目 ID 列表 JSON 字符串，如 [1,2,3] */
    private String questionIds;

    /** 0草稿 1已发布 */
    private Integer status;

    private LocalDateTime createTime;
}
