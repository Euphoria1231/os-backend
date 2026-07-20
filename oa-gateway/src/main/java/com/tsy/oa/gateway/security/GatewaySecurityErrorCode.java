package com.tsy.oa.gateway.security;

import com.tsy.oa.common.error.ErrorCode;

public enum GatewaySecurityErrorCode implements ErrorCode {

    TOKEN_INVALID(40102, "登录状态无效或已过期"),
    FORBIDDEN(40302, "无接口访问权限");

    private final int code;
    private final String message;

    GatewaySecurityErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
