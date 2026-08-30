package com.ailearning.module.course.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 课程审核参数
 */
@Data
public class ReviewDTO {

    @NotNull(message = "课程 ID 不能为空")
    private Long courseId;

    /** true 通过上架，false 驳回（回到待编辑状态，即下架） */
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    /** 驳回原因（驳回时填写） */
    private String reason;
}
