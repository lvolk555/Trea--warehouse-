package com.ailearning.module.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 题目保存请求：新增时 id 为空；课程 + 章节两级归属必填
 */
@Data
public class QuestionSaveDTO {

    /** 更新时传入 */
    private Long id;

    @NotNull(message = "请选择所属课程")
    private Long courseId;

    @NotNull(message = "请选择所属章节")
    private Long chapterId;

    /** 题型：1单选 2多选 3判断 4简答 */
    @NotNull(message = "请选择题型")
    private Integer type;

    @NotBlank(message = "题干不能为空")
    private String content;

    /** 选项列表（客观题），如 ["A选项","B选项"] */
    private List<String> options;

    @NotBlank(message = "正确答案不能为空")
    private String answer;

    /** 解析 */
    private String analysis;
}
