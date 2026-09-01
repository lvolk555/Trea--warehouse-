package com.ailearning.module.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 小节实体：归属于章节，可为视频小节或文章小节
 */
@Data
@TableName("video")
public class Video {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long chapterId;

    private String title;

    /** 小节类型：1视频 2文章（默认视频） */
    private Integer sectionType;

    private String url;

    /** 时长（秒） */
    private Integer duration;

    /** 文章内容（HTML，sectionType=2 时有效） */
    private String articleContent;

    private Integer sortOrder;
}
