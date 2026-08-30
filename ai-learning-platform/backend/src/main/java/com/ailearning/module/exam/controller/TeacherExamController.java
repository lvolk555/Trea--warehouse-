package com.ailearning.module.exam.controller;

import com.ailearning.common.Result;
import com.ailearning.module.exam.dto.ExamSaveDTO;
import com.ailearning.module.exam.dto.ExamVO;
import com.ailearning.module.exam.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师组卷接口：创建/编辑/发布/删除试卷
 */
@RestController
@RequestMapping("/teacher/exam")
@RequiredArgsConstructor
public class TeacherExamController {

    private final ExamService examService;

    /** 我的试卷列表 */
    @GetMapping("/list")
    public Result<List<ExamVO>> list(@RequestParam(required = false) Long courseId) {
        return Result.ok(examService.teacherExams(courseId));
    }

    /** 创建/更新试卷 */
    @PostMapping("/save")
    public Result<ExamVO> save(@Valid @RequestBody ExamSaveDTO dto) {
        return Result.ok(examService.save(dto));
    }

    /** 发布试卷 */
    @PostMapping("/publish/{examId}")
    public Result<ExamVO> publish(@PathVariable Long examId) {
        return Result.ok(examService.publish(examId));
    }

    /** 删除试卷 */
    @DeleteMapping("/{examId}")
    public Result<Void> delete(@PathVariable Long examId) {
        examService.delete(examId);
        return Result.ok();
    }
}
