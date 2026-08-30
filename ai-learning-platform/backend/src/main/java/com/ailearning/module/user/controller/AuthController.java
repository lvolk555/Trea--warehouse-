package com.ailearning.module.user.controller;

import com.ailearning.common.Result;
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

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }
}
