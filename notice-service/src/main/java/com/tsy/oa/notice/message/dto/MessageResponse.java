package com.tsy.oa.notice.message.dto;

import com.tsy.oa.notice.message.model.Message;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        String title,
        String content,
        String businessType,
        Long businessId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getTitle(),
                message.getContent(),
                message.getBusinessType(),
                message.getBusinessId(),
                message.getReadAt() != null,
                message.getReadAt(),
                message.getCreatedAt()
        );
    }
}
