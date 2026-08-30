package com.ailearning.module.ops.controller;

import com.ailearning.common.Result;
import com.ailearning.common.UserContext;
import com.ailearning.module.ops.entity.CourseComment;
import com.ailearning.module.ops.entity.Notice;
import com.ailearning.module.ops.service.CommentService;
import com.ailearning.module.ops.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生端运营接口：公告查看、课程评论
 */
@RestController
@RequestMapping("/ops")
@RequiredArgsConstructor
public class StudentOpsController {

    private final NoticeService noticeService;
    private final CommentService commentService;

    /** 已发布公告列表 */
    @GetMapping("/notices")
    public Result<List<Notice>> notices() {
        return Result.ok(noticeService.published());
    }

    /** 课程评论（已展示） */
    @GetMapping("/comments/{courseId}")
    public Result<List<Map<String, Object>>> comments(@PathVariable Long courseId) {
        return Result.ok(commentService.visibleComments(courseId));
    }

    /** 发表评论 */
    @PostMapping("/comments/{courseId}")
    public Result<CourseComment> publish(@PathVariable Long courseId, @RequestBody Map<String, String> body) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        return Result.ok(commentService.publish(courseId, body.get("content")));
    }
}
