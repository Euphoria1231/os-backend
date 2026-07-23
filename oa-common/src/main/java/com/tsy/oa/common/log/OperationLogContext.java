package com.tsy.oa.common.log;

public record OperationLogContext(
        Long operatorId,
        String operatorName,
        String businessModule,
        String operationType,
        String targetType,
        String targetId,
        String summary,
        String requestPath,
        String httpMethod,
        String clientIp
) {
    public OperationLogContext withTargetId(String resolvedTargetId) {
        return new OperationLogContext(
                operatorId, operatorName, businessModule, operationType, targetType,
                resolvedTargetId, summary, requestPath, httpMethod, clientIp
        );
    }
}
