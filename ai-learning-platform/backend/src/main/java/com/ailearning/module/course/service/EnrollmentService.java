package com.ailearning.module.course.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.entity.CourseEnrollment;
import com.ailearning.module.course.mapper.CourseEnrollmentMapper;
import com.ailearning.module.course.mapper.CourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 选课服务：免费课程直接选课；积分课程由积分模块兑换（阶段五）
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper enrollmentMapper;

    /**
     * 免费课程选课
     */
    @Transactional(rollbackFor = Exception.class)
    public CourseEnrollment enroll(Long courseId) {
        long studentId = UserContext.userId();
        Course course = courseMapper.selectById(courseId);
        if (course == null || !course.getStatus().equals(CourseService.STATUS_ONLINE)) {
            throw new BizException("课程不存在或未上架");
        }
        if (course.getPriceType() == 2) {
            throw new BizException("该课程为积分兑换课程，请到积分商城兑换");
        }
        return doEnroll(studentId, courseId);
    }

    /**
     * 执行选课（幂等：已选课直接返回原记录）
     */
    public CourseEnrollment doEnroll(long studentId, Long courseId) {
        CourseEnrollment existing = enrollmentMapper.selectOne(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getStudentId, studentId)
                .eq(CourseEnrollment::getCourseId, courseId)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setProgress(BigDecimal.ZERO);
        enrollmentMapper.insert(enrollment);
        return enrollment;
    }
}
