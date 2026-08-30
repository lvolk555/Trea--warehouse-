package com.ailearning.module.user.dto;

import com.ailearning.module.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录返回：token + 用户信息
 */
@Data
@AllArgsConstructor
public class LoginVO {

    private String token;

    private User user;
}
