package com.ailearning.module.exam.controller;

import com.ailearning.common.Result;
import com.ailearning.module.exam.dto.QuestionSaveDTO;
import com.ailearning.module.exam.dto.QuestionVO;
import com.ailearning.module.exam.service.QuestionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 教师题库接口：按 课程 → 章节 两级归属管理题目
 */
@RestController
@RequestMapping("/teacher/question")
@RequiredArgsConstructor
public class TeacherQuestionController {

    private final QuestionService questionService;

    /** 分页查询题库（支持按课程/章节/题型筛选） */
    @GetMapping("/page")
    public Result<IPage<QuestionVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) Integer type) {
        return Result.ok(questionService.page(pageNum, pageSize, courseId, chapterId, type));
    }

    /** 新增/编辑题目 */
    @PostMapping("/save")
    public Result<QuestionVO> save(@Valid @RequestBody QuestionSaveDTO dto) {
        return Result.ok(questionService.save(dto));
    }

    /** 删除题目 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.ok();
    }
}
