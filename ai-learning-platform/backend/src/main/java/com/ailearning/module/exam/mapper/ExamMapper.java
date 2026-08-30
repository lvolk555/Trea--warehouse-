package com.ailearning.module.exam.mapper;

import com.ailearning.module.exam.entity.Exam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 试卷 Mapper
 */
@Mapper
public interface ExamMapper extends BaseMapper<Exam> {
}
