package com.ailearning.module.points.controller;

import com.ailearning.common.Result;
import com.ailearning.common.UserContext;
import com.ailearning.module.points.dto.ActivityClaimResult;
import com.ailearning.module.points.entity.CourseExchangeRecord;
import com.ailearning.module.points.entity.PointsAccount;
import com.ailearning.module.points.entity.PointsActivity;
import com.ailearning.module.points.entity.PointsRecord;
import com.ailearning.module.points.entity.SignRecord;
import com.ailearning.module.points.entity.UserCoupon;
import com.ailearning.module.points.mapper.PointsRecordMapper;
import com.ailearning.module.points.service.ActivityService;
import com.ailearning.module.points.service.ExchangeService;
import com.ailearning.module.points.service.PointsService;
import com.ailearning.module.points.service.SignService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生端积分接口：账户、明细、签到、兑换、兑换记录
 */
@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class StudentPointsController {

    private final PointsService pointsService;
    private final SignService signService;
    private final ExchangeService exchangeService;
    private final ActivityService activityService;
    private final PointsRecordMapper recordMapper;

    /** 我的积分账户 */
    @GetMapping("/account")
    public Result<PointsAccount> account() {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        return Result.ok(pointsService.getOrCreate(UserContext.userId()));
    }

    /** 积分明细（分页） */
    @GetMapping("/records")
    public Result<IPage<PointsRecord>> records(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        IPage<PointsRecord> result = recordMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getUserId, UserContext.userId())
                        .orderByDesc(PointsRecord::getCreateTime));
        return Result.ok(result);
    }

    /** 每日签到 */
    @PostMapping("/sign")
    public Result<SignRecord> sign() {
        return Result.ok(signService.sign());
    }

    /** 本月签到记录 */
    @GetMapping("/sign/month")
    public Result<List<SignRecord>> signMonth() {
        return Result.ok(signService.monthRecords());
    }

    /** 兑换积分课程 */
    @PostMapping("/exchange/{courseId}")
    public Result<CourseExchangeRecord> exchange(@PathVariable Long courseId) {
        return Result.ok(exchangeService.exchange(courseId));
    }

    /** 我的兑换记录 */
    @GetMapping("/exchange/my")
    public Result<List<CourseExchangeRecord>> myExchanges() {
        return Result.ok(exchangeService.myExchanges());
    }

    /** 积分活动列表（携带当日完成状态） */
    @GetMapping("/activities")
    public Result<List<PointsActivity>> activities() {
        return Result.ok(activityService.list());
    }

    /** 领取积分活动奖励（积分/优惠券） */
    @PostMapping("/activities/{id}/claim")
    public Result<ActivityClaimResult> claimActivity(@PathVariable Long id) {
        return Result.ok(activityService.claim(id));
    }

    /** 我的优惠券 */
    @GetMapping("/coupons")
    public Result<List<UserCoupon>> coupons() {
        return Result.ok(activityService.myCoupons());
    }
}
