package com.ailearning.module.exam.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 考试结果视图：总分 + 每题判分明细
 */
@Data
public class ExamResultVO {

    private Long recordId;

    private Long examId;

    /** 总分（百分制） */
    private BigDecimal score;

    /** 答对题数 */
    private int correctCount;

    /** 总题数 */
    private int totalCount;

    /** 每题判分明细 */
    private List<AnswerDetail> details;

    @Data
    public static class AnswerDetail {

        private Long questionId;

        private Integer type;

        private String content;

        private List<String> options;

        private String studentAnswer;

        private String answer;

        private String analysis;

        /** 0错 1对 */
        private Integer correct;
    }
}
