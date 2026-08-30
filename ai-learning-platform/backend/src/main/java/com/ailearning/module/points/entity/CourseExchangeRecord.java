package com.ailearning.module.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程兑换记录实体
 */
@Data
@TableName("course_exchange_record")
public class CourseExchangeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long courseId;

    /** 消耗积分 */
    private Integer pointsCost;

    /** 1成功 2失败（积分不足） */
    private Integer status;

    private LocalDateTime createTime;
}
