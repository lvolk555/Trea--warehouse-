package com.ailearning.module.course.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.course.dto.ReviewDTO;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.mapper.CourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理端课程服务：审核上下架、全部课程管理
 */
@Service
@RequiredArgsConstructor
public class CourseAdminService {

    private final CourseMapper courseMapper;

    /**
     * 待审核课程列表（管理员）
     */
    public List<Course> pendingCourses() {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        return courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, CourseService.STATUS_PENDING)
                .orderByAsc(Course::getCreateTime));
    }

    /**
     * 审核课程：通过 → 上架；驳回 → 下架
     */
    public Course review(ReviewDTO dto) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        Course course = courseMapper.selectById(dto.getCourseId());
        if (course == null) {
            throw new BizException("课程不存在");
        }
        if (!course.getStatus().equals(CourseService.STATUS_PENDING)) {
            throw new BizException("该课程不在待审核状态");
        }
        course.setStatus(dto.getApproved() ? CourseService.STATUS_ONLINE : CourseService.STATUS_OFFLINE);
        courseMapper.updateById(course);
        return course;
    }

    /**
     * 上架/下架课程（管理员对已审核课程的操作）
     * 未审核通过的课程（待审核状态）不允许上下架，必须先完成审核
     */
    public Course changeStatus(Long courseId, boolean online) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException("课程不存在");
        }
        if (course.getStatus().equals(CourseService.STATUS_PENDING)) {
            throw new BizException("该课程尚未审核通过，请先在课程审核中处理后再上下架");
        }
        course.setStatus(online ? CourseService.STATUS_ONLINE : CourseService.STATUS_OFFLINE);
        courseMapper.updateById(course);
        return course;
    }

    /**
     * 全部课程分页列表（管理员，支持状态筛选）
     */
    public IPage<Course> allCourses(int page, int size, Integer status, String keyword) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(status != null, Course::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), Course::getTitle, keyword)
                .orderByDesc(Course::getCreateTime);
        return courseMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
