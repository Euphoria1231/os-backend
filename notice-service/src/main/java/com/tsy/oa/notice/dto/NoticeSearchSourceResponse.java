package com.tsy.oa.notice.dto;

import com.tsy.oa.notice.model.Notice;

import java.time.LocalDateTime;

public record NoticeSearchSourceResponse(
        Long id,
        String title,
        String content,
        Long publisherId,
        String status,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoticeSearchSourceResponse from(Notice notice) {
        return new NoticeSearchSourceResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getPublisherId(),
                notice.getStatus(),
                notice.getPublishedAt(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
