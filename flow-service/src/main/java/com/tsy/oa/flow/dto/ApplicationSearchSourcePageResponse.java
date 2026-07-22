package com.tsy.oa.flow.dto;

import java.util.List;

public record ApplicationSearchSourcePageResponse(
        List<ApplicationSearchSourceResponse> items,
        long total,
        int page,
        int pageSize,
        boolean hasNext
) {
}
