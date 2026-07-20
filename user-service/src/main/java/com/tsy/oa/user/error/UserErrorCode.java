package com.tsy.oa.user.error;

import com.tsy.oa.common.error.ErrorCode;

public enum UserErrorCode implements ErrorCode {

    DEPARTMENT_NOT_FOUND(40401, "部门不存在"),
    POSITION_NOT_FOUND(40402, "岗位不存在"),
    EMPLOYEE_NOT_FOUND(40403, "员工不存在"),
    DEPARTMENT_NAME_EXISTS(40901, "部门名称已存在"),
    POSITION_CODE_EXISTS(40902, "岗位编码已存在"),
    EMPLOYEE_NO_EXISTS(40903, "员工编号已存在"),
    USERNAME_EXISTS(40904, "登录账号已存在");

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
