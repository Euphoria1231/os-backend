package com.tsy.oa.attendance.controller;

import com.tsy.oa.attendance.AttendanceServiceApplication;
import com.tsy.oa.attendance.calculation.AttendanceDailyCalculationService;
import com.tsy.oa.attendance.calculation.AttendanceManualCalculationService;
import com.tsy.oa.attendance.calculation.UserAttendanceClient;
import com.tsy.oa.attendance.calculation.UserAttendanceWorkforceProvider;
import com.tsy.oa.attendance.model.AttendanceDailySummary;
import com.tsy.oa.attendance.web.GlobalExceptionHandler;
import com.tsy.oa.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceCalculationController.class)
@ContextConfiguration(classes = AttendanceServiceApplication.class)
@Import({
        AttendanceManualCalculationService.class,
        UserAttendanceWorkforceProvider.class,
        GlobalExceptionHandler.class
})
class AttendanceCalculationControllerTests {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";
    private static final LocalDate WORK_DATE = LocalDate.of(2026, 7, 21);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAttendanceClient userAttendanceClient;

    @MockitoBean
    private AttendanceDailyCalculationService calculationService;

    @Test
    void allowsAdministratorToTriggerDailyCalculation() throws Exception {
        when(userAttendanceClient.findAuthorization(1L)).thenReturn(ApiResponse.success(
                new UserAttendanceClient.AuthorizationSummary(
                        List.of(new UserAttendanceClient.RoleSummary("SUPER_ADMIN", 1)),
                        List.of()
                )
        ));
        when(userAttendanceClient.findEmployees()).thenReturn(ApiResponse.success(List.of(
                new UserAttendanceClient.EmployeeSummary(10L, 1),
                new UserAttendanceClient.EmployeeSummary(11L, 1),
                new UserAttendanceClient.EmployeeSummary(12L, 0)
        )));
        when(calculationService.calculateAll(List.of(10L, 11L), WORK_DATE)).thenReturn(List.of(
                new AttendanceDailySummary(),
                new AttendanceDailySummary()
        ));

        mockMvc.perform(post("/api/attendance/calculations/daily")
                        .header(EMPLOYEE_HEADER, "1")
                        .param("date", "2026-07-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.date").value("2026-07-21"))
                .andExpect(jsonPath("$.data.processedCount").value(2));
    }

    @Test
    void rejectsOrdinaryEmployeeWithoutCalculationPermission() throws Exception {
        when(userAttendanceClient.findAuthorization(10L)).thenReturn(ApiResponse.success(
                new UserAttendanceClient.AuthorizationSummary(
                        List.of(new UserAttendanceClient.RoleSummary("EMPLOYEE", 1)),
                        List.of()
                )
        ));

        mockMvc.perform(post("/api/attendance/calculations/daily")
                        .header(EMPLOYEE_HEADER, "10")
                        .param("date", "2026-07-21"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        verifyNoInteractions(calculationService);
    }

    @Test
    void rejectsInvalidCalculationDate() throws Exception {
        mockMvc.perform(post("/api/attendance/calculations/daily")
                        .header(EMPLOYEE_HEADER, "1")
                        .param("date", "2026-07-32"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        verifyNoInteractions(userAttendanceClient, calculationService);
    }
}
