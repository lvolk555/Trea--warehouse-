package com.ailearning.module.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户优惠券实体：学生领取优惠券活动后生成的券快照
 * 快照设计：券的名称/类型/面值/门槛在领取时复制，管理端后续修改活动不影响已发券
 */
@Data
@TableName("user_coupon")
public class UserCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 来源活动（可空） */
    private Long activityId;

    /** 券名称 */
    private String name;

    /** 券类型：1满减券 2折扣券 */
    private Integer type;

    /** 券值：满减券为减免金额，折扣券为折扣（85 表示 8.5 折） */
    private Integer value;

    /** 使用门槛（满多少可用，0 表示无门槛） */
    private Integer threshold;

    /** 状态：0未使用 1已使用 2已过期 */
    private Integer status;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;
}