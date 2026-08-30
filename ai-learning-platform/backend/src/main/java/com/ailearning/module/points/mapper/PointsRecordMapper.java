package com.ailearning.module.points.mapper;

import com.ailearning.module.points.entity.PointsRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分明细 Mapper
 */
@Mapper
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {
}
