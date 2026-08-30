package com.ailearning.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 个人中心修改密码参数
 */
@Data
public class ChangePasswordDTO {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}