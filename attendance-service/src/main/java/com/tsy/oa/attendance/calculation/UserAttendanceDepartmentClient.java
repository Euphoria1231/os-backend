package com.tsy.oa.attendance.calculation;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", contextId = "attendanceDepartmentClient", path = "/api/user/departments")
public interface UserAttendanceDepartmentClient {

    @GetMapping("/{departmentId}")
    ApiResponse<DepartmentSummary> findDepartment(@PathVariable Long departmentId);

    record DepartmentSummary(Long id, Long leaderEmployeeId, Integer status) {
    }
}
