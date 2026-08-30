package com.ailearning.module.exam.controller;

import com.ailearning.common.Result;
import com.ailearning.module.exam.dto.PracticeResultVO;
import com.ailearning.module.exam.dto.PracticeSubmitDTO;
import com.ailearning.module.exam.dto.QuestionVO;
import com.ailearning.module.exam.service.PracticeService;
import com.ailearning.module.exam.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生章节练习接口：抽题、即时判分、错题本
 */
@RestController
@RequestMapping("/student/practice")
@RequiredArgsConstructor
public class StudentPracticeController {

    private final QuestionService questionService;
    private final PracticeService practiceService;

    /** 按章节抽题（随机客观题） */
    @GetMapping("/questions")
    public Result<List<QuestionVO>> questions(
            @RequestParam Long chapterId,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(questionService.pickQuestions(chapterId, limit, false));
    }

    /** 提交单题，即时判分并返回正确答案与解析 */
    @PostMapping("/submit")
    public Result<PracticeResultVO> submit(@Valid @RequestBody PracticeSubmitDTO dto) {
        return Result.ok(practiceService.submit(dto));
    }

    /** 错题本（可按课程筛选） */
    @GetMapping("/error-book")
    public Result<Map<String, Object>> errorBook(@RequestParam(required = false) Long courseId) {
        return Result.ok(practiceService.errorBook(courseId));
    }

    /** 标记错题已掌握 */
    @PostMapping("/mastered/{recordId}")
    public Result<Void> markMastered(@PathVariable Long recordId) {
        practiceService.markMastered(recordId);
        return Result.ok();
    }
}
