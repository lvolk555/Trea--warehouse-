package com.ailearning.module.points.service;

import com.ailearning.common.BizException;
import com.ailearning.module.points.entity.PointsAccount;
import com.ailearning.module.points.entity.PointsRecord;
import com.ailearning.module.points.entity.PointsRule;
import com.ailearning.module.points.mapper.PointsAccountMapper;
import com.ailearning.module.points.mapper.PointsRecordMapper;
import com.ailearning.module.points.mapper.PointsRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 积分核心服务：账户初始化、按规则发放（含每日上限校验）、扣减、明细查询
 *
 * 防刷设计：
 * 1. 每条规则可配置每日上限（daily_limit），按"当日该类型已发放总额"校验；
 * 2. 规则停用（enabled=0）时不发放；
 * 3. 发放动作要求调用方保证幂等（如完课只在 firstFinish 触发一次、签到靠唯一索引兜底）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsAccountMapper accountMapper;
    private final PointsRecordMapper recordMapper;
    private final PointsRuleMapper ruleMapper;

    /** 明细类型：1完课 2签到 3考试奖励 4AI提问 5兑换扣减 6注册赠送 */
    public static final int TYPE_VIDEO_FINISH = 1;
    public static final int TYPE_DAILY_SIGN = 2;
    public static final int TYPE_EXAM_PASS = 3;
    public static final int TYPE_AI_ASK = 4;
    public static final int TYPE_EXCHANGE = 5;
    public static final int TYPE_REGISTER_GIFT = 6;

    /** 规则键 → 明细类型 */
    private static final Map<String, Integer> RULE_TYPE_MAP = Map.of(
            "video_finish", TYPE_VIDEO_FINISH,
            "daily_sign", TYPE_DAILY_SIGN,
            "exam_pass", TYPE_EXAM_PASS,
            "ai_ask", TYPE_AI_ASK,
            "register_gift", TYPE_REGISTER_GIFT
    );

    /**
     * 获取账户（不存在则自动创建，余额 0）
     */
    public PointsAccount getOrCreate(long userId) {
        PointsAccount account = accountMapper.selectOne(new LambdaQueryWrapper<PointsAccount>()
                .eq(PointsAccount::getUserId, userId)
                .last("LIMIT 1"));
        if (account == null) {
            account = new PointsAccount();
            account.setUserId(userId);
            account.setBalance(0);
            account.setTotalEarned(0);
            account.setTotalSpent(0);
            accountMapper.insert(account);
        }
        return account;
    }

    /**
     * 按规则发放积分：规则停用或已达每日上限时静默跳过（返回 false），不阻断主业务
     *
     * @return 是否实际发放
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean grantByRule(long userId, String ruleKey, String description) {
        PointsRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<PointsRule>()
                .eq(PointsRule::getRuleKey, ruleKey)
                .last("LIMIT 1"));
        if (rule == null || rule.getEnabled() == 0 || rule.getRuleValue() <= 0) {
            return false;
        }
        Integer type = RULE_TYPE_MAP.get(ruleKey);
        if (type == null) {
            return false;
        }
        // 每日上限校验：统计今日该类型已发放总额
        if (rule.getDailyLimit() != null && rule.getDailyLimit() > 0) {
            int todayEarned = sumTodayEarned(userId, type);
            if (todayEarned >= rule.getDailyLimit()) {
                log.info("积分每日上限已达：user={}, rule={}, today={}", userId, ruleKey, todayEarned);
                return false;
            }
        }
        doGrant(userId, type, rule.getRuleValue(), description);
        return true;
    }

    /**
     * 幂等发放：同类型且说明中包含 uniqueTag 的明细已存在时跳过
     * 典型场景：考试及格奖励按 "exam#{examId}" 去重，交卷与 AI 批改重算分数时都可能触发，但只发一次
     *
     * @return 是否实际发放
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean grantOnceByRule(long userId, String ruleKey, String uniqueTag, String description) {
        Integer type = RULE_TYPE_MAP.get(ruleKey);
        if (type == null) {
            return false;
        }
        Long exists = recordMapper.selectCount(new LambdaQueryWrapper<PointsRecord>()
                .eq(PointsRecord::getUserId, userId)
                .eq(PointsRecord::getType, type)
                .like(PointsRecord::getDescription, uniqueTag));
        if (exists > 0) {
            return false;
        }
        return grantByRule(userId, ruleKey, description);
    }

    /**
     * 直接发放（不走规则，如注册赠送兜底）
     */
    @Transactional(rollbackFor = Exception.class)
    public void grantDirect(long userId, int type, int value, String description) {
        if (value <= 0) {
            return;
        }
        doGrant(userId, type, value, description);
    }

    /** 发放：账户不存在先建户，再加分 + 记明细 */
    private void doGrant(long userId, int type, int value, String description) {
        getOrCreate(userId);
        accountMapper.addPoints(userId, value);
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setChangeValue(value);
        record.setDescription(description);
        recordMapper.insert(record);
    }

    /**
     * 扣减积分（事务内调用）：余额不足抛业务异常，由条件 UPDATE 保证并发安全
     */
    public void deduct(long userId, int cost, String description) {
        int rows = accountMapper.deductIfEnough(userId, cost);
        if (rows == 0) {
            throw new BizException("积分不足，无法兑换");
        }
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType(TYPE_EXCHANGE);
        record.setChangeValue(-cost);
        record.setDescription(description);
        recordMapper.insert(record);
    }

    /** 统计今日某类型已发放积分总额 */
    private int sumTodayEarned(long userId, int type) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<PointsRecord> records = recordMapper.selectList(new LambdaQueryWrapper<PointsRecord>()
                .eq(PointsRecord::getUserId, userId)
                .eq(PointsRecord::getType, type)
                .gt(PointsRecord::getChangeValue, 0)
                .ge(PointsRecord::getCreateTime, startOfDay));
        return records.stream().mapToInt(PointsRecord::getChangeValue).sum();
    }
}
