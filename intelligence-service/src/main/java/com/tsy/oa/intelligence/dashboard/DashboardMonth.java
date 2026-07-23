package com.tsy.oa.intelligence.dashboard;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

final class DashboardMonth {

    private DashboardMonth() {
    }

    static String normalize(String month) {
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        try {
            return YearMonth.parse(month).toString();
        } catch (DateTimeParseException exception) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
    }
}
