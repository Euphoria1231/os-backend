package com.tsy.oa.notice.model;

import java.time.LocalDateTime;

public class PersonalNotification {

    private Long id;
    private Long recipientEmployeeId;
    private String notificationType;
    private String title;
    private String content;
    private String relatedBusinessType;
    private Long relatedBusinessId;
    private String eventId;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRecipientEmployeeId() { return recipientEmployeeId; }
    public void setRecipientEmployeeId(Long recipientEmployeeId) { this.recipientEmployeeId = recipientEmployeeId; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getRelatedBusinessType() { return relatedBusinessType; }
    public void setRelatedBusinessType(String relatedBusinessType) { this.relatedBusinessType = relatedBusinessType; }
    public Long getRelatedBusinessId() { return relatedBusinessId; }
    public void setRelatedBusinessId(Long relatedBusinessId) { this.relatedBusinessId = relatedBusinessId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
