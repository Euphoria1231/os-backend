package com.tsy.oa.common.log;

import jakarta.servlet.http.HttpServletRequest;

public final class HttpOperationLogContexts {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private HttpOperationLogContexts() {
    }

    public static OperationLogContext create(
            HttpServletRequest request,
            Long operatorId,
            String operatorName,
            String businessModule,
            String operationType,
            String targetType,
            String targetId,
            String summary
    ) {
        return new OperationLogContext(
                operatorId,
                operatorName,
                businessModule,
                operationType,
                targetType,
                targetId,
                summary,
                request.getRequestURI(),
                request.getMethod(),
                resolveClientIp(request)
        );
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }
        int separator = forwardedFor.indexOf(',');
        return (separator < 0 ? forwardedFor : forwardedFor.substring(0, separator)).trim();
    }
}
