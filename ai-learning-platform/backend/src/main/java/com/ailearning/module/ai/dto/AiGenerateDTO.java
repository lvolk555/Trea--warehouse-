package com.ailearning.module.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 出题请求：指定课程 + 章节 + 知识点，生成指定数量与题型的题目
 */
@Data
public class AiGenerateDTO {

    @NotNull(message = "请选择课程")
    private Long courseId;

    @NotNull(message = "请选择章节")
    private Long chapterId;

    /** 知识点描述（可空，为空则按章节整体出题） */
    private String knowledgePoint;

    /** 题型：1单选 2多选 3判断 */
    @NotNull(message = "请选择题型")
    private Integer type;

    /** 生成数量 1~10 */
    @Min(1)
    @Max(10)
    private Integer count = 5;
}
