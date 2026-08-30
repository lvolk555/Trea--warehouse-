package com.ailearning.module.course.mapper;

import com.ailearning.module.course.entity.CourseEnrollment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 选课 Mapper
 */
@Mapper
public interface CourseEnrollmentMapper extends BaseMapper<CourseEnrollment> {
}
