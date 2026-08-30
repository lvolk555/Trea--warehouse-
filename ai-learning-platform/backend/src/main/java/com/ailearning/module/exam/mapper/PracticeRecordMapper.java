package com.ailearning.module.exam.mapper;

import com.ailearning.module.exam.entity.PracticeRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 练习/错题记录 Mapper
 */
@Mapper
public interface PracticeRecordMapper extends BaseMapper<PracticeRecord> {
}
