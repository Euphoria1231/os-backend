package com.tsy.oa.attendance.event;

public interface AttendanceAbnormalEventPublisher {

    void publish(AttendanceAbnormalEvent event);
}
