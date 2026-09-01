package com.ailearning.module.points.controller;

import com.ailearning.common.Result;
import com.ailearning.module.points.dto.ActivitySaveDTO;
import com.ailearning.module.points.dto.CourseExchangeRecordVO;
import com.ailearning.module.points.entity.PointsActivity;
import com.ailearning.module.points.entity.PointsRule;
import com.ailearning.module.points.service.ActivityService;
import com.ailearning.module.points.service.ExchangeService;
import com.ailearning.module.points.service.PointsRuleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端积分接口：规则配置、兑换记录、积分活动管理（需管理员角色）
 */
@RestController
@RequestMapping("/admin/points")
@RequiredArgsConstructor
public class AdminPointsController {

    private final PointsRuleService ruleService;
    private final ExchangeService exchangeService;
    private final ActivityService activityService;

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
    public Result<IPage<CourseExchangeRecordVO>> exchanges(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(required = false) Long userId) {
        return Result.ok(exchangeService.adminPage(page, size, userId));
    }

    /** 活动列表（含未发布） */
    @GetMapping("/activities")
    public Result<List<PointsActivity>> activities() {
        return Result.ok(activityService.adminList());
    }

    /** 新建活动 */
    @PostMapping("/activities")
    public Result<PointsActivity> createActivity(@RequestBody ActivitySaveDTO dto) {
        return Result.ok(activityService.create(dto));
    }

    /** 编辑活动 */
    @PostMapping("/activities/{id}")
    public Result<PointsActivity> updateActivity(@PathVariable Long id, @RequestBody ActivitySaveDTO dto) {
        return Result.ok(activityService.update(id, dto));
    }

    /** 发布/下线活动 */
    @PostMapping("/activities/{id}/status")
    public Result<PointsActivity> toggleActivity(@PathVariable Long id, @RequestParam Integer enabled) {
        return Result.ok(activityService.toggle(id, enabled));
    }

    /** 删除活动 */
    @DeleteMapping("/activities/{id}")
    public Result<Void> deleteActivity(@PathVariable Long id) {
        activityService.delete(id);
        return Result.ok();
    }
}
