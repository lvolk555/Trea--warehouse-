package com.ailearning.module.course.controller;

import com.ailearning.common.Result;
import com.ailearning.module.course.dto.CourseSaveDTO;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师课程接口：创建/编辑/提交审核/删除（需教师角色）
 */
@RestController
@RequestMapping("/teacher/course")
@RequiredArgsConstructor
public class TeacherCourseController {

    private final CourseService courseService;

    /** 我的课程列表 */
    @GetMapping("/list")
    public Result<List<Course>> list() {
        return Result.ok(courseService.myCourses());
    }

    /** 创建/更新课程（含章节视频结构） */
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
