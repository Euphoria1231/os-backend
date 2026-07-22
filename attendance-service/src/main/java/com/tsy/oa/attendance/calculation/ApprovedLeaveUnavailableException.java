package com.tsy.oa.attendance.calculation;

public class ApprovedLeaveUnavailableException extends RuntimeException {

    public ApprovedLeaveUnavailableException(String message) {
        super(message);
    }

    public ApprovedLeaveUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
