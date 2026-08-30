package com.ailearning.module.ai.controller;

import com.ailearning.common.Result;
import com.ailearning.module.ai.dto.AiGenerateDTO;
import com.ailearning.module.ai.dto.AiGradeVO;
import com.ailearning.module.ai.dto.AiQuestionSaveDTO;
import com.ailearning.module.ai.service.AiGenerateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 教师 AI 工具接口：AI 出题（生成草稿/审核入库）、AI 批改（待批改列表/批改/改分）
 */
@RestController
@RequestMapping("/teacher/ai")
@RequiredArgsConstructor
public class TeacherAiController {

    private final AiGenerateService aiGenerateService;

    /** AI 出题：返回题目草稿（不入库） */
    @PostMapping("/generate")
    public Result<List<AiQuestionSaveDTO.Draft>> generate(@Valid @RequestBody AiGenerateDTO dto) {
        return Result.ok(aiGenerateService.generate(dto));
    }

    /** 审核入库：勾选的 AI 题目草稿批量入库 */
    @PostMapping("/save-drafts")
    public Result<Integer> saveDrafts(@Valid @RequestBody AiQuestionSaveDTO dto) {
        return Result.ok(aiGenerateService.saveDrafts(dto));
    }

    /** 待批改简答题列表 */
    @GetMapping("/pending-grades")
    public Result<List<Map<String, Object>>> pendingGrades() {
        return Result.ok(aiGenerateService.pendingGrades());
    }

    /** AI 批改单道简答题 */
    @PostMapping("/grade/{answerId}")
    public Result<AiGradeVO> grade(@PathVariable Long answerId) {
        return Result.ok(aiGenerateService.grade(answerId));
    }

    /** 教师采纳/改分 */
    @PostMapping("/grade/{answerId}/adjust")
    public Result<AiGradeVO> adjust(@PathVariable Long answerId,
                                    @RequestParam BigDecimal score,
                                    @RequestParam(required = false) String comment) {
        return Result.ok(aiGenerateService.adjustScore(answerId, score, comment));
    }
}
