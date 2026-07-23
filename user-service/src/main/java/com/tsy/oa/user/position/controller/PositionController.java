package com.tsy.oa.user.position.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.log.BusinessOperationLogger;
import com.tsy.oa.common.log.HttpOperationLogContexts;
import com.tsy.oa.common.log.OperationLogContext;
import com.tsy.oa.user.position.dto.PositionRequest;
import com.tsy.oa.user.position.dto.PositionResponse;
import com.tsy.oa.user.position.service.PositionService;
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
@RequestMapping("/api/user/positions")
public class PositionController {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    private final PositionService positionService;
    private final BusinessOperationLogger operationLogger;

    public PositionController(
            PositionService positionService,
            BusinessOperationLogger operationLogger
    ) {
        this.positionService = positionService;
        this.operationLogger = operationLogger;
    }

    @PostMapping
    public ApiResponse<PositionResponse> create(
            @Valid @RequestBody PositionRequest request,
            @RequestHeader(value = EMPLOYEE_HEADER, required = false) Long operatorId,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, operatorId, "CREATE_POSITION", null,
                "创建岗位：" + request.code()
        );
        PositionResponse response = operationLogger.execute(
                context,
                () -> positionService.create(request),
                result -> result.id().toString()
        );
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<PositionResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(positionService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<PositionResponse>> list() {
        return ApiResponse.success(positionService.list());
    }

    @PutMapping("/{id}")
    public ApiResponse<PositionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PositionRequest request,
            @RequestHeader(value = EMPLOYEE_HEADER, required = false) Long operatorId,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, operatorId, "UPDATE_POSITION", id,
                "修改岗位：" + request.code()
        );
        return ApiResponse.success(operationLogger.execute(
                context,
                () -> positionService.update(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = EMPLOYEE_HEADER, required = false) Long operatorId,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, operatorId, "DELETE_POSITION", id, "删除岗位"
        );
        operationLogger.execute(context, () -> positionService.delete(id));
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
                "POSITION", targetId == null ? null : targetId.toString(), summary
        );
    }
}
