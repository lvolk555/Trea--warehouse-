package com.ailearning.module.ops.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.user.entity.User;
import com.ailearning.module.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户管理服务（管理员）：分页查询、启用/禁用、角色调整
 */
@Service
@RequiredArgsConstructor
public class UserManageService {

    private final UserMapper userMapper;

    /** 用户分页（可按角色/状态筛选，关键字匹配用户名或昵称） */
    public IPage<User> page(int page, int size, Integer role, Integer status, String keyword) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(role != null, User::getRole, role)
                .eq(status != null, User::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(User::getUsername, keyword)
                        .or()
                        .like(User::getNickname, keyword))
                .orderByDesc(User::getCreateTime);
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /** 启用/禁用用户（不能禁用自己） */
    public User changeStatus(Long userId, boolean enable) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        if (userId == UserContext.userId()) {
            throw new BizException("不能修改自己的账号状态");
        }
        User user = getRequired(userId);
        user.setStatus(enable ? 1 : 0);
        userMapper.updateById(user);
        return user;
    }

    /** 调整角色（1学生 2教师 3管理员，不能修改自己） */
    public User changeRole(Long userId, Integer role) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        if (userId == UserContext.userId()) {
            throw new BizException("不能修改自己的角色");
        }
        if (role == null || role < 1 || role > 3) {
            throw new BizException("角色不合法");
        }
        User user = getRequired(userId);
        user.setRole(role);
        userMapper.updateById(user);
        return user;
    }

    private User getRequired(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }
}
