package com.ailearning.module.points.mapper;

import com.ailearning.module.points.entity.PointsActivityRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分活动领取记录 Mapper
 */
@Mapper
public interface PointsActivityRecordMapper extends BaseMapper<PointsActivityRecord> {
}