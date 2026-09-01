package com.ailearning.module.points.dto;

import com.ailearning.module.points.entity.CourseExchangeRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程兑换记录 VO：补充学生名称与课程名称，便于管理端展示
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseExchangeRecordVO extends CourseExchangeRecord {

    /** 学生昵称（优先昵称，无昵称回退用户名） */
    private String studentName;

    /** 课程名称 */
    private String courseName;
}