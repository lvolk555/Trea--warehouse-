package com.ailearning.module.ops.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告实体：类型 1系统通知 2活动公告 3课程上新；状态 0已撤回 1已发布
 */
@Data
@TableName("notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Integer type;

    /** 0否 1置顶 */
    private Integer top;

    /** 0已撤回 1已发布 */
    private Integer status;

    private LocalDateTime createTime;
}
