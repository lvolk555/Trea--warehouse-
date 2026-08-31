package com.ailearning.module.points.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.points.entity.PointsActivity;
import com.ailearning.module.points.entity.PointsActivityRecord;
import com.ailearning.module.points.mapper.PointsActivityMapper;
import com.ailearning.module.points.mapper.PointsActivityRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 积分活动服务：活动列表（携带当日完成状态）+ 领取奖励
 * 幂等保障：(user_id, activity_id, claim_date) 唯一索引 + DuplicateKey 兜底
 */
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final PointsActivityMapper activityMapper;
    private final PointsActivityRecordMapper recordMapper;
    private final PointsService pointsService;

    /** 活动列表（启用的活动，按排序号升序，标记当日是否已领取） */
    public List<PointsActivity> list() {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        long userId = UserContext.userId();
        List<PointsActivity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<PointsActivity>()
                        .eq(PointsActivity::getEnabled, 1)
                        .orderByAsc(PointsActivity::getSortOrder));
        List<PointsActivityRecord> todayRecords = recordMapper.selectList(
                new LambdaQueryWrapper<PointsActivityRecord>()
                        .eq(PointsActivityRecord::getUserId, userId)
                        .eq(PointsActivityRecord::getClaimDate, LocalDate.now()));
        Set<Long> claimedIds = todayRecords.stream()
                .map(PointsActivityRecord::getActivityId)
                .collect(Collectors.toSet());
        activities.forEach(a -> a.setClaimed(claimedIds.contains(a.getId())));
        return activities;
    }

    /** 领取活动奖励：每日限一次，重复领取抛业务异常 */
    @Transactional(rollbackFor = Exception.class)
    public PointsActivityRecord claim(Long activityId) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        long userId = UserContext.userId();
        PointsActivity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getEnabled() != 1) {
            throw new BizException("活动不存在或已下线");
        }

        PointsActivityRecord record = new PointsActivityRecord();
        record.setUserId(userId);
        record.setActivityId(activityId);
        record.setClaimDate(LocalDate.now());
        record.setReward(activity.getReward());
        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new BizException("今日已完成该活动");
        }
        pointsService.grantDirect(userId, PointsService.TYPE_ACTIVITY, activity.getReward(), activity.getTitle());
        return record;
    }
}