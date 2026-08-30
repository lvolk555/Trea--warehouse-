package com.ailearning.common;

import lombok.Data;

/**
 * 统一响应封装：{code, message, data}
 */
@Data
public class Result<T> {

    /** 200 成功；401 未登录；403 无权限；500 业务/系统错误 */
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
