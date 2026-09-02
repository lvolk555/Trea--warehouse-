package com.ailearning.module.course.controller;

import com.ailearning.common.Result;
import com.ailearning.module.course.service.CourseStudentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课程学生管理接口（教师 + 管理员）：
 * 教师可管理自己创建课程的学生；管理员可管理任意课程的学生。
 */
@RestController
@RequestMapping("/course/{courseId}/students")
@RequiredArgsConstructor
public class CourseStudentController {

    private final CourseStudentService studentService;

    /** 课程学生分页（含学生信息、学习进度） */
    @GetMapping
    public Result<IPage<Map<String, Object>>> page(@PathVariable Long courseId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(required = false) String keyword) {
        return Result.ok(studentService.page(courseId, page, size, keyword));
    }

    /** 可添加的学生候选（未选本课的学生） */
    @GetMapping("/candidates")
    public Result<List<Map<String, Object>>> candidates(@PathVariable Long courseId,
                                                        @RequestParam(required = false) String keyword) {
        return Result.ok(studentService.candidates(courseId, keyword));
    }

    /** 添加学生到课程 */
    @PostMapping
    public Result<Map<String, Object>> add(@PathVariable Long courseId,
                                           @RequestBody CourseStudentService.AddStudentDTO dto) {
        return Result.ok(studentService.addStudent(courseId, dto));
    }

    /** 更新学生选课信息（进度） */
    @PutMapping("/{enrollmentId}")
    public Result<Map<String, Object>> update(@PathVariable Long courseId,
                                              @PathVariable Long enrollmentId,
                                              @RequestBody CourseStudentService.UpdateStudentDTO dto) {
        return Result.ok(studentService.updateStudent(courseId, enrollmentId, dto));
    }

    /** 移除学生 */
    @DeleteMapping("/{enrollmentId}")
    public Result<Void> remove(@PathVariable Long courseId, @PathVariable Long enrollmentId) {
        studentService.removeStudent(courseId, enrollmentId);
        return Result.ok();
    }
}
