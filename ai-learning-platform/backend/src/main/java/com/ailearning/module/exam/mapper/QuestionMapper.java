package com.ailearning.module.exam.mapper;

import com.ailearning.module.exam.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题库 Mapper
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
