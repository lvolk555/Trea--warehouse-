package com.ailearning.module.course.controller;

import com.ailearning.common.Result;
import com.ailearning.module.course.dto.CourseSaveDTO;
import com.ailearning.module.course.dto.ReviewDTO;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.service.CourseAdminService;
import com.ailearning.module.course.service.CourseService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端课程接口：审核、上下架、课程管理（需管理员角色）
 * 管理员拥有与教师一致的课程管理能力：新增/编辑/删除课程
 */
@RestController
@RequestMapping("/admin/course")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseAdminService courseAdminService;
    private final CourseService courseService;

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

    /** 新增/编辑课程（管理员，与教师一致，含章节视频或文章内容） */
    @PostMapping("/save")
    public Result<Course> save(@Valid @RequestBody CourseSaveDTO dto) {
        return Result.ok(courseService.saveCourse(dto));
    }

    /** 提交审核 */
    @PostMapping("/submit/{courseId}")
    public Result<Course> submit(@PathVariable Long courseId) {
        return Result.ok(courseService.submitReview(courseId));
    }

    /** 删除课程 */
    @DeleteMapping("/{courseId}")
    public Result<Void> delete(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return Result.ok();
    }
}
