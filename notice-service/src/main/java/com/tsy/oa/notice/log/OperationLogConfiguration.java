package com.tsy.oa.notice.log;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.log.BusinessOperationLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OperationLogConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationLogConfiguration.class);

    @Bean
    public BusinessOperationLogger noticeBusinessOperationLogger(OperationLogClient client) {
        return new BusinessOperationLogger(
                "notice-service",
                command -> requireSuccess(client.append(command)),
                (command, exception) -> LOGGER.error(
                        "业务操作日志写入失败，模块={}，操作={}",
                        command.businessModule(),
                        command.operationType(),
                        exception
                )
        );
    }

    private void requireSuccess(ApiResponse<Void> response) {
        if (response == null || response.code() != 0) {
            throw new IllegalStateException("user-service rejected operation log");
        }
    }
}
