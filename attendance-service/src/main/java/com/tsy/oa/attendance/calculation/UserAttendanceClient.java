package com.tsy.oa.attendance.calculation;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user-service", contextId = "attendanceUserClient", path = "/api/user/employees")
public interface UserAttendanceClient {

    @GetMapping("/{employeeId}/permissions")
    ApiResponse<AuthorizationSummary> findAuthorization(@PathVariable Long employeeId);

    @GetMapping
    ApiResponse<List<EmployeeSummary>> findEmployees();

    record AuthorizationSummary(
            List<RoleSummary> roles,
            List<ApiPermissionSummary> apiPermissions
    ) {
    }

    record RoleSummary(String code, Integer status) {
    }

    record ApiPermissionSummary(String authority, Integer status) {
    }

    record EmployeeSummary(Long id, Long departmentId, Integer status) {

        public EmployeeSummary(Long id, Integer status) {
            this(id, null, status);
        }
    }
}
