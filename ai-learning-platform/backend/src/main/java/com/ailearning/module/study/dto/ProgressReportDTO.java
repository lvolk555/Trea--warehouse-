package com.ailearning.module.study.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学习进度上报参数
 */
@Data
public class ProgressReportDTO {

    @NotNull(message = "视频 ID 不能为空")
    private Long videoId;

    /** 当前播放位置（秒） */
    @NotNull(message = "播放位置不能为空")
    private Integer position;

    /** 是否已看完（播放到结尾时前端置为 true） */
    private Boolean finished;
}
