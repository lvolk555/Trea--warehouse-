package com.ailearning.module.exam.controller;

import com.ailearning.common.Result;
import com.ailearning.module.exam.dto.ExamResultVO;
import com.ailearning.module.exam.dto.ExamSubmitDTO;
import com.ailearning.module.exam.dto.ExamVO;
import com.ailearning.module.exam.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生考试接口：考试列表、进入考试、交卷判分、成绩查询
 */
@RestController
@RequestMapping("/student/exam")
@RequiredArgsConstructor
public class StudentExamController {

    private final ExamService examService;

    /** 可参加的考试列表（已选课程的已发布试卷） */
    @GetMapping("/list")
    public Result<List<ExamVO>> list() {
        return Result.ok(examService.studentExams());
    }

    /** 进入考试：返回题目（隐藏答案），限时由前端按 duration 倒计时 */
    @GetMapping("/start/{examId}")
    public Result<ExamVO> start(@PathVariable Long examId) {
        return Result.ok(examService.startExam(examId));
    }

    /** 交卷：自动判分并返回成绩与每题明细 */
    @PostMapping("/submit")
    public Result<ExamResultVO> submit(@Valid @RequestBody ExamSubmitDTO dto) {
        return Result.ok(examService.submit(dto));
    }

    /** 我的考试成绩 */
    @GetMapping("/scores")
    public Result<List<Map<String, Object>>> scores() {
        return Result.ok(examService.myScores());
    }

    /** 成绩详情：回看该次考试的题目、作答与判分（仅本人记录） */
    @GetMapping("/record/{recordId}")
    public Result<ExamResultVO> recordDetail(@PathVariable Long recordId) {
        return Result.ok(examService.recordDetail(recordId));
    }
}
