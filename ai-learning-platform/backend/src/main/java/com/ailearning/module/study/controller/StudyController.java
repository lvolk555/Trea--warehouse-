package com.ailearning.module.study.controller;

import com.ailearning.common.Result;
import com.ailearning.module.study.dto.NoteSaveDTO;
import com.ailearning.module.study.dto.ProgressReportDTO;
import com.ailearning.module.study.entity.LearningRecord;
import com.ailearning.module.study.service.NoteService;
import com.ailearning.module.study.service.StudyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学习接口：进度上报、断点续播、学习笔记（需登录）
 */
@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;
    private final NoteService noteService;

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

    /** 获取某视频的笔记内容 */
    @GetMapping("/note/{videoId}")
    public Result<String> getNote(@PathVariable Long videoId) {
        return Result.ok(noteService.getContent(videoId));
    }

    /** 保存/更新某视频笔记 */
    @PostMapping("/note")
    public Result<String> saveNote(@Valid @RequestBody NoteSaveDTO dto) {
        return Result.ok(noteService.save(dto));
    }

    /** 删除某条笔记 */
    @DeleteMapping("/note/{id}")
    public Result<Void> deleteNote(@PathVariable Long id) {
        noteService.delete(id);
        return Result.ok();
    }

    /** 我的笔记列表 */
    @GetMapping("/notes")
    public Result<List<Map<String, Object>>> myNotes() {
        return Result.ok(noteService.list());
    }
}
