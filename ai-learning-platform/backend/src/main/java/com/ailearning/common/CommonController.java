package com.ailearning.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公共接口：健康检查（无需登录）
 */
@RestController
@RequestMapping("/common")
public class CommonController {

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.ok(Map.of(
                "status", "UP",
                "app", "ai-learning-platform",
                "time", System.currentTimeMillis()
        ));
    }
}
