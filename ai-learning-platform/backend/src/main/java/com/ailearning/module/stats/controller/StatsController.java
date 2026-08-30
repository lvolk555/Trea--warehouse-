package com.ailearning.module.stats.controller;

import com.ailearning.common.Result;
import com.ailearning.module.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 数据统计接口：三端看板数据（各接口内部按角色鉴权）
 */
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** 学生看板数据 */
    @GetMapping("/student")
    public Result<Map<String, Object>> student() {
        return Result.ok(statsService.studentDashboard());
    }

    /** 教师看板数据 */
    @GetMapping("/teacher")
    public Result<Map<String, Object>> teacher() {
        return Result.ok(statsService.teacherDashboard());
    }

    /** 管理员看板数据 */
    @GetMapping("/admin")
    public Result<Map<String, Object>> admin() {
        return Result.ok(statsService.adminDashboard());
    }
}
