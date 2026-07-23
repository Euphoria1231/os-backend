package com.tsy.oa.common.log;

public record OperationLogCommand(
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
        String errorMessage
) {
}
