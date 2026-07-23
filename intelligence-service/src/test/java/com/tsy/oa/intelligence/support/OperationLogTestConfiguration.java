package com.tsy.oa.intelligence.support;

import com.tsy.oa.common.log.BusinessOperationLogger;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class OperationLogTestConfiguration {

    @Bean
    public RecordedOperationLogs recordedOperationLogs() {
        return new RecordedOperationLogs();
    }

    @Bean
    @Primary
    public BusinessOperationLogger testBusinessOperationLogger(RecordedOperationLogs logs) {
        return new BusinessOperationLogger(
                "intelligence-service", logs::add, (command, exception) -> {
                }
        );
    }
}
