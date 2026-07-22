package com.tsy.oa.notice.message.service;

import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.notice.error.NoticeErrorCode;
import com.tsy.oa.notice.message.dto.MessageResponse;
import com.tsy.oa.notice.message.event.ApprovalCompletedEvent;
import com.tsy.oa.notice.message.mapper.MessageMapper;
import com.tsy.oa.notice.message.model.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageService {

    private static final String APPROVAL_EVENTS_TOPIC = "oa-approval-events";
    private static final String BUSINESS_TYPE_APPROVAL = "APPROVAL";
    private static final int MAX_PAGE_SIZE = 100;

    private final MessageMapper messageMapper;

    public MessageService(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Transactional
    public void createApprovalMessage(ApprovalCompletedEvent event) {
        if (messageMapper.insertConsumeRecordIfAbsent(event.eventId(), APPROVAL_EVENTS_TOPIC) == 0) {
            return;
        }
        Message message = new Message();
        message.setRecipientId(event.applicantId());
        message.setTitle("Approval result notice");
        message.setContent(buildApprovalContent(event));
        message.setBusinessType(BUSINESS_TYPE_APPROVAL);
        message.setBusinessId(event.applicationId());
        message.setSourceEventId(event.eventId());
        messageMapper.insertMessage(message);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(Long recipientId, Boolean read, int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int offset = (normalizedPage - 1) * normalizedPageSize;
        return messageMapper.findMessagesForRecipient(recipientId, read, offset, normalizedPageSize).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MessageResponse getMessage(Long id, Long recipientId) {
        return MessageResponse.from(requireMessage(id, recipientId));
    }

    @Transactional
    public void markRead(Long id, Long recipientId) {
        requireMessage(id, recipientId);
        messageMapper.markRead(id, recipientId);
    }

    @Transactional
    public void markAllRead(Long recipientId) {
        messageMapper.markAllRead(recipientId);
    }

    @Transactional(readOnly = true)
    public int countUnread(Long recipientId) {
        return messageMapper.countUnread(recipientId);
    }

    private Message requireMessage(Long id, Long recipientId) {
        Message message = messageMapper.findMessageForRecipient(id, recipientId);
        if (message == null) {
            throw new BusinessException(NoticeErrorCode.MESSAGE_NOT_FOUND);
        }
        return message;
    }

    private String buildApprovalContent(ApprovalCompletedEvent event) {
        return event.applicationType() + " application " + event.applicationId() + " was " + event.result();
    }
}
