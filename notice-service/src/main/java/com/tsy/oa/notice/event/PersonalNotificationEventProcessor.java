package com.tsy.oa.notice.event;

import com.tsy.oa.common.notification.PersonalNotificationEvent;
import com.tsy.oa.notice.mapper.PersonalNotificationMapper;
import com.tsy.oa.notice.model.PersonalNotification;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalNotificationEventProcessor implements PersonalNotificationEventHandler {

    private final PersonalNotificationMapper notificationMapper;

    public PersonalNotificationEventProcessor(PersonalNotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    @Transactional
    public boolean handle(PersonalNotificationEvent event) {
        PersonalNotification notification = new PersonalNotification();
        notification.setRecipientEmployeeId(event.recipientEmployeeId());
        notification.setNotificationType(event.notificationType().name());
        notification.setTitle(event.title());
        notification.setContent(event.content());
        notification.setRelatedBusinessType(event.relatedBusinessType().name());
        notification.setRelatedBusinessId(event.relatedBusinessId());
        notification.setEventId(event.eventId());
        notification.setCreatedAt(event.occurredAt());
        try {
            return notificationMapper.insert(notification) == 1;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }
}
