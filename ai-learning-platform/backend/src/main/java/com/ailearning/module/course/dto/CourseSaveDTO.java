package com.ailearning.module.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 课程保存参数（创建/更新共用），支持一次性提交章节与视频结构
 */
@Data
public class CourseSaveDTO {

    /** 更新时必传，创建时为空 */
    private Long id;

    @NotBlank(message = "课程名称不能为空")
    private String title;

    private String cover;

    private String category;

    private String description;

    /** 定价方式：1免费 2积分兑换 */
    @NotNull(message = "请选择定价方式")
    private Integer priceType;

    /** 兑换所需积分（priceType=2 时必填） */
    private Integer pointsPrice;

    /** 章节与视频结构 */
    @Valid
    private List<ChapterItem> chapters;

    @Data
    public static class ChapterItem {
        private Long id;

        @NotBlank(message = "章节标题不能为空")
        private String title;

        private Integer sortOrder;

        private List<VideoItem> videos;
    }

    @Data
    public static class VideoItem {
        private Long id;

        @NotBlank(message = "视频标题不能为空")
        private String title;

        private String url;

        private Integer duration;

        private Integer sortOrder;
    }
}
