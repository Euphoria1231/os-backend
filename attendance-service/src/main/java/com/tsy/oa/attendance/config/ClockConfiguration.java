package com.tsy.oa.attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfiguration {

    @Bean
    public Clock attendanceClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }
}
