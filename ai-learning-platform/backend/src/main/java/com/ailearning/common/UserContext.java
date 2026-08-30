package com.ailearning.common;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 当前登录用户上下文：获取用户 ID、角色，校验角色权限
 */
public class UserContext {

    public static final int ROLE_STUDENT = 1;
    public static final int ROLE_TEACHER = 2;
    public static final int ROLE_ADMIN = 3;

    /** 当前登录用户 ID */
    public static long userId() {
        return StpUtil.getLoginIdAsLong();
    }

    /** 当前登录用户角色 */
    public static int role() {
        Object role = StpUtil.getSession().get("role");
        return role == null ? 0 : (int) role;
    }

    /** 校验当前用户是否属于指定角色之一，否则抛出 403 */
    public static void checkRole(int... roles) {
        int current = role();
        for (int r : roles) {
            if (current == r) {
                return;
            }
        }
        throw new BizException(403, "无权限执行该操作");
    }
}
