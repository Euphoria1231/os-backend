package com.tsy.oa.user.department.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.log.BusinessOperationLogger;
import com.tsy.oa.common.log.HttpOperationLogContexts;
import com.tsy.oa.common.log.OperationLogContext;
import com.tsy.oa.user.department.dto.DepartmentRequest;
import com.tsy.oa.user.department.dto.DepartmentResponse;
import com.tsy.oa.user.department.service.DepartmentService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/departments")
public class DepartmentController {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    private final DepartmentService departmentService;
    private final BusinessOperationLogger operationLogger;

    public DepartmentController(
            DepartmentService departmentService,
            BusinessOperationLogger operationLogger
    ) {
        this.departmentService = departmentService;
        this.operationLogger = operationLogger;
    }

    @PostMapping
    public ApiResponse<DepartmentResponse> create(
            @Valid @RequestBody DepartmentRequest request,
            @RequestHeader(value = EMPLOYEE_HEADER, required = false) Long operatorId,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, operatorId, "CREATE_DEPARTMENT", null,
                "创建部门：" + request.name()
        );
        DepartmentResponse response = operationLogger.execute(
                context,
                () -> departmentService.create(request),
                result -> result.id().toString()
        );
        return ApiResponse.success(response);
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
            @Valid @RequestBody DepartmentRequest request,
            @RequestHeader(value = EMPLOYEE_HEADER, required = false) Long operatorId,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, operatorId, "UPDATE_DEPARTMENT", id,
                "修改部门：" + request.name()
        );
        return ApiResponse.success(operationLogger.execute(
                context,
                () -> departmentService.update(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = EMPLOYEE_HEADER, required = false) Long operatorId,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, operatorId, "DELETE_DEPARTMENT", id, "删除部门"
        );
        operationLogger.execute(context, () -> departmentService.delete(id));
        return ApiResponse.success(null);
    }

    private OperationLogContext logContext(
            HttpServletRequest request,
            Long operatorId,
            String operationType,
            Long targetId,
            String summary
    ) {
        return HttpOperationLogContexts.create(
                request,
                operatorId,
                null,
                "ORGANIZATION",
                operationType,
                "DEPARTMENT",
                targetId == null ? null : targetId.toString(),
                summary
        );
    }
}
