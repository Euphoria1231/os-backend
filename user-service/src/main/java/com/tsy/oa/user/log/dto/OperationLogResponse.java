package com.tsy.oa.user.log.dto;

import com.tsy.oa.user.log.model.BusinessOperationLog;

import java.time.LocalDateTime;

public record OperationLogResponse(
        Long id,
        Long operatorId,
        String operatorName,
        String serviceName,
        String businessModule,
        String operationType,
        String targetType,
        String targetId,
        String summary,
        String operationStatus,
        String requestPath,
        String httpMethod,
        String clientIp,
        String errorMessage,
        LocalDateTime operatedAt
) {
    public static OperationLogResponse from(BusinessOperationLog log) {
        return new OperationLogResponse(
                log.getId(), log.getOperatorId(), log.getOperatorName(), log.getServiceName(),
                log.getBusinessModule(), log.getOperationType(), log.getTargetType(),
                log.getTargetId(), log.getSummary(), log.getOperationStatus(),
                log.getRequestPath(), log.getHttpMethod(), log.getClientIp(),
                log.getErrorMessage(), log.getOperatedAt()
        );
    }
}
