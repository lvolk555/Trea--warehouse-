package com.ailearning.module.course.controller;

import com.ailearning.common.Result;
import com.ailearning.module.course.dto.CourseVO;
import com.ailearning.module.course.dto.SquareQueryDTO;
import com.ailearning.module.course.entity.CourseEnrollment;
import com.ailearning.module.course.service.CourseQueryService;
import com.ailearning.module.course.service.EnrollmentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生端课程接口：课程广场、课程详情、我的课程、选课（需登录）
 */
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class StudentCourseController {

    private final CourseQueryService courseQueryService;
    private final EnrollmentService enrollmentService;

    /** 课程广场（分页 + 搜索 + 分类筛选）。使用 POST + JSON 参数，避免中文关键字/分类经 URL 查询串传输时被转码导致 400。 */
    @PostMapping("/square")
    public Result<IPage<CourseVO>> square(@RequestBody SquareQueryDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? 1 : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? 8 : query.getSize();
        return Result.ok(courseQueryService.courseSquare(page, size, query.getKeyword(), query.getCategory()));
    }

    /** 课程详情（章节视频树 + 学习进度） */
    @GetMapping("/{courseId}")
    public Result<CourseVO> detail(@PathVariable Long courseId) {
        return Result.ok(courseQueryService.courseDetail(courseId));
    }

    /** 我的课程 */
    @GetMapping("/my")
    public Result<List<CourseVO>> my() {
        return Result.ok(courseQueryService.myCourses());
    }

    /** 免费课程选课 */
    @PostMapping("/enroll/{courseId}")
    public Result<CourseEnrollment> enroll(@PathVariable Long courseId) {
        return Result.ok(enrollmentService.enroll(courseId));
    }
}
