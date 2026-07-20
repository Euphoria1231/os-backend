package com.tsy.oa.flow.employee;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/api/user/employees")
public interface UserServiceClient {

    @GetMapping("/{id}")
    ApiResponse<EmployeeSummary> getEmployee(@PathVariable Long id);

    record EmployeeSummary(Long id, Long leaderId) {
    }
}
