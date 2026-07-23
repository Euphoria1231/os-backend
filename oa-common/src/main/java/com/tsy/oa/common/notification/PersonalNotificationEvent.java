package com.tsy.oa.common.notification;

import java.time.LocalDateTime;

public record PersonalNotificationEvent(
        String eventId,
        long recipientEmployeeId,
        NotificationType notificationType,
        String title,
        String content,
        RelatedBusinessType relatedBusinessType,
        long relatedBusinessId,
        LocalDateTime occurredAt
) {

    private static final int MAX_EVENT_ID_LENGTH = 64;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 1000;

    public PersonalNotificationEvent {
        eventId = requireText(eventId, MAX_EVENT_ID_LENGTH, "eventId");
        if (recipientEmployeeId <= 0) {
            throw new IllegalArgumentException("recipientEmployeeId must be positive");
        }
        if (notificationType == null) {
            throw new IllegalArgumentException("notificationType must not be null");
        }
        title = requireText(title, MAX_TITLE_LENGTH, "title");
        content = requireText(content, MAX_CONTENT_LENGTH, "content");
        if (relatedBusinessType == null) {
            throw new IllegalArgumentException("relatedBusinessType must not be null");
        }
        if (relatedBusinessId <= 0) {
            throw new IllegalArgumentException("relatedBusinessId must be positive");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
    }

    private static String requireText(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must contain between 1 and " + maxLength + " characters"
            );
        }
        return value.trim();
    }

    public enum NotificationType {
        APPROVAL_TASK,
        APPLICATION_REJECTED,
        APPLICATION_APPROVED,
        ATTENDANCE_ABNORMAL
    }

    public enum RelatedBusinessType {
        FLOW_APPLICATION,
        ATTENDANCE_RECORD
    }
}
