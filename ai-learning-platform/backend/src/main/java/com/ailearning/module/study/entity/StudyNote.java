package com.ailearning.module.study.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习笔记实体：学生对某个视频记录的富文本笔记
 */
@Data
@TableName("study_note")
public class StudyNote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long videoId;

    /** 笔记内容（富文本 HTML） */
    private String content;

    private LocalDateTime createTime;
}