package com.ailearning.module.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 章节练习单题提交请求：即时判分
 */
@Data
public class PracticeSubmitDTO {

    @NotNull(message = "题目不存在")
    private Long questionId;

    @NotBlank(message = "答案不能为空")
    private String studentAnswer;
}
