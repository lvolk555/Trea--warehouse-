package com.ailearning.module.points.controller;

import com.ailearning.common.Result;
import com.ailearning.module.points.entity.CourseExchangeRecord;
import com.ailearning.module.points.entity.PointsRule;
import com.ailearning.module.points.service.ExchangeService;
import com.ailearning.module.points.service.PointsRuleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端积分接口：规则配置、兑换记录查询（需管理员角色）
 */
@RestController
@RequestMapping("/admin/points")
@RequiredArgsConstructor
public class AdminPointsController {

    private final PointsRuleService ruleService;
    private final ExchangeService exchangeService;

    /** 积分规则列表 */
    @GetMapping("/rules")
    public Result<List<PointsRule>> rules() {
        return Result.ok(ruleService.list());
    }

    /** 更新积分规则 */
    @PostMapping("/rules/{id}")
    public Result<PointsRule> updateRule(@PathVariable Long id,
                                         @RequestParam(required = false) Integer ruleValue,
                                         @RequestParam(required = false) Integer dailyLimit,
                                         @RequestParam(required = false) Integer enabled) {
        return Result.ok(ruleService.update(id, ruleValue, dailyLimit, enabled));
    }

    /** 兑换记录分页 */
    @GetMapping("/exchanges")
    public Result<IPage<CourseExchangeRecord>> exchanges(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) Long userId) {
        return Result.ok(exchangeService.adminPage(page, size, userId));
    }
}
