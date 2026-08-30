package com.ailearning.module.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 考试交卷请求：题目 ID → 学生答案
 */
@Data
public class ExamSubmitDTO {

    @NotNull(message = "考试不存在")
    private Long examId;

    /** key=题目ID，value=学生答案 */
    @NotNull(message = "答案不能为空")
    private Map<Long, String> answers;
}
