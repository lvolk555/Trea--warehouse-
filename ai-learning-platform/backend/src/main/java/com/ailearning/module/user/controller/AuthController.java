package com.ailearning.module.user.controller;

import com.ailearning.common.BizException;
import com.ailearning.common.Result;
import com.ailearning.module.ops.service.SystemConfigService;
import com.ailearning.module.user.dto.LoginDTO;
import com.ailearning.module.user.dto.LoginVO;
import com.ailearning.module.user.dto.RegisterDTO;
import com.ailearning.module.user.entity.User;
import com.ailearning.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录 / 注册（无需登录即可访问）
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final SystemConfigService systemConfigService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterDTO dto) {
        // 管理员可在系统设置中关闭自主注册（管理端"用户管理"仍可直接建号）
        if (!systemConfigService.isEnabled("register_enabled")) {
            throw new BizException("平台已关闭自主注册，请联系管理员开通账号");
        }
        return Result.ok(userService.register(dto));
    }
}
