package com.ailearning.module.points.mapper;

import com.ailearning.module.points.entity.PointsAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 积分账户 Mapper
 */
@Mapper
public interface PointsAccountMapper extends BaseMapper<PointsAccount> {

    /**
     * 条件扣减（防透支核心）：仅当余额充足时才扣减，返回受影响行数
     * 依赖数据库行锁，天然规避并发超扣
     */
    @Update("UPDATE points_account SET balance = balance - #{cost}, total_spent = total_spent + #{cost} " +
            "WHERE user_id = #{userId} AND balance >= #{cost}")
    int deductIfEnough(@Param("userId") long userId, @Param("cost") int cost);

    /**
     * 增加积分
     */
    @Update("UPDATE points_account SET balance = balance + #{value}, total_earned = total_earned + #{value} " +
            "WHERE user_id = #{userId}")
    int addPoints(@Param("userId") long userId, @Param("value") int value);
}
