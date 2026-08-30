package com.ailearning.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体：角色 1学生 2教师 3管理员
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 密码不参与序列化输出 */
    @JsonIgnore
    private String password;

    private String nickname;

    private String avatar;

    /** 角色：1学生 2教师 3管理员 */
    private Integer role;

    /** 状态：0禁用 1正常 */
    private Integer status;

    private LocalDateTime createTime;
}
