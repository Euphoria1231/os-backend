package com.tsy.oa.notice.dto;

import java.util.List;

public record PersonalNotificationPageResponse(
        List<PersonalNotificationResponse> items,
        long total,
        int page,
        int pageSize
) {
}
