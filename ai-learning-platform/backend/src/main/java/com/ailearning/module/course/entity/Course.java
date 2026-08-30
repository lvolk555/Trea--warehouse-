package com.ailearning.module.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程实体：状态 0待审核 1已上架 2已下架；定价 1免费 2积分兑换
 */
@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;

    private String title;

    private String cover;

    private String category;

    private String description;

    /** 定价方式：1免费 2积分兑换 */
    private Integer priceType;

    /** 兑换所需积分（priceType=2 时有效） */
    private Integer pointsPrice;

    /** 状态：0待审核 1已上架 2已下架 */
    private Integer status;

    private LocalDateTime createTime;
}
