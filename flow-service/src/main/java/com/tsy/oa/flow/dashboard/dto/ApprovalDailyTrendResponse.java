package com.tsy.oa.flow.dashboard.dto;

import java.time.LocalDate;

public record ApprovalDailyTrendResponse(
        LocalDate date,
        Long applicationCount
) {
}
