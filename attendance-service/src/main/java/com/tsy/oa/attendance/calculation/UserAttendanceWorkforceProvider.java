package com.tsy.oa.attendance.calculation;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserAttendanceWorkforceProvider {

    public static final String DAILY_CALCULATION_AUTHORITY =
            "POST:/api/attendance/calculations/daily";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final int ENABLED = 1;

    private final UserAttendanceClient client;

    public UserAttendanceWorkforceProvider(UserAttendanceClient client) {
        this.client = client;
    }

    public void requireDailyCalculationPermission(Long employeeId) {
        ApiResponse<UserAttendanceClient.AuthorizationSummary> response =
                client.findAuthorization(employeeId);
        UserAttendanceClient.AuthorizationSummary authorization = requireData(response);
        boolean superAdministrator = safeList(authorization.roles()).stream().anyMatch(role ->
                ENABLED == role.status() && SUPER_ADMIN.equals(role.code())
        );
        boolean hasApiPermission = safeList(authorization.apiPermissions()).stream().anyMatch(permission ->
                ENABLED == permission.status()
                        && DAILY_CALCULATION_AUTHORITY.equals(permission.authority())
        );
        if (!superAdministrator && !hasApiPermission) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    public List<Long> findActiveEmployeeIds() {
        List<UserAttendanceClient.EmployeeSummary> employees = requireData(client.findEmployees());
        return employees.stream()
                .filter(employee -> employee.id() != null && ENABLED == employee.status())
                .map(UserAttendanceClient.EmployeeSummary::id)
                .toList();
    }

    public List<Long> findActiveEmployeeIdsByDepartment(Long departmentId) {
        List<UserAttendanceClient.EmployeeSummary> employees = requireData(client.findEmployees());
        return employees.stream()
                .filter(employee -> employee.id() != null && ENABLED == employee.status())
                .filter(employee -> departmentId.equals(employee.departmentId()))
                .map(UserAttendanceClient.EmployeeSummary::id)
                .toList();
    }

    private <T> T requireData(ApiResponse<T> response) {
        if (response == null || response.code() != 0 || response.data() == null) {
            throw new IllegalStateException("用户服务返回错误");
        }
        return response.data();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
