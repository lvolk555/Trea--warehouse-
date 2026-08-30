package com.ailearning.module.exam.dto;

import lombok.Data;

/**
 * 章节练习单题判分结果
 */
@Data
public class PracticeResultVO {

    /** 是否答对 */
    private boolean correct;

    /** 正确答案 */
    private String answer;

    /** 解析 */
    private String analysis;
}
