package com.tsy.oa.user.employee.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.user.employee.dto.EmployeeCreateRequest;
import com.tsy.oa.user.employee.dto.EmployeeResponse;
import com.tsy.oa.user.employee.dto.EmployeeUpdateRequest;
import com.tsy.oa.user.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/employees")
public class EmployeeController {
    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    public ApiResponse<EmployeeResponse> create(@Valid @RequestBody EmployeeCreateRequest request) {
        return ApiResponse.success(employeeService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<EmployeeResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(employeeService.getById(id));
    }

    @GetMapping("/direct-reports")
    public ApiResponse<List<EmployeeResponse>> listDirectReports(
            @RequestHeader(EMPLOYEE_HEADER) Long leaderId
    ) {
        return ApiResponse.success(employeeService.listDirectReports(leaderId));
    }

    @GetMapping
    public ApiResponse<List<EmployeeResponse>> list() {
        return ApiResponse.success(employeeService.list());
    }

    @PutMapping("/{id}")
    public ApiResponse<EmployeeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request
    ) {
        return ApiResponse.success(employeeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ApiResponse.success(null);
    }
}
