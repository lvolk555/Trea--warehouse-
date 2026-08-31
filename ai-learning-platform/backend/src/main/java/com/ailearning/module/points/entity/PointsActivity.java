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

    /** 活动类型：1积分任务 2优惠券 */
    private Integer activityType;

    /** 奖励积分（积分任务） */
    private Integer reward;

    /** 券名称（优惠券） */
    private String couponName;

    /** 券类型：1满减券 2折扣券 */
    private Integer couponType;

    /** 券值：满减为减免金额，折扣为折扣（85 = 8.5 折） */
    private Integer couponValue;

    /** 使用门槛（满多少可用，0 无门槛） */
    private Integer couponThreshold;

    /** 券有效期（天） */
    private Integer couponExpireDays;

    /** 0未发布/停用 1已发布 */
    private Integer enabled;

    private Integer sortOrder;

    /** 当日是否已领取（非数据库字段，查询后填充） */
    @TableField(exist = false)
    private Boolean claimed;
}