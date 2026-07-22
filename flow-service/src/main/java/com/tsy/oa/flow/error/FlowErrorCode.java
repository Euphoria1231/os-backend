package com.tsy.oa.flow.error;

import com.tsy.oa.common.error.ErrorCode;

public enum FlowErrorCode implements ErrorCode {

    NOT_APPROVER(40301, "当前员工不是该申请的审批人"),
    SELF_APPROVAL_FORBIDDEN(40302, "申请人不能审批自己的申请"),
    APPLICATION_NOT_FOUND(40401, "审批申请不存在"),
    ALREADY_PROCESSED(40901, "该申请已经处理"),
    DIRECT_LEADER_MISSING(42201, "当前员工未配置直属领导"),
    SECOND_APPROVER_MISSING(42202, "请假流程未配置二级审批人");

    private final int code;
    private final String message;

    FlowErrorCode(int code, String message) {
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
