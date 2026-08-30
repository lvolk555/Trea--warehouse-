package com.ailearning.module.ai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 批改结果：简答题评分（0-10）+ 建议
 */
@Data
public class AiGradeVO {

    private Long answerId;

    /** AI 评分（0-10） */
    private BigDecimal aiScore;

    /** AI 批改建议 */
    private String aiComment;

    /** 重算后的考试总分 */
    private BigDecimal examScore;

    /** 题目参考答案（供教师比对） */
    private String referenceAnswer;

    /** 学生答案 */
    private String studentAnswer;
}
