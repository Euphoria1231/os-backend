package com.tsy.oa.attendance.calculation;

import com.tsy.oa.attendance.model.AttendanceDailySummary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceScheduledCalculationService {

    private final UserAttendanceWorkforceProvider workforceProvider;
    private final AttendanceDailyCalculationService calculationService;

    public AttendanceScheduledCalculationService(
            UserAttendanceWorkforceProvider workforceProvider,
            AttendanceDailyCalculationService calculationService
    ) {
        this.workforceProvider = workforceProvider;
        this.calculationService = calculationService;
    }

    public List<AttendanceDailySummary> calculate(LocalDate workDate) {
        List<Long> employeeIds = workforceProvider.findActiveEmployeeIds();
        return calculationService.calculateAll(employeeIds, workDate);
    }
}
