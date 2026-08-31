package com.ailearning.module.points.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.ai.entity.AiChatSession;
import com.ailearning.module.ai.mapper.AiChatSessionMapper;
import com.ailearning.module.course.entity.Video;
import com.ailearning.module.course.mapper.VideoMapper;
import com.ailearning.module.exam.entity.ExamRecord;
import com.ailearning.module.exam.mapper.ExamRecordMapper;
import com.ailearning.module.points.dto.ActivityClaimResult;
import com.ailearning.module.points.dto.ActivitySaveDTO;
import com.ailearning.module.points.entity.PointsActivity;
import com.ailearning.module.points.entity.PointsActivityRecord;
import com.ailearning.module.points.entity.UserCoupon;
import com.ailearning.module.points.mapper.PointsActivityMapper;
import com.ailearning.module.points.mapper.PointsActivityRecordMapper;
import com.ailearning.module.points.mapper.UserCouponMapper;
import com.ailearning.module.study.entity.LearningRecord;
import com.ailearning.module.study.mapper.LearningRecordMapper;
import com.ailearning.module.user.entity.User;
import com.ailearning.module.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 积分活动服务：活动列表（携带当日完成状态）+ 领取奖励（真实校验任务）+ 我的优惠券 + 管理端 CRUD
 * 活动类型：1 积分任务（任务达标发积分）/ 2 优惠券（发券）
 * 任务真实校验：profile 完善资料 / ai_ask 完成AI答疑 / chapter_finish 完成章节 / exam_pass 考试及格
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

    private final UserMapper userMapper;
    private final AiChatSessionMapper chatSessionMapper;
    private final VideoMapper videoMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final ExamRecordMapper examRecordMapper;

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

    /** 领取活动奖励：积分任务须真实完成，优惠券直接发券；每日限一次，重复领取抛业务异常 */
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

        // 积分任务：真实校验任务是否完成，未完成不允许领取
        if (!checkTaskDone(userId, activity.getTaskKey())) {
            throw new BizException("任务尚未完成，暂不能领取");
        }

        record.setReward(activity.getReward());
        insertRecordOnce(record, "今日已完成该活动");
        int reward = activity.getReward() == null ? 0 : activity.getReward();
        pointsService.grantDirect(userId, PointsService.TYPE_ACTIVITY, reward, activity.getTitle());
        result.setReward(reward);
        result.setMessage("积分已到账");
        return result;
    }

    /** 校验任务是否真实完成（无 taskKey 视为完成） */
    private boolean checkTaskDone(long userId, String taskKey) {
        if (taskKey == null || taskKey.isBlank()) {
            return true;
        }
        return switch (taskKey) {
            case "profile" -> {
                User u = userMapper.selectById(userId);
                yield u != null && notBlank(u.getNickname()) && notBlank(u.getAvatar());
            }
            case "ai_ask" -> chatSessionMapper.selectCount(new LambdaQueryWrapper<AiChatSession>()
                    .eq(AiChatSession::getStudentId, userId)) > 0;
            case "chapter_finish" -> hasFinishedChapter(userId);
            case "exam_pass" -> examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getStudentId, userId)
                    .ge(ExamRecord::getScore, new BigDecimal("60"))) > 0;
            default -> true;
        };
    }

    /** 是否存在「学完全部视频」的章节 */
    private boolean hasFinishedChapter(long studentId) {
        List<LearningRecord> finished = learningRecordMapper.selectList(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getStudentId, studentId)
                .eq(LearningRecord::getFinished, 1));
        if (finished.isEmpty()) {
            return false;
        }
        Set<Long> finishedVideoIds = finished.stream().map(LearningRecord::getVideoId).collect(Collectors.toSet());
        List<Video> finishedVideos = videoMapper.selectBatchIds(finishedVideoIds);
        Set<Long> chapterIds = finishedVideos.stream().map(Video::getChapterId).collect(Collectors.toSet());
        for (Long chapterId : chapterIds) {
            List<Long> chapterVideoIds = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                    .eq(Video::getChapterId, chapterId)).stream().map(Video::getId).toList();
            if (!chapterVideoIds.isEmpty() && chapterVideoIds.stream().allMatch(finishedVideoIds::contains)) {
                return true;
            }
        }
        return false;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
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
            a.setTaskKey(dto.getTaskKey());
            a.setReward(dto.getReward() == null ? 0 : dto.getReward());
            a.setCouponName(null);
            a.setCouponType(null);
            a.setCouponValue(null);
            a.setCouponThreshold(null);
            a.setCouponExpireDays(null);
        } else {
            a.setTaskKey(null);
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