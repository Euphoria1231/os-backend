package com.tsy.oa.flow.employee;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/internal/user/employees")
public interface UserServiceClient {

    @GetMapping("/{id}/approval-route")
    ApiResponse<ApprovalRouteResponse> getApprovalRoute(@PathVariable Long id);

    record ApprovalRouteResponse(
            Long applicantId,
            Long directLeaderId,
            String directLeaderName,
            Long departmentLeaderId,
            String departmentLeaderName
    ) {
    }
}
