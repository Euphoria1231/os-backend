package com.tsy.oa.notice.event;

import com.tsy.oa.common.notification.PersonalNotificationEvent;

@FunctionalInterface
public interface PersonalNotificationEventHandler {

    boolean handle(PersonalNotificationEvent event);
}
