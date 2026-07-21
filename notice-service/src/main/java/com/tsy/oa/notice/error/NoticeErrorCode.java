package com.tsy.oa.notice.error;

import com.tsy.oa.common.error.ErrorCode;

public enum NoticeErrorCode implements ErrorCode {

    NOTICE_NOT_FOUND(40401, "公告不存在或未发布");

    private final int code;
    private final String message;

    NoticeErrorCode(int code, String message) {
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
