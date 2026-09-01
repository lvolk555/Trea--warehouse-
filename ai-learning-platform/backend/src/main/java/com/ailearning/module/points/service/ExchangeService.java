package com.ailearning.module.points.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.course.service.CourseService;
import com.ailearning.module.course.service.EnrollmentService;
import com.ailearning.module.points.dto.CourseExchangeRecordVO;
import com.ailearning.module.points.entity.CourseExchangeRecord;
import com.ailearning.module.points.entity.UserCoupon;
import com.ailearning.module.points.mapper.CourseExchangeRecordMapper;
import com.ailearning.module.points.mapper.UserCouponMapper;
import com.ailearning.module.user.entity.User;
import com.ailearning.module.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 积分商城兑换服务：校验课程 → 防重复兑换 → 校验/核销优惠券 → 事务内扣积分 + 自动选课 + 记流水
 *
 * 优惠券核销：满减券（满 threshold 减 value）、折扣券（value 折，如 85 = 8.5 折），
 * 抵扣后按实际应付积分扣减；券标记已使用并记录核销时间。
 * 一致性设计：整个兑换在同一事务内完成，任一步失败全部回滚；
 * 扣积分使用条件 UPDATE（balance >= cost）兜底并发超扣。
 */
@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final CourseMapper courseMapper;
    private final CourseExchangeRecordMapper exchangeMapper;
    private final UserCouponMapper couponMapper;
    private final PointsService pointsService;
    private final EnrollmentService enrollmentService;
    private final UserMapper userMapper;

    /**
     * 学生兑换积分课程（可选使用优惠券抵扣）
     */
    @Transactional(rollbackFor = Exception.class)
    public CourseExchangeRecord exchange(Long courseId, Long couponId) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        long studentId = UserContext.userId();

        Course course = courseMapper.selectById(courseId);
        if (course == null || !course.getStatus().equals(CourseService.STATUS_ONLINE)) {
            throw new BizException("课程不存在或未上架");
        }
        if (course.getPriceType() != 2 || course.getPointsPrice() == null || course.getPointsPrice() <= 0) {
            throw new BizException("该课程不是积分兑换课程");
        }
        // 已兑换/已选课不重复扣积分
        Long exchanged = exchangeMapper.selectCount(new LambdaQueryWrapper<CourseExchangeRecord>()
                .eq(CourseExchangeRecord::getUserId, studentId)
                .eq(CourseExchangeRecord::getCourseId, courseId)
                .eq(CourseExchangeRecord::getStatus, 1));
        if (exchanged > 0) {
            throw new BizException("你已兑换过该课程，请直接学习");
        }

        int cost = course.getPointsPrice();
        int discount = 0;
        UserCoupon usedCoupon = null;
        if (couponId != null) {
            usedCoupon = resolveUsableCoupon(studentId, couponId, cost);
            discount = calcDiscount(usedCoupon, cost);
        }

        int pay = Math.max(0, cost - discount);
        if (pay > 0) {
            pointsService.deduct(studentId, pay, "兑换课程《" + course.getTitle() + "》");
        }
        // 自动选课（幂等）
        enrollmentService.doEnroll(studentId, courseId);

        // 核销优惠券
        if (usedCoupon != null) {
            usedCoupon.setStatus(1);
            usedCoupon.setUsedTime(LocalDateTime.now());
            couponMapper.updateById(usedCoupon);
        }

        CourseExchangeRecord record = new CourseExchangeRecord();
        record.setUserId(studentId);
        record.setCourseId(courseId);
        record.setPointsCost(pay);
        record.setCouponId(couponId);
        record.setDiscount(discount > 0 ? discount : null);
        record.setStatus(1);
        exchangeMapper.insert(record);
        return record;
    }

    /** 校验优惠券归属/状态/过期/门槛，返回可用的券 */
    private UserCoupon resolveUsableCoupon(long userId, Long couponId, int cost) {
        UserCoupon c = couponMapper.selectById(couponId);
        if (c == null || !c.getUserId().equals(userId)) {
            throw new BizException("优惠券不存在");
        }
        if (c.getStatus() != 0) {
            throw new BizException("优惠券不可用");
        }
        if (c.getExpireTime() != null && c.getExpireTime().isBefore(LocalDateTime.now())) {
            c.setStatus(2);
            couponMapper.updateById(c);
            throw new BizException("优惠券已过期");
        }
        // 满减券校验使用门槛
        if (c.getType() == 1 && c.getThreshold() != null && c.getThreshold() > 0 && cost < c.getThreshold()) {
            throw new BizException("未满足优惠券使用门槛");
        }
        return c;
    }

    /** 计算抵扣积分 */
    private int calcDiscount(UserCoupon c, int cost) {
        if (c.getType() == 2) {
            // 折扣券：value 如 85 → 应付 cost*0.85
            int pay = (int) Math.round(cost * c.getValue() / 100.0);
            return Math.max(0, cost - pay);
        }
        // 满减券：减 value（不超过课程原价）
        return Math.min(c.getValue() == null ? 0 : c.getValue(), cost);
    }

    /**
     * 我的兑换记录
     */
    public List<CourseExchangeRecord> myExchanges() {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        return exchangeMapper.selectList(new LambdaQueryWrapper<CourseExchangeRecord>()
                .eq(CourseExchangeRecord::getUserId, UserContext.userId())
                .orderByDesc(CourseExchangeRecord::getCreateTime));
    }

    /**
     * 管理员：兑换记录分页（可按学生筛选），补充学生名称与课程名称
     */
    public IPage<CourseExchangeRecordVO> adminPage(int page, int size, Long userId) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        LambdaQueryWrapper<CourseExchangeRecord> wrapper = new LambdaQueryWrapper<CourseExchangeRecord>()
                .eq(userId != null, CourseExchangeRecord::getUserId, userId)
                .orderByDesc(CourseExchangeRecord::getCreateTime);
        IPage<CourseExchangeRecord> raw = exchangeMapper.selectPage(new Page<>(page, size), wrapper);
        List<CourseExchangeRecord> records = raw.getRecords();

        // 批量查询学生与课程，用于填充名称
        List<Long> userIds = records.stream().map(CourseExchangeRecord::getUserId).distinct().toList();
        List<Long> courseIds = records.stream().map(CourseExchangeRecord::getCourseId).distinct().toList();
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Course> courseMap = courseIds.isEmpty() ? Map.of()
                : courseMapper.selectBatchIds(courseIds).stream()
                        .collect(Collectors.toMap(Course::getId, Function.identity()));

        return raw.convert(r -> {
            CourseExchangeRecordVO vo = new CourseExchangeRecordVO();
            org.springframework.beans.BeanUtils.copyProperties(r, vo);
            User u = userMap.get(r.getUserId());
            vo.setStudentName(u == null ? null
                    : (u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername()));
            Course c = courseMap.get(r.getCourseId());
            vo.setCourseName(c == null ? null : c.getTitle());
            return vo;
        });
    }
}