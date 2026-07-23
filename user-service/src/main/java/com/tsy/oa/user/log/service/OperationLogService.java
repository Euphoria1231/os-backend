package com.tsy.oa.user.log.service;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.user.employee.mapper.EmployeeMapper;
import com.tsy.oa.user.employee.model.Employee;
import com.tsy.oa.user.log.dto.OperationLogAppendRequest;
import com.tsy.oa.user.log.dto.OperationLogPageResponse;
import com.tsy.oa.user.log.dto.OperationLogResponse;
import com.tsy.oa.user.log.mapper.OperationLogMapper;
import com.tsy.oa.user.log.model.BusinessOperationLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class OperationLogService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final OperationLogMapper operationLogMapper;
    private final EmployeeMapper employeeMapper;
    private final SensitiveLogTextSanitizer sanitizer;

    public OperationLogService(
            OperationLogMapper operationLogMapper,
            EmployeeMapper employeeMapper,
            SensitiveLogTextSanitizer sanitizer
    ) {
        this.operationLogMapper = operationLogMapper;
        this.employeeMapper = employeeMapper;
        this.sanitizer = sanitizer;
    }

    @Transactional
    public OperationLogResponse append(OperationLogAppendRequest request) {
        BusinessOperationLog operationLog = new BusinessOperationLog();
        operationLog.setOperatorId(request.operatorId());
        operationLog.setOperatorName(resolveOperatorName(request));
        operationLog.setServiceName(normalize(request.serviceName(), 50));
        operationLog.setBusinessModule(normalizeCode(request.businessModule(), 50));
        operationLog.setOperationType(normalizeCode(request.operationType(), 50));
        operationLog.setTargetType(normalizeOptionalCode(request.targetType(), 50));
        operationLog.setTargetId(sanitizer.sanitize(request.targetId(), 100));
        operationLog.setSummary(sanitizer.sanitize(request.summary(), 500));
        operationLog.setOperationStatus(request.operationStatus());
        operationLog.setRequestPath(normalizeRequestPath(request.requestPath()));
        operationLog.setHttpMethod(normalizeCode(request.httpMethod(), 10));
        operationLog.setClientIp(sanitizer.sanitize(request.clientIp(), 64));
        operationLog.setErrorMessage(sanitizer.sanitize(request.errorMessage(), 500));
        operationLogMapper.insert(operationLog);
        return OperationLogResponse.from(operationLogMapper.findById(operationLog.getId()));
    }

    @Transactional(readOnly = true)
    public OperationLogPageResponse listMine(
            Long operatorId,
            String businessModule,
            String operationStatus,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int page,
            int pageSize
    ) {
        return query(
                operatorId, null, businessModule, operationStatus,
                startTime, endTime, page, pageSize
        );
    }

    @Transactional(readOnly = true)
    public OperationLogPageResponse listAll(
            List<String> roles,
            String operatorKeyword,
            String businessModule,
            String operationStatus,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int page,
            int pageSize
    ) {
        if (roles == null || roles.stream().noneMatch(SUPER_ADMIN::equals)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        return query(
                null, normalizeFilter(operatorKeyword), businessModule, operationStatus,
                startTime, endTime, page, pageSize
        );
    }

    private OperationLogPageResponse query(
            Long operatorId,
            String operatorKeyword,
            String businessModule,
            String operationStatus,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int page,
            int pageSize
    ) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        String normalizedModule = normalizeOptionalCode(businessModule, 50);
        String normalizedStatus = normalizeOptionalCode(operationStatus, 20);
        int offset = checkedOffset(page, pageSize);
        long total = operationLogMapper.count(
                operatorId, operatorKeyword, normalizedModule, normalizedStatus, startTime, endTime
        );
        List<OperationLogResponse> items = operationLogMapper.findPage(
                        operatorId, operatorKeyword, normalizedModule, normalizedStatus,
                        startTime, endTime, offset, pageSize
                ).stream()
                .map(OperationLogResponse::from)
                .toList();
        return new OperationLogPageResponse(items, total, page, pageSize);
    }

    private String resolveOperatorName(OperationLogAppendRequest request) {
        if (request.operatorId() != null) {
            Employee employee = employeeMapper.findById(request.operatorId());
            return employee == null
                    ? "员工#" + request.operatorId()
                    : sanitizer.sanitize(employee.getRealName(), 100);
        }
        String fallback = sanitizer.sanitize(request.operatorName(), 100);
        return fallback == null || fallback.isBlank() ? "未知用户" : fallback;
    }

    private String normalizeRequestPath(String requestPath) {
        String path = requestPath.trim();
        int queryStart = path.indexOf('?');
        if (queryStart >= 0) {
            path = path.substring(0, queryStart);
        }
        return sanitizer.sanitize(path, 255);
    }

    private String normalize(String value, int maxLength) {
        return sanitizer.sanitize(value, maxLength);
    }

    private String normalizeCode(String value, int maxLength) {
        return normalize(value, maxLength).toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalCode(String value, int maxLength) {
        String normalized = normalizeFilter(value);
        return normalized == null ? null : normalizeCode(normalized, maxLength);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int checkedOffset(int page, int pageSize) {
        long offset = Math.multiplyExact((long) page - 1L, pageSize);
        if (offset > Integer.MAX_VALUE) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        return (int) offset;
    }
}
