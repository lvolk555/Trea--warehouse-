package com.ailearning.module.course.controller;

import com.ailearning.common.Result;
import com.ailearning.module.course.dto.ReviewDTO;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.service.CourseAdminService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端课程接口：审核、上下架、课程管理（需管理员角色）
 */
@RestController
@RequestMapping("/admin/course")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseAdminService courseAdminService;

    /** 待审核课程列表 */
    @GetMapping("/pending")
    public Result<List<Course>> pending() {
        return Result.ok(courseAdminService.pendingCourses());
    }

    /** 审核课程（通过/驳回） */
    @PostMapping("/review")
    public Result<Course> review(@Valid @RequestBody ReviewDTO dto) {
        return Result.ok(courseAdminService.review(dto));
    }

    /** 上架/下架课程 */
    @PostMapping("/status/{courseId}")
    public Result<Course> changeStatus(@PathVariable Long courseId, @RequestParam boolean online) {
        return Result.ok(courseAdminService.changeStatus(courseId, online));
    }

    /** 全部课程分页列表 */
    @GetMapping("/page")
    public Result<IPage<Course>> page(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) String keyword) {
        return Result.ok(courseAdminService.allCourses(page, size, status, keyword));
    }
}
