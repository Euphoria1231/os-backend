package com.tsy.oa.attendance.error;

import com.tsy.oa.common.error.ErrorCode;

public enum AttendanceErrorCode implements ErrorCode {

    NOT_DIRECT_LEADER(40301, "当前员工不是目标员工的直属领导"),
    RECORD_NOT_FOUND(40401, "当日考勤记录不存在"),
    ALREADY_CLOCKED_IN(40901, "今日已经完成上班打卡"),
    ALREADY_CLOCKED_OUT(40902, "今日已经完成下班打卡"),
    CLOCK_IN_REQUIRED(40903, "请先完成上班打卡"),
    OPERATION_BUSY(40904, "打卡操作处理中，请稍后重试"),
    MAKEUP_QUOTA_BELOW_USED(40905, "补签总次数不能小于已使用次数");

    private final int code;
    private final String message;

    AttendanceErrorCode(int code, String message) {
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
