package com.ailearning.module.points.dto;

import lombok.Data;

/**
 * 积分活动保存 DTO（管理端创建/编辑活动用）
 */
@Data
public class ActivitySaveDTO {

    private String title;

    private String description;

    private String icon;

    /** 活动类型：1积分任务 2优惠券 */
    private Integer activityType;

    /** 积分奖励（积分任务） */
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

    private Integer sortOrder;
}