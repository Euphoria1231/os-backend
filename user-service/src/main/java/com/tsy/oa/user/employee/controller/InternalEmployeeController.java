package com.tsy.oa.user.employee.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.user.employee.dto.ApprovalRouteResponse;
import com.tsy.oa.user.employee.service.EmployeeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/user/employees")
public class InternalEmployeeController {

    private final EmployeeService employeeService;

    public InternalEmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/{id}/approval-route")
    public ApiResponse<ApprovalRouteResponse> getApprovalRoute(@PathVariable Long id) {
        return ApiResponse.success(employeeService.getApprovalRoute(id));
    }
}
