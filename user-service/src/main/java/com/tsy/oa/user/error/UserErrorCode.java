package com.tsy.oa.user.error;

import com.tsy.oa.common.error.ErrorCode;

public enum UserErrorCode implements ErrorCode {

    DEPARTMENT_NOT_FOUND(40401, "部门不存在"),
    DEPARTMENT_NAME_EXISTS(40901, "部门名称已存在");

    private final int code;
    private final String message;

    UserErrorCode(int code, String message) {
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
