package com.tsy.oa.notice.dto;

import java.util.List;

public record NoticeSearchSourcePageResponse(
        List<NoticeSearchSourceResponse> items,
        long total,
        int page,
        int pageSize,
        boolean hasNext
) {
}
