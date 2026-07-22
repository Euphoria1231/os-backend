package com.tsy.oa.attendance.job;

import com.tsy.oa.attendance.calculation.AttendanceScheduledCalculationService;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class AttendanceDailyCalculationJob {

    private final AttendanceScheduledCalculationService calculationService;
    private final Clock clock;

    public AttendanceDailyCalculationJob(
            AttendanceScheduledCalculationService calculationService,
            Clock clock
    ) {
        this.calculationService = calculationService;
        this.clock = clock;
    }

    @XxlJob("attendanceDailyCalculationJob")
    public void execute() {
        calculationService.calculate(LocalDate.now(clock).minusDays(1));
    }
}
