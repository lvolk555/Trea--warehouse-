package com.ailearning.module.study.controller;

import com.ailearning.common.Result;
import com.ailearning.module.study.dto.ProgressReportDTO;
import com.ailearning.module.study.entity.LearningRecord;
import com.ailearning.module.study.service.StudyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学习进度接口：进度上报、断点续播（需登录）
 */
@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    /** 上报播放进度（含完课判定） */
    @PostMapping("/progress")
    public Result<LearningRecord> report(@Valid @RequestBody ProgressReportDTO dto) {
        return Result.ok(studyService.reportProgress(dto));
    }

    /** 断点续播：获取视频上次播放位置 */
    @GetMapping("/resume/{videoId}")
    public Result<Map<String, Integer>> resume(@PathVariable Long videoId) {
        return Result.ok(Map.of("position", studyService.resumePosition(videoId)));
    }
}
