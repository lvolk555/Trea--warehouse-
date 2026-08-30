package com.ailearning.module.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 视频实体：归属于章节
 */
@Data
@TableName("video")
public class Video {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long chapterId;

    private String title;

    private String url;

    /** 时长（秒） */
    private Integer duration;

    private Integer sortOrder;
}
