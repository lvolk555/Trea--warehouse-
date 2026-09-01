package com.ailearning.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 生成教程文章请求：在弹窗中输入主题、关键词等信息即可生成
 */
@Data
public class AiArticleDTO {

    /** 文章主题/标题 */
    @NotBlank(message = "请输入文章主题")
    private String title;

    /** 关键词/知识点（可空，辅助生成更聚焦的内容） */
    private String keywords;

    /** 补充要求（可空，如篇幅、受众、大纲等） */
    private String requirements;
}