package com.ailearning.module.course.dto;

import lombok.Data;

/**
 * 课程广场查询参数（分页 + 关键字 + 分类筛选）
 */
@Data
public class SquareQueryDTO {

    private Integer page = 1;

    private Integer size = 8;

    private String keyword;

    private String category;
}