package com.ailearning.module.ops.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 公告保存 DTO（新建/编辑）
 */
@Data
public class NoticeSaveDTO {

    private Long id;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String content;

    /** 1系统通知 2活动公告 3课程上新 */
    private Integer type;

    /** 0否 1置顶 */
    private Integer top;
}
