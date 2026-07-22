package com.tsy.oa.notice.dto;

import com.tsy.oa.notice.model.Notice;

import java.time.LocalDateTime;

public record NoticeSearchSourceResponse(
        Long id,
        String title,
        String content,
        String status,
        LocalDateTime publishedAt
) {
    public static NoticeSearchSourceResponse from(Notice notice) {
        return new NoticeSearchSourceResponse(
                notice.getId(), notice.getTitle(), notice.getContent(),
                notice.getStatus(), notice.getPublishedAt()
        );
    }
}
