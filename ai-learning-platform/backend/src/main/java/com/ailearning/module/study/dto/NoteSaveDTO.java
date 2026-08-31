package com.ailearning.module.study.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学习笔记保存参数
 */
@Data
public class NoteSaveDTO {

    @NotNull(message = "视频 ID 不能为空")
    private Long videoId;

    /** 笔记内容（富文本 HTML，空内容表示删除笔记） */
    private String content;
}