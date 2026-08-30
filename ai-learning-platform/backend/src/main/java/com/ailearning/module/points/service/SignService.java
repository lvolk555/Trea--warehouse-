package com.ailearning.module.points.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.points.entity.SignRecord;
import com.ailearning.module.points.mapper.SignRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日签到服务：签到成功按 daily_sign 规则发放积分
 * 幂等保障：(user_id, sign_date) 唯一索引 + 先查后插 + DuplicateKey 兜底
 */
@Service
@RequiredArgsConstructor
public class SignService {

    private final SignRecordMapper signRecordMapper;
    private final PointsService pointsService;

    /**
     * 签到：今日已签抛业务异常；签到成功发放积分（受每日上限约束）
     */
    @Transactional(rollbackFor = Exception.class)
    public SignRecord sign() {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        long userId = UserContext.userId();
        LocalDate today = LocalDate.now();

        Long count = signRecordMapper.selectCount(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, userId)
                .eq(SignRecord::getSignDate, today));
        if (count > 0) {
            throw new BizException("今日已签到，明天再来吧");
        }

        SignRecord record = new SignRecord();
        record.setUserId(userId);
        record.setSignDate(today);
        try {
            signRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发重复签到兜底
            throw new BizException("今日已签到，明天再来吧");
        }
        pointsService.grantByRule(userId, "daily_sign", "每日签到");
        return record;
    }

    /**
     * 签到状态：今日是否已签 + 本月签到日期列表
     */
    public List<SignRecord> monthRecords() {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        LocalDate today = LocalDate.now();
        return signRecordMapper.selectList(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, UserContext.userId())
                .ge(SignRecord::getSignDate, today.withDayOfMonth(1))
                .le(SignRecord::getSignDate, today)
                .orderByAsc(SignRecord::getSignDate));
    }
}
