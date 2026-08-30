package com.ailearning.module.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分账户实体：一人一户，余额 = 累计获得 - 累计消耗
 */
@Data
@TableName("points_account")
public class PointsAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 当前可用积分 */
    private Integer balance;

    /** 累计获得 */
    private Integer totalEarned;

    /** 累计消耗 */
    private Integer totalSpent;

    private LocalDateTime updateTime;
}
