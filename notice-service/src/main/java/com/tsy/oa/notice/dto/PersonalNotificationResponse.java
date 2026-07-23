package com.tsy.oa.notice.dto;

import com.tsy.oa.notice.model.PersonalNotification;

import java.time.LocalDateTime;

public record PersonalNotificationResponse(
        Long id,
        String eventId,
        String notificationType,
        String title,
        String content,
        String relatedBusinessType,
        Long relatedBusinessId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {

    public static PersonalNotificationResponse from(PersonalNotification notification) {
        return new PersonalNotificationResponse(
                notification.getId(),
                notification.getEventId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getRelatedBusinessType(),
                notification.getRelatedBusinessId(),
                notification.getReadAt() != null,
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
