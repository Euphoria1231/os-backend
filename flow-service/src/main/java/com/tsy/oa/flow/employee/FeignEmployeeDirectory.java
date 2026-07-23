package com.tsy.oa.flow.employee;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.flow.employee.UserServiceClient.ApprovalRouteResponse;
import org.springframework.stereotype.Component;

@Component
public class FeignEmployeeDirectory implements EmployeeDirectory {

    private final UserServiceClient userServiceClient;

    public FeignEmployeeDirectory(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    @Override
    public ApprovalRoute findApprovalRoute(Long employeeId) {
        ApiResponse<ApprovalRouteResponse> response = userServiceClient.getApprovalRoute(employeeId);
        ApprovalRouteResponse route = response == null ? null : response.data();
        if (route == null) {
            return null;
        }
        return new ApprovalRoute(
                route.applicantId(), route.directLeaderId(), route.directLeaderName(),
                route.departmentLeaderId(), route.departmentLeaderName()
        );
    }
}
