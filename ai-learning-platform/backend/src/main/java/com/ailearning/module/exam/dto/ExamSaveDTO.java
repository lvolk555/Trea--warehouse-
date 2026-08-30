package com.ailearning.module.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 试卷保存请求：组卷 = 选课程 + 挑题目 + 设时长
 */
@Data
public class ExamSaveDTO {

    /** 更新时传入 */
    private Long id;

    @NotNull(message = "请选择所属课程")
    private Long courseId;

    @NotBlank(message = "试卷名称不能为空")
    private String title;

    /** 考试时长（分钟） */
    @NotNull(message = "请设置考试时长")
    private Integer duration;

    /** 题目 ID 列表 */
    @NotEmpty(message = "试卷至少包含一道题")
    private List<Long> questionIds;

    /** 是否直接发布：0草稿 1发布 */
    private Integer status;
}
