package com.tsy.oa.user.error;

import com.tsy.oa.common.error.ErrorCode;

public enum UserErrorCode implements ErrorCode {

    INVALID_CREDENTIALS(40101, "账号或密码错误"),
    TOKEN_INVALID(40102, "登录状态无效或已过期"),
    EMPLOYEE_DISABLED(40301, "员工账号已禁用"),
    DEPARTMENT_NOT_FOUND(40401, "部门不存在"),
    POSITION_NOT_FOUND(40402, "岗位不存在"),
    EMPLOYEE_NOT_FOUND(40403, "员工不存在"),
    ROLE_NOT_FOUND(40404, "角色不存在"),
    MENU_NOT_FOUND(40405, "菜单不存在"),
    API_PERMISSION_NOT_FOUND(40406, "接口权限不存在"),
    DEPARTMENT_NAME_EXISTS(40901, "部门名称已存在"),
    POSITION_CODE_EXISTS(40902, "岗位编码已存在"),
    EMPLOYEE_NO_EXISTS(40903, "员工编号已存在"),
    USERNAME_EXISTS(40904, "登录账号已存在"),
    ROLE_CODE_EXISTS(40905, "角色编码已存在"),
    API_PERMISSION_CODE_EXISTS(40906, "接口权限编码已存在");

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
