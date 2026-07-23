package com.tsy.oa.user.employee.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.log.BusinessOperationLogger;
import com.tsy.oa.common.log.HttpOperationLogContexts;
import com.tsy.oa.common.log.OperationLogContext;
import com.tsy.oa.user.employee.dto.EmployeeCreateRequest;
import com.tsy.oa.user.employee.dto.EmployeeResponse;
import com.tsy.oa.user.employee.dto.EmployeeUpdateRequest;
import com.tsy.oa.user.employee.service.EmployeeService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
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
    private final BusinessOperationLogger operationLogger;

    public EmployeeController(
            EmployeeService employeeService,
            BusinessOperationLogger operationLogger
    ) {
        this.employeeService = employeeService;
        this.operationLogger = operationLogger;
    }

    @PostMapping
    public ApiResponse<EmployeeResponse> create(
            @Valid @RequestBody EmployeeCreateRequest request,
            @RequestHeader(value = EMPLOYEE_HEADER, required = false) Long operatorId,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, operatorId, "CREATE_EMPLOYEE", null,
                "创建员工：" + request.employeeNo() + "/" + request.username()
        );
        EmployeeResponse response = operationLogger.execute(
                context,
                () -> employeeService.create(request),
                result -> result.id().toString()
        );
        return ApiResponse.success(response);
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
            @Valid @RequestBody EmployeeUpdateRequest request,
            @RequestHeader(value = EMPLOYEE_HEADER, required = false) Long operatorId,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, operatorId, "UPDATE_EMPLOYEE", id, "修改员工资料"
        );
        return ApiResponse.success(operationLogger.execute(
                context,
                () -> employeeService.update(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = EMPLOYEE_HEADER, required = false) Long operatorId,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, operatorId, "DELETE_EMPLOYEE", id, "删除员工"
        );
        operationLogger.execute(context, () -> employeeService.delete(id));
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
                request, operatorId, null, "ORGANIZATION", operationType,
                "EMPLOYEE", targetId == null ? null : targetId.toString(), summary
        );
    }
}
