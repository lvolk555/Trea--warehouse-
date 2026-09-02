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

    /** 学生小节完成明细（章节 → 小节 + 完成状态，教师/管理员查看学生学习情况） */
    @GetMapping("/{studentId}/progress")
    public Result<List<Map<String, Object>>> progress(@PathVariable Long courseId,
                                                       @PathVariable Long studentId) {
        return Result.ok(studentService.progressDetail(courseId, studentId));
    }

    /** 移除学生 */
    @DeleteMapping("/{enrollmentId}")
    public Result<Void> remove(@PathVariable Long courseId, @PathVariable Long enrollmentId) {
        studentService.removeStudent(courseId, enrollmentId);
        return Result.ok();
    }
}
