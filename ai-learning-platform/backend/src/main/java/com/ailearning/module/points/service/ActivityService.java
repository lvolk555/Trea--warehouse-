package com.ailearning.module.points.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.points.dto.ActivityClaimResult;
import com.ailearning.module.points.dto.ActivitySaveDTO;
import com.ailearning.module.points.entity.PointsActivity;
import com.ailearning.module.points.entity.PointsActivityRecord;
import com.ailearning.module.points.entity.UserCoupon;
import com.ailearning.module.points.mapper.PointsActivityMapper;
import com.ailearning.module.points.mapper.PointsActivityRecordMapper;
import com.ailearning.module.points.mapper.UserCouponMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 积分活动服务：活动列表（携带当日完成状态）+ 领取奖励 + 我的优惠券 + 管理端 CRUD
 * 活动类型：1 积分任务（发积分）/ 2 优惠券（发券）
 * 幂等保障：(user_id, activity_id, claim_date) 唯一索引 + DuplicateKey 兜底
 */
@Service
@RequiredArgsConstructor
public class ActivityService {

    public static final int TYPE_POINTS = 1;
    public static final int TYPE_COUPON = 2;

    private final PointsActivityMapper activityMapper;
    private final PointsActivityRecordMapper recordMapper;
    private final UserCouponMapper couponMapper;
    private final PointsService pointsService;

    /** 活动列表（已发布的活动，按排序号升序，标记当日是否已领取） */
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

    /** 领取活动奖励：区分积分与优惠券，每日限一次，重复领取抛业务异常 */
    @Transactional(rollbackFor = Exception.class)
    public ActivityClaimResult claim(Long activityId) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        long userId = UserContext.userId();
        PointsActivity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getEnabled() != 1) {
            throw new BizException("活动不存在或尚未发布");
        }

        PointsActivityRecord record = new PointsActivityRecord();
        record.setUserId(userId);
        record.setActivityId(activityId);
        record.setClaimDate(LocalDate.now());

        ActivityClaimResult result = new ActivityClaimResult();
        result.setActivityType(activity.getActivityType());

        if (activity.getActivityType() != null && activity.getActivityType() == TYPE_COUPON) {
            record.setReward(0);
            insertRecordOnce(record, "今日已领取该优惠券");
            UserCoupon coupon = grantCoupon(userId, activity);
            result.setCouponName(coupon.getName());
            result.setCouponValue(coupon.getValue());
            result.setMessage("优惠券已发放到「我的优惠券」");
            return result;
        }

        record.setReward(activity.getReward());
        insertRecordOnce(record, "今日已完成该活动");
        int reward = activity.getReward() == null ? 0 : activity.getReward();
        pointsService.grantDirect(userId, PointsService.TYPE_ACTIVITY, reward, activity.getTitle());
        result.setReward(reward);
        result.setMessage("积分已到账");
        return result;
    }

    private void insertRecordOnce(PointsActivityRecord record, String duplicateMessage) {
        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new BizException(duplicateMessage);
        }
    }

    /** 发放优惠券：按活动配置生成券快照 */
    private UserCoupon grantCoupon(long userId, PointsActivity activity) {
        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(userId);
        coupon.setActivityId(activity.getId());
        coupon.setName(activity.getCouponName() == null ? activity.getTitle() : activity.getCouponName());
        coupon.setType(activity.getCouponType() == null ? 1 : activity.getCouponType());
        coupon.setValue(activity.getCouponValue());
        coupon.setThreshold(activity.getCouponThreshold() == null ? 0 : activity.getCouponThreshold());
        coupon.setStatus(0);
        int days = activity.getCouponExpireDays() == null || activity.getCouponExpireDays() <= 0
                ? 30 : activity.getCouponExpireDays();
        coupon.setExpireTime(LocalDateTime.now().plusDays(days));
        couponMapper.insert(coupon);
        return coupon;
    }

    /** 我的优惠券（过期券标记为已过期） */
    public List<UserCoupon> myCoupons() {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        long userId = UserContext.userId();
        List<UserCoupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getCreateTime));
        LocalDateTime now = LocalDateTime.now();
        coupons.forEach(c -> {
            if (c.getStatus() == 0 && c.getExpireTime() != null && c.getExpireTime().isBefore(now)) {
                c.setStatus(2);
            }
        });
        return coupons;
    }

    // ==================== 管理端 ====================

    /** 活动列表（含未发布） */
    public List<PointsActivity> adminList() {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        return activityMapper.selectList(new LambdaQueryWrapper<PointsActivity>()
                .orderByAsc(PointsActivity::getSortOrder));
    }

    /** 新建活动（默认未发布） */
    public PointsActivity create(ActivitySaveDTO dto) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        PointsActivity a = new PointsActivity();
        applyDto(a, dto);
        a.setEnabled(0);
        activityMapper.insert(a);
        return a;
    }

    /** 编辑活动 */
    public PointsActivity update(Long id, ActivitySaveDTO dto) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        PointsActivity a = activityMapper.selectById(id);
        if (a == null) {
            throw new BizException("活动不存在");
        }
        applyDto(a, dto);
        activityMapper.updateById(a);
        return a;
    }

    /** 发布/下线活动 */
    public PointsActivity toggle(Long id, Integer enabled) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        PointsActivity a = activityMapper.selectById(id);
        if (a == null) {
            throw new BizException("活动不存在");
        }
        a.setEnabled(enabled != null && enabled == 1 ? 1 : 0);
        activityMapper.updateById(a);
        return a;
    }

    /** 删除活动 */
    public void delete(Long id) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        activityMapper.deleteById(id);
    }

    private void applyDto(PointsActivity a, ActivitySaveDTO dto) {
        a.setTitle(dto.getTitle());
        a.setDescription(dto.getDescription());
        a.setIcon(dto.getIcon());
        int type = dto.getActivityType() != null && dto.getActivityType() == TYPE_COUPON ? TYPE_COUPON : TYPE_POINTS;
        a.setActivityType(type);
        if (type == TYPE_POINTS) {
            a.setReward(dto.getReward() == null ? 0 : dto.getReward());
            a.setCouponName(null);
            a.setCouponType(null);
            a.setCouponValue(null);
            a.setCouponThreshold(null);
            a.setCouponExpireDays(null);
        } else {
            a.setReward(0);
            a.setCouponName(dto.getCouponName());
            a.setCouponType(dto.getCouponType() == null ? 1 : dto.getCouponType());
            a.setCouponValue(dto.getCouponValue());
            a.setCouponThreshold(dto.getCouponThreshold() == null ? 0 : dto.getCouponThreshold());
            a.setCouponExpireDays(dto.getCouponExpireDays() == null ? 30 : dto.getCouponExpireDays());
        }
        a.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
    }
}