package com.ailearning.module.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 积分活动领取记录实体：(user_id, activity_id, claim_date) 唯一约束保证每日限领一次
 */
@Data
@TableName("points_activity_record")
public class PointsActivityRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long activityId;

    private LocalDate claimDate;

    /** 实际到账积分 */
    private Integer reward;

    private LocalDateTime createTime;
}