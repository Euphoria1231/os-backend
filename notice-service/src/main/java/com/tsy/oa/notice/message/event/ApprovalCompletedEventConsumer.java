package com.tsy.oa.notice.message.event;

import com.tsy.oa.notice.message.service.MessageService;
import org.springframework.stereotype.Component;

@Component
public class ApprovalCompletedEventConsumer {

    private final MessageService messageService;

    public ApprovalCompletedEventConsumer(MessageService messageService) {
        this.messageService = messageService;
    }

    public void consume(ApprovalCompletedEvent event) {
        messageService.createApprovalMessage(event);
    }
}
