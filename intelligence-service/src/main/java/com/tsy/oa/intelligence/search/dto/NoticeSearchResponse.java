package com.tsy.oa.intelligence.search.dto;

import java.time.LocalDateTime;

public record NoticeSearchResponse(
        long noticeId,
        String title,
        String content,
        String titleHighlight,
        String contentHighlight,
        LocalDateTime publishedAt
) {
}
