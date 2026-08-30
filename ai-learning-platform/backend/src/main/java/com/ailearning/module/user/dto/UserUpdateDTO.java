package com.ailearning.module.user.dto;

import lombok.Data;

/**
 * 个人中心资料更新参数
 */
@Data
public class UserUpdateDTO {

    private String nickname;

    private String avatar;
}
