package com.tsy.oa.intelligence.dashboard;

import java.time.LocalDate;

public record ApprovalDailyTrendResponse(
        LocalDate date,
        long applicationCount
) {
}
