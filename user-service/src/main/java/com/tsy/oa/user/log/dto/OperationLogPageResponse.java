package com.tsy.oa.user.log.dto;

import java.util.List;

public record OperationLogPageResponse(
        List<OperationLogResponse> items,
        long total,
        int page,
        int pageSize
) {
}
