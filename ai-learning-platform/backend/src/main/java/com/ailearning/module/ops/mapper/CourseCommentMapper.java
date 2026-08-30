package com.ailearning.module.ops.mapper;

import com.ailearning.module.ops.entity.CourseComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程评论 Mapper
 */
@Mapper
public interface CourseCommentMapper extends BaseMapper<CourseComment> {
}
