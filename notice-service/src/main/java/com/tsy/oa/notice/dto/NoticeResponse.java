package com.tsy.oa.notice.dto;

import com.tsy.oa.notice.model.Notice;
import com.tsy.oa.notice.model.NoticeView;

import java.time.LocalDateTime;

public record NoticeResponse(
        Long id,
        String title,
        String content,
        Long publisherId,
        String status,
        LocalDateTime publishedAt,
        boolean read,
        LocalDateTime readAt
) {
    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getId(), notice.getTitle(), notice.getContent(), notice.getPublisherId(),
                notice.getStatus(), notice.getPublishedAt(), false, null
        );
    }

    public static NoticeResponse from(NoticeView notice) {
        return new NoticeResponse(
                notice.getId(), notice.getTitle(), notice.getContent(), notice.getPublisherId(),
                notice.getStatus(), notice.getPublishedAt(), notice.isRead(), notice.getReadAt()
        );
    }
}
