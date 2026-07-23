package com.tsy.oa.flow.error;

import com.tsy.oa.common.error.ErrorCode;

public enum FlowErrorCode implements ErrorCode {

    NOT_APPROVER(40301, "当前员工不是该申请的审批人"),
    APPLICATION_NOT_FOUND(40401, "审批申请不存在"),
    ALREADY_PROCESSED(40901, "该申请已经处理"),
    DUPLICATE_MAKEUP_APPLICATION(40902, "该考勤记录已有有效补签申请"),
    MAKEUP_NOT_ELIGIBLE(40903, "当前考勤记录不满足补签条件"),
    MAKEUP_COMPLETION_FAILED(40904, "考勤补签结果更新失败"),
    DIRECT_LEADER_MISSING(42201, "当前员工未配置直属领导"),
    ATTENDANCE_SERVICE_UNAVAILABLE(50301, "考勤服务暂不可用");

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
