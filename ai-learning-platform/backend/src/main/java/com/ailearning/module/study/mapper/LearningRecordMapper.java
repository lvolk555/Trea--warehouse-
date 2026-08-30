package com.ailearning.module.study.mapper;

import com.ailearning.module.study.entity.LearningRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学习进度 Mapper
 */
@Mapper
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {
}
