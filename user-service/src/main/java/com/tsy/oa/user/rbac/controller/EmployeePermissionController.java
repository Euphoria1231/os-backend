package com.tsy.oa.user.rbac.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.log.BusinessOperationLogger;
import com.tsy.oa.common.log.HttpOperationLogContexts;
import com.tsy.oa.common.log.OperationLogContext;
import com.tsy.oa.user.rbac.dto.EmployeeAuthorizationResponse;
import com.tsy.oa.user.rbac.dto.EmployeeRoleRequest;
import com.tsy.oa.user.rbac.service.RbacService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/employees")
public class EmployeePermissionController {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    private final RbacService rbacService;
    private final BusinessOperationLogger operationLogger;

    public EmployeePermissionController(
            RbacService rbacService,
            BusinessOperationLogger operationLogger
    ) {
        this.rbacService = rbacService;
        this.operationLogger = operationLogger;
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRoleRequest request,
            @RequestHeader(value = EMPLOYEE_HEADER, required = false) Long operatorId,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = HttpOperationLogContexts.create(
                httpRequest,
                operatorId,
                null,
                "RBAC",
                "ASSIGN_EMPLOYEE_ROLES",
                "EMPLOYEE",
                id.toString(),
                "分配员工角色，角色数量：" + request.roleIds().size()
        );
        operationLogger.execute(context, () -> rbacService.assignEmployeeRoles(id, request));
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/permissions")
    public ApiResponse<EmployeeAuthorizationResponse> getAuthorization(@PathVariable Long id) {
        return ApiResponse.success(rbacService.getEmployeeAuthorization(id));
    }
}
