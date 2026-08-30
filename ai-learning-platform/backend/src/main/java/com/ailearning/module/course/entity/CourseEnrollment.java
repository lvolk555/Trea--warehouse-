package com.ailearning.module.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 选课实体：学生与课程的多对多关系，携带完成度
 */
@Data
@TableName("course_enrollment")
public class CourseEnrollment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    /** 完成度百分比（0-100） */
    private BigDecimal progress;

    private LocalDateTime createTime;
}
