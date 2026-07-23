package com.tsy.oa.flow.notification;

import com.tsy.oa.common.notification.PersonalNotificationEvent;

@FunctionalInterface
public interface PersonalNotificationPublisher {

    void publish(PersonalNotificationEvent event);
}
