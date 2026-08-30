package com.ailearning.module.points.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.points.entity.PointsRule;
import com.ailearning.module.points.mapper.PointsRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 积分规则管理服务（管理员）：查询/调整奖励值、每日上限、启停
 */
@Service
@RequiredArgsConstructor
public class PointsRuleService {

    private final PointsRuleMapper ruleMapper;

    /** 规则列表 */
    public List<PointsRule> list() {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        return ruleMapper.selectList(new LambdaQueryWrapper<PointsRule>()
                .orderByAsc(PointsRule::getId));
    }

    /** 更新规则（奖励值/上限/开关） */
    public PointsRule update(Long id, Integer ruleValue, Integer dailyLimit, Integer enabled) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        PointsRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BizException("规则不存在");
        }
        if (ruleValue != null) {
            if (ruleValue < 0) {
                throw new BizException("奖励值不能为负数");
            }
            rule.setRuleValue(ruleValue);
        }
        if (dailyLimit != null) {
            if (dailyLimit < 0) {
                throw new BizException("每日上限不能为负数");
            }
            rule.setDailyLimit(dailyLimit);
        }
        if (enabled != null) {
            rule.setEnabled(enabled == 1 ? 1 : 0);
        }
        ruleMapper.updateById(rule);
        return rule;
    }
}
