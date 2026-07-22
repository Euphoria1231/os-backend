package com.tsy.oa.intelligence.search.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record NoticeSearchDocument(
        long noticeId,
        String title,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime publishedAt,
        String status
) {
}
