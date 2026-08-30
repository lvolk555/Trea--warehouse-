package com.ailearning.module.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分明细实体：每笔积分变动一条记录
 * 类型：1完课 2签到 3考试奖励 4AI提问 5兑换扣减 6注册赠送
 */
@Data
@TableName("points_record")
public class PointsRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer type;

    /** 变动值：正为获得，负为消耗 */
    private Integer changeValue;

    private String description;

    private LocalDateTime createTime;
}
