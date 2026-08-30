package com.ailearning.module.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 积分规则实体：管理员可调整奖励值/每日上限/开关
 * 规则键：video_finish / daily_sign / exam_pass / ai_ask / register_gift
 */
@Data
@TableName("points_rule")
public class PointsRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleKey;

    /** 奖励积分值 */
    private Integer ruleValue;

    /** 每日获取上限（0 表示不限） */
    private Integer dailyLimit;

    /** 0停用 1启用 */
    private Integer enabled;
}
