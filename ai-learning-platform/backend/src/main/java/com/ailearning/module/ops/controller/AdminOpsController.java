package com.ailearning.module.ops.controller;

import com.ailearning.common.Result;
import com.ailearning.module.ops.dto.NoticeSaveDTO;
import com.ailearning.module.ops.entity.CourseComment;
import com.ailearning.module.ops.entity.Notice;
import com.ailearning.module.ops.entity.SystemConfig;
import com.ailearning.module.ops.service.CommentService;
import com.ailearning.module.ops.service.NoticeService;
import com.ailearning.module.ops.service.SystemConfigService;
import com.ailearning.module.ops.service.UserManageService;
import com.ailearning.module.user.entity.User;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端运营接口：公告管理、评论审核、用户管理（增删改查/重置密码）、系统设置（需管理员角色）
 */
@RestController
@RequestMapping("/admin/ops")
@RequiredArgsConstructor
public class AdminOpsController {

    private final NoticeService noticeService;
    private final CommentService commentService;
    private final UserManageService userManageService;
    private final SystemConfigService systemConfigService;

    // ---------- 公告 ----------

    /** 公告分页 */
    @GetMapping("/notices")
    public Result<IPage<Notice>> notices(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) Integer status) {
        return Result.ok(noticeService.page(page, size, status));
    }

    /** 新建/编辑公告 */
    @PostMapping("/notices")
    public Result<Notice> saveNotice(@Valid @RequestBody NoticeSaveDTO dto) {
        return Result.ok(noticeService.save(dto));
    }

    /** 发布/撤回公告 */
    @PostMapping("/notices/{id}/status")
    public Result<Notice> noticeStatus(@PathVariable Long id, @RequestParam boolean publish) {
        return Result.ok(noticeService.changeStatus(id, publish));
    }

    /** 置顶/取消置顶 */
    @PostMapping("/notices/{id}/top")
    public Result<Notice> noticeTop(@PathVariable Long id, @RequestParam boolean top) {
        return Result.ok(noticeService.changeTop(id, top));
    }

    /** 删除公告 */
    @DeleteMapping("/notices/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok();
    }

    // ---------- 评论审核 ----------

    /** 评论分页 */
    @GetMapping("/comments")
    public Result<IPage<Map<String, Object>>> comments(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) Integer status) {
        return Result.ok(commentService.adminPage(page, size, status));
    }

    /** 审核评论（展示/隐藏） */
    @PostMapping("/comments/{id}/review")
    public Result<CourseComment> reviewComment(@PathVariable Long id, @RequestParam boolean visible) {
        return Result.ok(commentService.review(id, visible));
    }

    /** 删除评论 */
    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok();
    }

    // ---------- 用户管理 ----------

    /** 用户分页 */
    @GetMapping("/users")
    public Result<IPage<User>> users(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) Integer role,
                                     @RequestParam(required = false) Integer status,
                                     @RequestParam(required = false) String keyword) {
        return Result.ok(userManageService.page(page, size, role, status, keyword));
    }

    /** 新增用户（可指定学生/教师/管理员角色） */
    @PostMapping("/users")
    public Result<User> createUser(@RequestBody UserManageService.UserSaveDTO dto) {
        return Result.ok(userManageService.create(dto));
    }

    /** 编辑用户（昵称/头像/角色/状态） */
    @PutMapping("/users/{userId}")
    public Result<User> updateUser(@PathVariable Long userId, @RequestBody UserManageService.UserSaveDTO dto) {
        return Result.ok(userManageService.update(userId, dto));
    }

    /** 删除用户（连同其选课/学习/考试等数据清理） */
    @DeleteMapping("/users/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        userManageService.delete(userId);
        return Result.ok();
    }

    /** 重置密码 */
    @PostMapping("/users/{userId}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        userManageService.resetPassword(userId, body.get("newPassword"));
        return Result.ok();
    }

    /** 启用/禁用用户 */
    @PostMapping("/users/{userId}/status")
    public Result<User> userStatus(@PathVariable Long userId, @RequestParam boolean enable) {
        return Result.ok(userManageService.changeStatus(userId, enable));
    }

    /** 调整角色 */
    @PostMapping("/users/{userId}/role")
    public Result<User> userRole(@PathVariable Long userId, @RequestParam Integer role) {
        return Result.ok(userManageService.changeRole(userId, role));
    }

    // ---------- 系统设置 ----------

    /** 系统配置列表 */
    @GetMapping("/settings")
    public Result<List<SystemConfig>> settings() {
        return Result.ok(systemConfigService.list());
    }

    /** 批量保存系统配置 */
    @PostMapping("/settings")
    public Result<List<SystemConfig>> saveSettings(@RequestBody Map<String, String> configs) {
        return Result.ok(systemConfigService.save(configs));
    }
}
