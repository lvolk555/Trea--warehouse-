package com.ailearning.module.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 答题明细实体：每道题的作答结果
 */
@Data
@TableName("exam_answer")
public class ExamAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private Long questionId;

    private String studentAnswer;

    /** 0错 1对（主观题为空，等待 AI 批改） */
    private Integer correct;

    /** AI 评分（主观题，阶段四） */
    private BigDecimal aiScore;

    /** AI 批改建议（阶段四） */
    private String aiComment;
}
