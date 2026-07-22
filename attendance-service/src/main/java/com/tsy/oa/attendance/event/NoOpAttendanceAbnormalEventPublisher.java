package com.tsy.oa.attendance.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NoOpAttendanceAbnormalEventPublisher implements AttendanceAbnormalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(
            NoOpAttendanceAbnormalEventPublisher.class
    );

    @Override
    public void publish(AttendanceAbnormalEvent event) {
        log.debug(
                "RocketMQ template is unavailable, skip attendance abnormal event publishing, eventId={}",
                event.eventId()
        );
    }
}
