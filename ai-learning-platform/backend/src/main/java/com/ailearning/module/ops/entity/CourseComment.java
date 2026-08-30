package com.ailearning.module.ops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程评论实体：状态 0待审核 1已展示 2已隐藏
 */
@Data
@TableName("course_comment")
public class CourseComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long courseId;

    private String content;

    /** 0待审核 1已展示 2已隐藏 */
    private Integer status;

    private LocalDateTime createTime;
}
