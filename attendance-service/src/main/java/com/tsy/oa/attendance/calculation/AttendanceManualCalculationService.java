package com.tsy.oa.attendance.calculation;

import com.tsy.oa.attendance.dto.AttendanceCalculationRequest;
import com.tsy.oa.attendance.dto.AttendanceCalculationResponse;
import com.tsy.oa.attendance.model.AttendanceDailySummary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttendanceManualCalculationService {

    private final UserAttendanceWorkforceProvider workforceProvider;
    private final AttendanceDailyCalculationService calculationService;

    public AttendanceManualCalculationService(
            UserAttendanceWorkforceProvider workforceProvider,
            AttendanceDailyCalculationService calculationService
    ) {
        this.workforceProvider = workforceProvider;
        this.calculationService = calculationService;
    }

    public AttendanceCalculationResponse calculate(
            Long operatorEmployeeId,
            AttendanceCalculationRequest request
    ) {
        workforceProvider.requireDailyCalculationPermission(operatorEmployeeId);
        List<Long> employeeIds = workforceProvider.findActiveEmployeeIds();
        List<AttendanceDailySummary> summaries = calculationService.calculateAll(employeeIds, request.date());
        return new AttendanceCalculationResponse(request.date(), summaries.size());
    }
}
