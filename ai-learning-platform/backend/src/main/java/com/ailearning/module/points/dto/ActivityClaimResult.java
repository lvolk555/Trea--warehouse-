package com.ailearning.module.points.dto;

import lombok.Data;

/**
 * 领取活动奖励结果：区分积分与优惠券，便于前端展示不同提示
 */
@Data
public class ActivityClaimResult {

    /** 活动类型：1积分任务 2优惠券 */
    private Integer activityType;

    /** 到账积分（积分任务） */
    private Integer reward;

    /** 券名称（优惠券） */
    private String couponName;

    /** 券值（优惠券） */
    private Integer couponValue;

    private String message;
}