package com.ailearning.module.points.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.course.service.CourseService;
import com.ailearning.module.course.service.EnrollmentService;
import com.ailearning.module.points.entity.CourseExchangeRecord;
import com.ailearning.module.points.mapper.CourseExchangeRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 积分商城兑换服务：校验课程 → 防重复兑换 → 事务内扣积分 + 自动选课 + 记流水
 *
 * 一致性设计：整个兑换在同一事务内完成，任一步失败全部回滚；
 * 扣积分使用条件 UPDATE（balance >= cost）兜底并发超扣。
 */
@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final CourseMapper courseMapper;
    private final CourseExchangeRecordMapper exchangeMapper;
    private final PointsService pointsService;
    private final EnrollmentService enrollmentService;

    /**
     * 学生兑换积分课程
     */
    @Transactional(rollbackFor = Exception.class)
    public CourseExchangeRecord exchange(Long courseId) {
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
        // 扣积分（余额不足抛异常，事务回滚）
        pointsService.deduct(studentId, cost, "兑换课程《" + course.getTitle() + "》");
        // 自动选课（幂等）
        enrollmentService.doEnroll(studentId, courseId);

        CourseExchangeRecord record = new CourseExchangeRecord();
        record.setUserId(studentId);
        record.setCourseId(courseId);
        record.setPointsCost(cost);
        record.setStatus(1);
        exchangeMapper.insert(record);
        return record;
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
     * 管理员：兑换记录分页（可按学生筛选）
     */
    public IPage<CourseExchangeRecord> adminPage(int page, int size, Long userId) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        LambdaQueryWrapper<CourseExchangeRecord> wrapper = new LambdaQueryWrapper<CourseExchangeRecord>()
                .eq(userId != null, CourseExchangeRecord::getUserId, userId)
                .orderByDesc(CourseExchangeRecord::getCreateTime);
        return exchangeMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
