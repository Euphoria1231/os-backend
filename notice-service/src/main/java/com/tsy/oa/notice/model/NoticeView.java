package com.tsy.oa.notice.model;

import java.time.LocalDateTime;

public class NoticeView extends Notice {

    private boolean read;
    private LocalDateTime readAt;

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
