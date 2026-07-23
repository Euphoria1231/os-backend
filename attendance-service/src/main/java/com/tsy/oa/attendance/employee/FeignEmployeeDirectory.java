package com.tsy.oa.attendance.employee;

import com.tsy.oa.attendance.employee.UserServiceClient.EmployeeSummary;
import com.tsy.oa.common.api.ApiResponse;
import org.springframework.stereotype.Component;

@Component
public class FeignEmployeeDirectory implements EmployeeDirectory {

    private final UserServiceClient userServiceClient;

    public FeignEmployeeDirectory(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    @Override
    public Long findDirectLeaderId(Long employeeId) {
        ApiResponse<EmployeeSummary> response = userServiceClient.getEmployee(employeeId);
        return response == null || response.data() == null ? null : response.data().leaderId();
    }
}
