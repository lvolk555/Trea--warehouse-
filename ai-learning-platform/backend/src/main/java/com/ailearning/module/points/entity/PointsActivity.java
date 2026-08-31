package com.ailearning.module.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 积分活动实体：面向学生的额外积分任务，每日可领取一次奖励
 */
@Data
@TableName("points_activity")
public class PointsActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String icon;

    /** 奖励积分 */
    private Integer reward;

    /** 0停用 1启用 */
    private Integer enabled;

    private Integer sortOrder;

    /** 当日是否已领取（非数据库字段，查询后填充） */
    @TableField(exist = false)
    private Boolean claimed;
}