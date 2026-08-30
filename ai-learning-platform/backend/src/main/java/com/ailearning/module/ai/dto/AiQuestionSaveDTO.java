package com.ailearning.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * AI 出题入库请求：教师审核后勾选入库的题目草稿
 */
@Data
public class AiQuestionSaveDTO {

    @NotNull(message = "请选择课程")
    private Long courseId;

    @NotNull(message = "请选择章节")
    private Long chapterId;

    @NotEmpty(message = "请至少选择一道题入库")
    private List<Draft> questions;

    @Data
    public static class Draft {

        /** 题型：1单选 2多选 3判断 */
        @NotNull
        private Integer type;

        @NotBlank(message = "题干不能为空")
        private String content;

        /** 选项（客观题） */
        private List<String> options;

        @NotBlank(message = "答案不能为空")
        private String answer;

        /** 解析 */
        private String analysis;
    }
}
