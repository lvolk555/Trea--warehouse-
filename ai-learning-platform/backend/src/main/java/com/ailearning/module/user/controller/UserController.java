package com.ailearning.module.user.controller;

import com.ailearning.common.Result;
import com.ailearning.module.user.dto.ChangePasswordDTO;
import com.ailearning.module.user.dto.UserUpdateDTO;
import com.ailearning.module.user.entity.User;
import com.ailearning.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口：个人中心（需登录）
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public Result<User> me() {
        return Result.ok(userService.currentUser());
    }

    /** 更新个人资料 */
    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody UserUpdateDTO dto) {
        return Result.ok(userService.updateProfile(dto));
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return Result.ok();
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.ok();
    }
}
