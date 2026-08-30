package com.ailearning.module.study.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习进度实体：记录学生对每个视频的播放位置与完成状态
 */
@Data
@TableName("learning_record")
public class LearningRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long videoId;

    /** 播放位置（秒） */
    private Integer position;

    /** 0未完成 1已完成 */
    private Integer finished;

    private LocalDateTime updateTime;
}
