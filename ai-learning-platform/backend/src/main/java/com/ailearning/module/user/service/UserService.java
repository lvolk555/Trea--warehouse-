package com.ailearning.module.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.ailearning.common.BizException;
import com.ailearning.module.user.dto.LoginDTO;
import com.ailearning.module.user.dto.LoginVO;
import com.ailearning.module.user.dto.RegisterDTO;
import com.ailearning.module.user.dto.UserUpdateDTO;
import com.ailearning.module.user.entity.User;
import com.ailearning.module.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务：注册、登录、个人中心
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    /** BCrypt 密码加密器（兼容 $2a$/$2b$/$2y$ 前缀） */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /** 角色常量 */
    public static final int ROLE_STUDENT = 1;
    public static final int ROLE_TEACHER = 2;
    public static final int ROLE_ADMIN = 3;

    /**
     * 注册（默认学生角色），用户名唯一校验
     */
    public User register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PASSWORD_ENCODER.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setRole(ROLE_STUDENT);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    /**
     * 登录：校验密码与账号状态，签发 Sa-Token
     */
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !PASSWORD_ENCODER.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BizException("账号已被禁用，请联系管理员");
        }
        // 登录并绑定角色，便于后续按角色鉴权
        StpUtil.login(user.getId());
        StpUtil.getSession().set("role", user.getRole());
        return new LoginVO(StpUtil.getTokenValue(), user);
    }

    /**
     * 获取当前登录用户
     */
    public User currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        return user;
    }

    /**
     * 更新个人资料（昵称、头像）
     */
    public User updateProfile(UserUpdateDTO dto) {
        User user = currentUser();
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        userMapper.updateById(user);
        return user;
    }

    /**
     * 退出登录
     */
    public void logout() {
        StpUtil.logout();
    }
}
