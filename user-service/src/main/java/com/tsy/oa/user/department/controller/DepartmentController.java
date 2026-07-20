package com.tsy.oa.user.department.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.user.department.dto.DepartmentRequest;
import com.tsy.oa.user.department.dto.DepartmentResponse;
import com.tsy.oa.user.department.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    public ApiResponse<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.success(departmentService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<DepartmentResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<DepartmentResponse>> list() {
        return ApiResponse.success(departmentService.list());
    }

    @PutMapping("/{id}")
    public ApiResponse<DepartmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request
    ) {
        return ApiResponse.success(departmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.success(null);
    }
}
